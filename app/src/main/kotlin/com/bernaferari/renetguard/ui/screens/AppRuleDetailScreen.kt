package com.bernaferari.renetguard.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.graphics.drawable.toBitmap
import com.bernaferari.renetguard.DatabaseHelper
import com.bernaferari.renetguard.R
import com.bernaferari.renetguard.Rule
import com.bernaferari.renetguard.ui.components.FirewallTile
import com.bernaferari.renetguard.ui.main.persistRule
import com.bernaferari.renetguard.ui.theme.spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRuleDetailScreen(
    rule: Rule,
    allRules: List<Rule>,
    showBackButton: Boolean = true,
    enableSlideTransition: Boolean = true,
    onRuleChanged: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val appName = rule.name ?: rule.packageName.orEmpty()
    val iconBitmap = remember(rule.packageName) {
        runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(rule.packageName ?: "", 0)
            pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    val launchIntent = remember(rule.packageName) {
        context.packageManager.getLaunchIntentForPackage(rule.packageName.orEmpty())
    }

    var toggleKey by remember { mutableStateOf(0) }
    var isVisible by remember(enableSlideTransition) { mutableStateOf(!enableSlideTransition) }
    var isClosing by remember { mutableStateOf(false) }

    fun onToggle() {
        persistRule(context, rule, allRules)
        onRuleChanged()
        toggleKey++
    }

    fun closeWithAnimation() {
        if (isClosing) return
        isClosing = true
        if (enableSlideTransition) {
            isVisible = false
            scope.launch {
                delay(200)
                onBack()
            }
        } else {
            onBack()
        }
    }

    LaunchedEffect(enableSlideTransition, rule.uid) {
        if (enableSlideTransition) {
            isVisible = true
        }
        isClosing = false
    }

    BackHandler(enabled = !isClosing) {
        closeWithAnimation()
    }

    val detailContent: @Composable () -> Unit = {
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
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(iconTileSize)
                                        .clip(RoundedCornerShape(iconCorner)),
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(iconTileSize),
                                    shape = RoundedCornerShape(iconCorner),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            modifier = Modifier.size(
                                                lerp(26.dp, 18.dp, collapsedFraction),
                                            ),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Column {
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = titleSize,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                AnimatedVisibility(visible = collapsedFraction < 0.5f) {
                                    Text(
                                        text = rule.packageName.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = if (showBackButton) {
                        {
                            IconButton(onClick = ::closeWithAnimation) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
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
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                @Suppress("UNUSED_EXPRESSION")
                toggleKey

                // ── Firewall ────────────────────────────
                SectionLabel(stringResource(R.string.setting_section_firewall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    FirewallTile(
                        allowedIcon = Icons.Default.Wifi,
                        blockedIcon = Icons.Default.WifiOff,
                        label = stringResource(R.string.title_wifi),
                        allowed = !rule.wifi_blocked,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.wifi_blocked = !rule.wifi_blocked
                            onToggle()
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
                        allowedIcon = Icons.Default.PhoneAndroid,
                        blockedIcon = Icons.Default.MobileOff,
                        label = stringResource(R.string.title_mobile),
                        allowed = !rule.other_blocked,
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.other_blocked = !rule.other_blocked
                            onToggle()
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

                // ── Advanced ────────────────────────────
                SectionLabel(stringResource(R.string.setting_section_advanced))
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    ToggleRow(
                        icon = Icons.Default.Wifi,
                        label = stringResource(R.string.title_screen_wifi),
                        checked = rule.screen_wifi,
                        isFirst = true,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.screen_wifi = it
                            onToggle()
                        },
                    )
                    ToggleRow(
                        icon = Icons.Default.Smartphone,
                        label = stringResource(R.string.title_screen_other),
                        checked = rule.screen_other,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.screen_other = it
                            onToggle()
                        },
                    )
                    ToggleRow(
                        icon = Icons.Default.SignalCellularAlt,
                        label = stringResource(R.string.title_roaming),
                        checked = rule.roaming,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.roaming = it
                            onToggle()
                        },
                    )
                    ToggleRow(
                        icon = Icons.Default.Lock,
                        label = stringResource(R.string.title_lockdown),
                        checked = rule.lockdown,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.lockdown = it
                            onToggle()
                        },
                    )
                    ToggleRow(
                        icon = Icons.Default.Shield,
                        label = stringResource(R.string.title_apply),
                        checked = rule.apply,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.apply = it
                            onToggle()
                        },
                    )
                    ToggleRow(
                        icon = Icons.Default.Notifications,
                        label = stringResource(R.string.title_notify),
                        checked = rule.notify,
                        isLast = true,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            rule.notify = it
                            onToggle()
                        },
                    )
                }

                // ── Access log ──────────────────────────
                AccessLogSection(rule = rule)

                // ── Actions ─────────────────────────────
                SectionLabel(stringResource(R.string.setting_options))
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    ActionRow(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.menu_settings),
                        isFirst = true,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${rule.packageName}"))
                            context.startActivity(intent)
                        },
                    )
                    ActionRow(
                        icon = Icons.AutoMirrored.Filled.Launch,
                        label = stringResource(R.string.menu_launch),
                        enabled = launchIntent != null,
                        onClick = { launchIntent?.let(context::startActivity) },
                    )
                    ActionRow(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.menu_clear),
                        isLast = true,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            DatabaseHelper.getInstance(context).clearAccess(rule.uid, true)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(spacing.small))
            }
        }
    }

    if (enableSlideTransition) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing),
            ),
        ) {
            detailContent()
        }
    } else {
        detailContent()
    }
}


@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
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
                imageVector = icon,
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
                imageVector = Icons.Default.ChevronRight,
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AccessLogSection(rule: Rule) {
    val context = LocalContext.current
    var accessEntries by remember(rule.uid) { mutableStateOf<List<AccessEntry>>(emptyList()) }
    var loading by remember(rule.uid) { mutableStateOf(false) }

    LaunchedEffect(rule.uid) {
        loading = true
        accessEntries = loadAccess(context, rule.uid)
        loading = false
    }

    if (loading || accessEntries.isEmpty()) return

    SectionLabel(stringResource(R.string.menu_log))

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
                        imageVector = if (isAllowed) Icons.Outlined.CheckCircle
                        else Icons.Default.Block,
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

// ─── Data ───────────────────────────────────────────────────────────────────────

private data class AccessEntry(
    val time: Long,
    val timeText: String,
    val daddr: String,
    val dport: Int,
    val allowed: Int,
)

private suspend fun loadAccess(context: android.content.Context, uid: Int): List<AccessEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<AccessEntry>()
        DatabaseHelper.getInstance(context).getAccess(uid).use { cursor ->
            val colTime = cursor.getColumnIndexOrThrow("time")
            val colDAddr = cursor.getColumnIndexOrThrow("daddr")
            val colDPort = cursor.getColumnIndexOrThrow("dport")
            val colAllowed = cursor.getColumnIndexOrThrow("allowed")
            val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            while (cursor.moveToNext() && result.size < 10) {
                result.add(
                    AccessEntry(
                        time = cursor.getLong(colTime),
                        timeText = timeFormat.format(cursor.getLong(colTime)),
                        daddr = cursor.getString(colDAddr),
                        dport = cursor.getInt(colDPort),
                        allowed = cursor.getInt(colAllowed),
                    ),
                )
            }
        }
        result
    }
