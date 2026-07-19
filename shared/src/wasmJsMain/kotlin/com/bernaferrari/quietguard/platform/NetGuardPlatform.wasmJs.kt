package com.bernaferrari.quietguard.platform

actual object PlatformContext {
    actual fun isAndroid(): Boolean = false

    actual fun isDemoMode(): Boolean = true

    actual fun prefersReducedMotion(): Boolean =
        kotlinx.browser.window.matchMedia("(prefers-reduced-motion: reduce)").matches
}

actual fun onDemoFirewallToggled(enabled: Boolean) {
    NetGuardPlatform.firewall.reload(
        if (enabled) "demo start" else "demo stop",
        userInteraction = enabled,
    )
    NetGuardPlatform.widgets.updateFirewall()
}

actual fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
    onResult(true)
}

actual fun openNotificationSettings() {}

actual fun openUrl(url: String) {
    kotlinx.browser.window.open(url, "_blank")
}

actual fun launchApp(packageName: String): Boolean = false

actual fun openAppDetails(packageName: String) {}

actual fun showToast(message: String) {
    PlatformToast.show(message)
}

actual fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
