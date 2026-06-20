package com.bernaferari.renetguard

import android.os.Build
import android.service.quicksettings.Tile
import android.util.Log
import androidx.annotation.RequiresApi
import com.bernaferari.renetguard.data.PreferenceKeys
import com.bernaferari.renetguard.data.preferences

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileLockdown : PreferenceTileService() {
    override val logTag: String = TAG
    override val watchedKeys: Set<String> = setOf(PreferenceKeys.LOCKDOWN)

    override fun updateTileState() {
        val lockdown = applicationContext.preferences().getBoolean(PreferenceKeys.LOCKDOWN, false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (lockdown) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = lockIcon()
            tile.updateTile()
        }
    }

    override fun onClick() {
        Log.i(TAG, "Click")
        val enabled = !applicationContext.preferences().getBoolean(PreferenceKeys.LOCKDOWN, false)
        applicationContext.preferences().putBoolean(PreferenceKeys.LOCKDOWN, enabled)
        ServiceSinkhole.reload("tile", this, false)
        Widgets.updateLockdown(this)
    }

    companion object {
        private const val TAG = "NetGuard.TileLockdown"
    }
}