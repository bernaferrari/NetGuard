package com.bernaferari.renetguard.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable

@Composable
fun InfoScreen(
    title: String,
    body: String,
) {
    StatePlaceholder(
        title = title,
        message = body,
        icon = Icons.Default.Info,
    )
}
