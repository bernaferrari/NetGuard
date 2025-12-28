package eu.faircode.netguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    private val legacyPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    val enabledFlow: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[KEY_ENABLED] ?: legacyPrefs.getBoolean("enabled", false)
        }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
        legacyPrefs.edit().putBoolean("enabled", enabled).apply()
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("enabled")
    }
}
