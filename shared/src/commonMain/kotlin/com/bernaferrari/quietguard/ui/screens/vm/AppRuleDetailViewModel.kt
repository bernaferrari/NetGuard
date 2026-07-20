package com.bernaferrari.quietguard.ui.screens.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.quietguard.data.TrafficRepository
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.platform.AccessEntry
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import com.bernaferrari.quietguard.ui.util.asUiAsyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class AppAccessHistory(
    val uid: Int = -1,
    val entries: List<AccessEntry> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AppRuleDetailViewModel(
    private val trafficRepository: TrafficRepository,
) : ViewModel() {
    private val uidFlow = MutableStateFlow(-1)

    val accessState: StateFlow<UiAsyncState<AppAccessHistory>> =
        uidFlow
            .flatMapLatest { uid ->
                if (uid < 0) {
                    flowOf(AppAccessHistory())
                } else {
                    trafficRepository.observeAccess(uid).map { entries ->
                        AppAccessHistory(uid = uid, entries = entries)
                    }
                }
            }
            .asUiAsyncState(viewModelScope, AppAccessHistory())

    fun bindRule(rule: FirewallRule) {
        if (uidFlow.value != rule.uid) {
            uidFlow.value = rule.uid
        }
    }

    fun clearAccess(uid: Int) {
        viewModelScope.launch { trafficRepository.clearAccessForUid(uid) }
    }
}
