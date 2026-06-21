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
class ServiceTileGraph : PreferenceTileService() {
    override val logTag: String = TAG
    override val watchedKeys: Set<String> = setOf(PreferenceKeys.SHOW_STATS)

    override fun updateTileState() {
        val stats = applicationContext.preferences().getBoolean(PreferenceKeys.SHOW_STATS, false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (stats) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = equalizerIcon()
            tile.updateTile()
        }
    }

    override fun onClick() {
        Log.i(TAG, "Click")

        val stats = !applicationContext.preferences().getBoolean(PreferenceKeys.SHOW_STATS, false)
        if (stats && !IAB.isPurchased(ActivityPro.SKU_SPEED, this)) {
            Toast.makeText(this, R.string.title_pro_feature, Toast.LENGTH_SHORT).show()
        } else {
            applicationContext.preferences().putBoolean(PreferenceKeys.SHOW_STATS, stats)
        }
        ServiceSinkhole.reloadStats("tile", this)
    }

    companion object {
        private const val TAG = "NetGuard.TileGraph"
    }
}