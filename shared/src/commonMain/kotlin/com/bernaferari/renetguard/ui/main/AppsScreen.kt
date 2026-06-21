package com.bernaferari.renetguard.ui.main

import org.jetbrains.compose.resources.stringResource
import netguard.shared.generated.resources.Res
import netguard.shared.generated.resources.action_clear_search
import netguard.shared.generated.resources.home_apps_hint
import netguard.shared.generated.resources.menu_refresh
import netguard.shared.generated.resources.menu_search
import netguard.shared.generated.resources.menu_traffic_allowed
import netguard.shared.generated.resources.menu_traffic_blocked
import netguard.shared.generated.resources.title_mobile
import netguard.shared.generated.resources.title_wifi
import netguard.shared.generated.resources.ui_apps_search_empty
import netguard.shared.generated.resources.ui_apps_title
import netguard.shared.generated.resources.ui_empty_apps_body
import netguard.shared.generated.resources.ui_empty_apps_title
import netguard.shared.generated.resources.ui_filter_all
import netguard.shared.generated.resources.ui_filter_empty
import netguard.shared.generated.resources.ui_loading
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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Public
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
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
import com.bernaferari.renetguard.ui.components.AppIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferari.renetguard.domain.FirewallRule
import com.bernaferari.renetguard.platform.NetGuardPlatform
import com.bernaferari.renetguard.ui.components.DiagonalWipeIcon
import com.bernaferari.renetguard.ui.components.IndexedFastScroller
import com.bernaferari.renetguard.ui.theme.LocalMotion
import com.bernaferari.renetguard.ui.theme.spacing
import com.bernaferari.renetguard.ui.util.StatePlaceholder
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    selectedRuleUid: Int? = null,
    onNavigateToDetail: (FirewallRule) -> Unit = {},
    initialFilter: AppsFilter = AppsFilter.All,
    initialFilterVersion: Int = 0,
) {
    val spacing = MaterialTheme.spacing
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()
    val rules = rulesUiState.rules
    val rulesRevision = rulesUiState.revision
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

    val filteredRules = remember(rules, rulesRevision, filter, normalizedSearchQuery) {
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
    val groupedRules = remember(filteredRules, rulesRevision, selectedRuleUid) {
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
                            imageVector = Icons.Default.Refresh,
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
                    selectedIcon = Icons.Filled.Public,
                    unselectedIcon = Icons.Outlined.Public,
                ),
                FilterOption(
                    AppsFilter.Blocked,
                    stringResource(Res.string.menu_traffic_blocked),
                    selectedIcon = Icons.Filled.Block,
                    unselectedIcon = Icons.Outlined.Block,
                ),
                FilterOption(
                    AppsFilter.Allowed,
                    stringResource(Res.string.menu_traffic_allowed),
                    selectedIcon = Icons.Filled.CheckCircle,
                    unselectedIcon = Icons.Outlined.CheckCircle,
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
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
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
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
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
                isLoading -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_loading),
                        message = stringResource(Res.string.home_apps_hint),
                        icon = Icons.Default.Apps,
                        isLoading = true,
                    )
                }

                rules.isEmpty() -> {
                    StatePlaceholder(
                        title = stringResource(Res.string.ui_empty_apps_title),
                        message = stringResource(Res.string.ui_empty_apps_body),
                        icon = Icons.Default.Apps,
                        actionLabel = stringResource(Res.string.menu_refresh),
                        onAction = { viewModel.refreshRules() },
                    )
                }

                filteredRules.isEmpty() -> {
                    if (normalizedSearchQuery.isNotEmpty()) {
                        StatePlaceholder(
                            title = stringResource(Res.string.ui_empty_apps_title),
                            message = stringResource(Res.string.ui_apps_search_empty),
                            icon = Icons.Default.Search,
                            actionLabel = stringResource(Res.string.action_clear_search),
                            onAction = { searchQuery = "" },
                        )
                    } else {
                        StatePlaceholder(
                            title = stringResource(Res.string.ui_empty_apps_title),
                            message = stringResource(Res.string.ui_filter_empty),
                            icon = Icons.Default.Apps,
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
                                                    item.rule.wifi_blocked = !item.rule.wifi_blocked
                                                    viewModel.persistRule(item.rule)
                                                    viewModel.notifyRulesChanged()
                                                },
                                                onToggleMobile = {
                                                    item.rule.other_blocked =
                                                        !item.rule.other_blocked
                                                    viewModel.persistRule(item.rule)
                                                    viewModel.notifyRulesChanged()
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
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private enum class CardPosition {
    First, Middle, Last, Single
}

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

    val shape = when (position) {
        CardPosition.Single -> RoundedCornerShape(16.dp)
        CardPosition.First -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )

        CardPosition.Last -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )

        CardPosition.Middle -> RoundedCornerShape(4.dp)
    }
    val selectedShape = RoundedCornerShape(20.dp)
    val cardShape = if (isSelected) selectedShape else shape
    val cardColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = MaterialTheme.colorScheme.onSurface
    val cardModifier =
        if (isSelected) {
            Modifier
                .border(
                    width = 1.25.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = cardShape,
                )
        } else {
            Modifier
        }

    Surface(
        onClick = onClick,
        shape = cardShape,
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(
                top = if (position == CardPosition.First || position == CardPosition.Single) 0.dp else 2.dp,
            )
            .then(cardModifier),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                    allowedIcon = Icons.Default.Wifi,
                    blockedIcon = Icons.Default.WifiOff,
                    contentDescription = wifiDescription,
                    onToggle = onToggleWifi,
                )
                NetworkToggleButton(
                    blocked = rule.other_blocked,
                    allowedIcon = Icons.Default.PhoneAndroid,
                    blockedIcon = Icons.Default.MobileOff,
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
    allowedIcon: ImageVector,
    blockedIcon: ImageVector,
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


