package com.bernaferari.renetguard

import android.service.quicksettings.TileService
import android.util.Log
import com.bernaferari.renetguard.data.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick-settings tile that reacts to [com.bernaferari.renetguard.data.PreferencesRepository.changes].
 */
abstract class PreferenceTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null

    protected abstract val logTag: String
    protected abstract val watchedKeys: Set<String>

    protected abstract fun updateTileState()

    override fun onStartListening() {
        Log.i(logTag, "Start listening")
        watchJob =
            scope.launch {
                applicationContext.preferences().changes.collect { changed ->
                    if (changed.any { it in watchedKeys }) {
                        updateTileState()
                    }
                }
            }
        updateTileState()
    }

    override fun onStopListening() {
        Log.i(logTag, "Stop listening")
        watchJob?.cancel()
        watchJob = null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}