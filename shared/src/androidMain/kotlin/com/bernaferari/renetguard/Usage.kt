package com.bernaferari.renetguard

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class Usage {
    @JvmField
    var Time: Long = 0

    @JvmField
    var Version: Int = 0

    @JvmField
    var Protocol: Int = 0

    @JvmField
    var DAddr: String? = null

    @JvmField
    var DPort: Int = 0

    @JvmField
    var Uid: Int = 0

    @JvmField
    var Sent: Long = 0

    @JvmField
    var Received: Long = 0

    override fun toString(): String {
        return formatter.format(Date(Time).time) +
                " v" + Version + " p" + Protocol +
                " " + DAddr + "/" + DPort +
                " uid " + Uid +
                " out " + Sent + " in " + Received
    }

    companion object {
        private val formatter: DateFormat = SimpleDateFormat.getDateTimeInstance()
    }
}
