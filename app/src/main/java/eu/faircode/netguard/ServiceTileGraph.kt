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
class ServiceTileGraph : TileService(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onStartListening() {
        Log.i(TAG, "Start listening")
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        update()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == "show_stats") update()
    }

    private fun update() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val stats = prefs.getBoolean("show_stats", false)
        val tile = qsTile
        if (tile != null) {
            tile.state = if (stats) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(
                this,
                if (stats) R.drawable.ic_equalizer_white_24dp else R.drawable.ic_equalizer_white_24dp_60,
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
        val stats = !prefs.getBoolean("show_stats", false)
        if (stats && !IAB.isPurchased(ActivityPro.SKU_SPEED, this)) {
            Toast.makeText(this, R.string.title_pro_feature, Toast.LENGTH_SHORT).show()
        } else {
            prefs.edit().putBoolean("show_stats", stats).apply()
        }
        ServiceSinkhole.reloadStats("tile", this)
    }

    companion object {
        private const val TAG = "NetGuard.TileGraph"
    }
}
