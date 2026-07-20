package com.bernaferrari.quietguard.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.action_back
import com.bernaferrari.quietguard.generated.resources.action_more_options
import com.bernaferrari.quietguard.generated.resources.label_dns_summary
import com.bernaferrari.quietguard.generated.resources.label_ttl
import com.bernaferrari.quietguard.generated.resources.label_uid
import com.bernaferrari.quietguard.generated.resources.menu_cleanup
import com.bernaferrari.quietguard.generated.resources.menu_clear
import com.bernaferrari.quietguard.generated.resources.menu_export
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
import com.bernaferrari.quietguard.platform.DnsEntry
import com.bernaferrari.quietguard.platform.showToast
import com.bernaferrari.quietguard.ui.components.groupItemShape
import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols
import com.bernaferrari.quietguard.ui.screens.vm.DnsListFilter
import com.bernaferrari.quietguard.ui.screens.vm.DnsViewModel
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.util.LoadErrorPlaceholder
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(
    onBack: () -> Unit = {},
    viewModel: DnsViewModel = koinViewModel(),
) {
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current
    val dnsUi by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = dnsUi.entries.data
    val isLoading = !dnsUi.entries.isReady && entries.isEmpty() && !dnsUi.entries.hasFailed
    val completedMessage = stringResource(Res.string.msg_completed)
    val invalidMessage = stringResource(Res.string.msg_invalid)
    var showActions by remember { mutableStateOf(false) }
    var refreshTurns by remember { mutableIntStateOf(0) }
    var refreshAnimating by remember { mutableStateOf(false) }
    val refreshRotation by animateFloatAsState(
        targetValue = refreshTurns * 360f,
        animationSpec = tween(
            durationMillis = if (motion.reducedMotion) 0 else 600,
            easing = LinearEasing,
        ),
        finishedListener = { refreshAnimating = false },
        label = "dnsRefreshRotation",
    )

    fun refreshWithFeedback() {
        if (refreshAnimating) return
        refreshAnimating = true
        refreshTurns += 1
        viewModel.refresh()
    }

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
                    Text(
                        text = stringResource(Res.string.ui_dns_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(
                        onClick = ::refreshWithFeedback,
                        enabled = !refreshAnimating,
                    ) {
                        Icon(
                            icon = MaterialSymbols.Filled.Refresh,
                            contentDescription = stringResource(Res.string.menu_refresh),
                            modifier = Modifier.graphicsLayer { rotationZ = refreshRotation },
                        )
                    }
                    if (entries.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showActions = true }) {
                                Icon(
                                    icon = MaterialSymbols.Filled.MoreVert,
                                    contentDescription = stringResource(Res.string.action_more_options),
                                )
                            }
                            DropdownMenu(
                                expanded = showActions,
                                onDismissRequest = { showActions = false },
                            ) {
                                if (dnsUi.expiredCount > 0) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.menu_cleanup)) },
                                        leadingIcon = {
                                            Icon(icon = MaterialSymbols.Filled.Tune, contentDescription = null)
                                        },
                                        onClick = {
                                            showActions = false
                                            viewModel.cleanup()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.menu_export)) },
                                    leadingIcon = {
                                        Icon(icon = MaterialSymbols.Filled.Download, contentDescription = null)
                                    },
                                    onClick = {
                                        showActions = false
                                        viewModel.export { success, error ->
                                            showToast(
                                                if (success) completedMessage else error ?: invalidMessage,
                                            )
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(Res.string.menu_clear),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            icon = MaterialSymbols.Filled.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showActions = false
                                        viewModel.clearAll()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
                dnsUi.entries.hasFailed && entries.isEmpty() -> {
                    LoadErrorPlaceholder(
                        icon = MaterialSymbols.Filled.Dns,
                        onRetry = ::refreshWithFeedback,
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

                entries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_dns_title),
                        message = stringResource(Res.string.ui_empty_dns_body),
                        icon = MaterialSymbols.Filled.Dns,
                        actionLabel = stringResource(Res.string.menu_refresh),
                        onAction = ::refreshWithFeedback,
                    )
                }

                else -> {
                    DnsResults(
                        entries = entries,
                        filteredEntries = dnsUi.filtered,
                        filter = dnsUi.filter,
                        nowMs = dnsUi.nowMs,
                        expiredCount = dnsUi.expiredCount,
                        onFilterChanged = viewModel::setFilter,
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsResults(
    entries: List<DnsEntry>,
    filteredEntries: List<DnsEntry>,
    filter: DnsListFilter,
    nowMs: Long,
    expiredCount: Int,
    onFilterChanged: (DnsListFilter) -> Unit,
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
                DnsListFilter.All to stringResource(Res.string.ui_filter_all),
                DnsListFilter.Active to stringResource(Res.string.ui_dns_active),
                DnsListFilter.Expired to stringResource(Res.string.ui_dns_expired),
            )
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = filter == value,
                    onClick = { onFilterChanged(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = label, maxLines = 1)
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                StatePlaceholder(
                    title = stringResource(Res.string.ui_dns_title),
                    message = stringResource(Res.string.ui_dns_filter_empty),
                    icon = MaterialSymbols.Filled.Dns,
                    actionLabel = stringResource(Res.string.ui_filter_all),
                    onAction = { onFilterChanged(DnsListFilter.All) },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = spacing.default),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(
                    items = filteredEntries,
                    key = { _, entry -> "${entry.qname}_${entry.aname}_${entry.resource}_${entry.time}" },
                ) { index, entry ->
                    DnsEntryCard(
                        entry = entry,
                        expired = entry.time + entry.ttl < nowMs,
                        shape = groupItemShape(
                            isFirst = index == 0,
                            isLast = index == filteredEntries.lastIndex,
                        ),
                    )
                }
                item {
                    Text(
                        text = stringResource(
                            Res.string.label_dns_summary,
                            entries.size,
                            expiredCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.default, vertical = spacing.small),
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsEntryCard(
    entry: DnsEntry,
    expired: Boolean,
    shape: Shape,
) {
    val spacing = MaterialTheme.spacing
    val alias = entry.aname.trim().takeIf { it.isNotEmpty() && it != entry.qname }
    val headline = if (alias == null) entry.qname else "${entry.qname} → $alias"
    val ttl = stringResource(Res.string.label_ttl, (entry.ttl / 1000).toString())
    val uid = entry.uid.takeIf { it > 0 }?.let {
        stringResource(Res.string.label_uid, it.toString())
    }
    val metadata = listOfNotNull(entry.resource.takeIf(String::isNotBlank), ttl, uid).joinToString(" • ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.default, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (expired) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = stringResource(Res.string.ui_dns_expired),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
