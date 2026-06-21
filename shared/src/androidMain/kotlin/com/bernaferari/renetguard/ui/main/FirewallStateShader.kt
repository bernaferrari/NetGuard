package com.bernaferari.renetguard.ui.main

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import com.bernaferari.renetguard.ui.theme.LocalMotion
import android.graphics.Paint as AndroidPaint

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun FirewallStateShader(
    enabledProgress: Float,
    modifier: Modifier = Modifier,
) {
    val shader = remember {
        runCatching { RuntimeShader(FIREWALL_AGSL) }
            .onFailure { error -> Log.w("FirewallShader", "AGSL disabled: ${error.message}") }
            .getOrNull()
    } ?: return

    val motion = LocalMotion.current
    val phase by rememberInfiniteTransition(label = "firewallShader").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = motion.durationSlow * 24, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "firewallShaderPhase",
    )

    val paint = remember { AndroidPaint().apply { isAntiAlias = true } }
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Canvas(modifier = modifier) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", phase)
        shader.setFloatUniform("uEnabled", enabledProgress.coerceIn(0f, 1f))
        shader.setFloatUniform("uDark", if (isDarkTheme) 1f else 0f)
        shader.setFloatUniform(
            "uSecondary",
            secondary.red,
            secondary.green,
            secondary.blue,
            secondary.alpha
        )
        shader.setFloatUniform(
            "uTertiary",
            tertiary.red,
            tertiary.green,
            tertiary.blue,
            tertiary.alpha
        )
        shader.setFloatUniform("uOutline", outline.red, outline.green, outline.blue, outline.alpha)
        shader.setFloatUniform(
            "uSurfaceVariant",
            surfaceVariant.red,
            surfaceVariant.green,
            surfaceVariant.blue,
            surfaceVariant.alpha,
        )

        paint.shader = shader
        drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

private const val FIREWALL_AGSL =
    """
uniform float2 iResolution;
uniform float iTime;
uniform float uEnabled;
uniform float uDark;
uniform half4 uSecondary;
uniform half4 uTertiary;
uniform half4 uOutline;
uniform half4 uSurfaceVariant;

half hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return half(fract(p.x * p.y));
}

half3 mix3(half3 a, half3 b, float t) {
    return a + (b - a) * half(t);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float tau = 6.2831853;
    float theta = iTime * tau;
    float en = clamp(uEnabled, 0.0, 1.0);

    // Effect-only shader: no base/background recolor.
    half3 color = half3(0.0);
    float alpha = 0.0;

    half3 sigDisabled = mix3(uSurfaceVariant.rgb, uOutline.rgb, mix(0.36, 0.50, uDark));
    half3 sigEnabled = mix3(uSecondary.rgb, uTertiary.rgb, 0.52);
    half3 signal = mix3(sigDisabled, sigEnabled, en);

    // Keep center content readable without radial masking.
    float cx = abs(uv.x - 0.5);
    float cy = abs(uv.y - 0.52);
    float centerCut = max(
        smoothstep(0.06, 0.16, cx),
        smoothstep(0.05, 0.13, cy)
    );

    const int lanes = 6;
    for (int i = 0; i < lanes; ++i) {
        float laneT = float(i) / float(lanes - 1);
        float centerY = 0.20 + laneT * 0.60;
        float fi = float(i);
        float laneOff = fi * 0.85;
        float freqDis = 1.0 + (fi - 3.0 * floor(fi / 3.0));
        float freqEn = 2.0 + (fi - 2.0 * floor(fi / 2.0));

        // Disabled: slower, flatter, heavier cadence (sadder).
        float yDis = centerY
            + (0.005 + laneT * 0.003) * sin(uv.x * tau * freqDis + theta * 0.55 + laneOff)
            + 0.002 * sin(theta * 0.18 + laneOff * 1.7);
        float disLine = 1.0 - smoothstep(0.0, 0.0027, abs(uv.y - yDis));

        // Enabled: faster and livelier phase interference (happier).
        float yEn = centerY
            + (0.008 + laneT * 0.005) * sin(uv.x * tau * (freqEn + 1.2) + theta * 1.85 + laneOff)
            + 0.003 * sin(uv.x * tau * 3.0 - theta * 1.25 + laneOff * 0.6);
        float enLine = 1.0 - smoothstep(0.0, 0.0034, abs(uv.y - yEn));

        float lineStrength = mix(disLine * 0.030, enLine * 0.062, en) * centerCut;
        color += signal * half(lineStrength);
        alpha += lineStrength * 1.05;
    }

    // Very subtle pixel grain.
    float2 grainCell = float2(44.0, 44.0);
    float2 q = floor(uv * grainCell) / grainCell;
    half grain = hash21(q + float2(sin(theta), cos(theta))) - half(0.5);
    color += signal * grain * half(mix(0.004, 0.007, uDark) * centerCut);

    // Disabled stays dimmer; enabled pops slightly more.
    alpha *= mix(0.72, 0.92, en);
    alpha = clamp(alpha, 0.0, mix(0.12, 0.20, uDark));

    return half4(saturate(color), half(alpha));
}
"""
