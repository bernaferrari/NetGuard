package com.bernaferari.renetguard.ui.main

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FirewallStateShaderBackground(
    enabledProgress: Float,
    modifier: Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        FirewallStateShader(
            enabledProgress = enabledProgress,
            modifier = modifier,
        )
    } else {
        FirewallStateShaderCanvas(
            enabledProgress = enabledProgress,
            modifier = modifier,
        )
    }
}
