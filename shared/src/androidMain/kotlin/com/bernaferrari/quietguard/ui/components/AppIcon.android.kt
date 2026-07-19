package com.bernaferrari.quietguard.ui.components

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols



import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
actual fun AppIcon(
    packageName: String?,
    displayName: String?,
    modifier: Modifier,
    size: Dp,
    cornerRadius: Dp,
    contentDescription: String?,
    fallbackIcon: MaterialIcon?,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    val icon = fallbackIcon ?: MaterialSymbols.Filled.Apps
    val drawable =
        remember(packageName) {
            packageName?.let { pkg ->
                runCatching {
                    val pm = context.packageManager
                    pm.getApplicationIcon(pm.getApplicationInfo(pkg, 0))
                }.getOrNull()
            }
        }

    if (drawable == null) {
        Surface(
            modifier = modifier.size(size),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size / 2f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        AsyncImage(
            model =
                ImageRequest.Builder(context)
                    .data(drawable)
                    .crossfade(true)
                    .build(),
            contentDescription = contentDescription,
            modifier = modifier.size(size).clip(shape),
        )
    }
}
