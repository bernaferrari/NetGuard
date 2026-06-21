package com.bernaferari.renetguard.platform

import com.bernaferari.renetguard.demo.wasmDemoApps
import com.bernaferari.renetguard.demo.wasmSystemPackages

fun installWasmPlatformBindings() {
    NetGuardPlatform.firewall =
        object : FirewallBridge {
            override fun reload(reason: String, userInteraction: Boolean) {}

            override fun reloadStats(reason: String) {}

            override fun checkForUpdateNow(reason: String) {}

            override val updateCheckAction: String = "wasm.update.check"

            override val extraUpdateCheckStatus: String = "status"

            override val extraUpdateCheckVersion: String = "version"
        }

    NetGuardPlatform.widgets =
        object : WidgetBridge {
            override fun updateFirewall() {}

            override fun updateAll() {}

            override fun updateLockdown() {}
        }

    NetGuardPlatform.hosts =
        object : HostsBridge {
            override val downloadHostsAction: String = "wasm.download.hosts"

            override fun startHostsDownload() {}
        }

    NetGuardPlatform.workScheduler =
        object : WorkSchedulerBridge {
            override fun scheduleWatchdog(intervalMinutes: Int, enabled: Boolean) {}
        }

    NetGuardPlatform.proFeatures =
        object : ProFeaturesBridge {
            override val logSku: String = "log"

            override fun isPurchased(sku: String): Boolean = true

            override fun openProScreen() {}
        }

    NetGuardPlatform.ruleHelpers =
        object : RuleHelperBridge {
            override fun isSystem(packageName: String): Boolean = packageName in wasmSystemPackages

            override fun hasInternet(packageName: String): Boolean = true

            override fun isEnabled(packageName: String): Boolean = true
        }

    NetGuardPlatform.uiHelpers =
        object : UiHelperBridge {
            override fun canNotify(): Boolean = true

            override fun areNotificationsEnabled(): Boolean = true

            override fun getApplicationNames(uid: Int): List<String> =
                wasmDemoApps[uid]?.label?.let(::listOf) ?: listOf("UID $uid")

            override fun getProtocolName(protocol: Int, version: Int, brief: Boolean): String =
                when (protocol) {
                    6 -> "TCP"
                    17 -> "UDP"
                    else -> "Other"
                }

            override fun getPackageForUid(uid: Int): String? = wasmDemoApps[uid]?.packageName
        }
}