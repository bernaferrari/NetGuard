package com.bernaferrari.renetguard.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings

import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun NetGuardWebDemo() {
    val holder = remember { DemoStateHolder() }
    val state = holder.state
    val stats = holder.stats
    val filteredApps = holder.filteredApps
    val selectedApp = holder.selectedApp

    MaterialTheme(
        colorScheme = demoColorScheme(
            darkMode = state.settings.darkMode,
            theme = state.settings.theme,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x2E009688),
                            Color(0x140B1118),
                            Color(0xFF0B1118),
                        ),
                        radius = 1200f,
                    ),
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DemoHeroSection()
                Spacer(modifier = Modifier.height(28.dp))
                PhoneFrame(
                    holder = holder,
                    state = state,
                    stats = stats,
                    filteredApps = filteredApps,
                    selectedApp = selectedApp,
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun DemoHeroSection() {
    Column(
        modifier = Modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0x26009688),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x594DB6AC)),
        ) {
            Text(
                text = "Interactive demo",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color(0xFF9FF2E4),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "NetGuard",
            color = Color(0xFFF4F7F6),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Per-app firewall for Android — explore the UI with mock apps. Toggle Wi-Fi and mobile " +
                "rules, browse logs, and switch themes. No VPN, no real data.",
            color = Color(0xB8F4F7F6),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneFrame(
    holder: DemoStateHolder,
    state: DemoState,
    stats: DemoStats,
    filteredApps: List<AppRule>,
    selectedApp: AppRule?,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 390.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(44.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2A3038), Color(0xFF12151A)),
                ),
            )
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(120.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(Color(0xFF0A0C0F)),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(780.dp),
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                StatusBar()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (state.tab) {
                        DemoTab.Home -> HomeScreen(
                            enabled = state.firewallEnabled,
                            stats = stats,
                            onToggle = holder::setFirewall,
                            onOpenApps = { filter ->
                                holder.setFilter(filter)
                                holder.setTab(DemoTab.Apps)
                            },
                        )
                        DemoTab.Apps -> AppsScreen(
                            apps = filteredApps,
                            filter = state.appsFilter,
                            searchQuery = state.searchQuery,
                            selectedAppId = state.selectedAppId,
                            onFilterChange = holder::setFilter,
                            onSearchChange = holder::setSearch,
                            onSelectApp = holder::selectApp,
                            onToggleWifi = holder::toggleWifi,
                            onToggleMobile = holder::toggleMobile,
                        )
                        DemoTab.Logs -> LogsScreen(
                            logs = state.logs,
                            loggingEnabled = state.settings.logTraffic,
                        )
                        DemoTab.Settings -> SettingsScreen(
                            settings = state.settings,
                            onUpdateBoolean = holder::updateSetting,
                            onUpdateTheme = { holder.updateSetting(DemoSettingKey.Theme, it) },
                        )
                    }

                    if (selectedApp != null && state.tab == DemoTab.Apps) {
                        AppDetailScreen(
                            app = selectedApp,
                            onBack = { holder.selectApp(null) },
                            onToggleWifi = { holder.toggleWifi(selectedApp.id) },
                            onToggleMobile = { holder.toggleMobile(selectedApp.id) },
                        )
                    }
                }
                DemoBottomNav(
                    active = state.tab,
                    onChange = holder::setTab,
                )
            }
        }
    }
}

