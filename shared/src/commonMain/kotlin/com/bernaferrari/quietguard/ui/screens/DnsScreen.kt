package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.showToast
import com.bernaferrari.quietguard.ui.screens.vm.DnsListFilter
import com.bernaferrari.quietguard.ui.screens.vm.DnsViewModel
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.ui.util.LoadErrorPlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.label_dns_summary
import com.bernaferrari.quietguard.generated.resources.label_ttl
import com.bernaferrari.quietguard.generated.resources.label_uid
import com.bernaferrari.quietguard.generated.resources.menu_cleanup
import com.bernaferrari.quietguard.generated.resources.menu_clear
import com.bernaferrari.quietguard.generated.resources.menu_export
import com.bernaferrari.quietguard.generated.resources.action_back
import com.bernaferrari.quietguard.generated.resources.menu_refresh
import com.bernaferrari.quietguard.generated.resources.msg_completed
import com.bernaferrari.quietguard.generated.resources.msg_invalid
import com.bernaferrari.quietguard.generated.resources.ui_dns_active
import com.bernaferrari.quietguard.generated.resources.ui_dns_expired
import com.bernaferrari.quietguard.generated.resources.ui_dns_filter_empty
import com.bernaferrari.quietguard.generated.resources.ui_dns_hint
import com.bernaferrari.quietguard.generated.resources.ui_dns_title
import com.bernaferrari.quietguard.generated.resources.ui_empty_dns_body
import com.bernaferrari.quietguard.generated.resources.ui_empty_dns_title
import com.bernaferrari.quietguard.generated.resources.ui_filter_all
import com.bernaferrari.quietguard.generated.resources.ui_loading
import com.bernaferrari.quietguard.generated.resources.ui_logs_filter_status
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

import com.bernaferrari.quietguard.ui.icons.Icon
@ExperimentalMaterial3Api
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnsScreen(
    onBack: () -> Unit = {},
    viewModel: DnsViewModel = koinViewModel(),
) {
    val spacing = MaterialTheme.spacing
    val dnsUi by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = dnsUi.entries.data
    val isLoading = !dnsUi.entries.isReady && entries.isEmpty() && !dnsUi.entries.hasFailed
    val dnsFilter = dnsUi.filter
    val now = dnsUi.nowMs
    val filteredEntries = dnsUi.filtered
    val expiredCount = dnsUi.expiredCount
    val completedMessage = stringResource(Res.string.msg_completed)
    val invalidMessage = stringResource(Res.string.msg_invalid)

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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Text(
                            text = stringResource(Res.string.ui_dns_title),
                            fontWeight = FontWeight.Bold,
                        )
                        if (!isLoading && filteredEntries.isNotEmpty()) {
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
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            icon = MaterialSymbols.Filled.Refresh,
                            contentDescription = stringResource(Res.string.menu_refresh),
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
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                    maxItemsInEachRow = 2,
                ) {
                    FilledTonalButton(onClick = { viewModel.cleanup() }) {
                        Icon(
                            icon = MaterialSymbols.Filled.Tune,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.menu_cleanup))
                    }
                    OutlinedButton(onClick = { viewModel.clearAll() }) {
                        Icon(
                            icon = MaterialSymbols.Filled.Delete,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.menu_clear))
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.export { success, error ->
                                if (success) {
                                    showToast(completedMessage)
                                } else {
                                    showToast(error ?: invalidMessage)
                                }
                            }
                        },
                    ) {
                        Icon(
                            icon = MaterialSymbols.Filled.Download,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.menu_export))
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Text(
                        text = stringResource(Res.string.ui_logs_filter_status),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val options = listOf(
                            DnsListFilter.All to stringResource(Res.string.ui_filter_all),
                            DnsListFilter.Active to stringResource(Res.string.ui_dns_active),
                            DnsListFilter.Expired to stringResource(Res.string.ui_dns_expired),
                        )
                        options.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = dnsFilter == value,
                                onClick = { viewModel.setFilter(value) },
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
                dnsUi.entries.hasFailed && entries.isEmpty() -> {
                    LoadErrorPlaceholder(
                        icon = MaterialSymbols.Filled.Dns,
                        onRetry = viewModel::refresh,
                    )
                }

                isLoading -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_loading),
                        message = stringResource(Res.string.ui_dns_hint),
                        icon = MaterialSymbols.Filled.Dns,
                        isLoading = true,
                    )
                }

                filteredEntries.isEmpty() && entries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_dns_title),
                        message = stringResource(Res.string.ui_empty_dns_body),
                        icon = MaterialSymbols.Filled.Dns,
                        actionLabel = stringResource(Res.string.menu_refresh),
                        onAction = viewModel::refresh,
                    )
                }

                filteredEntries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_dns_title),
                        message = stringResource(Res.string.ui_dns_filter_empty),
                        icon = MaterialSymbols.Filled.Dns,
                        actionLabel = stringResource(Res.string.ui_filter_all),
                        onAction = { viewModel.setFilter(DnsListFilter.All) },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        items(
                            filteredEntries,
                            key = { "${it.qname}_${it.aname}_${it.resource}_${it.time}" }) { entry ->
                            val expired = entry.time + entry.ttl < now
                            DnsEntryCard(
                                entry = entry,
                                expired = expired,
                            )
                        }
                    }
                }
            }

            if (!isLoading && entries.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.label_dns_summary, entries.size, expiredCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DnsEntryCard(
    entry: DnsEntry,
    expired: Boolean,
) {
    val spacing = MaterialTheme.spacing
    val statusContainer =
        if (expired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val statusContent =
        if (expired) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        colors = CardDefaults.cardColors(
            containerColor =
                if (expired) MaterialTheme.colorScheme.surfaceContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${entry.qname} → ${entry.aname}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = statusContainer,
                    contentColor = statusContent,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        text = if (expired) {
                            stringResource(Res.string.ui_dns_expired)
                        } else {
                            stringResource(Res.string.ui_dns_active)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = spacing.small, vertical = 2.dp),
                    )
                }
            }

            Text(
                text = entry.resource,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        text = stringResource(Res.string.label_ttl, (entry.ttl / 1000).toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.small, vertical = 2.dp),
                    )
                }
                if (entry.uid > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(
                            text = stringResource(Res.string.label_uid, entry.uid.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = spacing.small,
                                vertical = 2.dp
                            ),
                        )
                    }
                }
            }
        }
    }
}
