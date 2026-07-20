package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols




import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.quietguard.ui.components.groupItemShape
import com.bernaferrari.quietguard.ui.components.AppIcon
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.platform.ForwardingEntry
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.loadAllRulesForPicker
import com.bernaferrari.quietguard.ui.screens.vm.ForwardingListFilter
import com.bernaferrari.quietguard.ui.screens.vm.ForwardingViewModel
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.ui.util.LoadErrorPlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.action_back
import com.bernaferrari.quietguard.generated.resources.menu_add
import com.bernaferrari.quietguard.generated.resources.menu_cancel
import com.bernaferrari.quietguard.generated.resources.menu_delete
import com.bernaferrari.quietguard.generated.resources.menu_protocol_tcp
import com.bernaferrari.quietguard.generated.resources.menu_protocol_udp
import com.bernaferrari.quietguard.generated.resources.protocolNames
import com.bernaferrari.quietguard.generated.resources.protocolValues
import com.bernaferrari.quietguard.generated.resources.setting_forwarding
import com.bernaferrari.quietguard.generated.resources.title_dport
import com.bernaferrari.quietguard.generated.resources.title_raddr
import com.bernaferrari.quietguard.generated.resources.title_rport
import com.bernaferrari.quietguard.generated.resources.title_ruid
import com.bernaferrari.quietguard.generated.resources.ui_empty_forwarding_body
import com.bernaferrari.quietguard.generated.resources.ui_empty_forwarding_title
import com.bernaferrari.quietguard.generated.resources.ui_filter_all
import com.bernaferrari.quietguard.generated.resources.ui_forwarding_filter_empty
import com.bernaferrari.quietguard.generated.resources.ui_forwarding_title
import com.bernaferrari.quietguard.generated.resources.ui_loading
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

