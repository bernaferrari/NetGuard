package com.bernaferari.renetguard.di

import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [SharedModule::class])
object NetGuardKoinApp

private var koinStarted = false

fun initKoin() {
    if (koinStarted) return
    startKoin<NetGuardKoinApp>()
    koinStarted = true
}