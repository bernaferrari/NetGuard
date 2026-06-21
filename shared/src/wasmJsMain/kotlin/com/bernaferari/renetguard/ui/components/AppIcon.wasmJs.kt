package com.bernaferari.renetguard.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

@Composable
actual fun AppIcon(
    packageName: String?,
    displayName: String?,
    modifier: Modifier,
    size: Dp,
    cornerRadius: Dp,
    contentDescription: String?,
    fallbackIcon: ImageVector?,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val icon = fallbackIcon ?: Icons.Default.Apps
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size / 2f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}