package com.bernaferrari.quietguard.ui.main

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols




import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.action_clear_search
import com.bernaferrari.quietguard.generated.resources.home_apps_hint
import com.bernaferrari.quietguard.generated.resources.menu_refresh
import com.bernaferrari.quietguard.generated.resources.menu_search
import com.bernaferrari.quietguard.generated.resources.menu_traffic_allowed
import com.bernaferrari.quietguard.generated.resources.menu_traffic_blocked
import com.bernaferrari.quietguard.generated.resources.title_mobile
import com.bernaferrari.quietguard.generated.resources.title_wifi
import com.bernaferrari.quietguard.generated.resources.ui_apps_search_empty
import com.bernaferrari.quietguard.generated.resources.ui_apps_title
import com.bernaferrari.quietguard.generated.resources.ui_empty_apps_body
import com.bernaferrari.quietguard.generated.resources.ui_empty_apps_title
import com.bernaferrari.quietguard.generated.resources.ui_filter_all
import com.bernaferrari.quietguard.generated.resources.ui_filter_empty
import com.bernaferrari.quietguard.generated.resources.ui_loading
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.ui.components.AppIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.ui.components.DiagonalWipeIcon
import com.bernaferrari.quietguard.ui.components.IndexedFastScroller
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.ui.util.LoadErrorPlaceholder
import kotlinx.coroutines.flow.distinctUntilChanged

