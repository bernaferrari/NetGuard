package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols




import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.action_back
import com.bernaferrari.quietguard.generated.resources.action_clear_access_history
import com.bernaferrari.quietguard.generated.resources.section_access_history
import com.bernaferrari.quietguard.generated.resources.menu_launch
import com.bernaferrari.quietguard.generated.resources.menu_settings
import com.bernaferrari.quietguard.generated.resources.setting_options
import com.bernaferrari.quietguard.generated.resources.setting_reset_to_value
import com.bernaferrari.quietguard.generated.resources.setting_section_advanced
import com.bernaferrari.quietguard.generated.resources.setting_section_firewall
import com.bernaferrari.quietguard.generated.resources.setting_value_off
import com.bernaferrari.quietguard.generated.resources.setting_value_on
import com.bernaferrari.quietguard.generated.resources.title_apply
import com.bernaferrari.quietguard.generated.resources.title_lockdown
import com.bernaferrari.quietguard.generated.resources.title_mobile
import com.bernaferrari.quietguard.generated.resources.title_notify
import com.bernaferrari.quietguard.generated.resources.title_roaming
import com.bernaferrari.quietguard.generated.resources.title_screen_other
import com.bernaferrari.quietguard.generated.resources.title_screen_wifi
import com.bernaferrari.quietguard.generated.resources.title_wifi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.lerp
import com.bernaferrari.quietguard.domain.FirewallRule
import com.bernaferrari.quietguard.platform.AccessEntry
import com.bernaferrari.quietguard.platform.PlatformContext
import com.bernaferrari.quietguard.ui.screens.vm.AppRuleDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.quietguard.platform.launchApp
import com.bernaferrari.quietguard.platform.openAppDetails
import com.bernaferrari.quietguard.ui.components.AppIcon
import com.bernaferrari.quietguard.ui.components.FirewallTile
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.theme.LocalMotion

