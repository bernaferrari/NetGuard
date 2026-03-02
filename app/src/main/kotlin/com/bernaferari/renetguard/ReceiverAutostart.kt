package com.bernaferari.renetguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bernaferari.renetguard.data.Prefs

open class ReceiverAutostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        val action = intent?.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            try {
                upgrade(true, context)

                if (Prefs.getBoolean("enabled", false)) {
                    ServiceSinkhole.start("receiver", context)
                } else if (Prefs.getBoolean("show_stats", false)) {
                    ServiceSinkhole.run("receiver", context)
                }

                if (Util.isInteractive(context)) {
                    ServiceSinkhole.reloadStats("receiver", context)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }
    }

    companion object {
        private const val TAG = "NetGuard.Receiver"

        fun upgrade(initialized: Boolean, context: Context) {
            synchronized(context.applicationContext) {
                val oldVersion = Prefs.getInt("version", -1)
                val newVersion = Util.getSelfVersionCode(context)
                if (oldVersion == newVersion) return
                Log.i(TAG, "Upgrading from version $oldVersion to $newVersion")

                if (initialized) {
                    if (oldVersion < 38) {
                        Log.i(TAG, "Converting screen wifi/mobile")
                        val unusedDefault = Prefs.getBoolean("unused", false)
                        Prefs.putBoolean("screen_wifi", unusedDefault)
                        Prefs.putBoolean("screen_other", unusedDefault)
                        Prefs.remove("unused")

                        val prefix = "unused_"
                        Prefs.keysWithPrefix("unused").forEach { key ->
                            val raw = key.removePrefix(prefix)
                            val value = Prefs.getBoolean(key, false)
                            Prefs.putBoolean(Prefs.namespaced("screen_wifi", raw), value)
                            Prefs.putBoolean(Prefs.namespaced("screen_other", raw), value)
                            Prefs.remove(key)
                        }
                    } else if (oldVersion <= 2017032112) {
                        Prefs.remove("ip6")
                    }
                } else {
                    Log.i(TAG, "Initializing sdk=" + Build.VERSION.SDK_INT)
                    Prefs.putBoolean("filter_udp", true)
                    Prefs.putBoolean("whitelist_wifi", false)
                    Prefs.putBoolean("whitelist_other", false)
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        Prefs.putBoolean("filter", true)
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Prefs.putBoolean("filter", true)
                }

                if (!Util.canFilter(context)) {
                    Prefs.putBoolean("log_app", false)
                    Prefs.putBoolean("filter", false)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Prefs.remove("show_top")
                    if ("data" == Prefs.getString("sort", "name")) {
                        Prefs.remove("sort")
                    }
                }

                if (Util.isPlayStoreInstall(context)) {
                    Prefs.remove("update_check")
                    Prefs.remove("use_hosts")
                    Prefs.remove("hosts_url")
                }

                if (!Util.isDebuggable(context)) {
                    Prefs.remove("loglevel")
                }

                Prefs.putInt("version", newVersion)
            }
        }
    }
}
