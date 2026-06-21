package com.bernaferari.renetguard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

private val fakeFileSystem = FakeFileSystem()
private val dataStorePath = "/data/$dataStoreFileName".toPath()

actual fun createPlatformDataStore(): DataStore<Preferences> {
    fakeFileSystem.createDirectories(dataStorePath.parent!!)
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStorePath },
    )
}