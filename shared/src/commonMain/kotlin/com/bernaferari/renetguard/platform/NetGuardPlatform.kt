package com.bernaferari.renetguard.platform

interface FirewallBridge {
    fun reload(reason: String, userInteraction: Boolean = false)

    fun reloadStats(reason: String)

    fun checkForUpdateNow(reason: String)

    val updateCheckAction: String

    val extraUpdateCheckStatus: String

    val extraUpdateCheckVersion: String
}

interface WidgetBridge {
    fun updateFirewall()

    fun updateAll()

    fun updateLockdown()
}

interface HostsBridge {
    val downloadHostsAction: String

    fun startHostsDownload()
}

interface WorkSchedulerBridge {
    fun scheduleWatchdog(intervalMinutes: Int, enabled: Boolean)
}

interface ProFeaturesBridge {
    val logSku: String

    fun isPurchased(sku: String): Boolean

    fun openProScreen()
}

interface UiHelperBridge {
    fun canNotify(): Boolean

    fun areNotificationsEnabled(): Boolean

    fun getApplicationNames(uid: Int): List<String>

    fun getProtocolName(protocol: Int, version: Int, brief: Boolean): String

    fun getPackageForUid(uid: Int): String?
}

interface RuleHelperBridge {
    fun isSystem(packageName: String): Boolean

    fun hasInternet(packageName: String): Boolean

    fun isEnabled(packageName: String): Boolean
}

object NetGuardPlatform {
    lateinit var firewall: FirewallBridge
    lateinit var widgets: WidgetBridge
    lateinit var hosts: HostsBridge
    lateinit var workScheduler: WorkSchedulerBridge
    lateinit var proFeatures: ProFeaturesBridge
    lateinit var uiHelpers: UiHelperBridge
    lateinit var ruleHelpers: RuleHelperBridge
}

expect object PlatformContext {
    fun isAndroid(): Boolean

    fun isDemoMode(): Boolean
}

expect fun onDemoFirewallToggled(enabled: Boolean)

expect fun requestNotificationPermission(onResult: (Boolean) -> Unit)

expect fun openNotificationSettings()

expect fun openUrl(url: String)

expect fun launchApp(packageName: String): Boolean

expect fun openAppDetails(packageName: String)

expect fun showToast(message: String)

expect fun currentTimeMillis(): Long