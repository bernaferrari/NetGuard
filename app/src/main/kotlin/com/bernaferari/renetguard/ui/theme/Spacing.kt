package com.bernaferari.renetguard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for consistent spacing throughout the app.
 * Based on an 4dp grid system.
 */
@Immutable
data class Spacing(
    /** 4.dp - Minimal spacing for tight layouts */
    val extraSmall: Dp = 4.dp,
    /** 8.dp - Small gaps, button icon spacing */
    val small: Dp = 8.dp,
    /** 12.dp - Medium gaps, card internal spacing */
    val medium: Dp = 12.dp,
    /** 16.dp - Default padding, screen edges */
    val default: Dp = 16.dp,
    /** 20.dp - Large padding, section spacing */
    val large: Dp = 20.dp,
    /** 24.dp - Extra large padding */
    val extraLarge: Dp = 24.dp,
    /** 32.dp - Section dividers, major spacing */
    val xxLarge: Dp = 32.dp,
)

/**
 * Minimum touch target sizes for accessibility.
 */
object TouchTargets {
    /** 48.dp - Minimum touch target per WCAG guidelines */
    val minimum: Dp = 48.dp

    /** 24.dp - Standard icon size */
    val iconSize: Dp = 24.dp

    /** 36.dp - App icon size in lists */
    val appIconSize: Dp = 36.dp
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }
