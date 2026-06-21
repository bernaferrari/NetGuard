package com.bernaferari.renetguard.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

@Composable
actual fun FirewallStateShaderBackground(
    enabledProgress: Float,
    modifier: Modifier,
) {
    val motion = com.bernaferari.renetguard.ui.theme.LocalMotion.current
    val phase by rememberInfiniteTransition(label = "firewallShader").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = motion.durationSlow * 24, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "firewallShaderPhase",
    )
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val blend = enabledProgress.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val wave = (kotlin.math.sin((phase * 6.28f).toDouble()) * 0.15f).toFloat()
        val start =
            if (isDark) {
                secondary.copy(alpha = 0.18f + blend * 0.22f + wave)
            } else {
                tertiary.copy(alpha = 0.12f + blend * 0.18f + wave)
            }
        val end =
            if (isDark) {
                tertiary.copy(alpha = 0.08f + blend * 0.12f)
            } else {
                secondary.copy(alpha = 0.06f + blend * 0.1f)
            }
        drawRect(
            brush =
                Brush.linearGradient(
                    colors = listOf(start, end),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
        )
    }
}