package com.bernaferari.renetguard.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.bernaferari.renetguard.AppContent
import com.bernaferari.renetguard.di.initKoin
import com.bernaferari.renetguard.platform.installWasmPlatformBindings

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    installWasmPlatformBindings()
    ComposeViewport {
        AppContent(
            onToggleEnabled = { /* Demo VPN handled by onDemoFirewallToggled */ },
        )
    }
}