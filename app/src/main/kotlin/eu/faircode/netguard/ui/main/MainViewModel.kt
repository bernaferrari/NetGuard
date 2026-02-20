package eu.faircode.netguard.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.faircode.netguard.data.PreferencesRepository
import eu.faircode.netguard.Rule
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import eu.faircode.netguard.data.Prefs
import dagger.hilt.android.qualifiers.ApplicationContext

data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {
    val enabled: StateFlow<Boolean> =
        preferencesRepository.enabledFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )
    private val _rulesUiState = MutableStateFlow(RulesUiState())
    val rulesUiState: StateFlow<RulesUiState> = _rulesUiState.asStateFlow()

    init {
        // Auto-refresh the app list whenever any preference changes (e.g. settings toggles)
        viewModelScope.launch {
            Prefs.data
                .drop(1) // skip the initial emission
                .debounce(300L) // coalesce rapid back-to-back changes
                .collect { refreshRules() }
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
                )
        }
    }

    fun notifyRulesChanged() {
        _rulesUiState.update { state ->
            state.copy(rules = state.rules.toList())
        }
    }
}
