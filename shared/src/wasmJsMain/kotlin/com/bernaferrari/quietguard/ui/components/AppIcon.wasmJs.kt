package com.bernaferrari.quietguard.ui.components

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols



import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.bernaferrari.quietguard.demo.wasmDemoAppVisual
import com.bernaferrari.quietguard.demo.wasmDemoFallbackVisual

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
    val shape = RoundedCornerShape(cornerRadius)
    val visual =
        remember(packageName, displayName) {
            wasmDemoAppVisual(packageName, displayName)
                ?: displayName?.let(::wasmDemoFallbackVisual)
        }
    val defaultIcon = fallbackIcon ?: MaterialSymbols.Filled.Apps

    if (visual == null) {
        Surface(
            modifier = modifier.size(size),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon = defaultIcon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size / 2f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = visual.backgroundColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                visual.letter != null ->
                    Text(
                        text = visual.letter,
                        color = visual.iconTint,
                        fontSize = letterFontSize(size, visual.letter),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                visual.icon != null ->
                    Icon(
                        icon = visual.icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(size * 0.55f),
                        tint = visual.iconTint,
                    )
                else ->
                    Icon(
                        icon = defaultIcon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(size / 2f),
                        tint = visual.iconTint,
                    )
            }
        }
    }
}

private fun letterFontSize(
    size: Dp,
    letter: String,
): TextUnit {
    val scale = if (letter.length > 1) 0.38f else 0.48f
    return (size.value * scale).sp
}
