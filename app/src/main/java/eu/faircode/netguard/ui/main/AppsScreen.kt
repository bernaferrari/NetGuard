package eu.faircode.netguard.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import eu.faircode.netguard.R
import eu.faircode.netguard.DatabaseHelper
import eu.faircode.netguard.Rule
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.Widgets
import eu.faircode.netguard.data.Prefs
import eu.faircode.netguard.ui.components.ExpandableContent
import eu.faircode.netguard.ui.components.animateContentHeight
import eu.faircode.netguard.ui.components.animatedRotation
import eu.faircode.netguard.ui.theme.TouchTargets
import eu.faircode.netguard.ui.theme.spacing
import eu.faircode.netguard.ui.util.StatePlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val rules = remember { mutableStateListOf<Rule>() }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            Rule.getRules(false, context)
        }
        rules.clear()
        rules.addAll(loaded)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.default),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.menu_firewall),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.home_apps_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { refreshKey += 1 }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Text(text = stringResource(R.string.menu_refresh))
            }
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
                    onAction = { refreshKey += 1 },
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    items(rules, key = { it.uid }) { rule ->
                        RuleCard(
                            rule = rule,
                            onToggle = {
                                persistRule(context, rule, rules)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    var expanded by remember(rule.uid) { mutableStateOf(rule.expanded) }

    val iconBitmap = remember(rule.packageName) {
        runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(rule.packageName ?: "", 0)
            pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    // Animated rotation for expand icon
    val iconRotation = animatedRotation(expanded)

    val appName = rule.name ?: rule.packageName.orEmpty()
    val expandDescription = if (expanded) {
        stringResource(R.string.action_collapse)
    } else {
        stringResource(R.string.action_expand)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.animateContentHeight(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = stringResource(R.string.content_desc_app_icon, appName),
                            modifier = Modifier.size(TouchTargets.appIconSize),
                        )
                    }
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Column {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = rule.packageName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Accessible expand button with proper touch target
                IconButton(
                    onClick = {
                        expanded = !expanded
                        rule.expanded = expanded
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.size(TouchTargets.minimum),
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = expandDescription,
                        modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
                    )
                }
            }

            // Primary toggles - always visible
            RuleToggleRow(
                label = stringResource(R.string.title_block_wifi),
                checked = rule.wifi_blocked,
                onCheckedChange = {
                    rule.wifi_blocked = it
                    onToggle()
                },
            )
            RuleToggleRow(
                label = stringResource(R.string.title_block_other),
                checked = rule.other_blocked,
                onCheckedChange = {
                    rule.other_blocked = it
                    onToggle()
                },
            )

            // Expanded content with animation
            ExpandableContent(expanded = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    RuleToggleRow(
                        label = stringResource(R.string.title_screen_wifi),
                        checked = rule.screen_wifi,
                        onCheckedChange = {
                            rule.screen_wifi = it
                            onToggle()
                        },
                    )
                    RuleToggleRow(
                        label = stringResource(R.string.title_screen_other),
                        checked = rule.screen_other,
                        onCheckedChange = {
                            rule.screen_other = it
                            onToggle()
                        },
                    )
                    RuleToggleRow(
                        label = stringResource(R.string.title_roaming),
                        checked = rule.roaming,
                        onCheckedChange = {
                            rule.roaming = it
                            onToggle()
                        },
                    )
                    RuleToggleRow(
                        label = stringResource(R.string.title_lockdown),
                        checked = rule.lockdown,
                        onCheckedChange = {
                            rule.lockdown = it
                            onToggle()
                        },
                    )
                    RuleToggleRow(
                        label = stringResource(R.string.title_apply),
                        checked = rule.apply,
                        onCheckedChange = {
                            rule.apply = it
                            onToggle()
                        },
                    )
                    RuleToggleRow(
                        label = stringResource(R.string.title_notify),
                        checked = rule.notify,
                        onCheckedChange = {
                            rule.notify = it
                            onToggle()
                        },
                    )

                    AccessLogSection(rule = rule)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:${rule.packageName}"))
                                context.startActivity(intent)
                            },
                        ) {
                            Text(text = stringResource(R.string.menu_settings))
                        }
                        FilledTonalButton(
                            onClick = {
                                val intent = context.packageManager
                                    .getLaunchIntentForPackage(rule.packageName.orEmpty())
                                if (intent != null) {
                                    context.startActivity(intent)
                                }
                            },
                        ) {
                            Text(text = stringResource(R.string.menu_launch))
                        }
                        FilledTonalButton(
                            onClick = {
                                DatabaseHelper.getInstance(context).clearAccess(rule.uid, true)
                            },
                        ) {
                            Text(text = stringResource(R.string.menu_clear))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val spacing = MaterialTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(newValue)
            },
            modifier = Modifier.semantics {
                contentDescription = "$label: ${if (checked) "enabled" else "disabled"}"
            },
        )
    }
}

private fun persistRule(context: android.content.Context, rule: Rule, allRules: List<Rule>) {
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
    if (rule.other_blocked == rule.other_default) Prefs.remove(otherKey) else Prefs.putBoolean(otherKey, rule.other_blocked)
    if (rule.apply) Prefs.remove(applyKey) else Prefs.putBoolean(applyKey, rule.apply)
    if (rule.screen_wifi == rule.screen_wifi_default) Prefs.remove(screenWifiKey) else Prefs.putBoolean(screenWifiKey, rule.screen_wifi)
    if (rule.screen_other == rule.screen_other_default) Prefs.remove(screenOtherKey) else Prefs.putBoolean(screenOtherKey, rule.screen_other)
    if (rule.roaming == rule.roaming_default) Prefs.remove(roamingKey) else Prefs.putBoolean(roamingKey, rule.roaming)
    if (rule.lockdown) Prefs.putBoolean(lockdownKey, rule.lockdown) else Prefs.remove(lockdownKey)
    if (rule.notify) Prefs.remove(notifyKey) else Prefs.putBoolean(notifyKey, rule.notify)

    rule.updateChanged(context)
    NotificationManagerCompat.from(context).cancel(rule.uid)
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

@Composable
private fun AccessLogSection(rule: Rule) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    var accessEntries by remember(rule.uid) { mutableStateOf<List<AccessEntry>>(emptyList()) }
    var loading by remember(rule.uid) { mutableStateOf(false) }

    LaunchedEffect(rule.uid) {
        loading = true
        accessEntries = loadAccess(context, rule.uid)
        loading = false
    }

    if (loading) {
        Text(
            text = stringResource(R.string.menu_log),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (accessEntries.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            Text(
                text = stringResource(R.string.menu_log),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            accessEntries.forEach { entry ->
                val color = if (entry.allowed > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(
                    text = "${entry.timeText} ${entry.daddr}:${entry.dport}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.allowed >= 0) color else Color.Unspecified,
                )
            }
        }
    }
}

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
            val colTime = cursor.getColumnIndex("time")
            val colDAddr = cursor.getColumnIndex("daddr")
            val colDPort = cursor.getColumnIndex("dport")
            val colAllowed = cursor.getColumnIndex("allowed")
            val timeFormat = java.text.SimpleDateFormat("HH:mm")
            while (cursor.moveToNext() && result.size < 5) {
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
