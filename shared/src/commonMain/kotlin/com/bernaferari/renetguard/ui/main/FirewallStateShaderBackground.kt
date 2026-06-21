package com.bernaferari.renetguard.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun FirewallStateShaderBackground(
    enabledProgress: Float,
    modifier: Modifier = Modifier,
)