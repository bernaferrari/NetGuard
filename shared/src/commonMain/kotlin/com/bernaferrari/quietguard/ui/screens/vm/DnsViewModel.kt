package com.bernaferrari.quietguard.ui.screens.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.quietguard.data.TrafficRepository
import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.currentTimeMillis
import com.bernaferrari.quietguard.platform.exportDnsToFile
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import com.bernaferrari.quietguard.ui.util.asUiAsyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

enum class DnsListFilter {
    All,
    Active,
    Expired,
}

data class DnsScreenState(
    val entries: UiAsyncState<List<DnsEntry>> = UiAsyncState(emptyList(), isLoading = true),
    val filter: DnsListFilter = DnsListFilter.All,
    val nowMs: Long = 0L,
) {
    val filtered: List<DnsEntry>
        get() {
            val list = entries.data
            return list.filter { entry ->
                val expired = entry.time + entry.ttl < nowMs
                when (filter) {
                    DnsListFilter.All -> true
                    DnsListFilter.Active -> !expired
                    DnsListFilter.Expired -> expired
                }
            }
        }

    val expiredCount: Int
        get() = entries.data.count { it.time + it.ttl < nowMs }
}

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class DnsViewModel(
    private val trafficRepository: TrafficRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(DnsListFilter.All)
    private val refreshVersion = MutableStateFlow(0L)
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val entriesState =
        refreshVersion
            .flatMapLatest { trafficRepository.observeDns() }
            .asUiAsyncState(
                scope = viewModelScope,
                initialData = emptyList(),
                retryRequests = retryRequests,
            )

    val uiState: StateFlow<DnsScreenState> =
        combine(entriesState, filter) { entries, f ->
            DnsScreenState(entries = entries, filter = f, nowMs = currentTimeMillis())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DnsScreenState(nowMs = currentTimeMillis()),
        )

    fun setFilter(value: DnsListFilter) {
        filter.value = value
    }

    fun refresh() {
        if (entriesState.value.hasFailed) {
            retryRequests.tryEmit(Unit)
        } else {
            refreshVersion.update { it + 1 }
        }
    }

    fun cleanup() {
        viewModelScope.launch { trafficRepository.cleanupDnsEntries() }
    }

    fun clearAll() {
        viewModelScope.launch { trafficRepository.clearAllDns() }
    }

    fun export(onComplete: (Boolean, String?) -> Unit) = exportDnsToFile(onComplete)
}
