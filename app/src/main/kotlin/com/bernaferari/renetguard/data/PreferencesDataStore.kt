package com.bernaferari.renetguard.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore("netguard_preferences")
