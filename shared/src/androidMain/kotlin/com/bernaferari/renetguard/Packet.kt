package com.bernaferari.renetguard

data class Packet(
    var time: Long = 0,
    var version: Int = 0,
    var protocol: Int = 0,
    var flags: String? = null,
    var saddr: String? = null,
    var sport: Int = 0,
    var daddr: String? = null,
    var dport: Int = 0,
    var data: String? = null,
    var uid: Int = 0,
    var allowed: Boolean = false,
) {
    override fun toString(): String {
        return "uid=$uid v$version p$protocol $daddr/$dport"
    }
}
