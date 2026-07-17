package com.bernaferari.renetguard.ui.screens.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferari.renetguard.data.TrafficRepository
import com.bernaferari.renetguard.domain.FirewallRule
import com.bernaferari.renetguard.platform.AccessEntry
import com.bernaferari.renetguard.ui.util.UiAsyncState
import com.bernaferari.renetguard.ui.util.asUiAsyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AppRuleDetailViewModel(
    private val trafficRepository: TrafficRepository,
) : ViewModel() {
    private val uidFlow = MutableStateFlow(-1)

    val accessState: StateFlow<UiAsyncState<List<AccessEntry>>> =
        uidFlow
            .flatMapLatest { uid ->
                if (uid < 0) flowOf(emptyList()) else trafficRepository.observeAccess(uid)
            }
            .asUiAsyncState(viewModelScope, emptyList())

    fun bindRule(rule: FirewallRule) {
        if (uidFlow.value != rule.uid) {
            uidFlow.value = rule.uid
        }
    }

    fun clearAccess(uid: Int) {
        viewModelScope.launch { trafficRepository.clearAccessForUid(uid) }
    }
}
