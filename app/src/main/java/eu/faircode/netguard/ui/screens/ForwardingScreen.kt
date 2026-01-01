package eu.faircode.netguard.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.faircode.netguard.DatabaseHelper
import eu.faircode.netguard.R
import eu.faircode.netguard.Rule
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.Util
import eu.faircode.netguard.ui.util.StatePlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@Composable
fun ForwardingScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<ForwardingEntry>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val forwardListener =
        remember {
            DatabaseHelper.ForwardChangedListener {
                scope.launch {
                    entries = loadForwarding(context)
                    loading = false
                }
            }
        }

    DisposableEffect(Unit) {
        DatabaseHelper.getInstance(context).addForwardChangedListener(forwardListener)
        onDispose {
            DatabaseHelper.getInstance(context).removeForwardChangedListener(forwardListener)
        }
    }

    LaunchedEffect(Unit) {
        entries = loadForwarding(context)
        loading = false
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
                    text = stringResource(R.string.setting_forwarding),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.menu_add),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { showDialog = true }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.menu_add))
            }
        }

        when {
            loading -> {
                StatePlaceholder(
                    title = stringResource(R.string.ui_loading),
                    message = stringResource(R.string.setting_forwarding),
                    icon = Icons.Default.Add,
                    isLoading = true,
                )
            }
            entries.isEmpty() -> {
                StatePlaceholder(
                    title = stringResource(R.string.ui_empty_forwarding_title),
                    message = stringResource(R.string.ui_empty_forwarding_body),
                    icon = Icons.Default.Add,
                    actionLabel = stringResource(R.string.menu_add),
                    onAction = { showDialog = true },
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(entries, key = { "${it.protocol}_${it.dport}_${it.raddr}_${it.rport}" }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Util.getProtocolName(entry.protocol, 0, false) +
                                        " ${entry.dport} > ${entry.raddr}/${entry.rport}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.title_ruid) + ": ${entry.ruid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    DatabaseHelper.getInstance(context).deleteForward(entry.protocol, entry.dport)
                                    ServiceSinkhole.reload("forwarding", context, false)
                                    scope.launch {
                                        entries = loadForwarding(context)
                                        loading = false
                                    }
                                },
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.menu_delete),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ForwardingAddDialog(
            onDismiss = { showDialog = false },
            onAdd = { protocol, dport, raddr, rport, ruid ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val iraddr = InetAddress.getByName(raddr)
                        if (rport < 1024 && (iraddr.isLoopbackAddress || iraddr.isAnyLocalAddress)) {
                            throw IllegalArgumentException("Port forwarding to privileged port on local address not possible")
                        }
                        DatabaseHelper.getInstance(context).deleteForward(protocol, dport)
                        DatabaseHelper.getInstance(context).addForward(protocol, dport, raddr, rport, ruid)
                        ServiceSinkhole.reload("forwarding", context, false)
                    } catch (ex: Throwable) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, ex.message ?: ex.toString(), Toast.LENGTH_LONG).show()
                        }
                    }
                }
                showDialog = false
            },
        )
    }
}

private data class ForwardingEntry(
    val protocol: Int,
    val dport: Int,
    val raddr: String,
    val rport: Int,
    val ruid: Int,
)

private suspend fun loadForwarding(context: Context): List<ForwardingEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<ForwardingEntry>()
        DatabaseHelper.getInstance(context).getForwarding().use { cursor ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardingAddDialog(
    onDismiss: () -> Unit,
    onAdd: (protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) -> Unit,
) {
    val context = LocalContext.current
    val protocolNames = stringArrayResource(R.array.protocolNames)
    val protocolValues = stringArrayResource(R.array.protocolValues)
    var protocolExpanded by remember { mutableStateOf(false) }
    var protocolIndex by remember { mutableStateOf(0) }
    var dport by remember { mutableStateOf("") }
    var raddr by remember { mutableStateOf("") }
    var rport by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf<List<Rule>>(emptyList()) }
    var rulesLoading by remember { mutableStateOf(true) }
    var ruleExpanded by remember { mutableStateOf(false) }
    var ruleIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val list = Rule.getRules(true, context)
            withContext(Dispatchers.Main) {
                rules = list
                rulesLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedProtocol = protocolValues.getOrNull(protocolIndex)?.toIntOrNull() ?: 0
                    val dPortValue = dport.toIntOrNull()
                    val rPortValue = rport.toIntOrNull()
                    val raddrValue = raddr.trim()
                    val selectedRule = rules.getOrNull(ruleIndex)
                    if (dPortValue == null || rPortValue == null || raddrValue.isBlank() || selectedRule == null) {
                        Toast.makeText(context, R.string.msg_invalid, Toast.LENGTH_LONG).show()
                        return@TextButton
                    }
                    onAdd(selectedProtocol, dPortValue, raddrValue, rPortValue, selectedRule.uid)
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.menu_add))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = protocolExpanded,
                    onExpandedChange = { protocolExpanded = !protocolExpanded },
                ) {
                    OutlinedTextField(
                        value = protocolNames.getOrNull(protocolIndex) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = stringResource(R.string.title_protocol)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = protocolExpanded,
                        onDismissRequest = { protocolExpanded = false },
                    ) {
                        protocolNames.forEachIndexed { index, item ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    protocolIndex = index
                                    protocolExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dport,
                    onValueChange = { dport = it },
                    label = { Text(text = stringResource(R.string.title_dport)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = raddr,
                    onValueChange = { raddr = it },
                    label = { Text(text = stringResource(R.string.title_raddr)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rport,
                    onValueChange = { rport = it },
                    label = { Text(text = stringResource(R.string.title_rport)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (rulesLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = ruleExpanded,
                        onExpandedChange = { ruleExpanded = !ruleExpanded },
                    ) {
                        OutlinedTextField(
                            value = rules.getOrNull(ruleIndex)?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(text = stringResource(R.string.title_ruid)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ruleExpanded) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = ruleExpanded,
                            onDismissRequest = { ruleExpanded = false },
                        ) {
                            rules.forEachIndexed { index, rule ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(rule.name ?: rule.packageName ?: "") },
                                    onClick = {
                                        ruleIndex = index
                                        ruleExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
