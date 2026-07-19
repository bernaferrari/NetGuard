package com.bernaferrari.quietguard.ui.components

import com.bernaferrari.quietguard.ui.icons.MaterialIcon

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun AppIcon(
    packageName: String?,
    displayName: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = size / 3f,
    contentDescription: String? = null,
    fallbackIcon: MaterialIcon? = null,
)
