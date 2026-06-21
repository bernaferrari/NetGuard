package com.bernaferari.renetguard

import com.bernaferari.renetguard.shared.R

import android.os.Build
import android.service.quicksettings.Tile
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bernaferari.renetguard.data.PreferenceKeys
import com.bernaferari.renetguard.data.preferences

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileFilter : PreferenceTileService() {
    override val logTag: String = TAG
    override val watchedKeys: Set<String> = setOf(PreferenceKeys.FILTER)

    override fun updateTileState() {
        val filter = applicationContext.preferences().getBoolean(PreferenceKeys.FILTER, false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (filter) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = filterListIcon()
            tile.updateTile()
        }
    }

    override fun onClick() {
        Log.i(TAG, "Click")

        if (Util.canFilter(this)) {
            if (IAB.isPurchased(ActivityPro.SKU_FILTER, this)) {
                val enabled = !applicationContext.preferences().getBoolean(PreferenceKeys.FILTER, false)
                applicationContext.preferences().putBoolean(PreferenceKeys.FILTER, enabled)
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