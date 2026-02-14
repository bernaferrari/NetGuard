package eu.faircode.netguard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import eu.faircode.netguard.R
import eu.faircode.netguard.ServiceExternal
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.Widgets
import eu.faircode.netguard.WorkScheduler
import eu.faircode.netguard.data.Prefs
import eu.faircode.netguard.ui.components.ExpandableContent
import eu.faircode.netguard.ui.components.animateContentHeight
import eu.faircode.netguard.ui.theme.LocalMotion
import eu.faircode.netguard.ui.theme.TouchTargets
import eu.faircode.netguard.ui.theme.spacing
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDns: () -> Unit,
    onOpenForwarding: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val prefs by Prefs.data.collectAsState()
    val scrollState = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Search state
    var searchQuery by rememberSaveable { mutableStateOf("") }

    fun bool(key: String, default: Boolean) = prefs[booleanPreferencesKey(key)] ?: default
    fun str(key: String, default: String) = prefs[stringPreferencesKey(key)] ?: default
    fun strSet(key: String, default: Set<String>) = prefs[stringSetPreferencesKey(key)] ?: default
    fun updateFlag(
        key: String,
        value: Boolean,
        reload: Boolean = false,
        reloadStats: Boolean = false,
        updateWidgets: (() -> Unit)? = null,
    ) {
        Prefs.putBoolean(key, value)
        if (reload) {
            ServiceSinkhole.reload("settings", context, false)
        }
        if (reloadStats) {
            ServiceSinkhole.reloadStats("settings", context)
        }
        updateWidgets?.invoke()
    }

    var showHostImportHint by remember { mutableStateOf(false) }
    val importHostsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importHosts(scope, context, uri, append = false) { showHostImportHint = it }
            }
        }
    val appendHostsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importHosts(scope, context, uri, append = true) { showHostImportHint = it }
            }
        }

    // Filter sections based on search
    val isSearching = searchQuery.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.default),
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.menu_settings),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.setting_options),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_settings)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_settings),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.action_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
        )

        // Appearance Section
        val appearanceTitle = stringResource(R.string.setting_section_appearance)
        if (!isSearching || appearanceTitle.contains(searchQuery, ignoreCase = true) ||
            "dark theme".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(
                title = appearanceTitle,
                initiallyExpanded = !isSearching,
            ) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_dark),
                    checked = bool("dark_theme", false),
                ) {
                    updateFlag("dark_theme", it)
                    Widgets.updateAll(context)
                }

                Text(text = stringResource(R.string.setting_theme, str("theme", "teal")))
                val themes = listOf("teal", "blue", "purple", "amber", "orange", "green")
                themes.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        row.forEach { theme ->
                            FilterChip(
                                selected = str("theme", "teal") == theme,
                                onClick = {
                                    Prefs.putString("theme", theme)
                                    Widgets.updateAll(context)
                                },
                                label = { Text(text = theme.replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
            }
        }

        // Firewall Section
        val firewallTitle = stringResource(R.string.setting_section_firewall)
        if (!isSearching || firewallTitle.contains(searchQuery, ignoreCase = true) ||
            "wifi block mobile".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = firewallTitle) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_whitelist_wifi),
                    checked = bool("whitelist_wifi", true),
                ) { updateFlag("whitelist_wifi", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_whitelist_other),
                    checked = bool("whitelist_other", true),
                ) { updateFlag("whitelist_other", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_whitelist_roaming),
                    checked = bool("whitelist_roaming", true),
                ) { updateFlag("whitelist_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_screen_on),
                    checked = bool("screen_on", true),
                ) { updateFlag("screen_on", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_screen_wifi),
                    checked = bool("screen_wifi", false),
                ) { updateFlag("screen_wifi", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_screen_other),
                    checked = bool("screen_other", false),
                ) { updateFlag("screen_other", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_subnet),
                    checked = bool("subnet", false),
                ) { updateFlag("subnet", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_tethering),
                    checked = bool("tethering", false),
                ) { updateFlag("tethering", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_lan),
                    checked = bool("lan", false),
                ) { updateFlag("lan", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_ip6),
                    checked = bool("ip6", true),
                ) { updateFlag("ip6", it, reload = true) }

                SettingTextRow(
                    title = stringResource(R.string.setting_delay, "0"),
                    value = str("screen_delay", "0"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("screen_delay", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_auto, "0"),
                    value = str("auto_enable", "0"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("auto_enable", it) },
                )

                val homes = strSet("wifi_homes", emptySet()).joinToString(",")
                SettingTextRow(
                    title = stringResource(R.string.setting_wifi_home, homes.ifEmpty { "-" }),
                    value = homes,
                    onValueChange = { value ->
                        val set = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        Prefs.putStringSet("wifi_homes", set)
                    },
                )

                SettingToggleRow(
                    title = stringResource(R.string.setting_metered),
                    checked = bool("use_metered", true),
                ) { updateFlag("use_metered", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_metered_2g),
                    checked = bool("unmetered_2g", false),
                ) { updateFlag("unmetered_2g", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_metered_3g),
                    checked = bool("unmetered_3g", false),
                ) { updateFlag("unmetered_3g", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_metered_4g),
                    checked = bool("unmetered_4g", false),
                ) { updateFlag("unmetered_4g", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_national_roaming),
                    checked = bool("national_roaming", false),
                ) { updateFlag("national_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_eu_roaming),
                    checked = bool("eu_roaming", false),
                ) { updateFlag("eu_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_call),
                    checked = bool("disable_on_call", false),
                ) { updateFlag("disable_on_call", it, reload = true) }
            }
        }

        // Advanced Section
        val advancedTitle = stringResource(R.string.setting_section_advanced)
        if (!isSearching || advancedTitle.contains(searchQuery, ignoreCase = true) ||
            "filter lockdown".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = advancedTitle) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_system),
                    checked = bool("manage_system", false),
                ) { updateFlag("manage_system", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_log_app),
                    checked = bool("log_app", false),
                ) { updateFlag("log_app", it) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_access),
                    checked = bool("notify_access", false),
                ) { updateFlag("notify_access", it) }
                SettingToggleRowWithTooltip(
                    title = stringResource(R.string.setting_filter),
                    tooltip = stringResource(R.string.tooltip_filter),
                    checked = bool("filter", false),
                ) { updateFlag("filter", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_filter_udp),
                    checked = bool("filter_udp", false),
                ) { updateFlag("filter_udp", it, reload = true) }
                SettingToggleRowWithTooltip(
                    title = stringResource(R.string.setting_lockdown),
                    tooltip = stringResource(R.string.tooltip_lockdown),
                    checked = bool("lockdown", false),
                ) {
                    updateFlag(
                        "lockdown",
                        it,
                        reload = true,
                        updateWidgets = { Widgets.updateLockdown(context) },
                    )
                }
                SettingToggleRow(
                    title = stringResource(R.string.setting_lockdown_wifi),
                    checked = bool("lockdown_wifi", false),
                ) { updateFlag("lockdown_wifi", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_lockdown_other),
                    checked = bool("lockdown_other", false),
                ) { updateFlag("lockdown_other", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_malware),
                    checked = bool("malware", false),
                ) { updateFlag("malware", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_track_usage),
                    checked = bool("track_usage", false),
                ) { updateFlag("track_usage", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_stats),
                    checked = bool("show_stats", false),
                ) { updateFlag("show_stats", it, reloadStats = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_stats_top),
                    checked = bool("show_top", false),
                ) { updateFlag("show_top", it, reloadStats = true) }

                SettingTextRow(
                    title = stringResource(R.string.setting_stats_frequency, str("stats_frequency", "0")),
                    value = str("stats_frequency", "1000"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("stats_frequency", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_stats_samples, str("stats_samples", "0")),
                    value = str("stats_samples", "10"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("stats_samples", it) },
                )
            }
        }

        // Hosts Section
        val hostsTitle = stringResource(R.string.setting_section_hosts)
        if (!isSearching || hostsTitle.contains(searchQuery, ignoreCase = true) ||
            "hosts dns block".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = hostsTitle) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_use_hosts),
                    checked = bool("use_hosts", false),
                ) { updateFlag("use_hosts", it, reload = true) }

                SettingTextRow(
                    title = stringResource(R.string.setting_hosts_url),
                    value = str("hosts_url", ""),
                    onValueChange = { Prefs.putString("hosts_url", it) },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    FilledTonalButton(onClick = { importHostsLauncher.launch(arrayOf("*/*")) }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(R.string.setting_hosts))
                    }
                    FilledTonalButton(onClick = { appendHostsLauncher.launch(arrayOf("*/*")) }) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(R.string.setting_hosts_append))
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val intent = android.content.Intent(context, ServiceExternal::class.java)
                        intent.action = ServiceExternal.ACTION_DOWNLOAD_HOSTS_FILE
                        ContextCompat.startForegroundService(context, intent)
                    },
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.setting_hosts_download))
                }
            }
        }

        // DNS Section
        val dnsTitle = stringResource(R.string.setting_section_dns)
        if (!isSearching || dnsTitle.contains(searchQuery, ignoreCase = true) ||
            "dns rcode ttl".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = dnsTitle) {
                SettingTextRowWithTooltip(
                    title = stringResource(R.string.setting_rcode, str("rcode", "3")),
                    tooltip = stringResource(R.string.tooltip_rcode),
                    value = str("rcode", "3"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("rcode", it) },
                )
                SettingTextRowWithTooltip(
                    title = stringResource(R.string.setting_ttl, str("ttl", "259200")),
                    tooltip = stringResource(R.string.tooltip_ttl),
                    value = str("ttl", "259200"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("ttl", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_validate, str("validate", "")),
                    value = str("validate", ""),
                    onValueChange = { Prefs.putString("validate", it) },
                )
                FilledTonalButton(onClick = onOpenDns) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.setting_show_resolved))
                }
            }
        }

        // Forwarding Section
        val forwardingTitle = stringResource(R.string.setting_forwarding)
        if (!isSearching || forwardingTitle.contains(searchQuery, ignoreCase = true) ||
            "forward port".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = forwardingTitle) {
                FilledTonalButton(onClick = onOpenForwarding) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.setting_forwarding))
                }
            }
        }

        // Proxy Section
        val proxyTitle = stringResource(R.string.setting_section_proxy)
        if (!isSearching || proxyTitle.contains(searchQuery, ignoreCase = true) ||
            "socks proxy".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = proxyTitle) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_socks5_enabled),
                    checked = bool("socks5_enabled", false),
                ) { Prefs.putBoolean("socks5_enabled", it) }
                SettingTextRow(
                    title = stringResource(R.string.setting_socks5_addr, str("socks5_addr", "")),
                    value = str("socks5_addr", ""),
                    onValueChange = { Prefs.putString("socks5_addr", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_socks5_port, str("socks5_port", "0")),
                    value = str("socks5_port", "0"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("socks5_port", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_socks5_username, str("socks5_username", "")),
                    value = str("socks5_username", ""),
                    onValueChange = { Prefs.putString("socks5_username", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_socks5_password, str("socks5_password", "")),
                    value = str("socks5_password", ""),
                    onValueChange = { Prefs.putString("socks5_password", it) },
                )
            }
        }

        // VPN Section
        val vpnTitle = stringResource(R.string.setting_section_vpn)
        if (!isSearching || vpnTitle.contains(searchQuery, ignoreCase = true) ||
            "vpn dns address".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = vpnTitle) {
                SettingTextRow(
                    title = stringResource(R.string.setting_vpn4, str("vpn4", "10.1.10.1")),
                    value = str("vpn4", "10.1.10.1"),
                    onValueChange = { Prefs.putString("vpn4", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_vpn6, str("vpn6", "")),
                    value = str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                    onValueChange = { Prefs.putString("vpn6", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_dns, str("dns", "")),
                    value = str("dns", ""),
                    onValueChange = { Prefs.putString("dns", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_dns2, str("dns2", "")),
                    value = str("dns2", ""),
                    onValueChange = { Prefs.putString("dns2", it) },
                )
            }
        }

        // Background Section
        val backgroundTitle = stringResource(R.string.setting_section_background)
        if (!isSearching || backgroundTitle.contains(searchQuery, ignoreCase = true) ||
            "watchdog update".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = backgroundTitle) {
                SettingTextRow(
                    title = stringResource(R.string.setting_watchdog, str("watchdog", "0")),
                    value = str("watchdog", "0"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { value ->
                        Prefs.putString("watchdog", value)
                        val enabled = Prefs.getBoolean("enabled", false)
                        WorkScheduler.scheduleWatchdog(context, value.toIntOrNull() ?: 0, enabled)
                    },
                )
                SettingToggleRow(
                    title = stringResource(R.string.setting_update),
                    checked = bool("update_check", true),
                ) { Prefs.putBoolean("update_check", it) }
            }
        }

        // Diagnostics Section
        val diagnosticsTitle = stringResource(R.string.setting_section_diagnostics)
        if (!isSearching || diagnosticsTitle.contains(searchQuery, ignoreCase = true) ||
            "pcap record".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = diagnosticsTitle) {
                SettingToggleRowWithTooltip(
                    title = stringResource(R.string.setting_pcap),
                    tooltip = stringResource(R.string.tooltip_pcap),
                    checked = bool("pcap", false),
                ) { Prefs.putBoolean("pcap", it) }
                SettingTextRow(
                    title = stringResource(R.string.setting_pcap_record_size, str("pcap_record_size", "64")),
                    value = str("pcap_record_size", "64"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("pcap_record_size", it) },
                )
                SettingTextRow(
                    title = stringResource(R.string.setting_pcap_file_size, str("pcap_file_size", "2")),
                    value = str("pcap_file_size", "2"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("pcap_file_size", it) },
                )
            }
        }

        // Support Section
        val supportTitle = stringResource(R.string.setting_section_support)
        if (!isSearching || supportTitle.contains(searchQuery, ignoreCase = true) ||
            "pro support".contains(searchQuery, ignoreCase = true)) {
            CollapsibleSettingsSection(title = supportTitle, initiallyExpanded = true) {
                FilledTonalButton(onClick = onOpenPro) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.title_pro))
                }
            }
        }
    }

    if (showHostImportHint) {
        AlertDialog(
            onDismissRequest = { showHostImportHint = false },
            confirmButton = {
                TextButton(onClick = { showHostImportHint = false }) {
                    Text(text = stringResource(R.string.menu_ok))
                }
            },
            title = { Text(text = stringResource(R.string.setting_hosts)) },
            text = { Text(text = stringResource(R.string.summary_block_domains)) },
        )
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    // Animated rotation for chevron
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(motion.durationMedium),
        label = "chevronRotation"
    )

    val expandDescription = if (expanded) {
        stringResource(R.string.action_collapse)
    } else {
        stringResource(R.string.action_expand)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.animateContentHeight(),
    ) {
        Column {
            // Header - always visible, clickable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(spacing.default),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(TouchTargets.minimum),
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = expandDescription,
                        modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
                    )
                }
            }

            // Expandable content
            ExpandableContent(expanded = expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = spacing.default,
                        end = spacing.default,
                        bottom = spacing.default,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(newValue)
            },
            modifier = Modifier.semantics {
                contentDescription = "$title: ${if (checked) "enabled" else "disabled"}"
            },
        )
    }
}

