package com.bernaferrari.quietguard.ui.main

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Adapted from Paper Shaders' Dithering shader (swirl, simplex noise, and 4x4 Bayer thresholding).
 * Modified for a transparent, low-contrast Compose background and a mathematically closed loop.
 * Source: https://github.com/paper-design/shaders/tree/4d78ae940db2c73c22b6633fb05a3e4743f22551
 */
@Composable
internal fun FirewallStateShaderCanvas(
    enabledProgress: Float,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val phase =
        if (motion.reducedMotion) {
            0f
        } else {
            val animatedPhase by rememberInfiniteTransition(label = "firewallDitherCanvas").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(
                            durationMillis = motion.durationSlow * 40,
                            easing = LinearEasing,
                        ),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "firewallDitherCanvasPhase",
            )
            animatedPhase
        }
    val colors = MaterialTheme.colorScheme
    val blend = enabledProgress.coerceIn(0f, 1f)
    val ink = lerp(colors.error, colors.secondary, blend)
    val isDark = colors.surface.luminance() < 0.5f

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val theta = phase * TAU
        val aspect = size.width / size.height
        val cellSize = max(8f * density, 8f)
        val columns = ceil(size.width / cellSize).toInt()
        val rows = ceil(size.height / cellSize).toInt()
        val alphaScale = if (isDark) 1f else 0.70f

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val centerX = (column + 0.5f) * cellSize
                val centerY = (row + 0.5f) * cellSize
                val x = (centerX / size.width - 0.5f) * aspect * 1.35f
                val y = (centerY / size.height - 0.53f) * 1.35f
                val noise = simplexNoise(
                    x * 1.9f + cos(theta) * 0.58f,
                    y * 1.9f + sin(theta) * 0.58f,
                )
                val radius = sqrt(x * x + y * y)
                val angle = 6f * atan2(y, x) + theta + noise * 0.62f
                val inverseRadius = 1f / max(radius, 0.08f).pow(1.2f)
                val swirl = fract(inverseRadius + angle / TAU)
                val middle = smoothstep(0f, 1f, radius.pow(1.2f))
                val noiseShape = 0.5f + 0.5f * noise
                val shape = (swirl * middle) * 0.76f + noiseShape * 0.24f

                if (shape <= bayer4x4(column, row)) continue

                val centerQuiet = 0.18f + 0.82f * smoothstep(0.16f, 0.52f, radius)
                val texture = 0.78f + noiseShape * 0.22f
                val alpha =
                    (0.030f + 0.032f * blend) * centerQuiet * texture * alphaScale
                drawRect(
                    color = ink.copy(alpha = alpha),
                    topLeft = Offset(column * cellSize, row * cellSize),
                    size = Size(
                        width = (cellSize - density).coerceAtLeast(1f),
                        height = (cellSize - density).coerceAtLeast(1f),
                    ),
                )
            }
        }
    }
}

private const val TAU = (2.0 * PI).toFloat()

private fun bayer2x2(x: Int, y: Int): Int = (2 * x + 3 * y) and 3

private fun bayer4x4(x: Int, y: Int): Float {
    val low = bayer2x2(x and 1, y and 1)
    val high = bayer2x2((x shr 1) and 1, (y shr 1) and 1)
    return (4 * low + high) / 16f
}

private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun fract(value: Float): Float = value - floor(value)

private fun mod289(value: Float): Float = value - floor(value / 289f) * 289f

private fun permute(value: Float): Float = mod289(((value * 34f) + 1f) * value)

private fun simplexNoise(x: Float, y: Float): Float {
    val cX = 0.211324865405187f
    val cY = 0.366025403784439f
    val cZ = -0.577350269189626f
    val cW = 0.024390243902439f
    var iX = floor(x + (x + y) * cY)
    var iY = floor(y + (x + y) * cY)
    val x0X = x - iX + (iX + iY) * cX
    val x0Y = y - iY + (iX + iY) * cX
    val i1X = if (x0X > x0Y) 1f else 0f
    val i1Y = if (x0X > x0Y) 0f else 1f
    val x1X = x0X + cX - i1X
    val x1Y = x0Y + cX - i1Y
    val x2X = x0X + cZ
    val x2Y = x0Y + cZ
    iX = mod289(iX)
    iY = mod289(iY)
    val p0 = permute(permute(iY) + iX)
    val p1 = permute(permute(iY + i1Y) + iX + i1X)
    val p2 = permute(permute(iY + 1f) + iX + 1f)

    fun contribution(px: Float, dx: Float, dy: Float): Float {
        var m = max(0.5f - dx * dx - dy * dy, 0f)
        m *= m
        m *= m
        val gradientX = 2f * fract(px * cW) - 1f
        val h = abs(gradientX) - 0.5f
        val ox = floor(gradientX + 0.5f)
        val a0 = gradientX - ox
        m *= 1.7928429f - 0.85373473f * (a0 * a0 + h * h)
        return m * (a0 * dx + h * dy)
    }

    return 130f * (
        contribution(p0, x0X, x0Y) +
            contribution(p1, x1X, x1Y) +
            contribution(p2, x2X, x2Y)
        )
}
