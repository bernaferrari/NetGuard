package eu.faircode.netguard

import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager

@RequiresApi(Build.VERSION_CODES.N)
class ServiceTileFilter : TileService(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        update()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == "filter") update()
    }

    private fun update() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val filter = prefs.getBoolean("filter", false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (filter) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(
                this,
                if (filter) R.drawable.ic_filter_list_white_24dp else R.drawable.ic_filter_list_white_24dp_60,
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

        if (Util.canFilter(this)) {
            if (IAB.isPurchased(ActivityPro.SKU_FILTER, this)) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                prefs.edit().putBoolean("filter", !prefs.getBoolean("filter", false)).apply()
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
