package com.bernaferrari.quietguard.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.quietguard.data.PreferenceKeys
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.domain.RulesRepository
import com.bernaferrari.quietguard.ui.util.UiAsyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val rulesRepository: RulesRepository,
) : ViewModel() {
    private var pendingRefreshJob: Job? = null

    val firewallState: StateFlow<UiAsyncState<Boolean>> =
        combine(preferencesRepository.enabledFlow, preferencesRepository.isLoaded) { enabled, loaded ->
            UiAsyncState(
                data = enabled,
                isLoading = !loaded,
                hasReceived = loaded,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UiAsyncState(data = false, isLoading = true),
        )
    private val _rulesUiState = MutableStateFlow(UiAsyncState<List<FirewallRule>>(emptyList()))
    val rulesUiState: StateFlow<UiAsyncState<List<FirewallRule>>> = _rulesUiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.changes.collect { changedKeys ->
                if (changedKeys.any { !PreferenceKeys.isPerAppRuleKey(it) }) {
                    pendingRefreshJob?.cancel()
                    pendingRefreshJob =
                        launch {
                            delay(300L)
                            refreshRules()
                        }
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnabled(enabled)
        }
    }

    fun ensureRulesLoaded() {
        val state = _rulesUiState.value
        if (state.hasReceived || state.isLoading) {
            return
        }
        refreshRules()
    }

    fun refreshRules() {
        if (_rulesUiState.value.isLoading) {
            return
        }
        viewModelScope.launch {
            _rulesUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val loaded =
                    rulesRepository.loadRules(refresh = false)
                        .sortedBy { (it.name ?: it.packageName.orEmpty()).lowercase() }
                _rulesUiState.value = UiAsyncState(
                    data = loaded,
                    isLoading = false,
                    hasReceived = true,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _rulesUiState.update {
                    it.copy(isLoading = false, error = error)
                }
            }
        }
    }

    fun updateRule(
        uid: Int,
        transform: (FirewallRule) -> FirewallRule,
    ) {
        val rules = _rulesUiState.value.data.toMutableList()
        val index = rules.indexOfFirst { it.uid == uid }
        if (index < 0) {
            return
        }

        val updatedRule = transform(rules[index])
        rules[index] = updatedRule
        rulesRepository.persistRule(updatedRule, rules)
        _rulesUiState.update { state -> state.copy(data = rules.toList()) }
    }

    override fun onCleared() {
        pendingRefreshJob?.cancel()
        super.onCleared()
    }
}