import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRuleDetailScreen(
    rule: FirewallRule,
    showBackButton: Boolean = true,
    onUpdateRule: ((FirewallRule) -> FirewallRule) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val detailViewModel: AppRuleDetailViewModel = koinViewModel()
    val haptic = LocalHapticFeedback.current
    val accessUi by detailViewModel.accessState.collectAsStateWithLifecycle()
    val accessHistoryBelongsToRule = accessUi.data.uid == rule.uid
    val accessEntries = if (accessHistoryBelongsToRule) accessUi.data.entries else emptyList()
    val hasAccessHistory = accessEntries.isNotEmpty()

    LaunchedEffect(rule.uid) {
        detailViewModel.bindRule(rule)
    }

    val appName = rule.name ?: rule.packageName.orEmpty()
    val canLaunch = PlatformContext.isAndroid() && rule.packageName != null
    val updateRule: ((FirewallRule) -> FirewallRule) -> Unit = { transform ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onUpdateRule(transform)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val iconTileSize = lerp(48.dp, 32.dp, collapsedFraction)
    val iconCorner = lerp(16.dp, 10.dp, collapsedFraction)
    val titleSize = lerp(
        MaterialTheme.typography.headlineMedium.fontSize,
        MaterialTheme.typography.titleLarge.fontSize,
        collapsedFraction,
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            lerp(14.dp, 10.dp, collapsedFraction),
                        ),
                    ) {
                        AppIcon(
                            packageName = rule.packageName,
                            size = iconTileSize,
                            cornerRadius = iconCorner,
                        )
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = titleSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = if (showBackButton) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(
                                icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back),
                            )
                        }
                    }
                } else {
                    {}
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = MaterialTheme.spacing.small, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Firewall ────────────────────────────
            DetailSection(
                title = stringResource(Res.string.setting_section_firewall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    FirewallTile(
                        allowedIcon = MaterialSymbols.Filled.Wifi,
                        blockedIcon = MaterialSymbols.Filled.WifiOff,
                        label = stringResource(Res.string.title_wifi),
                        allowed = !rule.wifi_blocked,
                        onToggle = {
                            updateRule { current ->
                                current.copy(wifi_blocked = !current.wifi_blocked)
                            }
                        },
                        shape = detailPairTileShape(
                            isLeadingTile = true,
                            isFirstRow = true,
                            isLastRow = true,
                            baseShape = MaterialTheme.shapes.small,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    FirewallTile(
                        allowedIcon = MaterialSymbols.Outlined.MobiledataArrows,
                        blockedIcon = MaterialSymbols.Outlined.MobiledataOff,
                        label = stringResource(Res.string.title_mobile),
                        allowed = !rule.other_blocked,
                        onToggle = {
                            updateRule { current ->
                                current.copy(other_blocked = !current.other_blocked)
                            }
                        },
                        shape = detailPairTileShape(
                            isLeadingTile = false,
                            isFirstRow = true,
                            isLastRow = true,
                            baseShape = MaterialTheme.shapes.small,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Advanced ────────────────────────────
            DetailSection(
                title = stringResource(Res.string.setting_section_advanced),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ToggleRow(
                        checkedIcon = MaterialSymbols.Filled.Wifi,
                        uncheckedIcon = MaterialSymbols.Outlined.Wifi,
                        label = stringResource(Res.string.title_screen_wifi),
                        checked = rule.screen_wifi,
                        defaultChecked = rule.screen_wifi_default,
                        isFirst = true,
                        onReset = {
                            updateRule { current ->
                                current.copy(screen_wifi = current.screen_wifi_default)
                            }
                        },
                        onCheckedChange = {
                            updateRule { current -> current.copy(screen_wifi = it) }
                        },
                    )
                    ToggleRow(
                        checkedIcon = MaterialSymbols.Filled.Smartphone,
                        uncheckedIcon = MaterialSymbols.Outlined.Smartphone,
                        label = stringResource(Res.string.title_screen_other),
                        checked = rule.screen_other,
                        defaultChecked = rule.screen_other_default,
                        onReset = {
                            updateRule { current ->
                                current.copy(screen_other = current.screen_other_default)
                            }
                        },
                        onCheckedChange = {
                            updateRule { current -> current.copy(screen_other = it) }
                        },
                    )
                    ToggleRow(
                        checkedIcon = MaterialSymbols.Filled.SignalCellularAlt,
                        uncheckedIcon = MaterialSymbols.Outlined.SignalCellularAlt,
                        label = stringResource(Res.string.title_roaming),
                        checked = rule.roaming,
                        defaultChecked = rule.roaming_default,
                        onReset = {
                            updateRule { current -> current.copy(roaming = current.roaming_default) }
                        },
                        onCheckedChange = {
                            updateRule { current -> current.copy(roaming = it) }
                        },
                    )
                    ToggleRow(
                        checkedIcon = MaterialSymbols.Filled.Lock,
                        uncheckedIcon = MaterialSymbols.Outlined.Lock,
                        label = stringResource(Res.string.title_lockdown),
                        checked = rule.lockdown,
                        defaultChecked = false,
                        onReset = {
                            updateRule { current -> current.copy(lockdown = false) }
                        },
                        onCheckedChange = {
                            updateRule { current -> current.copy(lockdown = it) }
                        },
                    )
                    ToggleRow(
                        checkedIcon = MaterialSymbols.Filled.Shield,
                        uncheckedIcon = MaterialSymbols.Outlined.Shield,
                        label = stringResource(Res.string.title_apply),
                        checked = rule.apply,
                        defaultChecked = true,
                        onReset = {
                            updateRule { current -> current.copy(apply = true) }
                        },
                        onCheckedChange = {
                            updateRule { current -> current.copy(apply = it) }
                        },
                    )
                    ToggleRow(
                        checkedIcon = MaterialSymbols.Filled.Notifications,
                        uncheckedIcon = MaterialSymbols.Outlined.Notifications,
                        label = stringResource(Res.string.title_notify),
                        checked = rule.notify,
                        defaultChecked = true,
                        isLast = true,
                        onReset = {
                            updateRule { current -> current.copy(notify = true) }
                        },
                        onCheckedChange = {
                            updateRule { current -> current.copy(notify = it) }
                        },
                    )
                }
            }

            // ── Access log ──────────────────────────
            AccessLogSection(
                accessEntries = accessEntries,
                loading = !accessHistoryBelongsToRule || accessUi.isInitialLoading,
            )

            // ── Actions ─────────────────────────────
            DetailSection(
                title = stringResource(Res.string.setting_options),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ActionRow(
                        icon = MaterialSymbols.Filled.Info,
                        label = stringResource(Res.string.menu_settings),
                        isFirst = true,
                        onClick = {
                            rule.packageName?.let { openAppDetails(it) }
                        },
                    )
                    ActionRow(
                        icon = MaterialSymbols.AutoMirrored.Filled.Launch,
                        label = stringResource(Res.string.menu_launch),
                        enabled = canLaunch,
                        isLast = !hasAccessHistory,
                        onClick = { rule.packageName?.let { launchApp(it) } },
                    )
                    if (hasAccessHistory) {
                        ActionRow(
                            icon = MaterialSymbols.Filled.Delete,
                            label = stringResource(Res.string.action_clear_access_history),
                            isLast = true,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = {
                                detailViewModel.clearAccess(rule.uid)
                            },
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun ToggleRow(
    checkedIcon: MaterialIcon,
    uncheckedIcon: MaterialIcon,
    label: String,
    checked: Boolean,
    defaultChecked: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onReset: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    val motion = LocalMotion.current
    val isCustomized = checked != defaultChecked
    val shape = detailSettingsItemShape(
        isFirst = isFirst,
        isLast = isLast,
        baseShape = MaterialTheme.shapes.small,
    )
    val containerColor =
        if (checked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    val iconTint =
        if (checked) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor =
        if (checked) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = shape,
            color = containerColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(shape)
                    .toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    icon = if (checked) checkedIcon else uncheckedIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = labelColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                )
            }
        }
        AnimatedVisibility(
            visible = isCustomized,
            enter = expandHorizontally(
                animationSpec = tween(
                    durationMillis = motion.durationMedium,
                    easing = motion.easingDecelerate,
                ),
                expandFrom = Alignment.End,
            ) + slideInHorizontally(
                animationSpec = tween(
                    durationMillis = motion.durationMedium,
                    easing = motion.easingDecelerate,
                ),
                initialOffsetX = { width -> width / 3 },
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = motion.durationFast,
                    easing = motion.easingDecelerate,
                ),
            ),
            exit = shrinkHorizontally(
                animationSpec = tween(
                    durationMillis = motion.durationFast,
                    easing = motion.easingAccelerate,
                ),
                shrinkTowards = Alignment.End,
            ) + slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = motion.durationFast,
                    easing = motion.easingAccelerate,
                ),
                targetOffsetX = { width -> width / 3 },
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = motion.durationFast,
                    easing = motion.easingAccelerate,
                ),
            ),
        ) {
            Row {
                Spacer(modifier = Modifier.size(4.dp))
                Surface(
                    onClick = onReset,
                    modifier = Modifier.size(60.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon = MaterialSymbols.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(
                                Res.string.setting_reset_to_value,
                                stringResource(
                                    if (defaultChecked) {
                                        Res.string.setting_value_on
                                    } else {
                                        Res.string.setting_value_off
                                    },
                                ),
                            ),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: MaterialIcon,
    label: String,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = detailSettingsItemShape(
        isFirst = isFirst,
        isLast = isLast,
        baseShape = MaterialTheme.shapes.small,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                icon = icon,
                contentDescription = null,
                tint = if (enabled) tint
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                icon = MaterialSymbols.Filled.ChevronRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun detailSettingsItemShape(
    isFirst: Boolean,
    isLast: Boolean,
    baseShape: Shape,
): Shape {
    if (!isFirst && !isLast) {
        return baseShape
    }
    val base = baseShape as? RoundedCornerShape ?: return baseShape
    val emphasis = MaterialTheme.shapes.large as? RoundedCornerShape ?: return baseShape
    return RoundedCornerShape(
        topStart = if (isFirst) emphasis.topStart else base.topStart,
        topEnd = if (isFirst) emphasis.topEnd else base.topEnd,
        bottomEnd = if (isLast) emphasis.bottomEnd else base.bottomEnd,
        bottomStart = if (isLast) emphasis.bottomStart else base.bottomStart,
    )
}

@Composable
private fun detailPairTileShape(
    isLeadingTile: Boolean,
    isFirstRow: Boolean,
    isLastRow: Boolean,
    baseShape: Shape,
): Shape {
    if (!isFirstRow && !isLastRow) {
        return baseShape
    }
    val base = baseShape as? RoundedCornerShape ?: return baseShape
    val emphasis = MaterialTheme.shapes.large as? RoundedCornerShape ?: return baseShape
    return RoundedCornerShape(
        topStart = if (isLeadingTile && isFirstRow) emphasis.topStart else base.topStart,
        topEnd = if (!isLeadingTile && isFirstRow) emphasis.topEnd else base.topEnd,
        bottomEnd = if (!isLeadingTile && isLastRow) emphasis.bottomEnd else base.bottomEnd,
        bottomStart = if (isLeadingTile && isLastRow) emphasis.bottomStart else base.bottomStart,
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    val spacing = MaterialTheme.spacing
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = spacing.default),
    )
}

@Composable
private fun AccessLogSection(
    accessEntries: List<AccessEntry>,
    loading: Boolean,
) {
    if (loading || accessEntries.isEmpty()) return

    DetailSection(
        title = stringResource(Res.string.section_access_history),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                accessEntries.forEachIndexed { index, entry ->
                    val isAllowed = entry.allowed > 0
                    val statusColor = if (isAllowed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            icon = if (isAllowed) MaterialSymbols.Outlined.CheckCircle
                            else MaterialSymbols.Filled.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (entry.allowed >= 0) statusColor
                            else MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = entry.timeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${entry.daddr}:${entry.dport}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < accessEntries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 48.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}
