package eu.faircode.netguard.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.core.content.ContextCompat
import eu.faircode.netguard.R
import eu.faircode.netguard.ServiceExternal
import eu.faircode.netguard.ServiceSinkhole
import eu.faircode.netguard.Widgets
import eu.faircode.netguard.WorkScheduler
import eu.faircode.netguard.data.Prefs
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
    val prefs by Prefs.data.collectAsState()
    val scrollState = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun bool(key: String, default: Boolean) = prefs[booleanPreferencesKey(key)] ?: default
    fun str(key: String, default: String) = prefs[stringPreferencesKey(key)] ?: default
    fun strSet(key: String, default: Set<String>) = prefs[stringSetPreferencesKey(key)] ?: default

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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

        SettingsSection(title = stringResource(R.string.setting_section_appearance)) {
            SettingToggleRow(
                title = stringResource(R.string.setting_dark),
                checked = bool("dark_theme", false),
            ) { Prefs.putBoolean("dark_theme", it) }

            Text(text = stringResource(R.string.setting_theme, str("theme", "teal")))
            val themes = listOf("teal", "blue", "purple", "amber", "orange", "green")
            themes.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { theme ->
                        FilterChip(
                            selected = str("theme", "teal") == theme,
                            onClick = { Prefs.putString("theme", theme) },
                            label = { Text(text = theme.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.setting_section_firewall)) {
            SettingToggleRow(
                title = stringResource(R.string.setting_whitelist_wifi),
                checked = bool("whitelist_wifi", true),
            ) { Prefs.putBoolean("whitelist_wifi", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_whitelist_other),
                checked = bool("whitelist_other", true),
            ) { Prefs.putBoolean("whitelist_other", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_whitelist_roaming),
                checked = bool("whitelist_roaming", true),
            ) { Prefs.putBoolean("whitelist_roaming", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_screen_on),
                checked = bool("screen_on", true),
            ) { Prefs.putBoolean("screen_on", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_screen_wifi),
                checked = bool("screen_wifi", false),
            ) { Prefs.putBoolean("screen_wifi", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_screen_other),
                checked = bool("screen_other", false),
            ) { Prefs.putBoolean("screen_other", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_subnet),
                checked = bool("subnet", false),
            ) { Prefs.putBoolean("subnet", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_tethering),
                checked = bool("tethering", false),
            ) { Prefs.putBoolean("tethering", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_lan),
                checked = bool("lan", false),
            ) { Prefs.putBoolean("lan", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_ip6),
                checked = bool("ip6", true),
            ) { Prefs.putBoolean("ip6", it) }

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
            ) { Prefs.putBoolean("use_metered", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_metered_2g),
                checked = bool("unmetered_2g", false),
            ) { Prefs.putBoolean("unmetered_2g", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_metered_3g),
                checked = bool("unmetered_3g", false),
            ) { Prefs.putBoolean("unmetered_3g", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_metered_4g),
                checked = bool("unmetered_4g", false),
            ) { Prefs.putBoolean("unmetered_4g", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_national_roaming),
                checked = bool("national_roaming", false),
            ) { Prefs.putBoolean("national_roaming", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_eu_roaming),
                checked = bool("eu_roaming", false),
            ) { Prefs.putBoolean("eu_roaming", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_call),
                checked = bool("disable_on_call", false),
            ) { Prefs.putBoolean("disable_on_call", it) }
        }

        SettingsSection(title = stringResource(R.string.setting_section_advanced)) {
            SettingToggleRow(
                title = stringResource(R.string.setting_system),
                checked = bool("manage_system", false),
            ) { Prefs.putBoolean("manage_system", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_log_app),
                checked = bool("log_app", false),
            ) { Prefs.putBoolean("log_app", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_access),
                checked = bool("notify_access", false),
            ) { Prefs.putBoolean("notify_access", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_filter),
                checked = bool("filter", false),
            ) {
                Prefs.putBoolean("filter", it)
                ServiceSinkhole.reload("settings", context, false)
            }
            SettingToggleRow(
                title = stringResource(R.string.setting_filter_udp),
                checked = bool("filter_udp", false),
            ) {
                Prefs.putBoolean("filter_udp", it)
                ServiceSinkhole.reload("settings", context, false)
            }
            SettingToggleRow(
                title = stringResource(R.string.setting_lockdown),
                checked = bool("lockdown", false),
            ) {
                Prefs.putBoolean("lockdown", it)
                ServiceSinkhole.reload("settings", context, false)
                Widgets.updateLockdown(context)
            }
            SettingToggleRow(
                title = stringResource(R.string.setting_lockdown_wifi),
                checked = bool("lockdown_wifi", false),
            ) {
                Prefs.putBoolean("lockdown_wifi", it)
                ServiceSinkhole.reload("settings", context, false)
            }
            SettingToggleRow(
                title = stringResource(R.string.setting_lockdown_other),
                checked = bool("lockdown_other", false),
            ) {
                Prefs.putBoolean("lockdown_other", it)
                ServiceSinkhole.reload("settings", context, false)
            }
            SettingToggleRow(
                title = stringResource(R.string.setting_malware),
                checked = bool("malware", false),
            ) { Prefs.putBoolean("malware", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_track_usage),
                checked = bool("track_usage", false),
            ) { Prefs.putBoolean("track_usage", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_stats),
                checked = bool("show_stats", false),
            ) { Prefs.putBoolean("show_stats", it) }
            SettingToggleRow(
                title = stringResource(R.string.setting_stats_top),
                checked = bool("show_top", false),
            ) { Prefs.putBoolean("show_top", it) }

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

        SettingsSection(title = stringResource(R.string.setting_section_hosts)) {
            SettingToggleRow(
                title = stringResource(R.string.setting_use_hosts),
                checked = bool("use_hosts", false),
            ) {
                Prefs.putBoolean("use_hosts", it)
                ServiceSinkhole.reload("settings", context, false)
            }

            SettingTextRow(
                title = stringResource(R.string.setting_hosts_url),
                value = str("hosts_url", ""),
                onValueChange = { Prefs.putString("hosts_url", it) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = { importHostsLauncher.launch(arrayOf("*/*")) }) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.setting_hosts))
                }
                FilledTonalButton(onClick = { appendHostsLauncher.launch(arrayOf("*/*")) }) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.setting_hosts_download))
            }
        }

        SettingsSection(title = stringResource(R.string.setting_section_dns)) {
            SettingTextRow(
                title = stringResource(R.string.setting_rcode, str("rcode", "3")),
                value = str("rcode", "3"),
                keyboardType = KeyboardType.Number,
                onValueChange = { Prefs.putString("rcode", it) },
            )
            SettingTextRow(
                title = stringResource(R.string.setting_ttl, str("ttl", "259200")),
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.setting_show_resolved))
            }
        }

        SettingsSection(title = stringResource(R.string.setting_forwarding)) {
            FilledTonalButton(onClick = onOpenForwarding) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.setting_forwarding))
            }
        }

        SettingsSection(title = stringResource(R.string.setting_section_proxy)) {
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

        SettingsSection(title = stringResource(R.string.setting_section_vpn)) {
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

        SettingsSection(title = stringResource(R.string.setting_section_background)) {
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

        SettingsSection(title = stringResource(R.string.setting_section_diagnostics)) {
            SettingToggleRow(
                title = stringResource(R.string.setting_pcap),
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

        SettingsSection(title = stringResource(R.string.setting_section_support)) {
            FilledTonalButton(onClick = onOpenPro) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.title_pro))
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                content()
            },
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingTextRow(
    title: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
