package com.bernaferari.renetguard.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferari.renetguard.DatabaseHelper
import com.bernaferari.renetguard.R
import com.bernaferari.renetguard.Rule
import com.bernaferari.renetguard.ServiceSinkhole
import com.bernaferari.renetguard.Util
import com.bernaferari.renetguard.ui.theme.spacing
import com.bernaferari.renetguard.ui.util.StatePlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

private enum class ForwardingProtocolFilter {
    All,
    Udp,
    Tcp,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardingScreen() {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    var entries by remember { mutableStateOf<List<ForwardingEntry>>(emptyList()) }
    var protocolFilter by remember { mutableStateOf(ForwardingProtocolFilter.All) }
    var showDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val filteredEntries by remember(entries, protocolFilter) {
        derivedStateOf {
            entries.filter { entry ->
                when (protocolFilter) {
                    ForwardingProtocolFilter.All -> true
                    ForwardingProtocolFilter.Udp -> entry.protocol == 17
                    ForwardingProtocolFilter.Tcp -> entry.protocol == 6
                }
            }
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Text(
                            text = stringResource(R.string.ui_forwarding_title),
                            fontWeight = FontWeight.Bold,
                        )
                        if (!loading && filteredEntries.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = filteredEntries.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = spacing.small,
                                        vertical = 2.dp
                                    ),
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.menu_add),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(spacing.default),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    FilledTonalButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(R.string.menu_add))
                    }
                    Text(
                        text = stringResource(R.string.ui_logs_filter_protocol),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            ForwardingProtocolFilter.All to stringResource(R.string.ui_filter_all),
                            ForwardingProtocolFilter.Udp to stringResource(R.string.menu_protocol_udp),
                            ForwardingProtocolFilter.Tcp to stringResource(R.string.menu_protocol_tcp),
                        )
                        options.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = protocolFilter == value,
                                onClick = { protocolFilter = value },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = label, maxLines = 1)
                            }
                        }
                    }
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

                filteredEntries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(R.string.ui_forwarding_title),
                        message = stringResource(R.string.ui_forwarding_filter_empty),
                        icon = Icons.AutoMirrored.Filled.Forward,
                        actionLabel = stringResource(R.string.ui_filter_all),
                        onAction = { protocolFilter = ForwardingProtocolFilter.All },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        items(
                            filteredEntries,
                            key = { "${it.protocol}_${it.dport}_${it.raddr}_${it.rport}" }) { entry ->
                            ForwardingEntryCard(
                                entry = entry,
                                onDelete = {
                                    DatabaseHelper.getInstance(context)
                                        .deleteForward(entry.protocol, entry.dport)
                                    ServiceSinkhole.reload("forwarding", context, false)
                                    scope.launch {
                                        entries = loadForwarding(context)
                                        loading = false
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    } // end Scaffold

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
                        DatabaseHelper.getInstance(context)
                            .addForward(protocol, dport, raddr, rport, ruid)
                        ServiceSinkhole.reload("forwarding", context, false)
                    } catch (ex: Throwable) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, ex.message ?: ex.toString(), Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
                showDialog = false
            },
        )
    }
}

@Composable
private fun ForwardingEntryCard(
    entry: ForwardingEntry,
    onDelete: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val protocolLabel = Util.getProtocolName(entry.protocol, 0, false)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(
                            text = protocolLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = spacing.small,
                                vertical = 2.dp
                            ),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(
                            text = stringResource(R.string.title_ruid) + ": ${entry.ruid}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = spacing.small,
                                vertical = 2.dp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = "${entry.dport} → ${entry.raddr}:${entry.rport}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onDelete) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.menu_delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
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
                    val selectedProtocol =
                        protocolValues.getOrNull(protocolIndex)?.toIntOrNull() ?: 0
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
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
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
