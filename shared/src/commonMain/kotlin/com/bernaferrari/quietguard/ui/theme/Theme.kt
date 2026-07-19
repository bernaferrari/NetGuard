@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.quietguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.platform.PlatformContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.koin.compose.koinInject

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

val MaterialTheme.motion: Motion
    @Composable
    @ReadOnlyComposable
    get() = LocalMotion.current

data class ThemeSwatchOption(
    val id: String,
    val seedColor: Color?,
)

/** Settings swatches ordered warm → cool by hue. [seedColor] is null for the dynamic theme. */
val ThemeSwatchOptions: List<ThemeSwatchOption> =
    listOf(
        ThemeSwatchOption(id = "dynamic", seedColor = null),
        ThemeSwatchOption(id = "orange", seedColor = OrangePrimary),
        ThemeSwatchOption(id = "amber", seedColor = AmberPrimary),
        ThemeSwatchOption(id = "lime", seedColor = LimePrimary),
        ThemeSwatchOption(id = "green", seedColor = GreenPrimary),
        ThemeSwatchOption(id = "teal", seedColor = Teal500),
        ThemeSwatchOption(id = "cyan", seedColor = CyanPrimary),
        ThemeSwatchOption(id = "blue", seedColor = BluePrimary),
        ThemeSwatchOption(id = "indigo", seedColor = IndigoPrimary),
        ThemeSwatchOption(id = "purple", seedColor = PurplePrimary),
        ThemeSwatchOption(id = "pink", seedColor = PinkPrimary),
    )

/** Fallback swatch color when the dynamic theme picker is shown but unavailable. */
val DynamicThemeSwatchColor: Color = Teal500

/** Swatches visible on the current platform (dynamic is Android-only). */
fun themeSwatchOptions(): List<ThemeSwatchOption> =
    if (supportsDynamicTheme()) {
        ThemeSwatchOptions
    } else {
        ThemeSwatchOptions.filter { it.id != "dynamic" }
    }

fun resolveThemeName(themeName: String): String =
    if (themeName == "dynamic" && !supportsDynamicTheme()) THEME_DEFAULT else themeName

private val ThemeSeeds =
    mapOf(
        "teal" to Teal500,
        "blue" to BluePrimary,
        "purple" to PurplePrimary,
        "amber" to AmberPrimary,
        "orange" to OrangePrimary,
        "green" to GreenPrimary,
        "cyan" to CyanPrimary,
        "indigo" to IndigoPrimary,
        "pink" to PinkPrimary,
        "lime" to LimePrimary,
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
    val seedColor = ThemeSeeds[themeName] ?: Teal500
    val colorScheme =
        if (dynamicColor && themeName == "dynamic") {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )
        } else {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )
        }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalMotion provides Motion(reducedMotion = PlatformContext.prefersReducedMotion()),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

@Composable
fun NetGuardAppTheme(content: @Composable () -> Unit) {
    val preferencesRepository: PreferencesRepository = koinInject()
    val prefsState by preferencesRepository.data.collectAsState()
    val prefs = prefsState
    val appearance = AppearanceMode.from(prefs[stringPreferencesKey("appearance")])
    val darkTheme =
        when (appearance) {
            AppearanceMode.Light -> false
            AppearanceMode.Dark -> true
            AppearanceMode.Auto -> isSystemInDarkTheme()
            null ->
                if (prefs.asMap().containsKey(booleanPreferencesKey("dark_theme"))) {
                    prefs[booleanPreferencesKey("dark_theme")] ?: false
                } else {
                    isSystemInDarkTheme()
                }
        }
    val themeName = resolveThemeName(prefs[stringPreferencesKey("theme")] ?: THEME_DEFAULT)
    NetGuardTheme(
        darkTheme = darkTheme,
        themeName = themeName,
        dynamicColor = themeName == "dynamic",
        content = content,
    )
}
