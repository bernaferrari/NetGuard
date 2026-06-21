package com.bernaferari.renetguard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class ScreenDimensions(
    val widthDp: Int,
    val heightDp: Int,
)

@Composable
expect fun rememberScreenDimensions(): ScreenDimensions

@Composable
fun isWideScreen(thresholdDp: Int = 600): Boolean {
    val dimensions = rememberScreenDimensions()
    return remember(dimensions, thresholdDp) {
        dimensions.widthDp >= thresholdDp
    }
}