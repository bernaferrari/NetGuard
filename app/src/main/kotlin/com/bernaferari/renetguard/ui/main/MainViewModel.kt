package com.bernaferari.renetguard.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.bernaferari.renetguard.Rule
import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val revision: Long = 0L,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {
    private var removePrefsListener: (() -> Unit)? = null
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
        // Auto-refresh list for global preference changes.
        // Skip per-app keys (wifi_<pkg>, other_<pkg>, etc.) to avoid noisy full reloads on row toggles.
        removePrefsListener = Prefs.addListener { key ->
            if (isPerAppRuleKey(key)) return@addListener
            pendingRefreshJob?.cancel()
            pendingRefreshJob = viewModelScope.launch {
                delay(300L)
                refreshRules()
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
                withContext(Dispatchers.IO) {
                    Rule.getRules(false, appContext)
                        .sortedBy { (it.name ?: it.packageName.orEmpty()).lowercase() }
                }
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

    override fun onCleared() {
        pendingRefreshJob?.cancel()
        removePrefsListener?.invoke()
        super.onCleared()
    }

    private fun isPerAppRuleKey(key: String): Boolean {
        val prefixes = listOf(
            "wifi",
            "other",
            "apply",
            "screen_wifi",
            "screen_other",
            "roaming",
            "lockdown",
            "notify",
            "unused",
        )
        return prefixes.any { prefix ->
            key.startsWith("${prefix}_") &&
                    key.substring(prefix.length + 1).contains('.')
        }
    }
}
