package com.bernaferari.renetguard

class Version(version: String) : Comparable<Version> {
    private val normalized = version.replace("-beta", "")

    override fun compareTo(other: Version): Int {
        val lhs = normalized.split(".")
        val rhs = other.normalized.split(".")
        val length = maxOf(lhs.size, rhs.size)
        for (i in 0 until length) {
            val vLhs = lhs.getOrNull(i)?.toIntOrNull() ?: 0
            val vRhs = rhs.getOrNull(i)?.toIntOrNull() ?: 0
            if (vLhs < vRhs) return -1
            if (vLhs > vRhs) return 1
        }
        return 0
    }

    override fun toString(): String = normalized
}
