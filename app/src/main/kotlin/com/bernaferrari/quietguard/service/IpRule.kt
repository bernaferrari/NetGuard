package com.bernaferrari.quietguard.service

/** Key for a UID-specific destination rule. Non-TCP/UDP traffic has no port. */
internal class IpRuleKey(
    val version: Int,
    val protocol: Int,
    destinationPort: Int,
    val uid: Int,
) {
    val destinationPort: Int = if (protocol == 6 || protocol == 17) destinationPort else 0

    override fun equals(other: Any?): Boolean =
        other is IpRuleKey &&
            version == other.version &&
            protocol == other.protocol &&
            destinationPort == other.destinationPort &&
            uid == other.uid

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + protocol
        result = 31 * result + destinationPort
        return 31 * result + uid
    }

    override fun toString(): String = "v$version p$protocol port=$destinationPort uid=$uid"
}

/** Mutable DNS-derived decision, refreshed when its TTL is observed again. */
internal class IpRule(
    private val key: IpRuleKey,
    private val name: String,
    private val block: Boolean,
    private var observedAt: Long,
    private var ttl: Long,
) {
    fun isBlocked(): Boolean = block

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now > observedAt + ttl * 2

    fun refresh(observedAt: Long, ttl: Long) {
        this.observedAt = observedAt
        this.ttl = ttl
    }

    override fun toString(): String = "$key $name"
}
