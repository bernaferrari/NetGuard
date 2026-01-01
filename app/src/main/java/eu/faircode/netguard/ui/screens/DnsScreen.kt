package eu.faircode.netguard.ui.screens

import android.util.Log
import android.util.Xml
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.faircode.netguard.DatabaseHelper
import eu.faircode.netguard.R
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.ui.util.StatePlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "NetGuard.DNS.Compose"

@Composable
fun DnsScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<DnsEntry>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            if (uri != null) {
                exportDns(context.contentResolver.openOutputStream(uri), context, scope)
            }
        }

    LaunchedEffect(refreshKey) {
        entries = loadDns(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.setting_show_resolved),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.menu_refresh),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { refreshKey += 1 }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.menu_refresh))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    runDnsCleanup(context, scope) {
                        refreshKey += 1
                    }
                },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.menu_cleanup))
            }
            FilledTonalButton(
                onClick = {
                    runDnsClear(context, scope) {
                        refreshKey += 1
                    }
                },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.menu_clear))
            }
            FilledTonalButton(
                onClick = {
                    val filename =
                        "netguard_dns_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + ".xml"
                    exportLauncher.launch(filename)
                },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.menu_export))
            }
        }

        if (entries.isEmpty()) {
            StatePlaceholder(
                title = stringResource(R.string.ui_empty_dns_title),
                message = stringResource(R.string.ui_empty_dns_body),
                icon = Icons.Default.Dns,
                actionLabel = stringResource(R.string.menu_refresh),
                onAction = { refreshKey += 1 },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(entries, key = { "${it.qname}_${it.aname}_${it.resource}_${it.time}" }) { entry ->
                    val expired = entry.time + entry.ttl < Date().time
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (expired) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${entry.qname} -> ${entry.aname}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = entry.resource,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.label_ttl, entry.ttl / 1000),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (entry.uid > 0) {
                                Text(
                                    text = stringResource(R.string.label_uid, entry.uid),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DnsEntry(
    val time: Long,
    val qname: String,
    val aname: String,
    val resource: String,
    val ttl: Int,
    val uid: Int,
)

private suspend fun loadDns(context: android.content.Context): List<DnsEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<DnsEntry>()
        DatabaseHelper.getInstance(context).getDns().use { cursor ->
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

private fun runDnsCleanup(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: () -> Unit,
) {
    scope.launch(Dispatchers.IO) {
        try {
            DatabaseHelper.getInstance(context).cleanupDns()
            ServiceSinkhole.reload("DNS cleanup", context, false)
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString(), ex)
        }
        withContext(Dispatchers.Main) { onDone() }
    }
}

private fun runDnsClear(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: () -> Unit,
) {
    scope.launch(Dispatchers.IO) {
        try {
            DatabaseHelper.getInstance(context).clearDns()
            ServiceSinkhole.reload("DNS clear", context, false)
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString(), ex)
        }
        withContext(Dispatchers.Main) { onDone() }
    }
}

private fun exportDns(
    out: OutputStream?,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (out == null) {
        Toast.makeText(context, R.string.msg_invalid, Toast.LENGTH_LONG).show()
        return
    }
    scope.launch(Dispatchers.IO) {
        var error: Throwable? = null
        try {
            xmlExport(out, context)
        } catch (ex: Throwable) {
            error = ex
            Log.e(TAG, ex.toString(), ex)
        } finally {
            try {
                out.close()
            } catch (ex: IOException) {
                Log.e(TAG, ex.toString(), ex)
            }
        }
        withContext(Dispatchers.Main) {
            if (error == null) {
                Toast.makeText(context, R.string.msg_completed, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.msg_export_failed, error.toString()),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}

private fun xmlExport(out: OutputStream, context: android.content.Context) {
    val serializer: XmlSerializer = Xml.newSerializer()
    serializer.setOutput(out, "UTF-8")
    serializer.startDocument(null, true)
    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
    serializer.startTag(null, "netguard")

    val df: DateFormat = SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.US)
    DatabaseHelper.getInstance(context).getDns().use { cursor ->
        val colTime = cursor.getColumnIndex("time")
        val colQName = cursor.getColumnIndex("qname")
        val colAName = cursor.getColumnIndex("aname")
        val colResource = cursor.getColumnIndex("resource")
        val colTTL = cursor.getColumnIndex("ttl")
        while (cursor.moveToNext()) {
            val time = cursor.getLong(colTime)
            val qname = cursor.getString(colQName)
            val aname = cursor.getString(colAName)
            val resource = cursor.getString(colResource)
            val ttl = cursor.getInt(colTTL)

            serializer.startTag(null, "dns")
            serializer.attribute(null, "time", df.format(time))
            serializer.attribute(null, "qname", qname)
            serializer.attribute(null, "aname", aname)
            serializer.attribute(null, "resource", resource)
            serializer.attribute(null, "ttl", ttl.toString())
            serializer.endTag(null, "dns")
        }
    }

    serializer.endTag(null, "netguard")
    serializer.endDocument()
    serializer.flush()
}
