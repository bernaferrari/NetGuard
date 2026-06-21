package com.bernaferari.renetguard.ui.screens

import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.domain.FirewallRule
import com.bernaferari.renetguard.platform.*
import com.bernaferari.renetguard.platform.showToast
import org.koin.compose.koinInject

import org.jetbrains.compose.resources.stringResource
import netguard.shared.generated.resources.Res
import netguard.shared.generated.resources.label_dns_summary
import netguard.shared.generated.resources.label_ttl
import netguard.shared.generated.resources.label_uid
import netguard.shared.generated.resources.menu_cleanup
import netguard.shared.generated.resources.menu_clear
import netguard.shared.generated.resources.menu_export
import netguard.shared.generated.resources.menu_refresh
import netguard.shared.generated.resources.msg_completed
import netguard.shared.generated.resources.msg_invalid
import netguard.shared.generated.resources.ui_dns_active
import netguard.shared.generated.resources.ui_dns_expired
import netguard.shared.generated.resources.ui_dns_filter_empty
import netguard.shared.generated.resources.ui_dns_hint
import netguard.shared.generated.resources.ui_dns_title
import netguard.shared.generated.resources.ui_empty_dns_body
import netguard.shared.generated.resources.ui_empty_dns_title
import netguard.shared.generated.resources.ui_filter_all
import netguard.shared.generated.resources.ui_loading
import netguard.shared.generated.resources.ui_logs_filter_status
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferari.renetguard.platform.NetGuardPlatform
import com.bernaferari.renetguard.ui.theme.spacing
import com.bernaferari.renetguard.ui.util.StatePlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "NetGuard.DNS.Compose"

private enum class DnsFilter {
    All,
    Active,
    Expired,
}

@ExperimentalMaterial3Api
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DnsScreen() {
    val spacing = MaterialTheme.spacing
    var entries by remember { mutableStateOf<List<com.bernaferari.renetguard.platform.DnsEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var dnsFilter by remember { mutableStateOf(DnsFilter.All) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        isLoading = true
        entries = loadDnsEntries()
        isLoading = false
    }

    val now = remember(entries, refreshKey) { com.bernaferari.renetguard.platform.currentTimeMillis() }
    val filteredEntries by remember(entries, dnsFilter, now) {
        derivedStateOf {
            entries.filter { entry ->
                val expired = entry.time + entry.ttl < now
                when (dnsFilter) {
                    DnsFilter.All -> true
                    DnsFilter.Active -> !expired
                    DnsFilter.Expired -> expired
                }
            }
        }
    }

    val expiredCount by remember(entries, now) {
        derivedStateOf { entries.count { it.time + it.ttl < now } }
    }
    val completedMessage = stringResource(Res.string.msg_completed)
    val invalidMessage = stringResource(Res.string.msg_invalid)

    Scaffold(
        topBar = {
            TopAppBar(
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
                    IconButton(onClick = { refreshKey += 1 }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
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
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                cleanupDns()
                                refreshKey++
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.menu_cleanup))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                clearDns()
                                refreshKey++
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.menu_clear))
                    }
                    OutlinedButton(
                        onClick = {
                            exportDnsToFile { success, error ->
                                if (success) {
                                    showToast(completedMessage)
                                } else {
                                    showToast(error ?: invalidMessage)
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
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
                            DnsFilter.All to stringResource(Res.string.ui_filter_all),
                            DnsFilter.Active to stringResource(Res.string.ui_dns_active),
                            DnsFilter.Expired to stringResource(Res.string.ui_dns_expired),
                        )
                        options.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = dnsFilter == value,
                                onClick = { dnsFilter = value },
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
                isLoading -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_loading),
                        message = stringResource(Res.string.ui_dns_hint),
                        icon = Icons.Default.Dns,
                        isLoading = true,
                    )
                }

                filteredEntries.isEmpty() && entries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_dns_title),
                        message = stringResource(Res.string.ui_empty_dns_body),
                        icon = Icons.Default.Dns,
                        actionLabel = stringResource(Res.string.menu_refresh),
                        onAction = { refreshKey += 1 },
                    )
                }

                filteredEntries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_dns_title),
                        message = stringResource(Res.string.ui_dns_filter_empty),
                        icon = Icons.Default.Dns,
                        actionLabel = stringResource(Res.string.ui_filter_all),
                        onAction = { dnsFilter = DnsFilter.All },
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