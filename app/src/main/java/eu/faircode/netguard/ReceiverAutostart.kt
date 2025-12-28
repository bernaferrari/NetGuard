package eu.faircode.netguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager

open class ReceiverAutostart : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        val action = intent?.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            try {
                upgrade(true, context)

                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                if (prefs.getBoolean("enabled", false)) {
                    ServiceSinkhole.start("receiver", context)
                } else if (prefs.getBoolean("show_stats", false)) {
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
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val oldVersion = prefs.getInt("version", -1)
                val newVersion = Util.getSelfVersionCode(context)
                if (oldVersion == newVersion) return
                Log.i(TAG, "Upgrading from version $oldVersion to $newVersion")

                val editor = prefs.edit()

                if (initialized) {
                    if (oldVersion < 38) {
                        Log.i(TAG, "Converting screen wifi/mobile")
                        editor.putBoolean("screen_wifi", prefs.getBoolean("unused", false))
                        editor.putBoolean("screen_other", prefs.getBoolean("unused", false))
                        editor.remove("unused")

                        val unused = context.getSharedPreferences("unused", Context.MODE_PRIVATE)
                        val screenWifi = context.getSharedPreferences("screen_wifi", Context.MODE_PRIVATE)
                        val screenOther = context.getSharedPreferences("screen_other", Context.MODE_PRIVATE)

                        val punused = unused.all
                        val editScreenWifi = screenWifi.edit()
                        val editScreenOther = screenOther.edit()
                        for ((key, value) in punused) {
                            if (value is Boolean) {
                                editScreenWifi.putBoolean(key, value)
                                editScreenOther.putBoolean(key, value)
                            }
                        }
                        editScreenWifi.apply()
                        editScreenOther.apply()
                    } else if (oldVersion <= 2017032112) {
                        editor.remove("ip6")
                    }
                } else {
                    Log.i(TAG, "Initializing sdk=" + Build.VERSION.SDK_INT)
                    editor.putBoolean("filter_udp", true)
                    editor.putBoolean("whitelist_wifi", false)
                    editor.putBoolean("whitelist_other", false)
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        editor.putBoolean("filter", true)
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    editor.putBoolean("filter", true)
                }

                if (!Util.canFilter(context)) {
                    editor.putBoolean("log_app", false)
                    editor.putBoolean("filter", false)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    editor.remove("show_top")
                    if ("data" == prefs.getString("sort", "name")) {
                        editor.remove("sort")
                    }
                }

                if (Util.isPlayStoreInstall(context)) {
                    editor.remove("update_check")
                    editor.remove("use_hosts")
                    editor.remove("hosts_url")
                }

                if (!Util.isDebuggable(context)) {
                    editor.remove("loglevel")
                }

                editor.putInt("version", newVersion)
                editor.apply()
            }
        }
    }
}
