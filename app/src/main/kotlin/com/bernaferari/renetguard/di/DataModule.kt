package com.bernaferari.renetguard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferari.renetguard.data.dataStore
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
class DataModule {
    @Single
    fun dataStore(context: Context): DataStore<Preferences> = context.dataStore
}