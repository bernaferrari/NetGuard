package com.bernaferari.renetguard.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences

internal const val dataStoreFileName = "netguard_preferences.preferences_pb"

internal fun createDataStore(storage: Storage<Preferences>): DataStore<Preferences> =
    DataStoreFactory.create(storage = storage)

expect fun createPlatformDataStore(): DataStore<Preferences>

private var dataStoreInstance: DataStore<Preferences>? = null

fun createPreferencesDataStore(): DataStore<Preferences> =
    dataStoreInstance ?: createPlatformDataStore().also { dataStoreInstance = it }

fun createPreferencesRepository(): PreferencesRepository =
    PreferencesRepository(createPreferencesDataStore())