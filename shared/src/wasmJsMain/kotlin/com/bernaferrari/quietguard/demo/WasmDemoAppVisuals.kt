package com.bernaferrari.quietguard.demo

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols





import androidx.compose.ui.graphics.Color

import com.bernaferrari.quietguard.ui.icons.MaterialIcon
internal data class WasmDemoAppVisual(
    val backgroundColor: Color,
    val icon: MaterialIcon? = null,
    val letter: String? = null,
    val iconTint: Color = Color.White,
)

internal val wasmDemoAppVisuals: Map<String, WasmDemoAppVisual> =
    mapOf(
        "com.android.chrome" to WasmDemoAppVisual(Color(0xFF4285F4), icon = MaterialSymbols.Filled.Language),
        "com.google.android.gm" to WasmDemoAppVisual(Color(0xFFEA4335), icon = MaterialSymbols.Filled.Email),
        "com.spotify.music" to WasmDemoAppVisual(Color(0xFF1DB954), icon = MaterialSymbols.Filled.MusicNote),
        "com.instagram.android" to WasmDemoAppVisual(Color(0xFFE1306C), icon = MaterialSymbols.Filled.CameraAlt),
        "com.whatsapp" to WasmDemoAppVisual(Color(0xFF25D366), icon = MaterialSymbols.AutoMirrored.Filled.Chat),
        "com.google.android.youtube" to WasmDemoAppVisual(Color(0xFFFF0000), icon = MaterialSymbols.Filled.PlayArrow),
        "com.twitter.android" to WasmDemoAppVisual(Color(0xFF000000), letter = "𝕏"),
        "com.facebook.katana" to WasmDemoAppVisual(Color(0xFF1877F2), icon = MaterialSymbols.Filled.Group),
        "com.google.android.apps.maps" to WasmDemoAppVisual(Color(0xFF34A853), icon = MaterialSymbols.Filled.Map),
        "com.amazon.mShop.android.shopping" to WasmDemoAppVisual(Color(0xFFFF9900), icon = MaterialSymbols.Filled.ShoppingCart),
        "com.netflix.mediaclient" to WasmDemoAppVisual(Color(0xFFE50914), icon = MaterialSymbols.Filled.Movie),
        "com.discord" to WasmDemoAppVisual(Color(0xFF5865F2), icon = MaterialSymbols.Filled.Headset),
        "com.android.vending" to WasmDemoAppVisual(Color(0xFF01875F), icon = MaterialSymbols.Filled.Shop),
        "com.google.android.gms" to WasmDemoAppVisual(Color(0xFF5F6368), icon = MaterialSymbols.Filled.Extension),
        "com.reddit.frontpage" to WasmDemoAppVisual(Color(0xFFFF4500), icon = MaterialSymbols.Filled.Forum),
        "org.telegram.messenger" to WasmDemoAppVisual(Color(0xFF2AABEE), icon = MaterialSymbols.AutoMirrored.Filled.Send),
        "com.zhiliaoapp.musically" to WasmDemoAppVisual(Color(0xFF010101), icon = MaterialSymbols.Filled.MusicVideo, iconTint = Color(0xFFFE2C55)),
        "com.Slack" to WasmDemoAppVisual(Color(0xFF4A154B), icon = MaterialSymbols.Filled.Work),
        "com.linkedin.android" to WasmDemoAppVisual(Color(0xFF0A66C2), icon = MaterialSymbols.Filled.BusinessCenter),
        "org.thoughtcrime.securesms" to WasmDemoAppVisual(Color(0xFF3A76F0), icon = MaterialSymbols.Filled.Lock),
        "com.google.android.apps.photos" to WasmDemoAppVisual(Color(0xFFFBBC04), icon = MaterialSymbols.Filled.PhotoLibrary, iconTint = Color(0xFF202124)),
        "com.google.android.apps.docs" to WasmDemoAppVisual(Color(0xFF0F9D58), icon = MaterialSymbols.Filled.Cloud),
        "com.ubercab" to WasmDemoAppVisual(Color(0xFF000000), icon = MaterialSymbols.Filled.LocalTaxi),
        "com.chase.sig.android" to WasmDemoAppVisual(Color(0xFF117ACA), icon = MaterialSymbols.Filled.AccountBalance),
    )

internal fun wasmDemoAppVisual(
    packageName: String?,
    displayName: String?,
): WasmDemoAppVisual? {
    packageName?.let { wasmDemoAppVisuals[it] }?.let { return it }
    return displayName
        ?.let { name ->
            wasmDemoApps.values
                .firstOrNull { it.label.equals(name, ignoreCase = true) }
                ?.packageName
                ?.let { wasmDemoAppVisuals[it] }
        }
}

internal fun wasmDemoFallbackVisual(label: String): WasmDemoAppVisual {
    val palette =
        listOf(
            Color(0xFF5C6BC0),
            Color(0xFF26A69A),
            Color(0xFFEF5350),
            Color(0xFFAB47BC),
            Color(0xFF42A5F5),
            Color(0xFFFF7043),
            Color(0xFF66BB6A),
            Color(0xFF8D6E63),
        )
    val index = label.fold(0) { acc, char -> acc + char.code } % palette.size
    val letter = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    return WasmDemoAppVisual(backgroundColor = palette[index], letter = letter)
}
