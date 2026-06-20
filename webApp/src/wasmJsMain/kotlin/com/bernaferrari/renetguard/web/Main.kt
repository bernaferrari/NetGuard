package com.bernaferrari.renetguard.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.bernaferrari.renetguard.demo.NetGuardWebDemo

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        NetGuardWebDemo()
    }
}