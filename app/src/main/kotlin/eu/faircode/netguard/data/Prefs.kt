package eu.faircode.netguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.preference.PreferenceManager
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object Prefs {
    private const val MIGRATED_KEY = "__prefs_migrated"
    private const val DEFAULT_PREFIX = ""

    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: CoroutineScope

    val data: StateFlow<Preferences> = state.asStateFlow()

    fun init(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.dataStore
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            dataStore.data.collect { prefs ->
                val previous = state.value
                state.value = prefs
                notifyChanges(previous, prefs)
            }
        }

        migrateLegacyPreferences(context)
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

    fun putBoolean(name: String, value: Boolean) = update { it[booleanPreferencesKey(name)] = value }

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

    fun putStringSet(name: String, value: Set<String>) = update { it[stringSetPreferencesKey(name)] = value }

    fun remove(name: String) =
        update {
            it.remove(booleanPreferencesKey(name))
            it.remove(intPreferencesKey(name))
            it.remove(longPreferencesKey(name))
            it.remove(floatPreferencesKey(name))
            it.remove(stringPreferencesKey(name))
            it.remove(stringSetPreferencesKey(name))
        }

    fun namespaced(prefix: String, key: String): String = if (prefix.isBlank()) key else "${prefix}_${key}"

    fun uidKey(prefix: String, uid: Int): String = namespaced(prefix, uid.toString())

    fun keysWithPrefix(prefix: String): Set<String> {
        if (prefix.isBlank()) return state.value.asMap().keys.map { it.name }.toSet()
        return state.value.asMap().keys.map { it.name }.filter { it.startsWith("${prefix}_") }.toSet()
    }

    private fun update(mutator: (MutablePreferences) -> Unit) {
        scope.launch {
            dataStore.edit { prefs ->
                mutator(prefs)
            }
        }
    }

    private fun migrateLegacyPreferences(context: Context) {
        if (getBoolean(MIGRATED_KEY, false)) return
        runBlocking(Dispatchers.IO) {
            val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val namespacedPrefs = listOf(
                "wifi",
                "other",
                "screen_wifi",
                "screen_other",
                "roaming",
                "lockdown",
                "apply",
                "notify",
                "unused",
                "IAB",
            )
            dataStore.edit { prefs ->
                copyAll(defaultPrefs.all, prefs, DEFAULT_PREFIX)
                for (name in namespacedPrefs) {
                    val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    copyAll(legacy.all, prefs, name)
                }
                prefs[booleanPreferencesKey(MIGRATED_KEY)] = true
            }
        }
    }

    private fun copyAll(
        values: Map<String, *>,
        prefs: MutablePreferences,
        prefix: String,
    ) {
        values.forEach { (key, value) ->
            val targetKey = namespaced(prefix, key)
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(targetKey)] = value
                is Int -> prefs[intPreferencesKey(targetKey)] = value
                is Long -> prefs[longPreferencesKey(targetKey)] = value
                is Float -> prefs[floatPreferencesKey(targetKey)] = value
                is String -> prefs[stringPreferencesKey(targetKey)] = value
                is Set<*> -> {
                    val set = value.filterIsInstance<String>().toSet()
                    prefs[stringSetPreferencesKey(targetKey)] = set
                }
            }
        }
    }

    private fun notifyChanges(old: Preferences, new: Preferences) {
        val oldKeys = old.asMap().keys
        val newKeys = new.asMap().keys
        val keys = (oldKeys + newKeys).map { it.name }.toSet()
        for (name in keys) {
            val oldValue = old.asMap()[findKey(oldKeys, name)]
            val newValue = new.asMap()[findKey(newKeys, name)]
            if (oldValue != newValue) {
                listeners.forEach { it(name) }
            }
        }
    }

    private fun findKey(keys: Set<Preferences.Key<*>>, name: String): Preferences.Key<*>? =
        keys.firstOrNull { it.name == name }
}
