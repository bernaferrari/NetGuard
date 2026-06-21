package com.bernaferari.renetguard.ui.theme

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

val Slate900 = Color(0xFF0E1217)
val Teal500 = Color(0xFF009688)
val BluePrimary = Color(0xFF2196F3)
val PurplePrimary = Color(0xFF9C27B0)
val AmberPrimary = Color(0xFFFFC107)
val OrangePrimary = Color(0xFFFF5722)
val GreenPrimary = Color(0xFF4CAF50)
val CyanPrimary = Color(0xFF00BCD4)
val IndigoPrimary = Color(0xFF3F51B5)
val PinkPrimary = Color(0xFFE91E63)
val LimePrimary = Color(0xFF8BC34A)

const val THEME_DEFAULT = "teal"

@ColorInt
val GraphGrayed = Color(0xFFAAAAAA).toArgb()

@ColorInt
val GraphSend = Color(0xFFFF0000).toArgb()

@ColorInt
val GraphReceive = Color(0xFF0000FF).toArgb()

@ColorInt
fun themePrimaryColor(theme: String?): Int =
    when (theme ?: THEME_DEFAULT) {
        "teal" -> Teal500.toArgb()
        "blue" -> BluePrimary.toArgb()
        "purple" -> PurplePrimary.toArgb()
        "amber" -> AmberPrimary.toArgb()
        "orange" -> OrangePrimary.toArgb()
        "green" -> GreenPrimary.toArgb()
        "cyan" -> CyanPrimary.toArgb()
        "indigo" -> IndigoPrimary.toArgb()
        "pink" -> PinkPrimary.toArgb()
        "lime" -> LimePrimary.toArgb()
        else -> Teal500.toArgb()
    }

@ColorInt
fun themeOnColor(theme: String?): Int =
    when (theme ?: THEME_DEFAULT) {
        "teal" -> Teal500.toArgb()
        "blue" -> BluePrimary.toArgb()
        "purple" -> PurplePrimary.toArgb()
        "amber" -> AmberPrimary.toArgb()
        "orange" -> OrangePrimary.toArgb()
        "green" -> GreenPrimary.toArgb()
        "cyan" -> CyanPrimary.toArgb()
        "indigo" -> IndigoPrimary.toArgb()
        "pink" -> PinkPrimary.toArgb()
        "lime" -> LimePrimary.toArgb()
        else -> Teal500.toArgb()
    }

@ColorInt
fun themeOffColor(theme: String?): Int =
    when (theme ?: THEME_DEFAULT) {
        "teal" -> OrangePrimary.toArgb()
        "blue" -> OrangePrimary.toArgb()
        "purple" -> Color(0xFFFF5252).toArgb()
        "amber" -> AmberPrimary.toArgb()
        "orange" -> OrangePrimary.toArgb()
        "green" -> Color(0xFFF44336).toArgb()
        "cyan" -> OrangePrimary.toArgb()
        "indigo" -> OrangePrimary.toArgb()
        "pink" -> Color(0xFFD32F2F).toArgb()
        "lime" -> Color(0xFFE53935).toArgb()
        else -> OrangePrimary.toArgb()
    }