@Composable
private fun SettingToggleRowWithTooltip(
    title: String,
    tooltip: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    var showTooltip by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = title)
                IconButton(
                    onClick = { showTooltip = !showTooltip },
                    modifier = Modifier.size(TouchTargets.minimum),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Show information about $title",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(newValue)
                },
            )
        }

        // Animated tooltip
        ExpandableContent(expanded = showTooltip) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(bottom = spacing.small),
            ) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(spacing.small),
                )
            }
        }
    }
}

@Composable
private fun SettingTextRow(
    title: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun SettingTextRowWithTooltip(
    title: String,
    tooltip: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var showTooltip by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            IconButton(
                onClick = { showTooltip = !showTooltip },
                modifier = Modifier.size(TouchTargets.minimum),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Show information about $title",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Animated tooltip
        ExpandableContent(expanded = showTooltip) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(bottom = spacing.small),
            ) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(spacing.small),
                )
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

private fun importHosts(
    scope: CoroutineScope,
    context: android.content.Context,
    uri: Uri,
    append: Boolean,
    onComplete: (Boolean) -> Unit,
) {
    scope.launch {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { input ->
                val target = File(context.filesDir, "hosts.txt")
                target.parentFile?.mkdirs()
                if (!append) {
                    target.outputStream().use { output -> input.copyTo(output) }
                } else {
                    FileOutputStream(target, true).use { output ->
                        output.write("\n".toByteArray())
                        input.copyTo(output)
                    }
                }
            }
        }
        ServiceSinkhole.reload("hosts file import", context, false)
        onComplete(true)
    }
}
