package eu.faircode.netguard.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.faircode.netguard.R
import eu.faircode.netguard.Rule
import eu.faircode.netguard.ui.components.PressableContent
import eu.faircode.netguard.ui.theme.LocalMotion
import eu.faircode.netguard.ui.theme.spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
    onNavigateToApps: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()

    // Stats state
    var blockedApps by remember { mutableIntStateOf(0) }
    var allowedApps by remember { mutableIntStateOf(0) }
    var totalApps by remember { mutableIntStateOf(0) }

    // Load stats from rules
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val rules = Rule.getRules(false, context)
            totalApps = rules.size
            blockedApps = rules.count { it.wifi_blocked || it.other_blocked }
            allowedApps = rules.count { !it.wifi_blocked && !it.other_blocked }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        // Header
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Main Status Card with animated color
        StatusCard(
            enabled = enabled,
            onToggle = onToggleEnabled,
        )

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_blocked_today),
                value = blockedApps,
                icon = Icons.Default.Block,
                tint = MaterialTheme.colorScheme.error,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_allowed_today),
                value = allowedApps,
                icon = Icons.Default.CheckCircle,
                tint = MaterialTheme.colorScheme.primary,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_active_rules),
                value = totalApps,
                icon = Icons.Default.Tune,
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }

        // Quick Navigation Cards
        QuickNavCard(
            title = stringResource(R.string.menu_firewall),
            description = stringResource(R.string.home_apps_hint),
            icon = Icons.Default.Tune,
            onClick = onNavigateToApps,
        )

        QuickNavCard(
            title = stringResource(R.string.menu_log),
            description = stringResource(R.string.home_logs_hint),
            icon = Icons.AutoMirrored.Filled.List,
            onClick = onNavigateToLogs,
        )

        QuickNavCard(
            title = stringResource(R.string.menu_settings),
            description = stringResource(R.string.setting_options),
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings,
        )
    }
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing

    // Animate color transition
    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(motion.durationMedium),
        label = "statusColor"
    )

    val statusDescription = if (enabled) {
        stringResource(R.string.status_enabled)
    } else {
        stringResource(R.string.status_disabled)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.default),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = stringResource(R.string.content_desc_security_status),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusDescription,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (enabled) {
                            stringResource(R.string.status_running)
                        } else {
                            stringResource(R.string.status_not_running)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = statusDescription
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilledTonalButton(
                    onClick = { onToggle(!enabled) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = if (enabled) {
                            stringResource(R.string.action_disable)
                        } else {
                            stringResource(R.string.action_enable)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    icon: ImageVector,
    tint: Color,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing

    // Animate count changes
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(motion.durationMedium),
        label = "statValue"
    )

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
            )
            Text(
                text = animatedValue.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickNavCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing

    PressableContent(onClick = onClick) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(spacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // Decorative, title is read
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.action_navigate),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
