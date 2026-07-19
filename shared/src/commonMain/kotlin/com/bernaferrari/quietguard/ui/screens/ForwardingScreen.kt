package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols




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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.loadAllRulesForPicker
import com.bernaferrari.quietguard.platform.showToast
import com.bernaferrari.quietguard.ui.screens.vm.ForwardingListFilter
import com.bernaferrari.quietguard.ui.screens.vm.ForwardingViewModel
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.menu_add
import com.bernaferrari.quietguard.generated.resources.menu_cancel
import com.bernaferrari.quietguard.generated.resources.menu_delete
import com.bernaferrari.quietguard.generated.resources.menu_ok
import com.bernaferrari.quietguard.generated.resources.menu_protocol_tcp
import com.bernaferrari.quietguard.generated.resources.menu_protocol_udp
import com.bernaferrari.quietguard.generated.resources.msg_invalid
import com.bernaferrari.quietguard.generated.resources.protocolNames
import com.bernaferrari.quietguard.generated.resources.protocolValues
import com.bernaferrari.quietguard.generated.resources.setting_forwarding
import com.bernaferrari.quietguard.generated.resources.title_dport
import com.bernaferrari.quietguard.generated.resources.title_protocol
import com.bernaferrari.quietguard.generated.resources.title_raddr
import com.bernaferrari.quietguard.generated.resources.title_rport
import com.bernaferrari.quietguard.generated.resources.title_ruid
import com.bernaferrari.quietguard.generated.resources.ui_empty_forwarding_body
import com.bernaferrari.quietguard.generated.resources.ui_empty_forwarding_title
import com.bernaferrari.quietguard.generated.resources.ui_filter_all
import com.bernaferrari.quietguard.generated.resources.ui_forwarding_filter_empty
import com.bernaferrari.quietguard.generated.resources.ui_forwarding_title
import com.bernaferrari.quietguard.generated.resources.ui_loading
import com.bernaferrari.quietguard.generated.resources.ui_logs_filter_protocol
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

import com.bernaferrari.quietguard.ui.icons.Icon
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardingScreen(viewModel: ForwardingViewModel = koinViewModel()) {
    val spacing = MaterialTheme.spacing
    val fwdUi by viewModel.uiState.collectAsState()
    val entries = fwdUi.entries.data
    val loading = fwdUi.entries.isLoading
    val protocolFilter = fwdUi.protocolFilter
    val showDialog = fwdUi.showAddDialog
    val filteredEntries = fwdUi.filtered

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Text(
                            text = stringResource(Res.string.ui_forwarding_title),
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
                    IconButton(onClick = { viewModel.setShowAddDialog(true) }) {
                        Icon(
                            icon = MaterialSymbols.Filled.Add,
                            contentDescription = stringResource(Res.string.menu_add),
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
                    FilledTonalButton(onClick = { viewModel.setShowAddDialog(true) }) {
                        Icon(
                            icon = MaterialSymbols.Filled.Add,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.menu_add))
                    }
                    Text(
                        text = stringResource(Res.string.ui_logs_filter_protocol),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            ForwardingListFilter.All to stringResource(Res.string.ui_filter_all),
                            ForwardingListFilter.Udp to stringResource(Res.string.menu_protocol_udp),
                            ForwardingListFilter.Tcp to stringResource(Res.string.menu_protocol_tcp),
                        )
                        options.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = protocolFilter == value,
                                onClick = { viewModel.setProtocolFilter(value) },
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
                        title = stringResource(Res.string.ui_loading),
                        message = stringResource(Res.string.setting_forwarding),
                        icon = MaterialSymbols.Filled.Add,
                        isLoading = true,
                    )
                }

                entries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_forwarding_title),
                        message = stringResource(Res.string.ui_empty_forwarding_body),
                        icon = MaterialSymbols.Filled.Add,
                        actionLabel = stringResource(Res.string.menu_add),
                        onAction = { viewModel.setShowAddDialog(true) },
                    )
                }

                filteredEntries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_forwarding_title),
                        message = stringResource(Res.string.ui_forwarding_filter_empty),
                        icon = MaterialSymbols.AutoMirrored.Filled.Forward,
                        actionLabel = stringResource(Res.string.ui_filter_all),
                        onAction = { viewModel.setProtocolFilter(ForwardingListFilter.All) },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        items(
                            filteredEntries,
                            key = { "${it.protocol}_${it.dport}_${it.raddr}_${it.rport}" },
                        ) { entry ->
                            ForwardingEntryCard(
                                entry = entry,
                                onDelete = { viewModel.deleteForward(entry) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ForwardingAddDialog(
            onDismiss = { viewModel.setShowAddDialog(false) },
            onAdd = { protocol, dport, raddr, rport, ruid ->
                viewModel.addForward(protocol, dport, raddr, rport, ruid)
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
    val protocolLabel = NetGuardPlatform.uiHelpers.getProtocolName(entry.protocol, 0, false)

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
                    icon = MaterialSymbols.AutoMirrored.Filled.Forward,
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
                            text = stringResource(Res.string.title_ruid) + ": ${entry.ruid}",
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
                        icon = MaterialSymbols.Filled.Delete,
                        contentDescription = stringResource(Res.string.menu_delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardingAddDialog(
    onDismiss: () -> Unit,
    onAdd: (protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) -> Unit,
) {
    val protocolNames = stringArrayResource(Res.array.protocolNames)
    val protocolValues = stringArrayResource(Res.array.protocolValues)
    var protocolExpanded by remember { mutableStateOf(false) }
    var protocolIndex by remember { mutableStateOf(0) }
    var dport by remember { mutableStateOf("") }
    var raddr by remember { mutableStateOf("") }
    var rport by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf<List<FirewallRule>>(emptyList()) }
    var rulesLoading by remember { mutableStateOf(true) }
    var ruleExpanded by remember { mutableStateOf(false) }
    var ruleIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        rules = loadAllRulesForPicker()
        rulesLoading = false
    }

    val invalidMessage = stringResource(Res.string.msg_invalid)

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
                    val selectedFirewallRule = rules.getOrNull(ruleIndex)
                    if (dPortValue == null || rPortValue == null || raddrValue.isBlank() || selectedFirewallRule == null) {
                        showToast(invalidMessage)
                        return@TextButton
                    }
                    onAdd(selectedProtocol, dPortValue, raddrValue, rPortValue, selectedFirewallRule.uid)
                },
            ) {
                Text(text = stringResource(Res.string.menu_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.menu_cancel))
            }
        },
        title = {
            Text(text = stringResource(Res.string.menu_add))
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
                        label = { Text(text = stringResource(Res.string.title_protocol)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = protocolExpanded,
                        onDismissRequest = { protocolExpanded = false },
                    ) {
                        protocolNames.forEachIndexed { index, item ->
                            DropdownMenuItem(
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
                    label = { Text(text = stringResource(Res.string.title_dport)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = raddr,
                    onValueChange = { raddr = it },
                    label = { Text(text = stringResource(Res.string.title_raddr)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rport,
                    onValueChange = { rport = it },
                    label = { Text(text = stringResource(Res.string.title_rport)) },
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
                            label = { Text(text = stringResource(Res.string.title_ruid)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ruleExpanded) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = ruleExpanded,
                            onDismissRequest = { ruleExpanded = false },
                        ) {
                            rules.forEachIndexed { index, rule ->
                                DropdownMenuItem(
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
