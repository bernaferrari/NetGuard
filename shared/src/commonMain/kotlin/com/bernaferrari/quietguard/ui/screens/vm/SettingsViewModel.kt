package com.bernaferrari.quietguard.ui.screens.vm

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.platform.importHostsFromFile
import com.bernaferrari.quietguard.platform.registerUpdateCheckListener
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<Preferences> =
        preferencesRepository.data.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            preferencesRepository.data.value,
        )

    fun putBoolean(key: String, value: Boolean) = preferencesRepository.putBoolean(key, value)

    fun putString(key: String, value: String?) = preferencesRepository.putString(key, value)

    fun putStringSet(key: String, value: Set<String>) = preferencesRepository.putStringSet(key, value)

    fun putInt(key: String, value: Int) = preferencesRepository.putInt(key, value)

    fun removeString(key: String) = preferencesRepository.removeString(key)

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        preferencesRepository.getBoolean(key, default)

    fun getString(key: String, default: String? = null): String? =
        preferencesRepository.getString(key, default)

    fun getInt(key: String, default: Int = 0): Int =
        preferencesRepository.getInt(key, default)

    fun registerUpdateCheck(onResult: (String?, String?) -> Unit): () -> Unit =
        registerUpdateCheckListener(onResult)

    fun importHosts(onComplete: (Boolean) -> Unit) = importHostsFromFile(onComplete)
}
