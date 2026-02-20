package eu.faircode.netguard.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore("netguard_preferences")
