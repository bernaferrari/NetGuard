package com.bernaferrari.quietguard.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.koin.core.context.GlobalContext

actual object PlatformContext {
    actual fun isAndroid(): Boolean = true

    actual fun isDemoMode(): Boolean = false

    actual fun prefersReducedMotion(): Boolean =
        Settings.Global.getFloat(
            appContext().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
}

actual fun onDemoFirewallToggled(enabled: Boolean) {}

actual fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
    onResult(NotificationManagerCompat.from(appContext()).areNotificationsEnabled())
}

actual fun openNotificationSettings() {
    val context = appContext()
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

actual fun openUrl(url: String) {
    val context = appContext()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

actual fun launchApp(packageName: String): Boolean {
    val context = appContext()
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    return true
}

actual fun openAppDetails(packageName: String) {
    val context = appContext()
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

actual fun showToast(message: String) {
    Toast.makeText(appContext(), message, Toast.LENGTH_LONG).show()
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private fun appContext(): Context = GlobalContext.get().get()
