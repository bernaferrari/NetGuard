package com.bernaferari.renetguard.platform

import com.bernaferari.renetguard.demo.wasmDemoApps
import com.bernaferari.renetguard.domain.FirewallRule

actual suspend fun loadLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): List<LogEntry> =
    demoLogs()
        .filter { entry ->
            val protocolOk =
                when {
                    entry.protocolLabel == "TCP" -> tcp
                    entry.protocolLabel == "UDP" -> udp
                    else -> other
                }
            val outcomeOk =
                when {
                    allowed && blocked -> true
                    allowed -> entry.allowed == 1
                    blocked -> entry.allowed == 0
                    else -> true
                }
            protocolOk && outcomeOk
        }
        .take(limit)

actual fun observeLogChanges(onChanged: () -> Unit): () -> Unit = { }

actual suspend fun loadDnsEntries(): List<DnsEntry> =
    listOf(
        DnsEntry(1_700_000_000_000L, "google.com", "google.com", "A", 300_000, 10101),
        DnsEntry(1_700_000_050_000L, "gmail.com", "gmail.com", "A", 300_000, 10102),
        DnsEntry(1_700_000_100_000L, "spotify.com", "spotify.com", "A", 300_000, 10103),
        DnsEntry(1_700_000_150_000L, "cdninstagram.com", "cdninstagram.com", "A", 300_000, 10104),
        DnsEntry(1_700_000_200_000L, "whatsapp.net", "whatsapp.net", "A", 300_000, 10105),
    )

actual suspend fun cleanupDns() {}

actual suspend fun clearDns() {}

actual suspend fun loadForwardingEntries(): List<ForwardingEntry> =
    listOf(
        ForwardingEntry(protocol = 6, dport = 8080, raddr = "192.168.1.10", rport = 80, ruid = 10101),
    )

actual fun observeForwardingChanges(onChanged: () -> Unit): () -> Unit = { }

actual suspend fun addForwardingEntry(
    protocol: Int,
    dport: Int,
    raddr: String,
    rport: Int,
    ruid: Int,
) {}

actual suspend fun deleteForwardingEntry(entry: ForwardingEntry) {}

actual fun loadAppDisplayInfo(uid: Int, fallbackLabel: String): AppDisplayInfo =
    wasmDemoApps[uid] ?: AppDisplayInfo(label = fallbackLabel, packageName = null)

actual suspend fun loadAllRulesForPicker(): List<FirewallRule> =
    wasmDemoApps.map { (uid, info) ->
        FirewallRule(
            uid = uid,
            packageName = info.packageName,
            name = info.label,
        )
    }

actual fun registerUpdateCheckListener(onResult: (status: String?, version: String?) -> Unit): () -> Unit =
    { }

actual fun exportDnsToFile(onComplete: (success: Boolean, error: String?) -> Unit) {
    onComplete(false, null)
}

actual fun importHostsFromFile(onComplete: (success: Boolean) -> Unit) {
    onComplete(false)
}

actual suspend fun loadAccessEntries(uid: Int): List<AccessEntry> =
    listOf(
        AccessEntry(time = 1_700_000_000_000L, timeText = "12:00", daddr = "142.250.80.46", dport = 443, allowed = 0),
        AccessEntry(time = 1_700_000_100_000L, timeText = "12:01", daddr = "8.8.8.8", dport = 53, allowed = 1),
    )

actual suspend fun clearAccess(uid: Int) {}

actual suspend fun clearLogs() {}

private fun demoLogs(): List<LogEntry> =
    listOf(
        LogEntry(1, 1_700_000_000_000L, "12:00:00", "TCP", "142.250.80.46", 443, "google.com", 10101, 0),
        LogEntry(2, 1_700_000_050_000L, "12:00:05", "TCP", "142.251.40.78", 443, "gmail.com", 10102, 0),
        LogEntry(3, 1_700_000_100_000L, "12:00:10", "UDP", "8.8.8.8", 53, null, 10103, 1),
        LogEntry(4, 1_700_000_150_000L, "12:00:15", "TCP", "31.13.64.35", 443, "facebook.com", 10108, 0),
        LogEntry(5, 1_700_000_200_000L, "12:00:20", "TCP", "151.101.1.140", 443, "reddit.com", 10107, 1),
        LogEntry(6, 1_700_000_250_000L, "12:00:25", "UDP", "1.1.1.1", 53, null, 10105, 1),
        LogEntry(7, 1_700_000_300_000L, "12:00:30", "TCP", "104.16.132.229", 443, "discord.com", 10112, 0),
    )