package com.bernaferrari.quietguard.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.bernaferrari.quietguard.DatabaseHelper
import com.bernaferrari.quietguard.Rule
import com.bernaferrari.quietguard.domain.FirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Locale

// already had SimpleDateFormat at top from first block - verify

private fun context(): Context = GlobalContext.get().get()

actual suspend fun loadLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): List<LogEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<LogEntry>()
        DatabaseHelper.getInstance(context()).getLog(udp, tcp, other, allowed, blocked, limit)
            .use { cursor ->
                val colId = cursor.getColumnIndex("ID")
                val colTime = cursor.getColumnIndex("time")
                val colProtocol = cursor.getColumnIndex("protocol")
                val colDAddr = cursor.getColumnIndex("daddr")
                val colDPort = cursor.getColumnIndex("dport")
                val colDName = cursor.getColumnIndex("dname")
                val colUid = cursor.getColumnIndex("uid")
                val colAllowed = cursor.getColumnIndex("allowed")
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                while (cursor.moveToNext()) {
                    val protocol =
                        if (cursor.isNull(colProtocol)) -1 else cursor.getInt(colProtocol)
                    result.add(
                        LogEntry(
                            id = cursor.getLong(colId),
                            time = cursor.getLong(colTime),
                            timeText = timeFormat.format(cursor.getLong(colTime)),
                            protocolLabel =
                                NetGuardPlatform.uiHelpers.getProtocolName(protocol, 0, false),
                            daddr = cursor.getString(colDAddr),
                            dport = if (cursor.isNull(colDPort)) -1 else cursor.getInt(colDPort),
                            dname = if (cursor.isNull(colDName)) null else cursor.getString(colDName),
                            uid = if (cursor.isNull(colUid)) -1 else cursor.getInt(colUid),
                            allowed = if (cursor.isNull(colAllowed)) -1 else cursor.getInt(colAllowed),
                        ),
                    )
                }
            }
        result
    }

actual fun observeLogChanges(onChanged: () -> Unit): () -> Unit {
    val listener = DatabaseHelper.LogChangedListener { onChanged() }
    DatabaseHelper.getInstance(context()).addLogChangedListener(listener)
    return { DatabaseHelper.getInstance(context()).removeLogChangedListener(listener) }
}

actual suspend fun loadDnsEntries(): List<DnsEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<DnsEntry>()
        DatabaseHelper.getInstance(context()).getDns().use { cursor ->
            val colTime = cursor.getColumnIndex("time")
            val colQName = cursor.getColumnIndex("qname")
            val colAName = cursor.getColumnIndex("aname")
            val colResource = cursor.getColumnIndex("resource")
            val colTTL = cursor.getColumnIndex("ttl")
            val colUid = cursor.getColumnIndex("uid")
            while (cursor.moveToNext()) {
                result.add(
                    DnsEntry(
                        time = cursor.getLong(colTime),
                        qname = cursor.getString(colQName),
                        aname = cursor.getString(colAName),
                        resource = cursor.getString(colResource),
                        ttl = cursor.getInt(colTTL),
                        uid = cursor.getInt(colUid),
                    ),
                )
            }
        }
        result
    }

actual suspend fun cleanupDns() {
    withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(context()).cleanupDns()
        NetGuardPlatform.firewall.reload("DNS cleanup", false)
    }
}

actual suspend fun clearDns() {
    withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(context()).clearDns()
        NetGuardPlatform.firewall.reload("DNS clear", false)
    }
}

actual suspend fun loadForwardingEntries(): List<ForwardingEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<ForwardingEntry>()
        DatabaseHelper.getInstance(context()).getForwarding().use { cursor ->
            val colProtocol = cursor.getColumnIndex("protocol")
            val colDport = cursor.getColumnIndex("dport")
            val colRaddr = cursor.getColumnIndex("raddr")
            val colRport = cursor.getColumnIndex("rport")
            val colRuid = cursor.getColumnIndex("ruid")
            while (cursor.moveToNext()) {
                result.add(
                    ForwardingEntry(
                        protocol = cursor.getInt(colProtocol),
                        dport = cursor.getInt(colDport),
                        raddr = cursor.getString(colRaddr),
                        rport = cursor.getInt(colRport),
                        ruid = cursor.getInt(colRuid),
                    ),
                )
            }
        }
        result
    }

actual fun observeForwardingChanges(onChanged: () -> Unit): () -> Unit {
    val listener = DatabaseHelper.ForwardChangedListener { onChanged() }
    DatabaseHelper.getInstance(context()).addForwardChangedListener(listener)
    return { DatabaseHelper.getInstance(context()).removeForwardChangedListener(listener) }
}

actual suspend fun addForwardingEntry(
    protocol: Int,
    dport: Int,
    raddr: String,
    rport: Int,
    ruid: Int,
) {
    withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(context())
            .addForward(protocol, dport, raddr, rport, ruid)
        NetGuardPlatform.firewall.reload("forwarding", false)
    }
}

actual suspend fun deleteForwardingEntry(entry: ForwardingEntry) {
    withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(context()).deleteForward(entry.protocol, entry.dport)
        NetGuardPlatform.firewall.reload("forwarding", false)
    }
}

