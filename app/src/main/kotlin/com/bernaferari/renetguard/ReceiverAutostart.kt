package com.bernaferari.renetguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.data.preferences

open class ReceiverAutostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        val action = intent?.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            try {
                upgrade(true, context)

                if (context.preferences().getBoolean("enabled", false)) {
                    ServiceSinkhole.start("receiver", context)
                } else if (context.preferences().getBoolean("show_stats", false)) {
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
                val oldVersion = context.preferences().getInt("version", -1)
                val newVersion = Util.getSelfVersionCode(context)
                if (oldVersion == newVersion) return
                Log.i(TAG, "Upgrading from version $oldVersion to $newVersion")

                if (initialized) {
                    if (oldVersion < 38) {
                        Log.i(TAG, "Converting screen wifi/mobile")
                        val unusedDefault = context.preferences().getBoolean("unused", false)
                        context.preferences().putBoolean("screen_wifi", unusedDefault)
                        context.preferences().putBoolean("screen_other", unusedDefault)
                        context.preferences().remove("unused")

                        val prefix = "unused_"
                        context.preferences().keysWithPrefix("unused").forEach { key ->
                            val raw = key.removePrefix(prefix)
                            val value = context.preferences().getBoolean(key, false)
                            context.preferences().putBoolean(PreferencesRepository.namespaced("screen_wifi", raw), value)
                            context.preferences().putBoolean(PreferencesRepository.namespaced("screen_other", raw), value)
                            context.preferences().remove(key)
                        }
                    } else if (oldVersion <= 2017032112) {
                        context.preferences().remove("ip6")
                    }
                } else {
                    Log.i(TAG, "Initializing sdk=" + Build.VERSION.SDK_INT)
                    context.preferences().putBoolean("filter_udp", true)
                    context.preferences().putBoolean("whitelist_wifi", false)
                    context.preferences().putBoolean("whitelist_other", false)
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        context.preferences().putBoolean("filter", true)
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    context.preferences().putBoolean("filter", true)
                }

                if (!Util.canFilter(context)) {
                    context.preferences().putBoolean("log_app", false)
                    context.preferences().putBoolean("filter", false)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.preferences().remove("show_top")
                    if ("data" == context.preferences().getString("sort", "name")) {
                        context.preferences().remove("sort")
                    }
                }

                if (Util.isPlayStoreInstall(context)) {
                    context.preferences().remove("update_check")
                    context.preferences().remove("use_hosts")
                    context.preferences().remove("hosts_url")
                }

                if (!Util.isDebuggable(context)) {
                    context.preferences().remove("loglevel")
                }

                context.preferences().putInt("version", newVersion)
            }
        }
    }
}
