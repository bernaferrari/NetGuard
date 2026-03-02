package com.bernaferari.renetguard

data class Forward(
    var protocol: Int = 0,
    var dport: Int = 0,
    var raddr: String? = null,
    var rport: Int = 0,
    var ruid: Int = 0,
) {
    override fun toString(): String {
        return "protocol=$protocol port $dport to $raddr/$rport uid $ruid"
    }
}
