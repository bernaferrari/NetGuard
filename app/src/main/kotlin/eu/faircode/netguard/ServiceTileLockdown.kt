package eu.faircode.netguard

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import eu.faircode.netguard.data.Prefs

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileLockdown : TileService() {
    private var removeListener: (() -> Unit)? = null

    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        removeListener = Prefs.addListener { key ->
            if (key == "lockdown") update()
        }
        update()
    }

    private fun update() {
        val lockdown = Prefs.getBoolean("lockdown", false)
            val tile = qsTile
        if (tile != null) {
            tile.state = if (lockdown) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = this.lockIcon()
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
        val enabled = !Prefs.getBoolean("lockdown", false)
        Prefs.putBoolean("lockdown", enabled)
        ServiceSinkhole.reload("tile", this, false)
        Widgets.updateLockdown(this)
    }

    companion object {
        private const val TAG = "NetGuard.TileLockdown"
    }
}
