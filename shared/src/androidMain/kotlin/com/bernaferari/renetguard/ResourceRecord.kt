package com.bernaferari.renetguard

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class ResourceRecord {
    @JvmField
    var Time: Long = 0

    @JvmField
    var QName: String? = null

    @JvmField
    var AName: String? = null

    @JvmField
    var Resource: String? = null

    @JvmField
    var TTL: Int = 0

    @JvmField
    var uid: Int = 0

    override fun toString(): String {
        return formatter.format(Date(Time).time) +
                " Q " + QName +
                " A " + AName +
                " R " + Resource +
                " TTL " + TTL +
                " uid " + uid +
                " " + formatter.format(Date(Time + TTL * 1000L).time)
    }

    companion object {
        private val formatter: DateFormat = SimpleDateFormat.getDateTimeInstance()
    }
}
