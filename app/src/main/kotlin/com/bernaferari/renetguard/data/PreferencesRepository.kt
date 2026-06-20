package com.bernaferari.renetguard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.CopyOnWriteArrayList

@Single
class PreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()
    private val changeEvents = MutableSharedFlow<Set<String>>(extraBufferCapacity = 64)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val data: StateFlow<Preferences> = state.asStateFlow()
    val changes: SharedFlow<Set<String>> = changeEvents.asSharedFlow()

    val enabledFlow: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[KEY_ENABLED] ?: false
        }

    init {
        scope.launch {
            dataStore.data.collect { prefs ->
                val previous = state.value
                state.value = prefs
                notifyChanges(previous, prefs)
            }
        }
    }

    fun addListener(listener: (String) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun getBoolean(name: String, default: Boolean = false): Boolean =
        state.value[booleanPreferencesKey(name)] ?: default

    fun getInt(name: String, default: Int = 0): Int =
        state.value[intPreferencesKey(name)] ?: default

    fun getLong(name: String, default: Long = 0L): Long =
        state.value[longPreferencesKey(name)] ?: default

    fun getFloat(name: String, default: Float = 0f): Float =
        state.value[floatPreferencesKey(name)] ?: default

    fun getString(name: String, default: String? = null): String? =
        state.value[stringPreferencesKey(name)] ?: default

    fun getStringSet(name: String, default: Set<String> = emptySet()): Set<String> =
        state.value[stringSetPreferencesKey(name)] ?: default

    fun putBoolean(name: String, value: Boolean) =
        update { it[booleanPreferencesKey(name)] = value }

    fun putInt(name: String, value: Int) = update { it[intPreferencesKey(name)] = value }

    fun putLong(name: String, value: Long) = update { it[longPreferencesKey(name)] = value }

    fun putFloat(name: String, value: Float) = update { it[floatPreferencesKey(name)] = value }

    fun putString(name: String, value: String?) =
        update {
            val key = stringPreferencesKey(name)
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }

    fun putStringSet(name: String, value: Set<String>) =
        update { it[stringSetPreferencesKey(name)] = value }

    fun remove(name: String) =
        update {
            it.remove(booleanPreferencesKey(name))
            it.remove(intPreferencesKey(name))
            it.remove(longPreferencesKey(name))
            it.remove(floatPreferencesKey(name))
            it.remove(stringPreferencesKey(name))
            it.remove(stringSetPreferencesKey(name))
        }

    fun keysWithPrefix(prefix: String): Set<String> {
        if (prefix.isBlank()) return state.value.asMap().keys.map { it.name }.toSet()
        return state.value.asMap().keys.map { it.name }.filter { it.startsWith("${prefix}_") }
            .toSet()
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }

    private fun update(mutator: (MutablePreferences) -> Unit) {
        scope.launch {
            dataStore.edit { prefs ->
                mutator(prefs)
            }
        }
    }

    private fun notifyChanges(old: Preferences, new: Preferences) {
        val changedKeys = changedKeys(old, new)
        if (changedKeys.isEmpty()) {
            return
        }
        changedKeys.forEach { name -> listeners.forEach { it(name) } }
        changeEvents.tryEmit(changedKeys)
    }

    internal fun changedKeys(old: Preferences, new: Preferences): Set<String> {
        val oldKeys = old.asMap().keys
        val newKeys = new.asMap().keys
        val names = (oldKeys + newKeys).map { it.name }.toSet()
        return names.filter { name ->
            old.asMap().entries.firstOrNull { it.key.name == name }?.value !=
                new.asMap().entries.firstOrNull { it.key.name == name }?.value
        }.toSet()
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("enabled")

        fun namespaced(prefix: String, key: String): String =
            if (prefix.isBlank()) key else "${prefix}_${key}"

        fun uidKey(prefix: String, uid: Int): String = namespaced(prefix, uid.toString())
    }
}