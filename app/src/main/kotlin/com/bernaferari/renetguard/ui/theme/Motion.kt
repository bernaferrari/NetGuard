package com.bernaferari.renetguard.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Design tokens for consistent, subtle animations throughout the app.
 * Follows Material Design 3 motion principles with a professional, minimal approach.
 */
@Immutable
data class Motion(
    // Duration constants - subtle and professional
    /** 150ms - Quick interactions like button presses */
    val durationFast: Int = 150,
    /** 250ms - Standard transitions like expand/collapse */
    val durationMedium: Int = 250,
    /** 350ms - Deliberate animations like screen transitions */
    val durationSlow: Int = 350,

    // Easing curves - Material Design 3 standard
    /** Standard easing for most transitions */
    val easingStandard: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** Emphasized easing for important state changes */
    val easingEmphasized: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** Decelerate for elements entering the screen */
    val easingDecelerate: CubicBezierEasing = CubicBezierEasing(0f, 0f, 0f, 1f),
    /** Accelerate for elements leaving the screen */
    val easingAccelerate: CubicBezierEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f),
)

val LocalMotion = staticCompositionLocalOf { Motion() }

// Extension functions for common animation specs

/** Fade animation spec for appearing/disappearing content */
fun Motion.fadeSpec() = tween<Float>(
    durationMillis = durationMedium,
    easing = easingStandard
)

/** Expand/collapse animation spec */
fun Motion.expandSpec() = tween<Int>(
    durationMillis = durationMedium,
    easing = easingEmphasized
)

/** Color transition animation spec */
fun Motion.colorSpec() = tween<androidx.compose.ui.graphics.Color>(
    durationMillis = durationMedium,
    easing = easingStandard
)

/** Scale animation spec for press effects */
fun Motion.scaleSpec() = tween<Float>(
    durationMillis = durationFast,
    easing = easingStandard
)

/** Rotation animation spec for icons */
fun Motion.rotationSpec() = tween<Float>(
    durationMillis = durationMedium,
    easing = easingStandard
)
