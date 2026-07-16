package com.bernaferari.renetguard.ui.screens

import com.bernaferari.renetguard.ui.screens.vm.LogsViewModel
import com.bernaferari.renetguard.platform.AppDisplayInfo
import com.bernaferari.renetguard.platform.LogEntry
import com.bernaferari.renetguard.platform.NetGuardPlatform
import com.bernaferari.renetguard.platform.PlatformContext
import com.bernaferari.renetguard.platform.showToast
import org.koin.compose.viewmodel.koinViewModel

import org.jetbrains.compose.resources.stringResource
import netguard.shared.generated.resources.Res
import netguard.shared.generated.resources.action_clear_search
import netguard.shared.generated.resources.action_enable
import netguard.shared.generated.resources.home_logs_hint
import netguard.shared.generated.resources.menu_protocol_other
import netguard.shared.generated.resources.menu_protocol_tcp
import netguard.shared.generated.resources.menu_protocol_udp
import netguard.shared.generated.resources.menu_refresh
import netguard.shared.generated.resources.menu_search
import netguard.shared.generated.resources.menu_traffic_allowed
import netguard.shared.generated.resources.menu_traffic_blocked
import netguard.shared.generated.resources.msg_log_disabled
import netguard.shared.generated.resources.setting_log_app
import netguard.shared.generated.resources.summary_log_app
import netguard.shared.generated.resources.title_enable_filtering
import netguard.shared.generated.resources.title_enable_help2
import netguard.shared.generated.resources.title_pro
import netguard.shared.generated.resources.ui_apps_search_empty
import netguard.shared.generated.resources.ui_empty_apps_title
import netguard.shared.generated.resources.ui_empty_logs_body
import netguard.shared.generated.resources.ui_empty_logs_title
import netguard.shared.generated.resources.ui_filter_all
import netguard.shared.generated.resources.ui_loading
import netguard.shared.generated.resources.ui_logs_filter_protocol
import netguard.shared.generated.resources.ui_logs_filter_status
import netguard.shared.generated.resources.ui_logs_filter_view
import netguard.shared.generated.resources.ui_logs_filters
import netguard.shared.generated.resources.ui_logs_group_by_app
import netguard.shared.generated.resources.ui_logs_group_timeline
import netguard.shared.generated.resources.ui_logs_title
import netguard.shared.generated.resources.ui_logs_unknown_source
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferari.renetguard.ui.components.AppIcon
import com.bernaferari.renetguard.ui.theme.LocalMotion
import com.bernaferari.renetguard.ui.theme.spacing
import com.bernaferari.renetguard.ui.util.StatePlaceholder

private val AllowedStatusContentLight = Color(0xFF1B5E20)
private val AllowedStatusContentDark = Color(0xFFA5D6A7)

private enum class LogCardPosition {
    First,
    Middle,
    Last,
    Single,
}


