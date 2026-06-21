package com.bernaferari.renetguard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun rememberScreenDimensions(): ScreenDimensions {
    val configuration = LocalConfiguration.current
    return ScreenDimensions(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
    )
}