import com.bernaferrari.quietguard.ui.icons.Icon
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForwardingScreen(
    onBack: () -> Unit = {},
    viewModel: ForwardingViewModel = koinViewModel(),
) {
    val spacing = MaterialTheme.spacing
    val fwdUi by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = fwdUi.entries.data
    val loading = !fwdUi.entries.isReady && entries.isEmpty() && !fwdUi.entries.hasFailed
    val protocolFilter = fwdUi.protocolFilter
    val showDialog = fwdUi.showAddDialog
    val filteredEntries = fwdUi.filtered

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                title = { Text(text = stringResource(Res.string.ui_forwarding_title)) },
                actions = {
                    IconButton(onClick = { viewModel.setShowAddDialog(true) }) {
                        Icon(
                            icon = MaterialSymbols.Filled.Add,
                            contentDescription = stringResource(Res.string.menu_add),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                fwdUi.entries.hasFailed && entries.isEmpty() -> {
                    LoadErrorPlaceholder(
                        icon = MaterialSymbols.AutoMirrored.Filled.Forward,
                        onRetry = viewModel::retry,
                    )
                }

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
                    ForwardingResults(
                        entries = filteredEntries,
                        protocolFilter = protocolFilter,
                        onFilterChanged = viewModel::setProtocolFilter,
                        onDelete = viewModel::deleteForward,
                    )
                }

                else -> {
                    ForwardingResults(
                        entries = filteredEntries,
                        protocolFilter = protocolFilter,
                        onFilterChanged = viewModel::setProtocolFilter,
                        onDelete = viewModel::deleteForward,
                    )
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
private fun ForwardingResults(
    entries: List<ForwardingEntry>,
    protocolFilter: ForwardingListFilter,
    onFilterChanged: (ForwardingListFilter) -> Unit,
    onDelete: (ForwardingEntry) -> Unit,
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.default),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
        ) {
            val options = listOf(
                ForwardingListFilter.All to stringResource(Res.string.ui_filter_all),
                ForwardingListFilter.Udp to stringResource(Res.string.menu_protocol_udp),
                ForwardingListFilter.Tcp to stringResource(Res.string.menu_protocol_tcp),
            )
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = protocolFilter == value,
                    onClick = { onFilterChanged(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = label, maxLines = 1)
                }
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                StatePlaceholder(
                    title = stringResource(Res.string.ui_forwarding_title),
                    message = stringResource(Res.string.ui_forwarding_filter_empty),
                    icon = MaterialSymbols.AutoMirrored.Filled.Forward,
                    actionLabel = stringResource(Res.string.ui_filter_all),
                    onAction = { onFilterChanged(ForwardingListFilter.All) },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(
                    entries,
                    key = { _, it -> "${it.protocol}_${it.dport}_${it.raddr}_${it.rport}" },
                ) { index, entry ->
                    ForwardingEntryCard(
                        entry = entry,
                        shape = groupItemShape(
                            isFirst = index == 0,
                            isLast = index == entries.lastIndex,
                        ),
                        onDelete = { onDelete(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ForwardingEntryCard(
    entry: ForwardingEntry,
    shape: Shape,
    onDelete: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val protocolLabel = NetGuardPlatform.uiHelpers.getProtocolName(entry.protocol, 0, false)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    Text(
                        text = entry.dport.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    Icon(
                        icon = MaterialSymbols.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "${entry.raddr}:${entry.rport}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
@Composable
private fun ForwardingAddDialog(
    onDismiss: () -> Unit,
    onAdd: (protocol: Int, dport: Int, raddr: String, rport: Int, ruid: Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val protocolNames = stringArrayResource(Res.array.protocolNames)
    val protocolValues = stringArrayResource(Res.array.protocolValues)
    var protocolIndex by remember { mutableStateOf(0) }
    var dport by remember { mutableStateOf("") }
    var raddr by remember { mutableStateOf("") }
    var rport by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf<List<FirewallRule>>(emptyList()) }
    var rulesLoading by remember { mutableStateOf(true) }
    var selectedRuleUid by remember { mutableStateOf<Int?>(null) }
    var ruleMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        rules = loadAllRulesForPicker()
        rulesLoading = false
    }

    val dportValue = dport.toIntOrNull()?.takeIf { it in 1..65535 }
    val rportValue = rport.toIntOrNull()?.takeIf { it in 1..65535 }
    val selectedRule = rules.firstOrNull { it.uid == selectedRuleUid }
    val canAdd =
        dportValue != null && rportValue != null && raddr.isNotBlank() && selectedRule != null

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.ui_forwarding_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    protocolNames.forEachIndexed { index, name ->
                        SegmentedButton(
                            selected = protocolIndex == index,
                            onClick = { protocolIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = protocolNames.size,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = name, maxLines = 1)
                        }
                    }
                }

                TextField(
                    value = dport,
                    onValueChange = { dport = it.filter(Char::isDigit).take(5) },
                    label = { Text(text = stringResource(Res.string.title_dport)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors,
                    singleLine = true,
                )
                TextField(
                    value = raddr,
                    onValueChange = { raddr = it },
                    label = { Text(text = stringResource(Res.string.title_raddr)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors,
                    singleLine = true,
                )
                TextField(
                    value = rport,
                    onValueChange = { rport = it.filter(Char::isDigit).take(5) },
                    label = { Text(text = stringResource(Res.string.title_rport)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = fieldShape,
                    colors = fieldColors,
                    singleLine = true,
                )

                // Destination app picker backed by a plain DropdownMenu, which anchors
                // reliably inside dialogs (ExposedDropdownMenuBox does not).
                Box {
                    Surface(
                        onClick = {
                            if (!rulesLoading && rules.isNotEmpty()) ruleMenuExpanded = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 56.dp)
                                .padding(horizontal = spacing.default),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        ) {
                            if (rulesLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else if (selectedRule != null) {
                                AppIcon(
                                    packageName = selectedRule.packageName,
                                    displayName = selectedRule.name,
                                    size = 32.dp,
                                    cornerRadius = 10.dp,
                                    fallbackIcon = MaterialSymbols.Filled.Apps,
                                )
                            } else {
                                Icon(
                                    icon = MaterialSymbols.Filled.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = selectedRule?.name
                                    ?: selectedRule?.packageName
                                    ?: stringResource(Res.string.title_ruid),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedRule != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                icon = MaterialSymbols.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = ruleMenuExpanded,
                        onDismissRequest = { ruleMenuExpanded = false },
                    ) {
                        rules.forEach { rule ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = rule.name
                                            ?: rule.packageName
                                            ?: rule.uid.toString(),
                                    )
                                },
                                leadingIcon = {
                                    AppIcon(
                                        packageName = rule.packageName,
                                        displayName = rule.name,
                                        size = 32.dp,
                                        cornerRadius = 10.dp,
                                        fallbackIcon = MaterialSymbols.Filled.Apps,
                                    )
                                },
                                onClick = {
                                    selectedRuleUid = rule.uid
                                    ruleMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canAdd,
                onClick = {
                    val protocol =
                        protocolValues.getOrNull(protocolIndex)?.toIntOrNull()
                            ?: return@TextButton
                    onAdd(protocol, dportValue!!, raddr.trim(), rportValue!!, selectedRule!!.uid)
                },
            ) {
                Text(text = stringResource(Res.string.menu_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.menu_cancel))
            }
        },
    )
}
