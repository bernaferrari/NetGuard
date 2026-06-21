package com.bernaferari.renetguard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences

private var appContext: Context? = null

fun initPreferencesDataStore(context: Context) {
    appContext = context.applicationContext
    createPreferencesDataStore()
}

actual fun createPlatformDataStore(): DataStore<Preferences> {
    val context =
        checkNotNull(appContext) {
            "Call initPreferencesDataStore(context) before accessing preferences on Android."
        }
    return PreferenceDataStoreFactory.create(
        produceFile = { context.filesDir.resolve(dataStoreFileName) },
    )
}