actual fun loadAppDisplayInfo(uid: Int, fallbackLabel: String): AppDisplayInfo {
    val label =
        NetGuardPlatform.uiHelpers.getApplicationNames(uid).joinToString(", ").ifBlank { "UID $uid" }
    val packageName = NetGuardPlatform.uiHelpers.getPackageForUid(uid)
    return AppDisplayInfo(label = label.ifBlank { fallbackLabel }, packageName = packageName)
}

actual suspend fun loadAllRulesForPicker(): List<FirewallRule> =
    Rule.getRules(true, context()).map { rule ->
        FirewallRule(
            uid = rule.uid,
            packageName = rule.packageName,
            name = rule.name,
        )
    }

actual fun registerUpdateCheckListener(onResult: (status: String?, version: String?) -> Unit): () -> Unit {
    val ctx = context()
    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != NetGuardPlatform.firewall.updateCheckAction) return
                onResult(
                    intent.getStringExtra(NetGuardPlatform.firewall.extraUpdateCheckStatus),
                    intent.getStringExtra(NetGuardPlatform.firewall.extraUpdateCheckVersion),
                )
            }
        }
    ContextCompat.registerReceiver(
        ctx,
        receiver,
        IntentFilter(NetGuardPlatform.firewall.updateCheckAction),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    return {
        runCatching { ctx.unregisterReceiver(receiver) }
    }
}

actual fun exportDnsToFile(onComplete: (success: Boolean, error: String?) -> Unit) {
    onComplete(false, "Export not available")
}

actual fun importHostsFromFile(onComplete: (success: Boolean) -> Unit) {
    onComplete(false)
}

actual suspend fun loadAccessEntries(uid: Int): List<AccessEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<AccessEntry>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        DatabaseHelper.getInstance(context()).getAccess(uid).use { cursor ->
            val colTime = cursor.getColumnIndex("time")
            val colDAddr = cursor.getColumnIndex("daddr")
            val colDPort = cursor.getColumnIndex("dport")
            val colAllowed = cursor.getColumnIndex("allowed")
            while (cursor.moveToNext() && result.size < 10) {
                val time = cursor.getLong(colTime)
                result.add(
                    AccessEntry(
                        time = time,
                        timeText = timeFormat.format(time),
                        daddr = cursor.getString(colDAddr),
                        dport = cursor.getInt(colDPort),
                        allowed = cursor.getInt(colAllowed),
                    ),
                )
            }
        }
        result
    }

actual suspend fun clearAccess(uid: Int) {
    withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(context()).clearAccess(uid, true)
    }
}

actual suspend fun clearLogs() {
    withContext(Dispatchers.IO) {
        DatabaseHelper.getInstance(context()).clearLog(-1)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> observeOnChanges(
    register: (onChanged: () -> Unit) -> (() -> Unit),
    load: suspend () -> T,
): Flow<T> =
    callbackFlow {
        val listener: () -> Unit = { trySend(Unit) }
        val unregister = register(listener)
        trySend(Unit)
        awaitClose {
            unregister()
        }
    }.mapLatest { load() }

actual fun observeLogs(
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): Flow<List<LogEntry>> =
    observeOnChanges(
        register = { callback ->
            val listener = DatabaseHelper.LogChangedListener { callback() }
            DatabaseHelper.getInstance(context()).addLogChangedListener(listener)
            val unregister: () -> Unit = {
                DatabaseHelper.getInstance(context()).removeLogChangedListener(listener)
            }
            unregister
        },
    ) { loadLogs(udp, tcp, other, allowed, blocked, limit) }

actual fun observeDnsEntries(): Flow<List<DnsEntry>> =
    observeOnChanges(
        register = { callback ->
            val listener = DatabaseHelper.DnsChangedListener { callback() }
            DatabaseHelper.getInstance(context()).addDnsChangedListener(listener)
            val unregister: () -> Unit = {
                DatabaseHelper.getInstance(context()).removeDnsChangedListener(listener)
            }
            unregister
        },
    ) { loadDnsEntries() }

actual fun observeForwardingEntries(): Flow<List<ForwardingEntry>> =
    observeOnChanges(
        register = { callback ->
            val listener = DatabaseHelper.ForwardChangedListener { callback() }
            DatabaseHelper.getInstance(context()).addForwardChangedListener(listener)
            val unregister: () -> Unit = {
                DatabaseHelper.getInstance(context()).removeForwardChangedListener(listener)
            }
            unregister
        },
    ) { loadForwardingEntries() }

actual fun observeAccessEntries(uid: Int): Flow<List<AccessEntry>> =
    observeOnChanges(
        register = { callback ->
            val listener = DatabaseHelper.AccessChangedListener { callback() }
            DatabaseHelper.getInstance(context()).addAccessChangedListener(listener)
            val unregister: () -> Unit = {
                DatabaseHelper.getInstance(context()).removeAccessChangedListener(listener)
            }
            unregister
        },
    ) { loadAccessEntries(uid) }
