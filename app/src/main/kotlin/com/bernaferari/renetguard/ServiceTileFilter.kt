package com.bernaferari.renetguard

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bernaferari.renetguard.data.Prefs

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileFilter : TileService() {
    private var removeListener: (() -> Unit)? = null

    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        removeListener = Prefs.addListener { key ->
            if (key == "filter") update()
        }
        update()
    }

    private fun update() {
        val filter = Prefs.getBoolean("filter", false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (filter) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = this.filterListIcon()
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

        if (Util.canFilter(this)) {
            if (IAB.isPurchased(ActivityPro.SKU_FILTER, this)) {
                val enabled = !Prefs.getBoolean("filter", false)
                Prefs.putBoolean("filter", enabled)
                ServiceSinkhole.reload("tile", this, false)
            } else {
                Toast.makeText(this, R.string.title_pro_feature, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.msg_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "NetGuard.TileFilter"
    }
}