import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    selectedRuleUid: Int? = null,
    onNavigateToDetail: (FirewallRule) -> Unit = {},
) {
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()
    val rules = rulesUiState.data
    val isInitialLoading = !rulesUiState.isReady && rules.isEmpty() && !rulesUiState.hasFailed
    val isRefreshing = rulesUiState.isRefreshing && rules.isNotEmpty()
    var filter by rememberSaveable { mutableStateOf(AppsFilter.All) }
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

    val filteredRules = remember(rules, filter, normalizedSearchQuery) {
        val base = when (filter) {
            AppsFilter.All -> rules
            AppsFilter.Blocked -> rules.filter { it.wifi_blocked || it.other_blocked }
            AppsFilter.Allowed -> rules.filter { !it.wifi_blocked && !it.other_blocked }
        }
        if (normalizedSearchQuery.isEmpty()) {
            base
        } else {
            base.filter { matchesAppQuery(it, normalizedSearchQuery) }
        }
    }
    val badgeCount = filteredRules.size

    // Group by first letter for section headers
    val groupedRules = remember(filteredRules) {
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
    val showFastScroller = filteredRules.size >= 24

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
                            text = stringResource(Res.string.ui_apps_title),
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
                    val rotation =
                        if (isRefreshing && !motion.reducedMotion) {
                            val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                            val animatedRotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 800, easing = LinearEasing),
                                ),
                                label = "refreshRotation",
                            )
                            animatedRotation
                        } else {
                            0f
                        }
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
                            icon = if (isSearchOpen) MaterialSymbols.Filled.Close else MaterialSymbols.Filled.Search,
                            contentDescription = if (isSearchOpen) {
                                stringResource(Res.string.action_clear_search)
                            } else {
                                stringResource(Res.string.menu_search)
                            },
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshRules() },
                        enabled = !isRefreshing,
                    ) {
                        Icon(
                            icon = MaterialSymbols.Filled.Refresh,
                            contentDescription = stringResource(Res.string.menu_refresh),
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
                FilterOption(
                    AppsFilter.All,
                    stringResource(Res.string.ui_filter_all),
                    selectedIcon = MaterialSymbols.Filled.Public,
                    unselectedIcon = MaterialSymbols.Outlined.Public,
                ),
                FilterOption(
                    AppsFilter.Blocked,
                    stringResource(Res.string.menu_traffic_blocked),
                    selectedIcon = MaterialSymbols.Filled.Block,
                    unselectedIcon = MaterialSymbols.Outlined.Block,
                ),
                FilterOption(
                    AppsFilter.Allowed,
                    stringResource(Res.string.menu_traffic_allowed),
                    selectedIcon = MaterialSymbols.Filled.CheckCircle,
                    unselectedIcon = MaterialSymbols.Outlined.CheckCircle,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.default),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                filterOptions.forEachIndexed { index, item ->
                    val isSelected = filter == item.option
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                filter = item.option
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
                            icon = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(text = item.label, maxLines = 1)
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
                    placeholder = { Text(stringResource(Res.string.menu_search)) },
                    leadingIcon = {
                        Icon(
                            icon = MaterialSymbols.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    icon = MaterialSymbols.Filled.Close,
                                    contentDescription = stringResource(Res.string.action_clear_search),
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
                rulesUiState.hasFailed && rules.isEmpty() -> {
                    LoadErrorPlaceholder(
                        icon = MaterialSymbols.Filled.Apps,
                        onRetry = viewModel::refreshRules,
                    )
                }

                isInitialLoading -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_loading),
                        message = stringResource(Res.string.home_apps_hint),
                        icon = MaterialSymbols.Filled.Apps,
                        isLoading = true,
                    )
                }

                rules.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_apps_title),
                        message = stringResource(Res.string.ui_empty_apps_body),
                        icon = MaterialSymbols.Filled.Apps,
                        actionLabel = stringResource(Res.string.menu_refresh),
                        onAction = { viewModel.refreshRules() },
                    )
                }

                filteredRules.isEmpty() -> {
                    if (normalizedSearchQuery.isNotEmpty()) {
                        StatePlaceholder(
                            title = stringResource(Res.string.ui_empty_apps_title),
                            message = stringResource(Res.string.ui_apps_search_empty),
                            icon = MaterialSymbols.Filled.Search,
                            actionLabel = stringResource(Res.string.action_clear_search),
                            onAction = { searchQuery = "" },
                        )
                    } else {
                        StatePlaceholder(
                            title = stringResource(Res.string.ui_empty_apps_title),
                            message = stringResource(Res.string.ui_filter_empty),
                            icon = MaterialSymbols.Filled.Apps,
                            actionLabel = stringResource(Res.string.ui_filter_all),
                            onAction = { filter = AppsFilter.All },
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                end = if (showFastScroller) 32.dp else 0.dp,
                                bottom = spacing.default,
                            ),
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
                                                isSelected = selectedRuleUid == item.rule.uid,
                                                position = item.position,
                                                searchQuery = normalizedSearchQuery,
                                                onToggleWifi = {
                                                    viewModel.updateRule(item.rule.uid) { current ->
                                                        current.copy(
                                                            wifi_blocked = !current.wifi_blocked,
                                                        )
                                                    }
                                                },
                                                onToggleMobile = {
                                                    viewModel.updateRule(item.rule.uid) { current ->
                                                        current.copy(
                                                            other_blocked = !current.other_blocked,
                                                        )
                                                    }
                                                },
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
                                        is AppListItem.App -> item.rule.name
                                            ?: item.rule.packageName.orEmpty()
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

private data class FilterOption(
    val option: AppsFilter,
    val label: String,
    val selectedIcon: MaterialIcon,
    val unselectedIcon: MaterialIcon,
)

private enum class CardPosition {
    First, Middle, Last, Single
}

private data class CornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
)

private sealed interface AppListItem {
    data class Header(val letter: String) : AppListItem
    data class App(val rule: FirewallRule, val position: CardPosition) : AppListItem
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
    rule: FirewallRule,
    isSelected: Boolean = false,
    position: CardPosition,
    searchQuery: String,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
    onClick: () -> Unit,
) {
    // Keep the row's press/release interaction stable across selection changes.
    val interactionSource = remember { MutableInteractionSource() }
    val appName = rule.name ?: rule.packageName.orEmpty()
    val wifiDescription = stringResource(Res.string.title_wifi) + " " +
            if (rule.wifi_blocked) stringResource(Res.string.menu_traffic_blocked)
            else stringResource(Res.string.menu_traffic_allowed)
    val mobileDescription = stringResource(Res.string.title_mobile) + " " +
            if (rule.other_blocked) stringResource(Res.string.menu_traffic_blocked)
            else stringResource(Res.string.menu_traffic_allowed)
    val highlightColor = MaterialTheme.colorScheme.primary
    val highlightedAppName = remember(appName, searchQuery, highlightColor) {
        buildMatchHighlightedText(
            text = appName,
            query = searchQuery,
            highlightColor = highlightColor,
        )
    }

    val baseCornerRadii = when (position) {
        CardPosition.Single -> CornerRadii(16.dp, 16.dp, 16.dp, 16.dp)
        CardPosition.First -> CornerRadii(16.dp, 16.dp, 4.dp, 4.dp)
        CardPosition.Last -> CornerRadii(4.dp, 4.dp, 16.dp, 16.dp)
        CardPosition.Middle -> CornerRadii(4.dp, 4.dp, 4.dp, 4.dp)
    }
    val selectedRadius = 20.dp
    val topStart = if (isSelected) selectedRadius else baseCornerRadii.topStart
    val topEnd = if (isSelected) selectedRadius else baseCornerRadii.topEnd
    val bottomEnd = if (isSelected) selectedRadius else baseCornerRadii.bottomEnd
    val bottomStart = if (isSelected) selectedRadius else baseCornerRadii.bottomStart
    val cardShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    val cardColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val selectionBorderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val selectionBorderWidth = if (isSelected) 2.dp else 0.dp

    Surface(
        shape = cardShape,
        color = cardColor,
        border = BorderStroke(
            width = selectionBorderWidth,
            color = selectionBorderColor,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(
                top = if (position == CardPosition.First || position == CardPosition.Single) 0.dp else 2.dp,
            )
            .semantics { selected = isSelected },
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(
                packageName = rule.packageName,
                size = 40.dp,
                cornerRadius = 12.dp,
            )

            // App name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightedAppName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = contentColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NetworkToggleButton(
                    blocked = rule.wifi_blocked,
                    allowedIcon = MaterialSymbols.Filled.Wifi,
                    blockedIcon = MaterialSymbols.Filled.WifiOff,
                    contentDescription = wifiDescription,
                    onToggle = onToggleWifi,
                )
                NetworkToggleButton(
                    blocked = rule.other_blocked,
                    allowedIcon = MaterialSymbols.Outlined.MobiledataArrows,
                    blockedIcon = MaterialSymbols.Outlined.MobiledataOff,
                    contentDescription = mobileDescription,
                    onToggle = onToggleMobile,
                )
            }
        }
    }
}

