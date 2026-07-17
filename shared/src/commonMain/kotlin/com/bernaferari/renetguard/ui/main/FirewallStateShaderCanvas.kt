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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import com.bernaferari.renetguard.ui.theme.LocalMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun FirewallStateShaderCanvas(
    enabledProgress: Float,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val phase by rememberInfiniteTransition(label = "firewallShaderCanvas").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = motion.durationSlow * 24, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "firewallShaderCanvasPhase",
    )
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val blend = enabledProgress.coerceIn(0f, 1f)
    val signalDisabled =
        Color(
            red = surfaceVariant.red * 0.52f + outline.red * 0.28f + error.red * 0.20f,
            green = surfaceVariant.green * 0.52f + outline.green * 0.28f + error.green * 0.20f,
            blue = surfaceVariant.blue * 0.52f + outline.blue * 0.28f + error.blue * 0.20f,
        )
    val signalEnabled =
        Color(
            red = secondary.red * 0.48f + tertiary.red * 0.52f,
            green = secondary.green * 0.48f + tertiary.green * 0.52f,
            blue = secondary.blue * 0.48f + tertiary.blue * 0.52f,
        )
    val signal =
        Color(
            red = signalDisabled.red + (signalEnabled.red - signalDisabled.red) * blend,
            green = signalDisabled.green + (signalEnabled.green - signalDisabled.green) * blend,
            blue = signalDisabled.blue + (signalEnabled.blue - signalDisabled.blue) * blend,
        )
    val lanePaths = remember { List(6) { Path() } }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val theta = phase * (2f * PI).toFloat()
        val tau = (2f * PI).toFloat()
        val sampleCount = width.toInt().coerceIn(48, 220)
        val stepX = width / sampleCount
        val auraRadius = max(width, height) * 0.58f
        val auraCenter = Offset(
            x = width * (0.24f + 0.025f * sin(theta)),
            y = height * (0.30f + 0.020f * cos(theta)),
        )
        val auraAlpha = if (isDark) 0.13f else 0.08f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    signal.copy(alpha = auraAlpha * (0.72f + 0.28f * blend)),
                    signal.copy(alpha = auraAlpha * 0.28f),
                    Color.Transparent,
                ),
                center = auraCenter,
                radius = auraRadius,
            ),
            center = auraCenter,
            radius = auraRadius,
        )

        val secondaryAuraCenter = Offset(
            x = width * (0.82f + 0.020f * cos(theta)),
            y = height * (0.70f + 0.020f * sin(theta)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    signal.copy(alpha = auraAlpha * 0.42f),
                    Color.Transparent,
                ),
                center = secondaryAuraCenter,
                radius = auraRadius * 0.58f,
            ),
            center = secondaryAuraCenter,
            radius = auraRadius * 0.58f,
        )

        repeat(6) { laneIndex ->
            val laneT = laneIndex / 5f
            val centerY = (0.10f + laneT * 0.80f) * height
            val laneOff = laneIndex * 0.85f
            val freqDis = 1f + (laneIndex % 3).toFloat()
            val freqEn = 2f + (laneIndex % 2).toFloat()
            val focusX1 = 0.24f + 0.018f * sin(theta)
            val focusY1 = (0.30f + (laneT - 0.5f) * 0.045f) * height
            val focusX2 = 0.82f + 0.016f * cos(theta)
            val focusY2 = (0.70f + (laneT - 0.5f) * 0.05f) * height
            val path = lanePaths[laneIndex]
            path.rewind()

            for (sample in 0..sampleCount) {
                val x = sample * stepX
                val uvX = x / width
                val organicDisabled =
                    centerY +
                        (0.030f + laneT * 0.010f) * height *
                        sin(uvX * tau * freqDis + theta + laneOff) +
                        0.014f * height * sin(uvX * tau * 2.3f - theta + laneOff)
                val organicEnabled =
                    centerY +
                        (0.045f + laneT * 0.012f) * height *
                        sin(uvX * tau * (freqEn + 0.7f) + theta * 2f + laneOff) +
                        0.021f * height *
                        sin(uvX * tau * 3.2f - theta * 3f + laneOff * 0.6f)
                val organicY = organicDisabled + (organicEnabled - organicDisabled) * blend
                val focusDistance1 = (uvX - focusX1) / 0.105f
                val focusPull1 =
                    exp(-(focusDistance1 * focusDistance1)) *
                        (0.64f + 0.12f * sin(theta + laneOff))
                val focusDistance2 = (uvX - focusX2) / 0.115f
                val focusPull2 =
                    exp(-(focusDistance2 * focusDistance2)) *
                        (0.62f + 0.13f * cos(theta * 2f + laneOff))
                val firstFocusY = organicY + (focusY1 - organicY) * focusPull1
                val y = firstFocusY + (focusY2 - firstFocusY) * focusPull2
                if (sample == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            val themeStrength = if (isDark) 1f else 0.72f
            drawPath(
                path = path,
                color = signal.copy(alpha = (0.018f + 0.014f * blend) * themeStrength),
                style = Stroke(width = 28f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = signal.copy(alpha = (0.032f + 0.020f * blend) * themeStrength),
                style = Stroke(width = 8f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                color = signal.copy(alpha = (0.13f + 0.05f * blend) * themeStrength),
                style = Stroke(width = 1.35f, cap = StrokeCap.Round),
            )
        }

    }
}
