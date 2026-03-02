package com.bernaferari.renetguard

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bernaferari.renetguard.data.Prefs

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileGraph : TileService() {
    private var removeListener: (() -> Unit)? = null

    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        removeListener = Prefs.addListener { key ->
            if (key == "show_stats") update()
        }
        update()
    }

    private fun update() {
        val stats = Prefs.getBoolean("show_stats", false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (stats) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = this.equalizerIcon()
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

        val stats = !Prefs.getBoolean("show_stats", false)
        if (stats && !IAB.isPurchased(ActivityPro.SKU_SPEED, this)) {
            Toast.makeText(this, R.string.title_pro_feature, Toast.LENGTH_SHORT).show()
        } else {
            Prefs.putBoolean("show_stats", stats)
        }
        ServiceSinkhole.reloadStats("tile", this)
    }

    companion object {
        private const val TAG = "NetGuard.TileGraph"
    }
}
