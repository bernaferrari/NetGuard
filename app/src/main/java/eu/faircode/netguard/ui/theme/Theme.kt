package eu.faircode.netguard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

@Composable
fun NetGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        if (dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColors else LightColors
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
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
