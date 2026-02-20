@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package eu.faircode.netguard.ui.theme

import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import eu.faircode.netguard.data.Prefs

/**
 * Extension property for accessing spacing design tokens.
 */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

/**
 * Extension property for accessing motion/animation design tokens.
 */
val MaterialTheme.motion: Motion
    @Composable
    @ReadOnlyComposable
    get() = LocalMotion.current

private val ThemeSeeds =
    mapOf(
        "teal" to Teal500,
        "blue" to BluePrimary,
        "purple" to PurplePrimary,
        "amber" to AmberPrimary,
        "orange" to OrangePrimary,
        "green" to GreenPrimary,
        "dynamic" to Teal500,
    )

private enum class AppearanceMode(val value: String) {
    Auto("auto"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun from(value: String?): AppearanceMode? = entries.firstOrNull { it.value == value }
    }
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
        dynamicColor && themeName == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val seedColor = ThemeSeeds[themeName] ?: Teal500
    val colorScheme =
        if (useDynamic) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            rememberDynamicMaterialThemeState(
                seedColor = seedColor,
                style = PaletteStyle.Vibrant,
                isDark = darkTheme,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            ).colorScheme
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            val window = activity.window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            val taskDescription =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityManager.TaskDescription.Builder()
                        .setPrimaryColor(colorScheme.primary.toArgb())
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    ActivityManager.TaskDescription(null, null, colorScheme.primary.toArgb())
                }
            activity.setTaskDescription(taskDescription)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalMotion provides Motion(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

@Composable
fun NetGuardThemeFromPrefs(content: @Composable () -> Unit) {
    val prefsState = Prefs.data.collectAsState()
    val prefs = prefsState.value
    val appearance = AppearanceMode.from(prefs[stringPreferencesKey("appearance")])
    val darkTheme = when (appearance) {
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
        AppearanceMode.Auto ->
            isSystemInDarkTheme()

        null ->
            if (prefs.asMap().containsKey(booleanPreferencesKey("dark_theme"))) {
                prefs[booleanPreferencesKey("dark_theme")] ?: false
            } else {
                isSystemInDarkTheme()
            }
    }
    val themeName = prefs[stringPreferencesKey("theme")] ?: "teal"
    NetGuardTheme(
        darkTheme = darkTheme,
        themeName = themeName,
        dynamicColor = true,
        content = content,
    )
}
