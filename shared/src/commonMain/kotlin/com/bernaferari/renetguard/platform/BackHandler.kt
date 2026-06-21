package com.bernaferari.renetguard.platform

import androidx.compose.runtime.Composable

@Composable
expect fun HandleBackPress(enabled: Boolean, onBack: () -> Unit)