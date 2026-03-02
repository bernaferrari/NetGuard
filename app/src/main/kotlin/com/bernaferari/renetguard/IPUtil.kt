package com.bernaferari.renetguard

import android.util.Log
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

object IPUtil {
    private const val TAG = "NetGuard.IPUtil"

    @Throws(UnknownHostException::class)
    fun toCIDR(start: String, end: String): List<CIDR> {
        return toCIDR(InetAddress.getByName(start), InetAddress.getByName(end))
    }

    @Throws(UnknownHostException::class)
    fun toCIDR(start: InetAddress, end: InetAddress): List<CIDR> {
        val listResult = ArrayList<CIDR>()
        Log.i(TAG, "toCIDR(${start.hostAddress},${end.hostAddress})")

        var from = inet2long(start)
        val to = inet2long(end)
        while (to >= from) {
            var prefix: Byte = 32
            while (prefix > 0) {
                val mask = prefix2mask(prefix - 1)
                if (from and mask != from) break
                prefix = (prefix - 1).toByte()
            }

            val max = (32 - floor(log((to - from + 1).toDouble(), 2.0))).toInt()
            if (prefix < max) prefix = max.toByte()

            listResult.add(CIDR(long2inet(from), prefix.toInt()))
            from += 2.0.pow((32 - prefix).toDouble()).toLong()
        }

        for (cidr in listResult) Log.i(TAG, cidr.toString())
        return listResult
    }

    private fun prefix2mask(bits: Int): Long {
        return (-1L shl bits) and 0xFFFFFFFFL
    }

    private fun inet2long(addr: InetAddress?): Long {
        var result = 0L
        addr?.address?.forEach { b ->
            result = result shl 8 or (b.toInt() and 0xFF).toLong()
        }
        return result
    }

    private fun long2inet(addr: Long): InetAddress? {
        var value = addr
        return try {
            val b = ByteArray(4)
            for (i in b.size - 1 downTo 0) {
                b[i] = (value and 0xFF).toByte()
                value = value shr 8
            }
            InetAddress.getByAddress(b)
        } catch (ignore: UnknownHostException) {
            null
        }
    }

    fun minus1(addr: InetAddress): InetAddress? {
        return long2inet(inet2long(addr) - 1)
    }

    fun plus1(addr: InetAddress): InetAddress? {
        return long2inet(inet2long(addr) + 1)
    }

    class CIDR(
        var address: InetAddress?,
        var prefix: Int,
    ) : Comparable<CIDR> {
        constructor(ip: String, prefix: Int) : this(null, prefix) {
            try {
                address = InetAddress.getByName(ip)
            } catch (ex: UnknownHostException) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        fun getStart(): InetAddress? {
            return long2inet(inet2long(address) and prefix2mask(prefix))
        }

        fun getEnd(): InetAddress? {
            return long2inet((inet2long(address) and prefix2mask(prefix)) + (1L shl (32 - prefix)) - 1)
        }

        override fun toString(): String {
            return address?.hostAddress + "/" + prefix + "=" + getStart()?.hostAddress + "..." + getEnd()?.hostAddress
        }

        override fun compareTo(other: CIDR): Int {
            val lcidr = inet2long(address)
            val lother = inet2long(other.address)
            return lcidr.compareTo(lother)
        }
    }
}
