package com.bernaferrari.quietguard.ui.screens.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.quietguard.data.TrafficRepository
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import com.bernaferrari.quietguard.ui.util.asUiAsyncState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

enum class ForwardingListFilter {
    All,
    Udp,
    Tcp,
}

data class ForwardingScreenState(
    val entries: UiAsyncState<List<ForwardingEntry>> = UiAsyncState(emptyList(), isLoading = true),
    val protocolFilter: ForwardingListFilter = ForwardingListFilter.All,
    val showAddDialog: Boolean = false,
) {
    val filtered: List<ForwardingEntry>
        get() =
            entries.data.filter { entry ->
                when (protocolFilter) {
                    ForwardingListFilter.All -> true
                    ForwardingListFilter.Udp -> entry.protocol == 17
                    ForwardingListFilter.Tcp -> entry.protocol == 6
                }
            }
}

@KoinViewModel
class ForwardingViewModel(
    private val trafficRepository: TrafficRepository,
) : ViewModel() {
    private val protocolFilter = MutableStateFlow(ForwardingListFilter.All)
    private val showAddDialog = MutableStateFlow(false)
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val entriesState =
        trafficRepository.observeForwarding().asUiAsyncState(
            scope = viewModelScope,
            initialData = emptyList(),
            retryRequests = retryRequests,
        )

    val uiState: StateFlow<ForwardingScreenState> =
        combine(entriesState, protocolFilter, showAddDialog) { entries, filter, showDialog ->
            ForwardingScreenState(entries = entries, protocolFilter = filter, showAddDialog = showDialog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForwardingScreenState())

    fun setProtocolFilter(filter: ForwardingListFilter) {
        protocolFilter.value = filter
    }

    fun setShowAddDialog(show: Boolean) {
        showAddDialog.value = show
    }

    fun retry() {
        retryRequests.tryEmit(Unit)
    }

    fun addForward(protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) {
        viewModelScope.launch { trafficRepository.addForward(protocol, dport, raddr, rport, ruid) }
        showAddDialog.value = false
    }

    fun deleteForward(entry: ForwardingEntry) {
        viewModelScope.launch { trafficRepository.deleteForward(entry) }
    }
}
