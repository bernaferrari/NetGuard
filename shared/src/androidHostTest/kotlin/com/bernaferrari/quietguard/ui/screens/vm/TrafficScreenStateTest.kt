package com.bernaferrari.quietguard.ui.screens.vm

import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.platform.observeOnChanges
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import com.bernaferrari.quietguard.ui.util.asUiAsyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficScreenStateTest {
    @Test
    fun asyncStateExposesFailureAndRetriesOnRequest() =
        runBlocking {
            var attempts = 0
            val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
            val sharingScope = CoroutineScope(coroutineContext + SupervisorJob())
            val state = flow {
                attempts += 1
                if (attempts == 1) error("first load failed")
                emit(9)
            }.asUiAsyncState(
                scope = sharingScope,
                initialData = 0,
                retryRequests = retryRequests,
            )

            val collector = launch { state.collect() }
            state.first { it.hasFailed }
            retryRequests.emit(Unit)

            assertEquals(9, state.first { it.isReady }.data)
            collector.cancelAndJoin()
            sharingScope.cancel()
        }

    @Test
    fun asyncStateKeepsLastDataWhenCollectionRestarts() =
        runBlocking {
            val source = MutableSharedFlow<Int>()
            val sharingScope = CoroutineScope(coroutineContext + SupervisorJob())
            val state = source.asUiAsyncState(
                scope = sharingScope,
                initialData = 0,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
            )

            val firstCollector = launch { state.collect() }
            source.subscriptionCount.first { it > 0 }
            source.emit(7)
            state.first { it.hasReceived }
            firstCollector.cancelAndJoin()
            source.subscriptionCount.first { it == 0 }

            val secondCollector = launch { state.collect() }
            source.subscriptionCount.first { it > 0 }

            assertEquals(
                UiAsyncState(data = 7, isLoading = true, hasReceived = true),
                state.value,
            )
            secondCollector.cancelAndJoin()
            sharingScope.cancel()
        }

    @Test
    fun dnsFiltersSeparateActiveAndExpiredRecords() {
        val active = dnsEntry(time = 8_000, ttl = 5_000)
        val expired = dnsEntry(time = 1_000, ttl = 5_000)
        val entries = UiAsyncState(data = listOf(active, expired), hasReceived = true)

        val activeState = DnsScreenState(entries = entries, filter = DnsListFilter.Active, nowMs = 10_000)
        val expiredState = DnsScreenState(entries = entries, filter = DnsListFilter.Expired, nowMs = 10_000)

        assertEquals(listOf(active), activeState.filtered)
        assertEquals(listOf(expired), expiredState.filtered)
        assertEquals(1, activeState.expiredCount)
    }

    @Test
    fun forwardingFiltersUseProtocolNumbers() {
        val udp = ForwardingEntry(protocol = 17, dport = 53, raddr = "1.1.1.1", rport = 53, ruid = 0)
        val tcp = ForwardingEntry(protocol = 6, dport = 443, raddr = "1.1.1.1", rport = 443, ruid = 0)
        val entries = UiAsyncState(data = listOf(udp, tcp), hasReceived = true)

        assertEquals(
            listOf(udp),
            ForwardingScreenState(entries = entries, protocolFilter = ForwardingListFilter.Udp).filtered,
        )
        assertEquals(
            listOf(tcp),
            ForwardingScreenState(entries = entries, protocolFilter = ForwardingListFilter.Tcp).filtered,
        )
    }

    @Test
    fun observerReloadsOnItsOwnChangeEventAndUnregisters() =
        runBlocking {
            var value = 0
            val listeners = mutableSetOf<() -> Unit>()
            val emissions = mutableListOf<Int>()

            val job = launch {
                observeOnChanges(
                    register = { listener ->
                        listeners += listener
                        val unregister: () -> Unit = { listeners -= listener }
                        unregister
                    },
                ) { value }
                    .take(2)
                    .toList(emissions)
            }

            withTimeout(2_000) { while (listeners.isEmpty()) kotlinx.coroutines.yield() }
            withTimeout(2_000) { while (emissions.isEmpty()) kotlinx.coroutines.yield() }
            value = 1
            listeners.single().invoke()
            job.join()

            assertEquals(listOf(0, 1), emissions)
            assertEquals(emptySet<() -> Unit>(), listeners)
        }

    private fun dnsEntry(time: Long, ttl: Int) =
        DnsEntry(time = time, qname = "example.com", aname = "example.com", resource = "1.1.1.1", ttl = ttl, uid = 1000)
}
