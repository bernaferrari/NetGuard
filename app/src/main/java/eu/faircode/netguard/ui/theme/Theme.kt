package eu.faircode.netguard.ui.theme

import android.app.Activity
import android.app.ActivityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.faircode.netguard.data.Prefs

private val LightColors =
    lightColorScheme(
        primary = Blue500,
        onPrimary = Color.White,
        primaryContainer = Blue200,
        onPrimaryContainer = Slate900,
        secondary = Mint300,
        onSecondary = Slate900,
        background = Slate50,
        onBackground = Slate900,
        surface = Color.White,
        onSurface = Slate900,
        surfaceVariant = Slate100,
        onSurfaceVariant = Slate800,
    )

private val DarkColors =
    darkColorScheme(
        primary = Blue200,
        onPrimary = Slate900,
        primaryContainer = Blue700,
        onPrimaryContainer = Slate100,
        secondary = Mint300,
        onSecondary = Slate900,
        background = Slate900,
        onBackground = Slate100,
        surface = Slate800,
        onSurface = Slate100,
        surfaceVariant = Slate800,
        onSurfaceVariant = Slate100,
    )

private data class ThemeAccent(
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
)

private val ThemeAccents =
    mapOf(
        "teal" to ThemeAccent(Teal500, Teal700, TealAccent),
        "blue" to ThemeAccent(BluePrimary, BluePrimaryDark, BlueAccent),
        "purple" to ThemeAccent(PurplePrimary, PurplePrimaryDark, PurpleAccent),
        "amber" to ThemeAccent(AmberPrimary, AmberPrimaryDark, AmberAccent),
        "orange" to ThemeAccent(OrangePrimary, OrangePrimaryDark, OrangeAccent),
        "green" to ThemeAccent(GreenPrimary, GreenPrimaryDark, GreenAccent),
    )

private fun Color.blendWith(other: Color, ratio: Float): Color {
    val clamped = ratio.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * clamped,
        green = green + (other.green - green) * clamped,
        blue = blue + (other.blue - blue) * clamped,
        alpha = alpha + (other.alpha - alpha) * clamped,
    )
}

private fun onColor(background: Color): Color = if (background.luminance() > 0.5f) Slate900 else Color.White

private fun themedScheme(themeName: String, darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    val base = if (darkTheme) DarkColors else LightColors
    val accent = ThemeAccents[themeName] ?: ThemeAccents.getValue("teal")
    val primary = if (darkTheme) accent.primaryDark else accent.primary
    val primaryContainer =
        if (darkTheme) {
            primary.blendWith(Slate900, 0.3f)
        } else {
            primary.blendWith(Color.White, 0.7f)
        }
    val secondary = accent.secondary
    val secondaryContainer =
        if (darkTheme) {
            secondary.blendWith(Slate900, 0.35f)
        } else {
            secondary.blendWith(Color.White, 0.7f)
        }
    return base.copy(
        primary = primary,
        onPrimary = onColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onColor(primaryContainer),
        secondary = secondary,
        onSecondary = onColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onColor(secondaryContainer),
    )
}

@Composable
fun NetGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeName: String = "teal",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic =
        dynamicColor &&
            themeName == "dynamic" &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val colorScheme =
        if (useDynamic) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            themedScheme(themeName, darkTheme)
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            val window = activity.window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            val taskDescription =
                ActivityManager.TaskDescription.Builder()
                    .setPrimaryColor(colorScheme.primary.toArgb())
                    .build()
            activity.setTaskDescription(taskDescription)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

@Composable
fun NetGuardThemeFromPrefs(content: @Composable () -> Unit) {
    val prefsState = Prefs.data.collectAsState()
    val prefs = prefsState.value
    val darkTheme =
        if (prefs.asMap().containsKey(booleanPreferencesKey("dark_theme"))) {
            prefs[booleanPreferencesKey("dark_theme")] ?: false
        } else {
            isSystemInDarkTheme()
        }
    val themeName = prefs[stringPreferencesKey("theme")] ?: "teal"
    NetGuardTheme(
        darkTheme = darkTheme,
        themeName = themeName,
        content = content,
    )
}
