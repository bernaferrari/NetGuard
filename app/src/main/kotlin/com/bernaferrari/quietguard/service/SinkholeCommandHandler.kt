package com.bernaferrari.quietguard.service

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

/** Serializes service commands and guarantees balanced queue accounting and cleanup. */
internal class SinkholeCommandHandler(
    looper: Looper,
    private val commandCode: (Intent) -> Int?,
    private val handleCommand: (Intent) -> Unit,
    private val reportQueueSize: (Int) -> Unit,
    private val releaseWakeLock: () -> Unit,
) : Handler(looper) {
    private var queueSize = 0

    fun queue(intent: Intent): Boolean {
        val code = commandCode(intent)
        if (code == null) {
            Log.w(TAG, "Ignoring intent without a command: $intent")
            return false
        }
        synchronized(this) {
            queueSize++
            reportQueueSize(queueSize)
        }
        if (sendMessage(obtainMessage(code, intent))) return true

        synchronized(this) {
            queueSize--
            reportQueueSize(queueSize)
        }
        Log.w(TAG, "Command handler is no longer accepting messages")
        return false
    }

    override fun handleMessage(message: Message) {
        try {
            handleCommand(message.obj as Intent)
        } catch (error: Throwable) {
            Log.e(TAG, "Command failed", error)
        } finally {
            synchronized(this) {
                queueSize--
                reportQueueSize(queueSize)
            }
            try {
                releaseWakeLock()
            } catch (error: Throwable) {
                Log.e(TAG, "Could not release command wake lock", error)
            }
        }
    }

    private companion object {
        const val TAG = "NetGuard.Command"
    }
}
