package eu.faircode.netguard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import eu.faircode.netguard.data.Prefs
import java.util.Date

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileMain : TileService() {
    private var removeListener: (() -> Unit)? = null

    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        removeListener = Prefs.addListener { key ->
            if (key == "enabled") update()
        }
        update()
    }

    private fun update() {
        val enabled = Prefs.getBoolean("enabled", false)
            val tile = qsTile
        if (tile != null) {
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = this.securityIcon()
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        Log.i(TAG, "Stop listening")
        removeListener?.invoke()
        removeListener = null
    }

    override fun onClick() {
        Log.i(TAG, "Click")

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(WidgetAdmin.INTENT_ON).setPackage(packageName)
        val pi = PendingIntentCompat.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        am.cancel(pi)

        val enabled = !Prefs.getBoolean("enabled", false)
        Prefs.putBoolean("enabled", enabled)
        if (enabled) {
            ServiceSinkhole.start("tile", this)
        } else {
            ServiceSinkhole.stop("tile", this, false)

            val auto = Prefs.getString("auto_enable", "0")?.toIntOrNull() ?: 0
            if (auto > 0) {
                Log.i(TAG, "Scheduling enabled after minutes=$auto")
                val trigger = Date().time + auto * 60 * 1000L
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                }
            }
        }
        Widgets.updateFirewall(this)
    }

    companion object {
        private const val TAG = "NetGuard.TileMain"
    }
}