@Composable
private fun StatusBar() {
    var hours by remember { mutableIntStateOf(14) }
    var minutes by remember { mutableIntStateOf(32) }
    var seconds by remember { mutableIntStateOf(8) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
            if (seconds >= 60) {
                seconds = 0
                minutes++
                if (minutes >= 60) {
                    minutes = 0
                    hours = (hours + 1) % 24
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Demo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = "100%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeScreen(
    enabled: Boolean,
    stats: DemoStats,
    onToggle: (Boolean) -> Unit,
    onOpenApps: (AppsFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = "NetGuard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.errorContainer,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (enabled) "Firewall active" else "Firewall off",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = if (enabled) {
                            "VPN is filtering traffic per app"
                        } else {
                            "All apps can access the network"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Blocked",
                value = stats.blocked,
                icon = Icons.Default.Block,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                onClick = { onOpenApps(AppsFilter.Blocked) },
            )
            StatCard(
                title = "Allowed",
                value = stats.allowed,
                icon = Icons.Default.CheckCircle,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { onOpenApps(AppsFilter.Allowed) },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StatCard(
            title = "Active rules",
            value = stats.total,
            icon = Icons.Default.Tune,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            emphasized = true,
            onClick = { onOpenApps(AppsFilter.All) },
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Demo uses mock apps — no real device data",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(if (emphasized) 18.dp else 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value.toString(),
                fontSize = if (emphasized) 32.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsScreen(
    apps: List<AppRule>,
    filter: AppsFilter,
    searchQuery: String,
    selectedAppId: String?,
    onFilterChange: (AppsFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectApp: (String?) -> Unit,
    onToggleWifi: (String) -> Unit,
    onToggleMobile: (String) -> Unit,
) {
    var searchOpen by remember { mutableStateOf(false) }
    val grouped = remember(apps) {
        val buckets = apps.groupBy { app ->
            val letter = app.name.firstOrNull()?.uppercaseChar() ?: '#'
            if (letter in 'A'..'Z') letter.toString() else "#"
        }
        buckets.keys.sorted().associateWith { buckets.getValue(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Firewall",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (apps.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = apps.size.toString(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (searchOpen) onSearchChange("")
                        searchOpen = !searchOpen
                    },
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searchOpen) "Close search" else "Search apps",
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppsFilterChip(
                    selected = filter == AppsFilter.All,
                    label = "All",
                    icon = Icons.Default.Public,
                    onClick = { onFilterChange(AppsFilter.All) },
                    modifier = Modifier.weight(1f),
                )
                AppsFilterChip(
                    selected = filter == AppsFilter.Blocked,
                    label = "Blocked",
                    icon = Icons.Default.Block,
                    onClick = { onFilterChange(AppsFilter.Blocked) },
                    modifier = Modifier.weight(1f),
                )
                AppsFilterChip(
                    selected = filter == AppsFilter.Allowed,
                    label = "Allowed",
                    icon = Icons.Default.CheckCircle,
                    onClick = { onFilterChange(AppsFilter.Allowed) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (searchOpen) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (apps.isEmpty()) {
                Text(
                    text = "No apps match this filter",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                grouped.forEach { (letter, sectionApps) ->
                    Text(
                        text = letter,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        sectionApps.forEachIndexed { index, app ->
                            AppRow(
                                app = app,
                                selected = selectedAppId == app.id,
                                isFirst = index == 0,
                                isLast = index == sectionApps.lastIndex,
                                onSelect = { onSelectApp(app.id) },
                                onToggleWifi = { onToggleWifi(app.id) },
                                onToggleMobile = { onToggleMobile(app.id) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsFilterChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        modifier = modifier,
        shape = RoundedCornerShape(if (selected) 14.dp else 12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun AppRow(
    app: AppRule,
    selected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onSelect: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(16.dp)
        isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        isLast -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
                else Color.Transparent,
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(app = app)
        Text(
            text = app.name,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        NetworkToggle(
            blocked = app.wifiBlocked,
            type = NetworkType.Wifi,
            onToggle = onToggleWifi,
        )
        NetworkToggle(
            blocked = app.mobileBlocked,
            type = NetworkType.Mobile,
            onToggle = onToggleMobile,
        )
    }
}

private enum class NetworkType { Wifi, Mobile }

@Composable
private fun NetworkToggle(
    blocked: Boolean,
    type: NetworkType,
    onToggle: () -> Unit,
) {
    val icon = when (type) {
        NetworkType.Wifi -> if (blocked) Icons.Default.WifiOff else Icons.Default.Wifi
        NetworkType.Mobile -> Icons.Default.PhoneAndroid
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (blocked) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AppIcon(
    app: AppRule,
    size: Int = 40,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(parseHexColor(app.iconColor)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = app.iconGlyph,
            color = Color.White,
            fontSize = (size * 0.32f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun AppDetailScreen(
    app: AppRule,
    onBack: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Back",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AppIcon(app = app, size = 56)
                    Column {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card {
                    DetailToggleRow(
                        icon = if (app.wifiBlocked) Icons.Default.WifiOff else Icons.Default.Wifi,
                        label = "Wi-Fi",
                        description = if (app.wifiBlocked) {
                            "Traffic blocked on Wi-Fi"
                        } else {
                            "Traffic allowed on Wi-Fi"
                        },
                        blocked = app.wifiBlocked,
                        onToggle = onToggleWifi,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    DetailToggleRow(
                        icon = Icons.Default.PhoneAndroid,
                        label = "Mobile data",
                        description = if (app.mobileBlocked) {
                            "Traffic blocked on mobile"
                        } else {
                            "Traffic allowed on mobile"
                        },
                        blocked = app.mobileBlocked,
                        onToggle = onToggleMobile,
                    )
                }

                if (app.system) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Text(
                            text = "System apps may require special handling on a real device. This is demo data only.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    blocked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = blocked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun LogsScreen(
    logs: List<LogEntry>,
    loggingEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Traffic log",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!loggingEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enable logging in Settings to capture traffic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "No traffic logged yet",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                logs.forEach { entry ->
                    LogCard(entry = entry)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry) {
    val allowed = entry.allowed
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.appName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (allowed) {
                        Color(0xFFC8E6C9)
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (allowed) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (allowed) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = if (allowed) "Allowed" else "Blocked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allowed) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Text(
                text = "${entry.time} · ${entry.protocol.name} · ${entry.destination}:${entry.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: DemoSettings,
    onUpdateBoolean: (DemoSettingKey, Boolean) -> Unit,
    onUpdateTheme: (DemoTheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "Settings",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        SettingsGroup(title = "Appearance") {
            SettingRow(
                icon = Icons.Default.DarkMode,
                label = "Dark mode",
                description = "Follow system or toggle manually",
            ) {
                Switch(
                    checked = settings.darkMode,
                    onCheckedChange = { onUpdateBoolean(DemoSettingKey.DarkMode, it) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            SettingRow(
                icon = Icons.Default.Palette,
                label = "Color theme",
                description = "Accent color for the app",
            ) {
                ThemeSelector(
                    selected = settings.theme,
                    onSelect = onUpdateTheme,
                )
            }
        }

        SettingsGroup(title = "Network") {
            SettingRow(
                icon = Icons.Default.Security,
                label = "Filter traffic",
                description = "Block ads and trackers (Pro)",
            ) {
                Switch(
                    checked = settings.filterEnabled,
                    onCheckedChange = { onUpdateBoolean(DemoSettingKey.FilterEnabled, it) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            SettingRow(
                icon = Icons.Default.Wifi,
                label = "Lockdown mode",
                description = "Block all traffic when enabled",
            ) {
                Switch(
                    checked = settings.lockdown,
                    onCheckedChange = { onUpdateBoolean(DemoSettingKey.Lockdown, it) },
                )
            }
        }

        SettingsGroup(title = "Logging") {
            SettingRow(
                icon = Icons.Default.Security,
                label = "Log network access",
                description = "Record allowed and blocked connections",
            ) {
                Switch(
                    checked = settings.logTraffic,
                    onCheckedChange = { onUpdateBoolean(DemoSettingKey.LogTraffic, it) },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Card { content() }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun ThemeSelector(
    selected: DemoTheme,
    onSelect: (DemoTheme) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DemoTheme.entries.forEach { theme ->
            val isSelected = theme == selected
            Surface(
                modifier = Modifier.clickable { onSelect(theme) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Text(
                    text = theme.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DemoBottomNav(
    active: DemoTab,
    onChange: (DemoTab) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.height(80.dp),
    ) {
        NavigationBarItem(
            selected = active == DemoTab.Home,
            onClick = { onChange(DemoTab.Home) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Home") },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = active == DemoTab.Apps,
            onClick = { onChange(DemoTab.Apps) },
            icon = { Icon(Icons.Default.Tune, contentDescription = "Firewall") },
            label = { Text("Firewall") },
        )
        NavigationBarItem(
            selected = active == DemoTab.Logs,
            onClick = { onChange(DemoTab.Logs) },
            icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
            label = { Text("Logs") },
        )
        NavigationBarItem(
            selected = active == DemoTab.Settings,
            onClick = { onChange(DemoTab.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
        )
    }
}

private fun demoColorScheme(
    darkMode: Boolean,
    theme: DemoTheme,
): androidx.compose.material3.ColorScheme {
    val primary = when (theme) {
        DemoTheme.Teal -> if (darkMode) Color(0xFF4DB6AC) else Color(0xFF009688)
        DemoTheme.Blue -> if (darkMode) Color(0xFF90CAF9) else Color(0xFF2196F3)
        DemoTheme.Purple -> if (darkMode) Color(0xFFCE93D8) else Color(0xFF9C27B0)
    }
    val onPrimary = when (theme) {
        DemoTheme.Teal -> if (darkMode) Color(0xFF003731) else Color.White
        DemoTheme.Blue -> if (darkMode) Color(0xFF003258) else Color.White
        DemoTheme.Purple -> if (darkMode) Color(0xFF4A148C) else Color.White
    }
    val primaryContainer = when (theme) {
        DemoTheme.Teal -> if (darkMode) Color(0xFF005048) else Color(0xFFB2DFDB)
        DemoTheme.Blue -> if (darkMode) Color(0xFF00497D) else Color(0xFFD0E4FF)
        DemoTheme.Purple -> if (darkMode) Color(0xFF6A1B9A) else Color(0xFFF8D8FF)
    }
    val onPrimaryContainer = when (theme) {
        DemoTheme.Teal -> if (darkMode) Color(0xFF9FF2E4) else Color(0xFF00251A)
        DemoTheme.Blue -> if (darkMode) Color(0xFFD0E4FF) else Color(0xFF001D36)
        DemoTheme.Purple -> if (darkMode) Color(0xFFF8D8FF) else Color(0xFF36003F)
    }
    val secondaryContainer = when (theme) {
        DemoTheme.Teal -> if (darkMode) Color(0xFF334B45) else Color(0xFFCCE8E2)
        DemoTheme.Blue -> if (darkMode) Color(0xFF334B45) else Color(0xFFC8E6FF)
        DemoTheme.Purple -> if (darkMode) Color(0xFF334B45) else Color(0xFFF0D4F7)
    }
    val onSecondaryContainer = when (theme) {
        DemoTheme.Teal -> if (darkMode) Color(0xFFCCE8E2) else Color(0xFF06201A)
        DemoTheme.Blue -> if (darkMode) Color(0xFFCCE8E2) else Color(0xFF001E31)
        DemoTheme.Purple -> if (darkMode) Color(0xFFCCE8E2) else Color(0xFF2F0A38)
    }

    return if (darkMode) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            surface = Color(0xFF0F1513),
            onSurface = Color(0xFFE0E3E1),
            onSurfaceVariant = Color(0xFFBEC9C5),
            surfaceContainerLow = Color(0xFF171D1B),
            surfaceContainerHigh = Color(0xFF222926),
            outlineVariant = Color(0xFF3F4946),
            error = Color(0xFFFFB4AB),
            errorContainer = Color(0xFF93000A),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            surface = Color(0xFFFBFDFC),
            onSurface = Color(0xFF171D1B),
            onSurfaceVariant = Color(0xFF3F4946),
            surfaceContainerLow = Color(0xFFF0F5F3),
            surfaceContainerHigh = Color(0xFFE6ECE9),
            outlineVariant = Color(0xFFBEC9C5),
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
        )
    }
}

private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLong(16)
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> Color.Gray
    }
}