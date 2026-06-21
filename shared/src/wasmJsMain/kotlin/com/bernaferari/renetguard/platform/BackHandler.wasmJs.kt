package com.bernaferari.renetguard.platform

import androidx.compose.runtime.Composable

@Composable
actual fun HandleBackPress(enabled: Boolean, onBack: () -> Unit) {
    // Browser back is handled by the host page.
}