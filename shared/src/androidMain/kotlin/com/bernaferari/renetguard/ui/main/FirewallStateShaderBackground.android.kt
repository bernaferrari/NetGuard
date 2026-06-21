package com.bernaferari.renetguard.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FirewallStateShaderBackground(
    enabledProgress: Float,
    modifier: Modifier,
) {
    FirewallStateShader(
        enabledProgress = enabledProgress,
        modifier = modifier,
    )
}