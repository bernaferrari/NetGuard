package com.bernaferari.renetguard.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [DataModule::class, PlatformDiModule::class])
@Configuration
@ComponentScan(
    "com.bernaferari.renetguard.ui",
    "com.bernaferari.renetguard.data",
    "com.bernaferari.renetguard.domain",
)
class SharedModule