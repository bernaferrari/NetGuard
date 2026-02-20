package eu.faircode.netguard.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.booleanPreferencesKey
import eu.faircode.netguard.ActivityPro
import eu.faircode.netguard.DatabaseHelper
import eu.faircode.netguard.IAB
import eu.faircode.netguard.R
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.Util
import eu.faircode.netguard.data.Prefs
import eu.faircode.netguard.ui.theme.LocalMotion
import eu.faircode.netguard.ui.theme.spacing
import eu.faircode.netguard.ui.util.StatePlaceholder
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val LOGS_UI_MAX_ROWS = 2000

private enum class LogsOutcomeFilter {
    All,
    Allowed,
    Blocked,
}

private enum class LogsProtocolFilter {
    All,
    Udp,
    Tcp,
    Other,
}

private enum class LogsGroupMode {
    Timeline,
    ByApp,
}

private enum class LogCardPosition {
    First,
    Middle,
    Last,
    Single,
}

private data class LogQueryFlags(
    val udp: Boolean,
    val tcp: Boolean,
    val other: Boolean,
    val allowed: Boolean,
    val blocked: Boolean,
)

private data class AppDisplayInfo(
    val label: String,
    val icon: ImageBitmap?,
)

private data class AppPickerOption(
    val uid: Int,
    val label: String,
    val count: Int,
    val icon: ImageBitmap?,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current
    val prefsState by Prefs.data.collectAsState(initial = null)
    val hasLog = remember { IAB.isPurchased(ActivityPro.SKU_LOG, context) }
    val unknownSourceLabel = stringResource(R.string.ui_logs_unknown_source)
    val loggingEnabled = prefsState?.get(booleanPreferencesKey("log")) ?: false
    val filteringEnabled = prefsState?.get(booleanPreferencesKey("filter")) ?: false

    var outcomeFilter by remember { mutableStateOf(defaultOutcomeFilterFromPrefs()) }
    var protocolFilter by remember { mutableStateOf(defaultProtocolFilterFromPrefs()) }
    var groupMode by remember { mutableStateOf(LogsGroupMode.Timeline) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var selectedAppUid by remember { mutableStateOf<Int?>(null) }

    var entries by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    val appDisplayCache = remember { mutableMapOf<Int, AppDisplayInfo>() }
    fun appDisplay(uid: Int): AppDisplayInfo {
        if (uid <= 0) return AppDisplayInfo(label = unknownSourceLabel, icon = null)
        return appDisplayCache.getOrPut(uid) {
            loadAppDisplayInfo(uid = uid, context = context, fallbackLabel = unknownSourceLabel)
        }
    }
    fun appLabel(uid: Int): String = appDisplay(uid).label

    val queryFlags = remember(protocolFilter, outcomeFilter) {
        buildLogQueryFlags(protocolFilter, outcomeFilter)
    }
    val latestQueryFlags by rememberUpdatedState(queryFlags)

    val logListener = remember {
        DatabaseHelper.LogChangedListener {
            entries = runBlockingLogs(
                context = context,
                udp = latestQueryFlags.udp,
                tcp = latestQueryFlags.tcp,
                other = latestQueryFlags.other,
                allowed = latestQueryFlags.allowed,
                blocked = latestQueryFlags.blocked,
                limit = LOGS_UI_MAX_ROWS,
            )
            isLoading = false
        }
    }

    DisposableEffect(hasLog) {
        if (!hasLog) return@DisposableEffect onDispose { }
        DatabaseHelper.getInstance(context).addLogChangedListener(logListener)
        onDispose {
            DatabaseHelper.getInstance(context).removeLogChangedListener(logListener)
        }
    }

    LaunchedEffect(queryFlags) {
        Prefs.putBoolean("proto_udp", queryFlags.udp)
        Prefs.putBoolean("proto_tcp", queryFlags.tcp)
        Prefs.putBoolean("proto_other", queryFlags.other)
        Prefs.putBoolean("traffic_allowed", queryFlags.allowed)
        Prefs.putBoolean("traffic_blocked", queryFlags.blocked)
    }

    LaunchedEffect(refreshKey, queryFlags) {
        if (!hasLog) return@LaunchedEffect
        isLoading = true
        entries = loadLogs(
            context = context,
            udp = queryFlags.udp,
            tcp = queryFlags.tcp,
            other = queryFlags.other,
            allowed = queryFlags.allowed,
            blocked = queryFlags.blocked,
            limit = LOGS_UI_MAX_ROWS,
        )
        isLoading = false
    }

    val groupedEntries by remember(entries, groupMode) {
        derivedStateOf {
            if (groupMode != LogsGroupMode.ByApp) {
                emptyList()
            } else {
                entries
                    .groupBy { it.uid }
                    .toList()
                    .sortedBy { (uid, _) -> appLabel(uid).lowercase() }
            }
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
                    icon = display.icon,
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

    LaunchedEffect(groupedEntries, selectedAppUid) {
        val selectedUid = selectedAppUid ?: return@LaunchedEffect
        if (groupedEntries.none { (uid, _) -> uid == selectedUid }) {
            selectedAppUid = null
        }
    }

    LaunchedEffect(groupMode) {
        if (groupMode != LogsGroupMode.ByApp) selectedAppUid = null
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
                            text = stringResource(R.string.ui_logs_title),
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
                        IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.ui_logs_filters),
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
                    IconButton(onClick = { refreshKey += 1 }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.menu_refresh),
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
                        dampingRatio = Spring.DampingRatioMediumBouncy,
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
                    onOutcomeFilterChange = { outcomeFilter = it },
                    onProtocolFilterChange = { protocolFilter = it },
                    onGroupModeChange = { groupMode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = spacing.default, vertical = spacing.medium),
                )
            }

            if (!hasLog) {
                StatePlaceholder(
                    title = stringResource(R.string.title_pro),
                    message = stringResource(R.string.msg_log_disabled),
                    icon = Icons.Default.Inbox,
                    actionLabel = stringResource(R.string.title_pro),
                    onAction = { context.startActivity(Intent(context, ActivityPro::class.java)) },
                )
                return@Column
            }

            if (loggingEnabled && !filteringEnabled) {
                EnableFilteringBanner(
                    onEnableFiltering = {
                        Prefs.putBoolean("filter", true)
                        ServiceSinkhole.reload("logs_enable_filtering", context, false)
                        refreshKey += 1
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
                        title = stringResource(R.string.setting_log_app),
                        message = stringResource(R.string.summary_log_app),
                        icon = Icons.Default.Inbox,
                        actionLabel = stringResource(R.string.action_enable),
                        onAction = {
                            Prefs.putBoolean("log", true)
                            ServiceSinkhole.reload("logs", context, false)
                            refreshKey += 1
                        },
                    )
                }

                isLoading -> {
                    StatePlaceholder(
                        title = stringResource(R.string.ui_loading),
                        message = stringResource(R.string.home_logs_hint),
                        icon = Icons.Default.Inbox,
                        isLoading = true,
                    )
                }

                entries.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(R.string.ui_empty_logs_title),
                        message = stringResource(R.string.ui_empty_logs_body),
                        icon = Icons.Default.Inbox,
                    )
                }

                else -> {
                    AnimatedContent(
                        targetState = groupMode,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(motion.durationFast, easing = motion.easingDecelerate)) +
                                    slideInVertically(
                                        animationSpec = tween(motion.durationMedium, easing = motion.easingDecelerate),
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
                                            appIcon = display.icon,
                                            showAppName = true,
                                            position = cardPositionFor(index, entries.size),
                                            modifier = Modifier.animateItem(),
                                        )
                                    }
                                }
                            }

                            LogsGroupMode.ByApp -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    AppPickerField(
                                        options = appPickerOptions,
                                        selectedUid = selectedAppUid,
                                        onSelectUid = { selectedAppUid = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = spacing.default, vertical = spacing.small),
                                    )

                                    if (filteredGroupedEntries.isEmpty()) {
                                        StatePlaceholder(
                                            title = stringResource(R.string.ui_empty_apps_title),
                                            message = stringResource(R.string.ui_apps_search_empty),
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
                                                        appIcon = display.icon,
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
                                                        appIcon = display.icon,
                                                        showAppName = false,
                                                        position = cardPositionFor(index, appEntries.size),
                                                        modifier = Modifier.animateItem(),
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
                text = stringResource(R.string.title_enable_filtering),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = stringResource(R.string.title_enable_help2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            TextButton(onClick = onEnableFiltering) {
                Text(text = stringResource(R.string.action_enable))
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
    val allAppsLabel = stringResource(R.string.ui_filter_all)

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
                        placeholder = { Text(text = stringResource(R.string.menu_search)) },
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
                                        contentDescription = stringResource(R.string.action_clear_search),
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
                                icon = null,
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
                                icon = option.icon,
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
    icon: ImageBitmap?,
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
            AppIconAvatar(icon = icon, size = 20.dp)
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
            title = stringResource(R.string.ui_logs_filter_status),
            options = listOf(
                LogsOutcomeFilter.All to stringResource(R.string.ui_filter_all),
                LogsOutcomeFilter.Allowed to stringResource(R.string.menu_traffic_allowed),
                LogsOutcomeFilter.Blocked to stringResource(R.string.menu_traffic_blocked),
            ),
            selected = outcomeFilter,
            onSelect = onOutcomeFilterChange,
        )

        SegmentedFilterRow(
            title = stringResource(R.string.ui_logs_filter_protocol),
            options = listOf(
                LogsProtocolFilter.All to stringResource(R.string.ui_filter_all),
                LogsProtocolFilter.Udp to stringResource(R.string.menu_protocol_udp),
                LogsProtocolFilter.Tcp to stringResource(R.string.menu_protocol_tcp),
                LogsProtocolFilter.Other to stringResource(R.string.menu_protocol_other),
            ),
            selected = protocolFilter,
            onSelect = onProtocolFilterChange,
        )

        SegmentedFilterRow(
            title = stringResource(R.string.ui_logs_filter_view),
            options = listOf(
                LogsGroupMode.Timeline to stringResource(R.string.ui_logs_group_timeline),
                LogsGroupMode.ByApp to stringResource(R.string.ui_logs_group_by_app),
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
    appIcon: ImageBitmap?,
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
        AppIconAvatar(icon = appIcon, size = 20.dp)
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
    appIcon: ImageBitmap?,
    showAppName: Boolean,
    position: LogCardPosition,
    modifier: Modifier = Modifier,
) {
    val isAllowed = entry.allowed > 0

    val statusContainerColor = if (isAllowed) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    }
    val statusContentColor = if (isAllowed) {
        MaterialTheme.colorScheme.onPrimaryContainer
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
    val protocolText = entry.protocolLabel.uppercase(Locale.getDefault())
    val statusLabel = if (isAllowed) stringResource(R.string.menu_traffic_allowed)
    else stringResource(R.string.menu_traffic_blocked)
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
        LogCardPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        LogCardPosition.Last -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
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
            AppIconAvatar(icon = appIcon, fallbackIcon = fallbackAppIcon, size = 40.dp)
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
private fun AppIconAvatar(
    icon: ImageBitmap?,
    fallbackIcon: ImageVector = Icons.Default.Apps,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val corner = size / 3f
    val shape = RoundedCornerShape(corner)
    if (icon == null) {
        Surface(
            modifier = modifier.size(size),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size / 2f),
                )
            }
        }
    } else {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape),
        )
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

    val normalized = address.trim().lowercase(Locale.ROOT)
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

private fun defaultOutcomeFilterFromPrefs(): LogsOutcomeFilter {
    val allowed = Prefs.getBoolean("traffic_allowed", true)
    val blocked = Prefs.getBoolean("traffic_blocked", true)
    return when {
        allowed && !blocked -> LogsOutcomeFilter.Allowed
        !allowed && blocked -> LogsOutcomeFilter.Blocked
        else -> LogsOutcomeFilter.All
    }
}

private fun defaultProtocolFilterFromPrefs(): LogsProtocolFilter {
    val udp = Prefs.getBoolean("proto_udp", true)
    val tcp = Prefs.getBoolean("proto_tcp", true)
    val other = Prefs.getBoolean("proto_other", true)
    return when {
        udp && !tcp && !other -> LogsProtocolFilter.Udp
        !udp && tcp && !other -> LogsProtocolFilter.Tcp
        !udp && !tcp && other -> LogsProtocolFilter.Other
        else -> LogsProtocolFilter.All
    }
}

private fun buildLogQueryFlags(
    protocolFilter: LogsProtocolFilter,
    outcomeFilter: LogsOutcomeFilter,
): LogQueryFlags {
    val protocolFlags = when (protocolFilter) {
        LogsProtocolFilter.All -> Triple(true, true, true)
        LogsProtocolFilter.Udp -> Triple(true, false, false)
        LogsProtocolFilter.Tcp -> Triple(false, true, false)
        LogsProtocolFilter.Other -> Triple(false, false, true)
    }
    val outcomeFlags = when (outcomeFilter) {
        LogsOutcomeFilter.All -> Pair(true, true)
        LogsOutcomeFilter.Allowed -> Pair(true, false)
        LogsOutcomeFilter.Blocked -> Pair(false, true)
    }

    return LogQueryFlags(
        udp = protocolFlags.first,
        tcp = protocolFlags.second,
        other = protocolFlags.third,
        allowed = outcomeFlags.first,
        blocked = outcomeFlags.second,
    )
}

private data class LogEntry(
    val id: Long,
    val time: Long,
    val timeText: String,
    val protocolLabel: String,
    val daddr: String,
    val dport: Int,
    val dname: String?,
    val uid: Int,
    val allowed: Int,
)

private fun loadAppDisplayInfo(
    uid: Int,
    context: android.content.Context,
    fallbackLabel: String,
): AppDisplayInfo {
    val label = Util.getApplicationNames(uid, context).joinToString(", ").ifBlank { "UID $uid" }
    val icon = runCatching {
        val pm = context.packageManager
        pm.getPackagesForUid(uid)
            ?.asSequence()
            ?.mapNotNull { packageName ->
                runCatching {
                    pm.getApplicationIcon(packageName).toBitmap().asImageBitmap()
                }.getOrNull()
            }
            ?.firstOrNull()
    }.getOrNull()
    return AppDisplayInfo(label = label.ifBlank { fallbackLabel }, icon = icon)
}

private suspend fun loadLogs(
    context: android.content.Context,
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): List<LogEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<LogEntry>()
        DatabaseHelper.getInstance(context).getLog(udp, tcp, other, allowed, blocked, limit).use { cursor ->
            val colId = cursor.getColumnIndex("ID")
            val colTime = cursor.getColumnIndex("time")
            val colProtocol = cursor.getColumnIndex("protocol")
            val colDAddr = cursor.getColumnIndex("daddr")
            val colDPort = cursor.getColumnIndex("dport")
            val colDName = cursor.getColumnIndex("dname")
            val colUid = cursor.getColumnIndex("uid")
            val colAllowed = cursor.getColumnIndex("allowed")
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (cursor.moveToNext()) {
                val id = cursor.getLong(colId)
                val time = cursor.getLong(colTime)
                val protocol = if (cursor.isNull(colProtocol)) -1 else cursor.getInt(colProtocol)
                val daddr = cursor.getString(colDAddr)
                val dport = if (cursor.isNull(colDPort)) -1 else cursor.getInt(colDPort)
                val dname = if (cursor.isNull(colDName)) null else cursor.getString(colDName)
                val uid = if (cursor.isNull(colUid)) -1 else cursor.getInt(colUid)
                val allow = if (cursor.isNull(colAllowed)) -1 else cursor.getInt(colAllowed)
                result.add(
                    LogEntry(
                        id = id,
                        time = time,
                        timeText = timeFormat.format(time),
                        protocolLabel = Util.getProtocolName(protocol, 0, false),
                        daddr = daddr,
                        dport = dport,
                        dname = dname,
                        uid = uid,
                        allowed = allow,
                    ),
                )
            }
        }
        result
    }

private fun runBlockingLogs(
    context: android.content.Context,
    udp: Boolean,
    tcp: Boolean,
    other: Boolean,
    allowed: Boolean,
    blocked: Boolean,
    limit: Int,
): List<LogEntry> {
    return runCatching {
        runBlocking {
            loadLogs(context, udp, tcp, other, allowed, blocked, limit)
        }
    }.getOrDefault(emptyList())
}
