package com.bernaferari.renetguard.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferari.renetguard.data.PreferenceKeys
import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.domain.FirewallRule
import com.bernaferari.renetguard.domain.RulesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class RulesUiState(
    val rules: List<FirewallRule> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val revision: Long = 0L,
)

@KoinViewModel
class MainViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val rulesRepository: RulesRepository,
) : ViewModel() {
    private var pendingRefreshJob: Job? = null

    val enabled: StateFlow<Boolean> =
        preferencesRepository.enabledFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )
    private val _rulesUiState = MutableStateFlow(RulesUiState())
    val rulesUiState: StateFlow<RulesUiState> = _rulesUiState.asStateFlow()

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
        if (state.hasLoaded || state.isLoading) {
            return
        }
        refreshRules()
    }

    fun refreshRules() {
        if (_rulesUiState.value.isLoading) {
            return
        }
        viewModelScope.launch {
            _rulesUiState.update { it.copy(isLoading = true) }
            val loaded =
                rulesRepository.loadRules(refresh = false)
                    .sortedBy { (it.name ?: it.packageName.orEmpty()).lowercase() }
            _rulesUiState.value =
                RulesUiState(
                    rules = loaded,
                    isLoading = false,
                    hasLoaded = true,
                    revision = _rulesUiState.value.revision + 1L,
                )
        }
    }

    fun notifyRulesChanged() {
        _rulesUiState.update { state ->
            state.copy(revision = state.revision + 1L)
        }
    }

    fun persistRule(rule: FirewallRule) {
        rulesRepository.persistRule(rule, _rulesUiState.value.rules)
        notifyRulesChanged()
    }

    override fun onCleared() {
        pendingRefreshJob?.cancel()
        super.onCleared()
    }
}