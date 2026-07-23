package com.bernaferrari.quietguard.service

import com.bernaferrari.quietguard.*

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.bernaferrari.quietguard.data.PreferencesRepository

/** Serializes database-backed packet logging without retaining the service as an implicit owner. */
internal class SinkholeLogHandler(
    looper: Looper,
    private val context: Context,
    private val prefs: PreferencesRepository,
    private val shouldShowAccessNotification: (Int) -> Boolean,
    private val showAccessNotification: (Int) -> Unit,
) : Handler(looper) {
    private var queueSize = 0

    fun queue(packet: Packet, connection: Int, interactive: Boolean) {
        enqueue(MSG_PACKET) { msg ->
            msg.obj = packet
            msg.arg1 = connection
            msg.arg2 = if (interactive) 1 else 0
        }
    }

    fun account(usage: Usage) {
        enqueue(MSG_USAGE) { msg -> msg.obj = usage }
    }

    override fun handleMessage(msg: Message) {
        try {
            when (msg.what) {
                MSG_PACKET -> log(msg.obj as Packet, msg.arg1, msg.arg2 != 0)
                MSG_USAGE -> usage(msg.obj as Usage)
                else -> Log.e(TAG, "Unknown log message=${msg.what}")
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Log message failed", error)
        } finally {
            synchronized(this) { queueSize-- }
        }
    }

    private fun enqueue(what: Int, configure: (Message) -> Unit) {
        synchronized(this) {
            if (queueSize >= MAX_QUEUE_SIZE) {
                Log.w(TAG, "Log queue full")
                return
            }
            val message = obtainMessage(what)
            configure(message)
            if (sendMessage(message)) {
                queueSize++
            } else {
                Log.w(TAG, "Log handler is no longer accepting messages")
            }
        }
    }

    private fun log(packet: Packet, connection: Int, interactive: Boolean) {
        val database = DatabaseHelper.getInstance(context)
        val destination = packet.daddr ?: return
        val domainName = database.getQName(packet.uid, destination)

        if (!prefs.getBoolean("log", false)) return
        database.insertLog(packet, domainName, connection, interactive)

        if (
            packet.uid < 0 ||
            (packet.uid == 0 && (packet.protocol == TCP || packet.protocol == UDP) && packet.dport == DNS_PORT)
        ) {
            return
        }
        if (packet.protocol != TCP && packet.protocol != UDP) packet.dport = 0

        if (database.updateAccess(packet, domainName, -1) && shouldShowAccessNotification(packet.uid)) {
            showAccessNotification(packet.uid)
        }
    }

    private fun usage(usage: Usage) {
        if (usage.Uid < 0 || (usage.Uid == 0 && usage.Protocol == UDP && usage.DPort == DNS_PORT)) {
            return
        }
        if (
            !prefs.getBoolean("filter", false) ||
            !prefs.getBoolean("log", false) ||
            !prefs.getBoolean("track_usage", false)
        ) {
            return
        }

        val destination = usage.DAddr ?: return
        val database = DatabaseHelper.getInstance(context)
        val domainName = database.getQName(usage.Uid, destination)
        Log.i(TAG, "Usage account $usage dname=$domainName")
        database.updateUsage(usage, domainName)
    }

    private companion object {
        const val TAG = "NetGuard.Log"
        const val MSG_PACKET = 1
        const val MSG_USAGE = 2
        const val MAX_QUEUE_SIZE = 250
        const val TCP = 6
        const val UDP = 17
        const val DNS_PORT = 53
    }
}
