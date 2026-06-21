package com.bernaferari.renetguard.demo

import com.bernaferari.renetguard.platform.AppDisplayInfo

internal val wasmDemoApps: Map<Int, AppDisplayInfo> =
    mapOf(
        10101 to AppDisplayInfo("Chrome", "com.android.chrome"),
        10102 to AppDisplayInfo("Gmail", "com.google.android.gm"),
        10103 to AppDisplayInfo("Spotify", "com.spotify.music"),
        10104 to AppDisplayInfo("Instagram", "com.instagram.android"),
        10105 to AppDisplayInfo("WhatsApp", "com.whatsapp"),
        10106 to AppDisplayInfo("YouTube", "com.google.android.youtube"),
        10107 to AppDisplayInfo("X", "com.twitter.android"),
        10108 to AppDisplayInfo("Facebook", "com.facebook.katana"),
        10109 to AppDisplayInfo("Maps", "com.google.android.apps.maps"),
        10110 to AppDisplayInfo("Amazon", "com.amazon.mShop.android.shopping"),
        10111 to AppDisplayInfo("Netflix", "com.netflix.mediaclient"),
        10112 to AppDisplayInfo("Discord", "com.discord"),
        10001 to AppDisplayInfo("Play Store", "com.android.vending"),
        10002 to AppDisplayInfo("Google Play services", "com.google.android.gms"),
    )

internal val wasmSystemPackages: Set<String> =
    setOf(
        "com.android.vending",
        "com.google.android.gms",
    )