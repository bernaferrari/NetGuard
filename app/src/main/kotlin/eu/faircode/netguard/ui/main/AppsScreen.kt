package eu.faircode.netguard.ui.main

import android.app.NotificationManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import eu.faircode.netguard.R
import eu.faircode.netguard.Rule
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.Widgets
import eu.faircode.netguard.data.Prefs
import eu.faircode.netguard.ui.components.IndexedFastScroller
import eu.faircode.netguard.ui.theme.spacing
import eu.faircode.netguard.ui.util.StatePlaceholder

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (Rule) -> Unit = {},
    initialFilter: AppsFilter = AppsFilter.All,
    initialFilterVersion: Int = 0,
) {
    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()
    val rules = rulesUiState.rules
    val isLoading = rulesUiState.isLoading && rulesUiState.rules.isEmpty()
    val isRefreshing = rulesUiState.isLoading
    var filter by remember(initialFilterVersion) { mutableStateOf(initialFilter) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val normalizedSearchQuery = searchQuery.trim()

    LaunchedEffect(Unit) {
        viewModel.ensureRulesLoaded()
    }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(listState, isSearchOpen) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling && isSearchOpen) {
                    focusManager.clearFocus()
                }
            }
    }

    val filteredRules by remember(rulesUiState.rules, filter, normalizedSearchQuery) {
        derivedStateOf {
            val base = when (filter) {
                AppsFilter.All -> rules
                AppsFilter.Blocked -> rules.filter { it.wifi_blocked || it.other_blocked }
                AppsFilter.Allowed -> rules.filter { !it.wifi_blocked && !it.other_blocked }
            }
            if (normalizedSearchQuery.isEmpty()) base else base.filter { matchesAppQuery(it, normalizedSearchQuery) }
        }
    }
    val badgeCount by remember(filteredRules) {
        derivedStateOf { filteredRules.size }
    }

    // Group by first letter for section headers
    val groupedRules by remember(filteredRules) {
        derivedStateOf {
            val items = mutableListOf<AppListItem>()
            var lastLetter = ""
            filteredRules.forEach { rule ->
                val name = rule.name ?: rule.packageName.orEmpty()
                val letter = name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                val section = if (letter.first().isLetter()) letter else "#"
                if (section != lastLetter) {
                    items.add(AppListItem.Header(section))
                    lastLetter = section
                }
                items.add(AppListItem.App(rule, position = CardPosition.Middle))
            }
            // Assign positions within each section
            var i = 0
            while (i < items.size) {
                if (items[i] is AppListItem.Header) {
                    val sectionStart = i + 1
                    var sectionEnd = sectionStart
                    while (sectionEnd < items.size && items[sectionEnd] is AppListItem.App) sectionEnd++
                    val count = sectionEnd - sectionStart
                    for (j in sectionStart until sectionEnd) {
                        val pos = when {
                            count == 1 -> CardPosition.Single
                            j == sectionStart -> CardPosition.First
                            j == sectionEnd - 1 -> CardPosition.Last
                            else -> CardPosition.Middle
                        }
                        items[j] = (items[j] as AppListItem.App).copy(position = pos)
                    }
                    i = sectionEnd
                } else {
                    i++
                }
            }
            items
        }
    }
    val showFastScroller by remember(filteredRules) {
        derivedStateOf { filteredRules.size >= 24 }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        Text(
                            text = stringResource(R.string.ui_apps_title),
                            fontWeight = FontWeight.Bold,
                        )
                        if (badgeCount > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = badgeCount.toString(),
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
                    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = if (isRefreshing) 360f else 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing),
                        ),
                        label = "refreshRotation",
                    )
                    IconButton(
                        onClick = {
                            if (isSearchOpen) {
                                isSearchOpen = false
                                searchQuery = ""
                                focusManager.clearFocus()
                            } else {
                                isSearchOpen = true
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchOpen) {
                                stringResource(R.string.action_clear_search)
                            } else {
                                stringResource(R.string.menu_search)
                            },
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshRules() },
                        enabled = !isRefreshing,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.menu_refresh),
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (isRefreshing) rotation else 0f
                            },
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
            // Filter chips
            val filterOptions = listOf(
                Triple(
                    AppsFilter.All,
                    stringResource(R.string.ui_filter_all),
                    Icons.Filled.Tune
                ),
                Triple(
                    AppsFilter.Blocked,
                    stringResource(R.string.menu_traffic_blocked),
                    Icons.Filled.Block
                ),
                Triple(
                    AppsFilter.Allowed,
                    stringResource(R.string.menu_traffic_allowed),
                    Icons.Filled.CheckCircle
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.default),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                filterOptions.forEachIndexed { index, (option, label, icons) ->
                    ToggleButton(
                        checked = filter == option,
                        onCheckedChange = { checked ->
                            if (checked) {
                                filter = option
                            }
                        },
                        shapes =
                            when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                filterOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                    ) {
                        Icon(
                            imageVector = icons,
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(text = label, maxLines = 1)
                    }
                }
            }

            if (isSearchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                        .padding(horizontal = spacing.default, vertical = spacing.small),
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.menu_search)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_clear_search),
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() },
                    ),
                )
            }

            when {
                isLoading -> {
                    StatePlaceholder(
                        title = stringResource(R.string.ui_loading),
                        message = stringResource(R.string.home_apps_hint),
                        icon = Icons.Default.Apps,
                        isLoading = true,
                    )
                }

                rules.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(R.string.ui_empty_apps_title),
                        message = stringResource(R.string.ui_empty_apps_body),
                        icon = Icons.Default.Apps,
                        actionLabel = stringResource(R.string.menu_refresh),
                        onAction = { viewModel.refreshRules() },
                    )
                }

                filteredRules.isEmpty() -> {
                    if (normalizedSearchQuery.isNotEmpty()) {
                        StatePlaceholder(
                            title = stringResource(R.string.ui_empty_apps_title),
                            message = stringResource(R.string.ui_apps_search_empty),
                            icon = Icons.Default.Search,
                            actionLabel = stringResource(R.string.action_clear_search),
                            onAction = { searchQuery = "" },
                        )
                    } else {
                        StatePlaceholder(
                            title = stringResource(R.string.ui_empty_apps_title),
                            message = stringResource(R.string.ui_filter_empty),
                            icon = Icons.Default.Apps,
                            actionLabel = stringResource(R.string.ui_filter_all),
                            onAction = { filter = AppsFilter.All },
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(end = if (showFastScroller) 32.dp else 0.dp),
                        ) {
                            groupedRules.forEach { item ->
                                when (item) {
                                    is AppListItem.Header -> {
                                        item(key = "header_${item.letter}") {
                                            SectionHeader(letter = item.letter)
                                        }
                                    }

                                    is AppListItem.App -> {
                                        item(key = "${item.rule.packageName ?: "uid"}_${item.rule.uid}") {
                                            RuleCard(
                                                rule = item.rule,
                                                position = item.position,
                                                searchQuery = normalizedSearchQuery,
                                                onClick = {
                                                    onNavigateToDetail(item.rule)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showFastScroller) {
                            IndexedFastScroller(
                                items = groupedRules,
                                listState = listState,
                                getIndexKey = { item ->
                                    when (item) {
                                        is AppListItem.Header -> item.letter
                                        is AppListItem.App -> item.rule.name ?: item.rule.packageName.orEmpty()
                                    }
                                },
                                scrollItemOffset = 2,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(vertical = spacing.small),
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class AppsFilter {
    All,
    Blocked,
    Allowed,
}

private enum class CardPosition {
    First, Middle, Last, Single
}

private sealed interface AppListItem {
    data class Header(val letter: String) : AppListItem
    data class App(val rule: Rule, val position: CardPosition) : AppListItem
}

@Composable
private fun SectionHeader(letter: String) {
    Text(
        text = letter,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 32.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 4.dp,
            ),
    )
}

@Composable
private fun RuleCard(
    rule: Rule,
    position: CardPosition,
    searchQuery: String,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing

    val iconBitmap = remember(rule.packageName) {
        runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(rule.packageName ?: "", 0)
            pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    val appName = rule.name ?: rule.packageName.orEmpty()
    val highlightColor = MaterialTheme.colorScheme.primary
    val highlightedAppName = remember(appName, searchQuery, highlightColor) {
        buildMatchHighlightedText(
            text = appName,
            query = searchQuery,
            highlightColor = highlightColor,
        )
    }

    val shape = when (position) {
        CardPosition.Single -> RoundedCornerShape(16.dp)
        CardPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        CardPosition.Last -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        CardPosition.Middle -> RoundedCornerShape(4.dp)
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(
                top = if (position == CardPosition.First || position == CardPosition.Single) 0.dp else 2.dp,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App icon
            if (iconBitmap == null) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }

            // App name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightedAppName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (rule.wifi_blocked || rule.other_blocked) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        if (rule.wifi_blocked) {
                            BlockedBadge(
                                icon = Icons.Default.WifiOff,
                                label = stringResource(R.string.title_wifi),
                            )
                        }
                        if (rule.other_blocked) {
                            BlockedBadge(
                                icon = Icons.Default.MobileOff,
                                label = stringResource(R.string.title_mobile),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedBadge(
    icon: ImageVector,
    label: String,
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
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
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private fun matchesAppQuery(rule: Rule, query: String): Boolean {
    if (query.isBlank()) return true
    val appName = rule.name ?: rule.packageName.orEmpty()
    val packageName = rule.packageName.orEmpty()
    return findSubsequenceMatchIndices(appName, query) != null ||
        findSubsequenceMatchIndices(packageName, query) != null
}

private fun buildMatchHighlightedText(
    text: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    val matchedIndices = findSubsequenceMatchIndices(text, query) ?: return AnnotatedString(text)
    if (matchedIndices.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        text.forEachIndexed { index, c ->
            if (index in matchedIndices) {
                withStyle(SpanStyle(color = highlightColor)) {
                    append(c)
                }
            } else {
                append(c)
            }
        }
    }
}

private fun findSubsequenceMatchIndices(text: String, query: String): Set<Int>? {
    if (query.isBlank()) return emptySet()

    val normalizedQuery = query.filterNot(Char::isWhitespace)
    if (normalizedQuery.isEmpty()) return emptySet()

    val matched = mutableSetOf<Int>()
    var qIndex = 0
    for (i in text.indices) {
        if (qIndex >= normalizedQuery.length) break
        if (text[i].equals(normalizedQuery[qIndex], ignoreCase = true)) {
            matched.add(i)
            qIndex++
        }
    }

    return if (qIndex == normalizedQuery.length) matched else null
}


fun persistRule(context: android.content.Context, rule: Rule, allRules: List<Rule>) {
    persistRuleInternal(context, rule, allRules, mutableSetOf())
}

private fun persistRuleInternal(
    context: android.content.Context,
    rule: Rule,
    allRules: List<Rule>,
    visited: MutableSet<String>,
) {
    val packageName = rule.packageName ?: return
    if (!visited.add(packageName)) return
    val wifiKey = Prefs.namespaced("wifi", packageName)
    val otherKey = Prefs.namespaced("other", packageName)
    val applyKey = Prefs.namespaced("apply", packageName)
    val screenWifiKey = Prefs.namespaced("screen_wifi", packageName)
    val screenOtherKey = Prefs.namespaced("screen_other", packageName)
    val roamingKey = Prefs.namespaced("roaming", packageName)
    val lockdownKey = Prefs.namespaced("lockdown", packageName)
    val notifyKey = Prefs.namespaced("notify", packageName)

    if (rule.wifi_blocked == rule.wifi_default) Prefs.remove(wifiKey) else Prefs.putBoolean(wifiKey, rule.wifi_blocked)
    if (rule.other_blocked == rule.other_default) Prefs.remove(otherKey) else Prefs.putBoolean(
        otherKey,
        rule.other_blocked
    )
    if (rule.apply) Prefs.remove(applyKey) else Prefs.putBoolean(applyKey, rule.apply)
    if (rule.screen_wifi == rule.screen_wifi_default) Prefs.remove(screenWifiKey) else Prefs.putBoolean(
        screenWifiKey,
        rule.screen_wifi
    )
    if (rule.screen_other == rule.screen_other_default) Prefs.remove(screenOtherKey) else Prefs.putBoolean(
        screenOtherKey,
        rule.screen_other
    )
    if (rule.roaming == rule.roaming_default) Prefs.remove(roamingKey) else Prefs.putBoolean(roamingKey, rule.roaming)
    if (rule.lockdown) Prefs.putBoolean(lockdownKey, rule.lockdown) else Prefs.remove(lockdownKey)
    if (rule.notify) Prefs.remove(notifyKey) else Prefs.putBoolean(notifyKey, rule.notify)

    rule.updateChanged(context)
    val notificationManager =
        context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(rule.uid)
    ServiceSinkhole.reload("rule changed", context, false)
    Widgets.updateFirewall(context)

    rule.related?.forEach { relatedPkg ->
        val related = allRules.firstOrNull { it.packageName == relatedPkg } ?: return@forEach
        related.wifi_blocked = rule.wifi_blocked
        related.other_blocked = rule.other_blocked
        related.apply = rule.apply
        related.screen_wifi = rule.screen_wifi
        related.screen_other = rule.screen_other
        related.roaming = rule.roaming
        related.lockdown = rule.lockdown
        related.notify = rule.notify
        persistRuleInternal(context, related, allRules, visited)
    }
}
