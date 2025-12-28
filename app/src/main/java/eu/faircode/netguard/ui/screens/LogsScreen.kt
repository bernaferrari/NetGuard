package eu.faircode.netguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import android.widget.ListView
import android.content.Intent
import eu.faircode.netguard.AdapterLog
import eu.faircode.netguard.ActivityPro
import eu.faircode.netguard.DatabaseHelper
import eu.faircode.netguard.IAB
import eu.faircode.netguard.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val hasLog = remember { IAB.isPurchased(ActivityPro.SKU_LOG, context) }
    var adapter by remember { mutableStateOf<AdapterLog?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    val logListener =
        remember {
            DatabaseHelper.LogChangedListener {
                val target = adapter ?: return@LogChangedListener
                val udp = prefs.getBoolean("proto_udp", true)
                val tcp = prefs.getBoolean("proto_tcp", true)
                val other = prefs.getBoolean("proto_other", true)
                val allowed = prefs.getBoolean("traffic_allowed", true)
                val blocked = prefs.getBoolean("traffic_blocked", true)
                target.changeCursor(
                    DatabaseHelper.getInstance(context).getLog(udp, tcp, other, allowed, blocked),
                )
            }
        }

    DisposableEffect(hasLog) {
        if (!hasLog) {
            return@DisposableEffect onDispose { }
        }
        DatabaseHelper.getInstance(context).addLogChangedListener(logListener)
        onDispose {
            DatabaseHelper.getInstance(context).removeLogChangedListener(logListener)
        }
    }

    LaunchedEffect(adapter, refreshKey) {
        if (!hasLog) {
            return@LaunchedEffect
        }
        val target = adapter ?: return@LaunchedEffect
        isLoading = true
        val cursor =
            withContext(Dispatchers.IO) {
                val udp = prefs.getBoolean("proto_udp", true)
                val tcp = prefs.getBoolean("proto_tcp", true)
                val other = prefs.getBoolean("proto_other", true)
                val allowed = prefs.getBoolean("traffic_allowed", true)
                val blocked = prefs.getBoolean("traffic_blocked", true)
                DatabaseHelper.getInstance(context).getLog(udp, tcp, other, allowed, blocked)
            }
        target.changeCursor(cursor)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!hasLog) {
            Text(
                text = stringResource(R.string.title_pro),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.msg_log_disabled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(context, ActivityPro::class.java))
                },
            ) {
                Text(text = stringResource(R.string.title_pro))
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.menu_log),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.home_logs_hint),
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

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        AndroidView(
            factory = { ctx ->
                ListView(ctx).apply {
                    val resolve = prefs.getBoolean("resolve", false)
                    val organization = prefs.getBoolean("organization", false)
                    val udp = prefs.getBoolean("proto_udp", true)
                    val tcp = prefs.getBoolean("proto_tcp", true)
                    val other = prefs.getBoolean("proto_other", true)
                    val allowed = prefs.getBoolean("traffic_allowed", true)
                    val blocked = prefs.getBoolean("traffic_blocked", true)
                    val cursor = DatabaseHelper.getInstance(ctx).getLog(udp, tcp, other, allowed, blocked)
                    val created = AdapterLog(ctx, cursor, resolve, organization)
                    adapter = created
                    this.adapter = created
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
