package com.bernaferrari.quietguard.ui.util

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols



import androidx.compose.runtime.Composable

@Composable
fun InfoScreen(
    title: String,
    body: String,
) {
    StatePlaceholder(
        title = title,
        message = body,
        icon = MaterialSymbols.Filled.Info,
    )
}
