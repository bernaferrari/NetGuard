package com.bernaferrari.quietguard.service

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

/** Owns statistics refresh lifecycle and scheduling on its dedicated handler thread. */
internal class SinkholeStatsHandler(
    looper: Looper,
    private val isEnabled: () -> Boolean,
    private val onStarted: () -> Unit,
    private val onStopped: () -> Unit,
    private val onRefresh: () -> Long?,
) : Handler(looper) {
    private var active = false

    fun restart() {
        stop()
        start()
    }

    fun start() = sendEmptyMessage(MSG_START)

    fun stop() = sendEmptyMessage(MSG_STOP)

    override fun handleMessage(message: Message) {
        try {
            when (message.what) {
                MSG_START -> startInternal()
                MSG_STOP -> stopInternal()
                MSG_REFRESH -> refresh()
                else -> Log.e(TAG, "Unknown stats message=${message.what}")
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Statistics handler failed", error)
            stopInternal()
        }
    }

    private fun startInternal() {
        if (active || !isEnabled()) return
        active = true
        onStarted()
        refresh()
    }

    private fun stopInternal() {
        if (!active) return
        active = false
        removeMessages(MSG_REFRESH)
        onStopped()
    }

    private fun refresh() {
        if (!active) return
        val delayMs = onRefresh()
        if (delayMs == null) {
            stopInternal()
        } else {
            sendEmptyMessageDelayed(MSG_REFRESH, delayMs)
        }
    }

    private companion object {
        const val TAG = "NetGuard.Stats"
        const val MSG_START = 1
        const val MSG_STOP = 2
        const val MSG_REFRESH = 3
    }
}
