package eu.faircode.netguard

import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileLockdown : TileService(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        update()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == "lockdown") update()
    }

    private fun update() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lockdown = prefs.getBoolean("lockdown", false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (lockdown) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(
                this,
                if (lockdown) R.drawable.ic_lock_outline_white_24dp else R.drawable.ic_lock_outline_white_24dp_60,
            )
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        Log.i(TAG, "Stop listening")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onClick() {
        Log.i(TAG, "Click")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putBoolean("lockdown", !prefs.getBoolean("lockdown", false)).apply()
        ServiceSinkhole.reload("tile", this, false)
        WidgetLockdown.updateWidgets(this)
    }

    companion object {
        private const val TAG = "NetGuard.TileLockdown"
    }
}
