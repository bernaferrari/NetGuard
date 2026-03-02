package com.bernaferari.renetguard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.bernaferari.renetguard.data.Prefs
import java.util.Date

class WidgetAdmin : ReceiverAutostart() {
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(INTENT_ON).setPackage(context.packageName)
        val pi = PendingIntentCompat.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)
        if (INTENT_ON == intent?.action || INTENT_OFF == intent?.action) {
            am.cancel(pi)
        }

        val vs =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        if (vs != null && vs.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vs.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vs.vibrate(50)
            }
        }

        try {
            when (intent?.action) {
                INTENT_ON, INTENT_OFF -> {
                    val enabled = INTENT_ON == intent.action
                    Prefs.putBoolean("enabled", enabled)
                    if (enabled) {
                        ServiceSinkhole.start("widget", context)
                    } else {
                        ServiceSinkhole.stop("widget", context, false)
                    }
                    Widgets.updateFirewall(context)

                    val auto = Prefs.getString("auto_enable", "0")?.toIntOrNull() ?: 0
                    if (!enabled && auto > 0) {
                        Log.i(TAG, "Scheduling enabled after minutes=$auto")
                        val trigger = Date().time + auto * 60 * 1000L
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
                        } else {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                        }
                    }
                }

                INTENT_LOCKDOWN_ON, INTENT_LOCKDOWN_OFF -> {
                    val lockdown = INTENT_LOCKDOWN_ON == intent.action
                    Prefs.putBoolean("lockdown", lockdown)
                    ServiceSinkhole.reload("widget", context, false)
                    Widgets.updateLockdown(context)
                }
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    companion object {
        private const val TAG = "NetGuard.Widget"

        const val INTENT_ON = "com.bernaferari.renetguard.ON"
        const val INTENT_OFF = "com.bernaferari.renetguard.OFF"
        const val INTENT_LOCKDOWN_ON = "com.bernaferari.renetguard.LOCKDOWN_ON"
        const val INTENT_LOCKDOWN_OFF = "com.bernaferari.renetguard.LOCKDOWN_OFF"
    }
}
