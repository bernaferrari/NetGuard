@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.bernaferari.renetguard

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

object LegacyTelephony {
    fun registerCallState(
        tm: TelephonyManager,
        onCallStateChanged: (Int) -> Unit,
    ): Any {
        val listener =
            object : PhoneStateListener() {
                @Deprecated("Deprecated in framework")
                override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                    onCallStateChanged(state)
                }
            }
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        return listener
    }

    fun unregisterCallState(
        tm: TelephonyManager,
        token: Any?,
    ) {
        val listener = token as? PhoneStateListener ?: return
        tm.listen(listener, PhoneStateListener.LISTEN_NONE)
    }

    fun registerDataConnection(
        tm: TelephonyManager,
        onDataConnectionStateChanged: (Int) -> Unit,
    ): Any {
        val listener =
            object : PhoneStateListener() {
                @Deprecated("Deprecated in framework")
                override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                    onDataConnectionStateChanged(state)
                }
            }
        tm.listen(listener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE)
        return listener
    }

    fun unregisterDataConnection(
        tm: TelephonyManager,
        token: Any?,
    ) {
        val listener = token as? PhoneStateListener ?: return
        tm.listen(listener, PhoneStateListener.LISTEN_NONE)
    }
}
