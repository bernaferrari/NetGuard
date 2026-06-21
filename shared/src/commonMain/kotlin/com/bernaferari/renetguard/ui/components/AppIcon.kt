package com.bernaferari.renetguard.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    fallbackIcon: ImageVector? = null,
)