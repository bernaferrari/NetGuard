package com.bernaferari.renetguard.di

import com.bernaferari.renetguard.domain.RulesRepository
import com.bernaferari.renetguard.domain.WasmRulesRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class PlatformDiModule {
    @Single
    fun rulesRepository(repository: WasmRulesRepository): RulesRepository = repository
}