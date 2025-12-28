package eu.faircode.netguard

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApplicationEx : Application() {
    private var prevHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }

        prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            if (Util.ownFault(this, ex) && Util.isPlayStoreInstall(this)) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                prevHandler?.uncaughtException(thread, ex)
            } else {
                Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                System.exit(1)
            }
        }

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && false) {
                        val content = activity.findViewById<View>(android.R.id.content)
                        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
                            val bars =
                                insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars() or
                                        WindowInsetsCompat.Type.displayCutout() or
                                        WindowInsetsCompat.Type.ime(),
                                )

                            val tv = TypedValue()
                            activity.theme.resolveAttribute(R.attr.colorPrimaryDark, tv, true)

                            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                            val dark = prefs.getBoolean("dark_theme", false)

                            activity.window.decorView.setBackgroundColor(tv.data)
                            content.setBackgroundColor(if (dark) Color.parseColor("#ff121212") else Color.WHITE)

                            val actionBarHeight = Util.dips2pixels(56, activity)
                            val decor = activity.window.decorView
                            WindowCompat.getInsetsController(activity.window, decor).apply {
                                isAppearanceLightStatusBars = false
                                isAppearanceLightNavigationBars = !dark
                            }
                            v.setPadding(bars.left, bars.top + actionBarHeight, bars.right, bars.bottom)

                            insets
                        }
                    }
                }

                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            },
        )
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val foreground = NotificationChannel(
            "foreground",
            getString(R.string.channel_foreground),
            NotificationManager.IMPORTANCE_MIN,
        )
        foreground.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        nm.createNotificationChannel(foreground)

        val notify = NotificationChannel(
            "notify",
            getString(R.string.channel_notify),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notify.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        notify.setBypassDnd(true)
        nm.createNotificationChannel(notify)

        val access = NotificationChannel(
            "access",
            getString(R.string.channel_access),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        access.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        access.setBypassDnd(true)
        nm.createNotificationChannel(access)

        val malware = NotificationChannel(
            "malware",
            getString(R.string.setting_malware),
            NotificationManager.IMPORTANCE_HIGH,
        )
        malware.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        malware.setBypassDnd(true)
        nm.createNotificationChannel(malware)
    }

    companion object {
        private const val TAG = "NetGuard.App"
    }
}
