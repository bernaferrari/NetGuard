package com.bernaferari.renetguard.di

import android.content.Context
import com.bernaferari.renetguard.domain.AndroidRulesRepository
import com.bernaferari.renetguard.domain.RulesRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Module
actual class PlatformDiModule {
    @Single
    fun rulesRepository(
        repository: AndroidRulesRepository,
    ): RulesRepository = repository

    @Single
    fun androidRulesRepository(
        @Provided context: Context,
        preferencesRepository: com.bernaferari.renetguard.data.PreferencesRepository,
    ): AndroidRulesRepository = AndroidRulesRepository(preferencesRepository, context)
}