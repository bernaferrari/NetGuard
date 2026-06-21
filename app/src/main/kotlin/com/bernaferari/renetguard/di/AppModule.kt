package com.bernaferari.renetguard.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [SharedModule::class])
@Configuration
@ComponentScan("com.bernaferari.renetguard.appfunctions")
class AppModule