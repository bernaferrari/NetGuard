package com.bernaferari.renetguard.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.bernaferari.renetguard.ActivityPro
import com.bernaferari.renetguard.IAB
import com.bernaferari.renetguard.ServiceExternal
import com.bernaferari.renetguard.ServiceSinkhole
import com.bernaferari.renetguard.Util
import com.bernaferari.renetguard.Widgets
import com.bernaferari.renetguard.WorkScheduler
import org.koin.core.context.GlobalContext

fun installNetGuardPlatformBindings() {
    val context: Context = GlobalContext.get().get()

    NetGuardPlatform.firewall =
        object : FirewallBridge {
            override fun reload(reason: String, userInteraction: Boolean) {
                ServiceSinkhole.reload(reason, context, userInteraction)
            }

            override fun reloadStats(reason: String) {
                ServiceSinkhole.reloadStats(reason, context)
            }

            override fun checkForUpdateNow(reason: String) {
                ServiceSinkhole.checkForUpdateNow(reason, context)
            }

            override val updateCheckAction: String = ServiceSinkhole.ACTION_UPDATE_CHECK_RESULT

            override val extraUpdateCheckStatus: String = ServiceSinkhole.EXTRA_UPDATE_CHECK_STATUS

            override val extraUpdateCheckVersion: String = ServiceSinkhole.EXTRA_UPDATE_CHECK_VERSION
        }

    NetGuardPlatform.widgets =
        object : WidgetBridge {
            override fun updateFirewall() = Widgets.updateFirewall(context)

            override fun updateAll() = Widgets.updateAll(context)

            override fun updateLockdown() = Widgets.updateLockdown(context)
        }

    NetGuardPlatform.hosts =
        object : HostsBridge {
            override val downloadHostsAction: String = ServiceExternal.ACTION_DOWNLOAD_HOSTS_FILE

            override fun startHostsDownload() {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ServiceExternal::class.java).apply {
                        action = downloadHostsAction
                    },
                )
            }
        }

    NetGuardPlatform.workScheduler =
        object : WorkSchedulerBridge {
            override fun scheduleWatchdog(intervalMinutes: Int, enabled: Boolean) {
                WorkScheduler.scheduleWatchdog(context, intervalMinutes, enabled)
            }
        }

    NetGuardPlatform.proFeatures =
        object : ProFeaturesBridge {
            override val logSku: String = ActivityPro.SKU_LOG

            override fun isPurchased(sku: String): Boolean = IAB.isPurchased(sku, context)

            override fun openProScreen() {
                context.startActivity(Intent(context, ActivityPro::class.java))
            }
        }

    NetGuardPlatform.ruleHelpers =
        object : RuleHelperBridge {
            override fun isSystem(packageName: String): Boolean = Util.isSystem(packageName, context)

            override fun hasInternet(packageName: String): Boolean = Util.hasInternet(packageName, context)

            override fun isEnabled(packageName: String): Boolean {
                val pm = context.packageManager
                val info = pm.getPackageInfo(packageName, 0)
                return Util.isEnabled(info, context)
            }
        }

    NetGuardPlatform.uiHelpers =
        object : UiHelperBridge {
            override fun canNotify(): Boolean = Util.canNotify(context)

            override fun areNotificationsEnabled(): Boolean =
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

            override fun getApplicationNames(uid: Int): List<String> = Util.getApplicationNames(uid, context)

            override fun getProtocolName(protocol: Int, version: Int, brief: Boolean): String =
                Util.getProtocolName(protocol, version, brief)

            override fun getPackageForUid(uid: Int): String? =
                context.packageManager.getPackagesForUid(uid)?.firstOrNull()
        }
}