private data class AppPickerOption(
    val uid: Int,
    val label: String,
    val count: Int,
    val packageName: String?,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel = koinViewModel()) {
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current
    val logsUi by viewModel.uiState.collectAsState()
    val hasLog = remember { viewModel.hasLogPro() }
    val unknownSourceLabel = stringResource(Res.string.ui_logs_unknown_source)
    val isDemoMode = PlatformContext.isDemoMode()
    val loggingEnabled = logsUi.loggingEnabled
    val filteringEnabled = logsUi.filteringEnabled
    val outcomeFilter = logsUi.outcomeFilter
    val protocolFilter = logsUi.protocolFilter
    val groupMode = logsUi.groupMode
    val filtersExpanded = logsUi.filtersExpanded
    val selectedAppUid = logsUi.selectedAppUid
    val entries = logsUi.logs.data
    val isLoading = logsUi.logs.isLoading && loggingEnabled

    val appDisplayCache = remember { mutableMapOf<Int, AppDisplayInfo>() }
    fun appDisplay(uid: Int): AppDisplayInfo {
        if (uid <= 0) return AppDisplayInfo(label = unknownSourceLabel, packageName = null)
        return appDisplayCache.getOrPut(uid) {
            viewModel.appDisplay(uid, unknownSourceLabel)
        }
    }

    fun appLabel(uid: Int): String = appDisplay(uid).label

    val groupedEntries by remember(entries) {
        derivedStateOf {
            entries
                .groupBy { it.uid }
                .toList()
                .sortedBy { (uid, _) -> appLabel(uid).lowercase() }
        }
    }
    val appPickerOptions by remember(groupedEntries) {
        derivedStateOf {
            groupedEntries.map { (uid, appEntries) ->
                val display = appDisplay(uid)
                AppPickerOption(
                    uid = uid,
                    label = display.label,
                    count = appEntries.size,
                    packageName = display.packageName,
                )
            }
        }
    }
    val filteredGroupedEntries by remember(groupedEntries, selectedAppUid) {
        derivedStateOf {
            val selectedUid = selectedAppUid
            if (selectedUid == null) groupedEntries
            else groupedEntries.filter { (uid, _) -> uid == selectedUid }
        }
    }

    // Count active filter overrides to show a badge on the filter button
    val activeFilterCount = remember(outcomeFilter, protocolFilter, groupMode) {
        listOf(
            outcomeFilter != LogsOutcomeFilter.All,
            protocolFilter != LogsProtocolFilter.All,
            groupMode != LogsGroupMode.Timeline,
        ).count { it }
    }

    val filterIconTint by animateColorAsState(
        targetValue = if (filtersExpanded || activeFilterCount > 0)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(motion.durationFast),
        label = "filterTint",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Text(
                            text = stringResource(Res.string.ui_logs_title),
                            fontWeight = FontWeight.Bold,
                        )
                        if (!isLoading && entries.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = entries.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = spacing.small,
                                        vertical = 2.dp,
                                    ),
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Filter toggle button — shows a dot badge when filters are active
                    Box {
                        IconButton(onClick = { viewModel.setFiltersExpanded(!filtersExpanded) }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(Res.string.ui_logs_filters),
                                tint = filterIconTint,
                            )
                        }
                        // Active filter dot badge
                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.extraLarge,
                                    ),
                            )
                        }
                    }
                    IconButton(onClick = { /* Room/DB flow auto-refreshes */ }) {
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
                .padding(padding),
        ) {
            // Collapsible filter panel — slides in/out below the TopAppBar
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    expandFrom = Alignment.Top,
                ) + fadeIn(tween(motion.durationFast)),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(tween(motion.durationFast)),
            ) {
                LogsFilterPanel(
                    outcomeFilter = outcomeFilter,
                    protocolFilter = protocolFilter,
                    groupMode = groupMode,
                    onOutcomeFilterChange = { viewModel.setOutcomeFilter(it) },
                    onProtocolFilterChange = { viewModel.setProtocolFilter(it) },
                    onGroupModeChange = { viewModel.setGroupMode(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = spacing.default, vertical = spacing.medium),
                )
            }

            if (!hasLog) {
                StatePlaceholder(
                    title = stringResource(Res.string.title_pro),
                    message = stringResource(Res.string.msg_log_disabled),
                    icon = Icons.Default.Inbox,
                    actionLabel = stringResource(Res.string.title_pro),
                    onAction = { NetGuardPlatform.proFeatures.openProScreen() },
                )
                return@Column
            }

            if (loggingEnabled && !filteringEnabled) {
                EnableFilteringBanner(
                    onEnableFiltering = {
                        viewModel.enableFiltering()
                        NetGuardPlatform.firewall.reload("logs_enable_filtering", false)
                        /* flow refreshes */
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.default)
                        .padding(top = spacing.small),
                )
            }

            when {
                !loggingEnabled -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.setting_log_app),
                        message = stringResource(Res.string.summary_log_app),
                        icon = Icons.Default.Inbox,
                        actionLabel = stringResource(Res.string.action_enable),
                        onAction = {
                            viewModel.enableLogging()
                            NetGuardPlatform.firewall.reload("logs", false)
                            /* flow refreshes */
                        },
                    )
                }

                isLoading -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_loading),
                        message = stringResource(Res.string.home_logs_hint),
                        icon = Icons.Default.Inbox,
                        isLoading = true,
                    )
                }

                entries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_logs_title),
                        message = stringResource(Res.string.ui_empty_logs_body),
                        icon = Icons.Default.Inbox,
                    )
                }

                else -> {
                    AnimatedContent(
                        targetState = groupMode,
                        transitionSpec = {
                            (fadeIn(
                                animationSpec = tween(
                                    motion.durationFast,
                                    easing = motion.easingDecelerate
                                )
                            ) +
                                    slideInVertically(
                                        animationSpec = tween(
                                            motion.durationMedium,
                                            easing = motion.easingDecelerate
                                        ),
                                    ) { it / 8 })
                                .togetherWith(
                                    fadeOut(
                                        animationSpec = tween(
                                            motion.durationFast,
                                            easing = motion.easingAccelerate
                                        )
                                    ) +
                                            slideOutVertically(
                                                animationSpec = tween(
                                                    motion.durationFast,
                                                    easing = motion.easingAccelerate
                                                ),
                                            ) { -it / 12 },
                                )
                        },
                        label = "logsModeSwitch",
                    ) { mode ->
                        when (mode) {
                            LogsGroupMode.Timeline -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        horizontal = spacing.default,
                                        vertical = spacing.small,
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    itemsIndexed(
                                        items = entries,
                                        key = { _, entry -> entry.id },
                                    ) { index, entry ->
                                        val display = appDisplay(entry.uid)
                                        LogEntryCard(
                                            entry = entry,
                                            appName = display.label,
                                            packageName = display.packageName,
                                            showAppName = true,
                                            position = cardPositionFor(index, entries.size),
                                            modifier = Modifier,
                                        )
                                    }
                                }
                            }

                            LogsGroupMode.ByApp -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    AppPickerField(
                                        options = appPickerOptions,
                                        selectedUid = selectedAppUid,
                                        onSelectUid = { viewModel.selectAppUid(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = spacing.default,
                                                vertical = spacing.small
                                            ),
                                    )

                                    if (filteredGroupedEntries.isEmpty()) {
                                        StatePlaceholder(
                                            title = stringResource(Res.string.ui_empty_apps_title),
                                            message = stringResource(Res.string.ui_apps_search_empty),
                                            icon = Icons.Default.Apps,
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(
                                                horizontal = spacing.default,
                                                vertical = spacing.small,
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(0.dp),
                                        ) {
                                            filteredGroupedEntries.forEach { (uid, appEntries) ->
                                                val display = appDisplay(uid)
                                                item(key = "group_$uid") {
                                                    AppLogGroupHeader(
                                                        appName = display.label,
                                                        packageName = display.packageName,
                                                        count = appEntries.size,
                                                        modifier = Modifier
                                                            .padding(top = 2.dp, bottom = 0.dp)
                                                            .animateContentSize(),
                                                    )
                                                }
                                                itemsIndexed(
                                                    items = appEntries,
                                                    key = { _, entry -> entry.id },
                                                ) { index, entry ->
                                                    LogEntryCard(
                                                        entry = entry,
                                                        appName = display.label,
                                                        packageName = display.packageName,
                                                        showAppName = false,
                                                        position = cardPositionFor(
                                                            index,
                                                            appEntries.size
                                                        ),
                                                        modifier = Modifier,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnableFilteringBanner(
    onEnableFiltering: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.title_enable_filtering),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = stringResource(Res.string.title_enable_help2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            TextButton(onClick = onEnableFiltering) {
                Text(text = stringResource(Res.string.action_enable))
            }
        }
    }
}

@Composable
private fun AppPickerField(
    options: List<AppPickerOption>,
    selectedUid: Int?,
    onSelectUid: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val allAppsLabel = stringResource(Res.string.ui_filter_all)

    val selectedLabel = options.firstOrNull { it.uid == selectedUid }?.label ?: allAppsLabel
    val filteredOptions = remember(options, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) options
        else options.filter { option ->
            option.label.contains(query, ignoreCase = true) || option.uid.toString().contains(query)
        }
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = options.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp),
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(90)),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .widthIn(min = 280.dp, max = 520.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(spacing.small),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { Text(text = stringResource(Res.string.menu_search)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.action_clear_search),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        item(key = "all_apps_option") {
                            AppPickerRow(
                                label = allAppsLabel,
                                count = options.size,
                                packageName = null,
                                selected = selectedUid == null,
                                onClick = {
                                    onSelectUid(null)
                                    expanded = false
                                    searchQuery = ""
                                },
                            )
                        }
                        items(
                            items = filteredOptions,
                            key = { it.uid },
                        ) { option ->
                            AppPickerRow(
                                label = option.label,
                                count = option.count,
                                packageName = option.packageName,
                                selected = selectedUid == option.uid,
                                onClick = {
                                    onSelectUid(option.uid)
                                    expanded = false
                                    searchQuery = ""
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    label: String,
    count: Int,
    packageName: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = rowColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppIcon(packageName = packageName, size = 20.dp, cornerRadius = 7.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogsFilterPanel(
    outcomeFilter: LogsOutcomeFilter,
    protocolFilter: LogsProtocolFilter,
    groupMode: LogsGroupMode,
    onOutcomeFilterChange: (LogsOutcomeFilter) -> Unit,
    onProtocolFilterChange: (LogsProtocolFilter) -> Unit,
    onGroupModeChange: (LogsGroupMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        SegmentedFilterRow(
            title = stringResource(Res.string.ui_logs_filter_status),
            options = listOf(
                LogsOutcomeFilter.All to stringResource(Res.string.ui_filter_all),
                LogsOutcomeFilter.Allowed to stringResource(Res.string.menu_traffic_allowed),
                LogsOutcomeFilter.Blocked to stringResource(Res.string.menu_traffic_blocked),
            ),
            selected = outcomeFilter,
            onSelect = onOutcomeFilterChange,
        )

        SegmentedFilterRow(
            title = stringResource(Res.string.ui_logs_filter_protocol),
            options = listOf(
                LogsProtocolFilter.All to stringResource(Res.string.ui_filter_all),
                LogsProtocolFilter.Udp to stringResource(Res.string.menu_protocol_udp),
                LogsProtocolFilter.Tcp to stringResource(Res.string.menu_protocol_tcp),
                LogsProtocolFilter.Other to stringResource(Res.string.menu_protocol_other),
            ),
            selected = protocolFilter,
            onSelect = onProtocolFilterChange,
        )

        SegmentedFilterRow(
            title = stringResource(Res.string.ui_logs_filter_view),
            options = listOf(
                LogsGroupMode.Timeline to stringResource(Res.string.ui_logs_group_timeline),
                LogsGroupMode.ByApp to stringResource(Res.string.ui_logs_group_by_app),
            ),
            selected = groupMode,
            onSelect = onGroupModeChange,
        )
    }
}

@Composable
private fun <T> SegmentedFilterRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val spacing = MaterialTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLogGroupHeader(
    appName: String,
    packageName: String?,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(packageName = packageName, size = 20.dp, cornerRadius = 7.dp)
        Text(
            text = appName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    appName: String,
    packageName: String?,
    showAppName: Boolean,
    position: LogCardPosition,
    modifier: Modifier = Modifier,
) {
    val isAllowed = entry.allowed > 0
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val allowedStatusContentColor =
        if (isDarkSurface) AllowedStatusContentDark else AllowedStatusContentLight

    val statusContainerColor = if (isAllowed) {
        allowedStatusContentColor.copy(alpha = if (isDarkSurface) 0.24f else 0.16f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    }
    val statusContentColor = if (isAllowed) {
        allowedStatusContentColor
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val destinationPresentation = remember(entry.daddr, entry.dport, entry.dname) {
        buildDestinationPresentation(
            address = entry.daddr,
            port = entry.dport,
            domain = entry.dname,
        )
    }
    val fallbackAppIcon = if (entry.uid > 0) Icons.Default.Apps else Icons.Default.Public
    val protocolText = entry.protocolLabel.uppercase()
    val statusLabel = if (isAllowed) stringResource(Res.string.menu_traffic_allowed)
    else stringResource(Res.string.menu_traffic_blocked)
    val metadataText = buildString {
        append(entry.timeText)
        append(" • ")
        append(protocolText)
        destinationPresentation.detail?.let {
            append(" • ")
            append(it)
        }
    }
    val shape = when (position) {
        LogCardPosition.Single -> RoundedCornerShape(16.dp)
        LogCardPosition.First -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )

        LogCardPosition.Last -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )

        LogCardPosition.Middle -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val contentVerticalPadding = if (showAppName) 12.dp else 13.dp
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = contentVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(
                packageName = packageName,
                size = 40.dp,
                cornerRadius = 12.dp,
                fallbackIcon = fallbackAppIcon,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (showAppName) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = destinationPresentation.headline,
                    style = if (showAppName) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = if (showAppName) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (showAppName) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatusTextBadge(
                        text = statusLabel,
                        containerColor = statusContainerColor,
                        contentColor = statusContentColor,
                        icon = if (isAllowed) Icons.Default.CheckCircle else Icons.Default.Block,
                    )
                    MetaTextBadge(text = metadataText)
                }
            }
        }
    }
}

@Composable
private fun StatusTextBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = contentColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun MetaTextBadge(text: String) {
    val spacing = MaterialTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.extraSmall, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDestinationAddress(address: String, port: Int): String {
    if (port <= 0) return address
    return "$address:$port"
}

private data class DestinationPresentation(
    val headline: String,
    val detail: String?,
)

private fun buildDestinationPresentation(
    address: String,
    port: Int,
    domain: String?,
): DestinationPresentation {
    val formattedAddress = formatDestinationAddress(address, port)
    val domainText = domain?.trim().orEmpty()
    if (domainText.isNotEmpty() && !domainText.equals(address, ignoreCase = true)) {
        return DestinationPresentation(
            headline = domainText,
            detail = formattedAddress,
        )
    }

    val normalized = address.trim().lowercase()
    val multicastHeadline = when {
        normalized == "ff02::1" -> "All nodes (IPv6 multicast)"
        normalized == "ff02::2" -> "All routers (IPv6 multicast)"
        normalized.startsWith("ff02::") -> "Local network multicast (IPv6)"
        normalized.startsWith("ff") -> "Multicast traffic (IPv6)"
        isIpv4MulticastAddress(normalized) -> "Multicast traffic (IPv4)"
        else -> null
    }
    return if (multicastHeadline == null) {
        DestinationPresentation(
            headline = formattedAddress,
            detail = null,
        )
    } else {
        DestinationPresentation(
            headline = multicastHeadline,
            detail = formattedAddress,
        )
    }
}

private fun isIpv4MulticastAddress(address: String): Boolean {
    val firstOctet = address.substringBefore('.').toIntOrNull() ?: return false
    return firstOctet in 224..239
}

private fun cardPositionFor(index: Int, totalCount: Int): LogCardPosition {
    return when {
        totalCount <= 1 -> LogCardPosition.Single
        index == 0 -> LogCardPosition.First
        index == totalCount - 1 -> LogCardPosition.Last
        else -> LogCardPosition.Middle
    }
}


