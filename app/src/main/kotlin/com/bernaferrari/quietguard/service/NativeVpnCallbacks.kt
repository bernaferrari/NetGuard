package com.bernaferrari.quietguard.service

import com.bernaferrari.quietguard.*

import com.bernaferrari.quietguard.netguard.NativeCallbacks
import com.bernaferrari.quietguard.netguard.NativeDnsRecord
import com.bernaferrari.quietguard.netguard.NativeFlow
import com.bernaferrari.quietguard.netguard.NativePacket
import com.bernaferrari.quietguard.netguard.NativeRedirect
import com.bernaferrari.quietguard.netguard.NativeUsage

/**
 * Android-policy adapter for the generated UniFFI callback contract.
 *
 * It deliberately contains only representation conversion. Service policy and
 * Android lifecycle remain owned by [ServiceSinkhole].
 */
internal class NativeVpnCallbacks(
    private val uidFor: (Int, Int, String, Int, String, Int) -> Int,
    private val addressAllowed: (Packet) -> Allowed?,
    private val domainBlocked: (String) -> Boolean,
    private val packetLogger: (Packet) -> Unit,
    private val dnsRecorder: (ResourceRecord) -> Unit,
    private val socketProtector: (Int) -> Boolean,
    private val usageRecorder: (Usage) -> Unit,
    private val exitReporter: (String) -> Unit,
) : NativeCallbacks {
    override fun uidFor(flow: NativeFlow): Int =
        uidFor(
            flow.version,
            flow.protocol,
            flow.source,
            flow.sourcePort,
            flow.destination,
            flow.destinationPort,
        )

    override fun allow(packet: NativePacket): NativeRedirect? {
        val allowed = addressAllowed(packet.toPacket()) ?: return null
        val redirected = allowed.raddr != null && allowed.rport != 0
        return NativeRedirect(
            allowed.raddr ?: packet.flow.destination,
            if (allowed.rport != 0) allowed.rport else packet.flow.destinationPort,
            redirected,
        )
    }

    override fun domainBlocked(name: String): Boolean = domainBlocked.invoke(name)

    override fun logDnsBlocked(packet: NativePacket) {
        packetLogger(packet.toPacket())
    }

    override fun dnsResolved(record: NativeDnsRecord) {
        dnsRecorder(ResourceRecord().apply {
            Time = record.timeMillis
            QName = record.question
            AName = record.answerName
            Resource = record.resource
            TTL = record.ttl
            uid = record.uid
        })
    }

    override fun protectSocket(fd: Int): Boolean = socketProtector(fd)

    override fun usage(usage: NativeUsage) {
        usageRecorder(Usage().apply {
            Time = usage.timeMillis
            Version = usage.flow.version
            Protocol = usage.flow.protocol
            DAddr = usage.flow.destination
            DPort = usage.flow.destinationPort
            Uid = usage.uid
            Sent = usage.sent
            Received = usage.received
        })
    }

    override fun reportExit(message: String) {
        exitReporter(message)
    }
}

private fun NativePacket.toPacket() = Packet(
    time = System.currentTimeMillis(),
    version = flow.version,
    protocol = flow.protocol,
    flags = flags,
    saddr = flow.source,
    sport = flow.sourcePort,
    daddr = flow.destination,
    dport = flow.destinationPort,
    data = data,
    uid = uid,
)
