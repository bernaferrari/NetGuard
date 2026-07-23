package com.bernaferrari.quietguard.service

internal data class TrafficRates(
    val transmitPerSecond: Float,
    val receivePerSecond: Float,
)

internal data class TrafficPoint(
    val timestampMs: Long,
    val transmitPerSecond: Float,
    val receivePerSecond: Float,
)

/** Stateful, Android-independent traffic-rate sampler. All calls are confined to one handler thread. */
internal class TrafficStatistics {
    private var previousTimestampMs = -1L
    private var previousTransmitBytes = -1L
    private var previousReceiveBytes = -1L
    private var lastElapsedSeconds = 0f
    private val points = ArrayDeque<TrafficPoint>()
    private val previousUidBytes = HashMap<Int, Long>()

    fun reset() {
        previousTimestampMs = -1L
        previousTransmitBytes = -1L
        previousReceiveBytes = -1L
        lastElapsedSeconds = 0f
        points.clear()
        previousUidBytes.clear()
    }

    fun sample(
        timestampMs: Long,
        transmitBytes: Long,
        receiveBytes: Long,
        retentionSeconds: Long,
    ): TrafficRates {
        while (points.firstOrNull()?.let { timestampMs - it.timestampMs > retentionSeconds * 1_000 } == true) {
            points.removeFirst()
        }
        val elapsedSeconds = ((timestampMs - previousTimestampMs).coerceAtLeast(1)) / 1_000f
        lastElapsedSeconds = if (previousTimestampMs < 0) 0f else elapsedSeconds
        val rates = if (previousTimestampMs < 0) {
            TrafficRates(0f, 0f)
        } else {
            TrafficRates(
                ((transmitBytes - previousTransmitBytes).coerceAtLeast(0)) / elapsedSeconds,
                ((receiveBytes - previousReceiveBytes).coerceAtLeast(0)) / elapsedSeconds,
            )
        }
        if (previousTimestampMs >= 0) {
            points.addLast(TrafficPoint(timestampMs, rates.transmitPerSecond, rates.receivePerSecond))
        }
        previousTimestampMs = timestampMs
        previousTransmitBytes = transmitBytes
        previousReceiveBytes = receiveBytes
        return rates
    }

    fun points(): List<TrafficPoint> = points.toList()

    fun initializeUidBytes(bytesByUid: Map<Int, Long>) {
        previousUidBytes.clear()
        previousUidBytes.putAll(bytesByUid)
    }

    fun clearUidBaseline() {
        previousUidBytes.clear()
    }

    fun hasUidBaseline(): Boolean = previousUidBytes.isNotEmpty()

    fun uidRates(bytesByUid: Map<Int, Long>): List<Pair<Int, Float>> {
        if (lastElapsedSeconds <= 0f) return emptyList()
        return bytesByUid.mapNotNull { (uid, bytes) ->
            val previous = previousUidBytes[uid] ?: return@mapNotNull null
            previousUidBytes[uid] = bytes
            val rate = ((bytes - previous).coerceAtLeast(0)) / lastElapsedSeconds
            (uid to rate).takeIf { rate > 0f }
        }.sortedByDescending { it.second }
    }
}