@Composable
private fun NetworkToggleButton(
    blocked: Boolean,
    allowedIcon: MaterialIcon,
    blockedIcon: MaterialIcon,
    contentDescription: String,
    onToggle: () -> Unit,
) {
    val motion = LocalMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "networkTogglePressScale",
    )
    val stateTransition = updateTransition(targetState = blocked, label = "networkToggleState")
    val bubbleAlpha by stateTransition.animateFloat(
        transitionSpec = {
            if (true isTransitioningTo false) {
                keyframes {
                    durationMillis = motion.durationFast + 100
                    1f at 0
                    1f at 64
                    0.64f at 160
                    0f at durationMillis
                }
            } else {
                spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = 0.82f,
                )
            }
        },
        label = "networkToggleBubbleAlpha",
    ) { isBlocked ->
        if (isBlocked) 1f else 0f
    }
    val bubbleScale by stateTransition.animateFloat(
        transitionSpec = {
            if (true isTransitioningTo false) {
                keyframes {
                    durationMillis = motion.durationFast + 100
                    1f at 0
                    1.1f at 64
                    0.68f at 160
                    0f at durationMillis
                }
            } else {
                spring(
                    stiffness = 640f,
                    dampingRatio = 0.76f,
                )
            }
        },
        label = "networkToggleBubbleScale",
    ) { isBlocked ->
        if (isBlocked) 1f else 0f
    }
    val allowedIconTint = if (blocked) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.44f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val blockedIconTint = if (blocked) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    }
    val dividerTint = if (blocked) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.76f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val iconScale by stateTransition.animateFloat(
        transitionSpec = {
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy,
            )
        },
        label = "networkToggleIconScale",
    ) { isBlocked ->
        if (isBlocked) 1f else 0.9f
    }
    val blockedBackground = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f)

    Surface(
        onClick = onToggle,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        alpha = bubbleAlpha
                        scaleX = bubbleScale
                        scaleY = bubbleScale
                    }
                    .clip(CircleShape)
                    .background(blockedBackground),
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            ) {
                DiagonalWipeIcon(
                    blocked = blocked,
                    allowedIcon = allowedIcon,
                    blockedIcon = blockedIcon,
                    allowedTint = allowedIconTint,
                    blockedTint = blockedIconTint,
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

private fun matchesAppQuery(rule: FirewallRule, query: String): Boolean {
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
