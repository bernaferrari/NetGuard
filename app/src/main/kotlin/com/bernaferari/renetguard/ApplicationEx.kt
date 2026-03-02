package com.bernaferari.renetguard

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.HiltAndroidApp
import com.bernaferari.renetguard.data.Prefs

@HiltAndroidApp
class ApplicationEx : Application() {
    private var prevHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(
            TAG,
            "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this)
        )
        Prefs.init(this)
        WorkScheduler.scheduleHousekeeping(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notifications.ensureChannels(this)
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

                            val dark = Prefs.getBoolean("dark_theme", false)
                            content.setBackgroundColor(if (dark) Color.parseColor("#ff121212") else Color.WHITE)

                            val actionBarHeight = Util.dips2pixels(56, activity)
                            val decor = activity.window.decorView
                            WindowCompat.getInsetsController(activity.window, decor).apply {
                                isAppearanceLightStatusBars = false
                                isAppearanceLightNavigationBars = !dark
                            }
                            v.setPadding(
                                bars.left,
                                bars.top + actionBarHeight,
                                bars.right,
                                bars.bottom
                            )

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

    companion object {
        private const val TAG = "NetGuard.App"
    }
}
