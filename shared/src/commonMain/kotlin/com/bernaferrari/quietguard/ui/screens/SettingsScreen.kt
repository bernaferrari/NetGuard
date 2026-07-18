package com.bernaferrari.quietguard.ui.screens

import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.app_description
import com.bernaferrari.quietguard.generated.resources.app_name
import com.bernaferrari.quietguard.generated.resources.content_desc_show_info
import com.bernaferrari.quietguard.generated.resources.menu_about
import com.bernaferrari.quietguard.generated.resources.menu_ok
import com.bernaferrari.quietguard.generated.resources.setting_default_value
import com.bernaferrari.quietguard.generated.resources.setting_reset_accessibility
import com.bernaferrari.quietguard.generated.resources.setting_reset_to_default
import com.bernaferrari.quietguard.generated.resources.setting_value_empty
import com.bernaferrari.quietguard.generated.resources.setting_value_off
import com.bernaferrari.quietguard.generated.resources.setting_value_on
import com.bernaferrari.quietguard.generated.resources.setting_access
import com.bernaferrari.quietguard.generated.resources.setting_auto
import com.bernaferrari.quietguard.generated.resources.setting_delay
import com.bernaferrari.quietguard.generated.resources.setting_dns
import com.bernaferrari.quietguard.generated.resources.setting_dns2
import com.bernaferrari.quietguard.generated.resources.setting_pcap_file_size
import com.bernaferrari.quietguard.generated.resources.setting_pcap_record_size
import com.bernaferrari.quietguard.generated.resources.setting_rcode
import com.bernaferrari.quietguard.generated.resources.setting_socks5_addr
import com.bernaferrari.quietguard.generated.resources.setting_socks5_password
import com.bernaferrari.quietguard.generated.resources.setting_socks5_port
import com.bernaferrari.quietguard.generated.resources.setting_socks5_username
import com.bernaferrari.quietguard.generated.resources.setting_stats_frequency
import com.bernaferrari.quietguard.generated.resources.setting_stats_samples
import com.bernaferrari.quietguard.generated.resources.setting_ttl
import com.bernaferrari.quietguard.generated.resources.setting_update_result_available
import com.bernaferrari.quietguard.generated.resources.setting_validate
import com.bernaferrari.quietguard.generated.resources.setting_vpn4
import com.bernaferrari.quietguard.generated.resources.setting_vpn6
import com.bernaferrari.quietguard.generated.resources.setting_watchdog
import com.bernaferrari.quietguard.generated.resources.setting_wifi_home
import com.bernaferrari.quietguard.generated.resources.setting_appearance_auto
import com.bernaferrari.quietguard.generated.resources.setting_appearance_dark
import com.bernaferrari.quietguard.generated.resources.setting_appearance_light
import com.bernaferrari.quietguard.generated.resources.setting_call
import com.bernaferrari.quietguard.generated.resources.setting_eu_roaming
import com.bernaferrari.quietguard.generated.resources.setting_filter
import com.bernaferrari.quietguard.generated.resources.setting_filter_udp
import com.bernaferrari.quietguard.generated.resources.setting_forwarding
import com.bernaferrari.quietguard.generated.resources.setting_hosts
import com.bernaferrari.quietguard.generated.resources.setting_hosts_append
import com.bernaferrari.quietguard.generated.resources.setting_hosts_download
import com.bernaferrari.quietguard.generated.resources.setting_hosts_url
import com.bernaferrari.quietguard.generated.resources.setting_ip6
import com.bernaferrari.quietguard.generated.resources.setting_lan
import com.bernaferrari.quietguard.generated.resources.setting_lockdown
import com.bernaferrari.quietguard.generated.resources.setting_lockdown_other
import com.bernaferrari.quietguard.generated.resources.setting_lockdown_wifi
import com.bernaferrari.quietguard.generated.resources.setting_log_app
import com.bernaferrari.quietguard.generated.resources.setting_log_retention_days
import com.bernaferrari.quietguard.generated.resources.setting_malware
import com.bernaferrari.quietguard.generated.resources.setting_metered
import com.bernaferrari.quietguard.generated.resources.setting_metered_2g
import com.bernaferrari.quietguard.generated.resources.setting_metered_3g
import com.bernaferrari.quietguard.generated.resources.setting_metered_4g
import com.bernaferrari.quietguard.generated.resources.setting_national_roaming
import com.bernaferrari.quietguard.generated.resources.setting_pcap
import com.bernaferrari.quietguard.generated.resources.setting_screen_on
import com.bernaferrari.quietguard.generated.resources.setting_screen_other
import com.bernaferrari.quietguard.generated.resources.setting_screen_wifi
import com.bernaferrari.quietguard.generated.resources.setting_section_advanced
import com.bernaferrari.quietguard.generated.resources.setting_section_appearance
import com.bernaferrari.quietguard.generated.resources.setting_section_background
import com.bernaferrari.quietguard.generated.resources.setting_section_diagnostics
import com.bernaferrari.quietguard.generated.resources.setting_section_dns
import com.bernaferrari.quietguard.generated.resources.setting_section_firewall
import com.bernaferrari.quietguard.generated.resources.setting_section_hosts
import com.bernaferrari.quietguard.generated.resources.setting_section_network
import com.bernaferrari.quietguard.generated.resources.setting_section_proxy
import com.bernaferrari.quietguard.generated.resources.setting_section_vpn
import com.bernaferrari.quietguard.generated.resources.setting_show_resolved
import com.bernaferrari.quietguard.generated.resources.setting_socks5_enabled
import com.bernaferrari.quietguard.generated.resources.setting_stats
import com.bernaferrari.quietguard.generated.resources.setting_stats_top
import com.bernaferrari.quietguard.generated.resources.setting_subnet
import com.bernaferrari.quietguard.generated.resources.setting_system
import com.bernaferrari.quietguard.generated.resources.setting_tethering
import com.bernaferrari.quietguard.generated.resources.setting_theme_palette
import com.bernaferrari.quietguard.generated.resources.setting_theme_teal
import com.bernaferrari.quietguard.generated.resources.setting_track_usage
import com.bernaferrari.quietguard.generated.resources.setting_update
import com.bernaferrari.quietguard.generated.resources.setting_update_checking
import com.bernaferrari.quietguard.generated.resources.setting_update_now
import com.bernaferrari.quietguard.generated.resources.setting_update_result_available_unknown
import com.bernaferrari.quietguard.generated.resources.setting_update_result_failed
import com.bernaferrari.quietguard.generated.resources.setting_update_result_unavailable
import com.bernaferrari.quietguard.generated.resources.setting_update_result_up_to_date
import com.bernaferrari.quietguard.generated.resources.setting_use_hosts
import com.bernaferrari.quietguard.generated.resources.setting_whitelist_roaming
import com.bernaferrari.quietguard.generated.resources.summary_block_domains
import com.bernaferrari.quietguard.generated.resources.summary_log_retention_days
import com.bernaferrari.quietguard.generated.resources.title_mobile
import com.bernaferrari.quietguard.generated.resources.title_block
import com.bernaferrari.quietguard.generated.resources.title_wifi
import com.bernaferrari.quietguard.generated.resources.tooltip_filter
import com.bernaferrari.quietguard.generated.resources.tooltip_lockdown
import com.bernaferrari.quietguard.generated.resources.tooltip_pcap
import com.bernaferrari.quietguard.generated.resources.tooltip_rcode
import com.bernaferrari.quietguard.generated.resources.tooltip_ttl
import com.bernaferrari.quietguard.generated.resources.ui_settings_title
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.PlatformContext

import com.bernaferrari.quietguard.platform.showToast
import com.bernaferrari.quietguard.ui.screens.vm.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.quietguard.ui.components.ExpandableContent
import com.bernaferrari.quietguard.ui.components.FirewallTile
import com.bernaferrari.quietguard.ui.theme.AmberPrimary
import com.bernaferrari.quietguard.ui.theme.BluePrimary
import com.bernaferrari.quietguard.ui.theme.CyanPrimary
import com.bernaferrari.quietguard.ui.theme.GreenPrimary
import com.bernaferrari.quietguard.ui.theme.IndigoPrimary
import com.bernaferrari.quietguard.ui.theme.LimePrimary
import com.bernaferrari.quietguard.ui.theme.OrangePrimary
import com.bernaferrari.quietguard.ui.theme.PinkPrimary
import com.bernaferrari.quietguard.ui.theme.PurplePrimary
import com.bernaferrari.quietguard.ui.theme.Teal500
import com.bernaferrari.quietguard.ui.theme.TouchTargets
import com.bernaferrari.quietguard.ui.theme.spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROJECT_GITHUB_URL = "https://github.com/bernaferrari/QuietGuard"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onOpenDns: () -> Unit,
    onOpenForwarding: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val viewModel: SettingsViewModel = koinViewModel()
    val preferencesRepository = viewModel.preferencesRepository
    val prefs by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

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
        preferencesRepository.putBoolean(key, value)
        if (reload) {
            NetGuardPlatform.firewall.reload("settings", false)
        }
        if (reloadStats) {
            NetGuardPlatform.firewall.reloadStats("settings")
        }
        updateWidgets?.invoke()
    }

    fun resetFlag(
        key: String,
        reload: Boolean = false,
        reloadStats: Boolean = false,
        updateWidgets: (() -> Unit)? = null,
    ) {
        preferencesRepository.removeBoolean(key)
        if (reload) {
            NetGuardPlatform.firewall.reload("settings", false)
        }
        if (reloadStats) {
            NetGuardPlatform.firewall.reloadStats("settings")
        }
        updateWidgets?.invoke()
    }

    var showHostImportHint by remember { mutableStateOf(false) }
    var updateCheckInProgress by remember { mutableStateOf(false) }
    var updateCheckStatus by remember { mutableStateOf<String?>(null) }
    var updateCheckVersion by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val dispose =
            viewModel.registerUpdateCheck { status, version ->
                updateCheckInProgress = false
                updateCheckStatus = status
                updateCheckVersion = version
            }
        onDispose { dispose() }
    }

    val appearanceMode =
        when (str("appearance", "")) {
            "light", "dark", "auto" -> str("appearance", "auto")
            else ->
                if (prefs.asMap().containsKey(booleanPreferencesKey("dark_theme"))) {
                    if (bool("dark_theme", false)) "dark" else "light"
                } else {
                    "auto"
                }
        }

    fun updateAppearance(mode: String) {
        preferencesRepository.putString("appearance", mode)
        when (mode) {
            "auto" -> preferencesRepository.removeString("dark_theme")
            "dark" -> preferencesRepository.putBoolean("dark_theme", true)
            "light" -> preferencesRepository.putBoolean("dark_theme", false)
        }
        NetGuardPlatform.widgets.updateAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.ui_settings_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.large, vertical = spacing.default),
                verticalArrangement = Arrangement.spacedBy(spacing.extraLarge),
            ) {


            // Appearance Section
            val appearanceTitle = stringResource(Res.string.setting_section_appearance)
            CollapsibleSettingsSection(title = appearanceTitle, framed = false) {
                val currentTheme = str("theme", "teal")
                val dynamicSwatchColor = Teal500
                val modeOptions = listOf(
                    Triple(
                        "light",
                        stringResource(Res.string.setting_appearance_light),
                        Pair(Icons.Default.LightMode, Icons.Outlined.LightMode),
                    ),
                    Triple(
                        "dark",
                        stringResource(Res.string.setting_appearance_dark),
                        Pair(Icons.Default.DarkMode, Icons.Outlined.DarkMode),
                    ),
                    Triple(
                        "auto",
                        stringResource(Res.string.setting_appearance_auto),
                        Pair(Icons.Default.BrightnessAuto, Icons.Outlined.BrightnessAuto),
                    ),
                )

                // ── Appearance mode toggle — connected buttons ──
                val selectedIndex = modeOptions.indexOfFirst { it.first == appearanceMode }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.extraSmall, bottom = 0.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    modeOptions.forEachIndexed { index, (mode, label, icons) ->
                        val selected = index == selectedIndex
                        ToggleButton(
                            checked = selected,
                            onCheckedChange = { isChecked ->
                                if (isChecked) updateAppearance(mode)
                            },
                            shapes =
                                when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    modeOptions.lastIndex ->
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()

                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.semantics { role = Role.RadioButton },
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Default.Check else icons.second,
                                contentDescription = null,
                                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.size(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                // ── Color theme swatches ──
                val themeChoices = buildList {
                    if (PlatformContext.isAndroid()) {
                        add(Pair("dynamic", null as Color?))
                    }
                    add(Pair("teal", Teal500 as Color?))
                    add(Pair("blue", BluePrimary as Color?))
                    add(Pair("purple", PurplePrimary as Color?))
                    add(Pair("amber", AmberPrimary as Color?))
                    add(Pair("orange", OrangePrimary as Color?))
                    add(Pair("green", GreenPrimary as Color?))
                    add(Pair("cyan", CyanPrimary as Color?))
                    add(Pair("indigo", IndigoPrimary as Color?))
                    add(Pair("pink", PinkPrimary as Color?))
                    add(Pair("lime", LimePrimary as Color?))
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.extraSmall, bottom = spacing.extraSmall),
                    horizontalArrangement =
                        Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    themeChoices.forEach { (theme, seedColor) ->
                        ThemeSwatch(
                            theme = theme,
                            seedColor = seedColor,
                            isSelected = currentTheme == theme,
                            isEnabled = true,
                            dynamicColor = dynamicSwatchColor,
                            onClick = {
                                preferencesRepository.putString("theme", theme)
                                NetGuardPlatform.widgets.updateAll()
                            },
                        )
                    }
                    if (currentTheme != "teal") {
                        SettingResetAction(
                            title = stringResource(Res.string.setting_theme_palette),
                            defaultValue = stringResource(Res.string.setting_theme_teal),
                            onReset = {
                                preferencesRepository.removeString("theme")
                                NetGuardPlatform.widgets.updateAll()
                            },
                        )
                    }
                }

            }

            // Firewall Section
            val firewallTitle = stringResource(Res.string.setting_section_firewall)
            CollapsibleSettingsSection(title = firewallTitle) {
                // Main allow/block toggles — uses the same FirewallTile as per-app detail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    FirewallTile(
                        allowedIcon = Icons.Default.Wifi,
                        blockedIcon = Icons.Default.WifiOff,
                        label = stringResource(Res.string.title_wifi),
                        allowed = !bool("whitelist_wifi", true),
                        onToggle = {
                            updateFlag(
                                "whitelist_wifi",
                                !bool("whitelist_wifi", true),
                                reload = true
                            )
                        },
                        shape = settingPairTileShape(
                            isLeadingTile = true,
                            isFirstRow = true,
                            isLastRow = false,
                            baseShape = MaterialTheme.shapes.small,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    FirewallTile(
                        allowedIcon = Icons.Default.PhoneAndroid,
                        blockedIcon = Icons.Default.MobileOff,
                        label = stringResource(Res.string.title_mobile),
                        allowed = !bool("whitelist_other", true),
                        onToggle = {
                            updateFlag(
                                "whitelist_other",
                                !bool("whitelist_other", true),
                                reload = true
                            )
                        },
                        shape = settingPairTileShape(
                            isLeadingTile = false,
                            isFirstRow = true,
                            isLastRow = false,
                            baseShape = MaterialTheme.shapes.small,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (bool("whitelist_wifi", true) != true ||
                    bool("whitelist_other", true) != true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (bool("whitelist_wifi", true) != true) {
                            SettingResetAction(
                                title = stringResource(Res.string.title_wifi),
                                defaultValue = stringResource(Res.string.title_block),
                                onReset = { resetFlag("whitelist_wifi", reload = true) },
                            )
                        }
                        if (bool("whitelist_other", true) != true) {
                            SettingResetAction(
                                title = stringResource(Res.string.title_mobile),
                                defaultValue = stringResource(Res.string.title_block),
                                onReset = { resetFlag("whitelist_other", reload = true) },
                            )
                        }
                    }
                }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_whitelist_roaming),
                    checked = bool("whitelist_roaming", true),
                    defaultChecked = true,
                    onReset = { resetFlag("whitelist_roaming", reload = true) },
                ) { updateFlag("whitelist_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_screen_on),
                    checked = bool("screen_on", true),
                    defaultChecked = true,
                    onReset = { resetFlag("screen_on", reload = true) },
                ) { updateFlag("screen_on", it, reload = true) }
                SettingTogglePairRow(
                    firstTitle = stringResource(Res.string.setting_screen_wifi),
                    firstChecked = bool("screen_wifi", false),
                    firstDefaultChecked = false,
                    onFirstCheckedChange = { updateFlag("screen_wifi", it, reload = true) },
                    onFirstReset = { resetFlag("screen_wifi", reload = true) },
                    secondTitle = stringResource(Res.string.setting_screen_other),
                    secondChecked = bool("screen_other", false),
                    secondDefaultChecked = false,
                    onSecondCheckedChange = { updateFlag("screen_other", it, reload = true) },
                    onSecondReset = { resetFlag("screen_other", reload = true) },
                )
                SettingToggleRow(
                    title = stringResource(Res.string.setting_subnet),
                    checked = bool("subnet", false),
                    defaultChecked = false,
                    onReset = { resetFlag("subnet", reload = true) },
                ) { updateFlag("subnet", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_tethering),
                    checked = bool("tethering", false),
                    defaultChecked = false,
                    onReset = { resetFlag("tethering", reload = true) },
                ) { updateFlag("tethering", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_lan),
                    checked = bool("lan", false),
                    defaultChecked = false,
                    onReset = { resetFlag("lan", reload = true) },
                ) { updateFlag("lan", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_ip6),
                    checked = bool("ip6", true),
                    defaultChecked = true,
                    onReset = { resetFlag("ip6", reload = true) },
                ) { updateFlag("ip6", it, reload = true) }

                SettingTextRow(
                    title = stringResource(Res.string.setting_delay, "0"),
                    value = str("screen_delay", "0"),
                    defaultValue = "0",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("screen_delay") },
                    onValueChange = { preferencesRepository.putString("screen_delay", it) },
                )
                SettingTextRow(
                    title = stringResource(Res.string.setting_auto, "0"),
                    value = str("auto_enable", "0"),
                    defaultValue = "0",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("auto_enable") },
                    onValueChange = { preferencesRepository.putString("auto_enable", it) },
                )

                val homes = strSet("wifi_homes", emptySet()).joinToString(",")
                SettingTextRow(
                    title = stringResource(Res.string.setting_wifi_home, homes.ifEmpty { "-" }),
                    value = homes,
                    defaultValue = "",
                    onReset = { preferencesRepository.removeStringSet("wifi_homes") },
                    onValueChange = { value ->
                        val set =
                            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        preferencesRepository.putStringSet("wifi_homes", set)
                    },
                )

                SettingToggleRow(
                    title = stringResource(Res.string.setting_metered),
                    checked = bool("use_metered", true),
                    defaultChecked = true,
                    onReset = { resetFlag("use_metered", reload = true) },
                ) { updateFlag("use_metered", it, reload = true) }
                SettingTogglePairRow(
                    firstTitle = stringResource(Res.string.setting_metered_2g),
                    firstChecked = bool("unmetered_2g", false),
                    firstDefaultChecked = false,
                    onFirstCheckedChange = { updateFlag("unmetered_2g", it, reload = true) },
                    onFirstReset = { resetFlag("unmetered_2g", reload = true) },
                    secondTitle = stringResource(Res.string.setting_metered_3g),
                    secondChecked = bool("unmetered_3g", false),
                    secondDefaultChecked = false,
                    onSecondCheckedChange = { updateFlag("unmetered_3g", it, reload = true) },
                    onSecondReset = { resetFlag("unmetered_3g", reload = true) },
                )
                SettingToggleRow(
                    title = stringResource(Res.string.setting_metered_4g),
                    checked = bool("unmetered_4g", false),
                    defaultChecked = false,
                    onReset = { resetFlag("unmetered_4g", reload = true) },
                ) { updateFlag("unmetered_4g", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_national_roaming),
                    checked = bool("national_roaming", false),
                    defaultChecked = false,
                    onReset = { resetFlag("national_roaming", reload = true) },
                ) { updateFlag("national_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_eu_roaming),
                    checked = bool("eu_roaming", false),
                    defaultChecked = false,
                    onReset = { resetFlag("eu_roaming", reload = true) },
                ) { updateFlag("eu_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_call),
                    checked = bool("disable_on_call", false),
                    defaultChecked = false,
                    isLast = true,
                    onReset = { resetFlag("disable_on_call", reload = true) },
                ) { updateFlag("disable_on_call", it, reload = true) }
            }

            // Advanced Section
            val advancedTitle = stringResource(Res.string.setting_section_advanced)
            CollapsibleSettingsSection(title = advancedTitle) {
                SettingToggleRow(
                    title = stringResource(Res.string.setting_system),
                    checked = bool("manage_system", false),
                    defaultChecked = false,
                    isFirst = true,
                    onReset = {
                        preferencesRepository.removeBoolean("manage_system")
                        preferencesRepository.removeBoolean("show_system")
                        NetGuardPlatform.firewall.reload("settings", false)
                    },
                ) { enabled ->
                    preferencesRepository.putBoolean("manage_system", enabled)
                    preferencesRepository.putBoolean("show_system", enabled)
                    NetGuardPlatform.firewall.reload("settings", false)
                }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_log_app),
                    checked = bool("log", false),
                    defaultChecked = false,
                    onReset = { resetFlag("log", reload = true) },
                ) { enabled ->
                    preferencesRepository.putBoolean("log", enabled)
                    NetGuardPlatform.firewall.reload("settings", false)
                }
                SettingTextRowWithTooltip(
                    title = stringResource(Res.string.setting_log_retention_days),
                    tooltip = stringResource(Res.string.summary_log_retention_days),
                    value = str("log_retention_days", "3"),
                    defaultValue = "3",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("log_retention_days") },
                ) { input ->
                    val numeric = input.filter(Char::isDigit).take(3)
                    val normalized = numeric.toIntOrNull()?.coerceIn(0, 365)?.toString() ?: numeric
                    preferencesRepository.putString("log_retention_days", normalized)
                }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_access),
                    checked = bool("notify_access", false),
                    defaultChecked = false,
                    onReset = { resetFlag("notify_access") },
                ) { updateFlag("notify_access", it) }
                SettingToggleRowWithTooltip(
                    title = stringResource(Res.string.setting_filter),
                    tooltip = stringResource(Res.string.tooltip_filter),
                    checked = bool("filter", false),
                    defaultChecked = false,
                    onReset = { resetFlag("filter", reload = true) },
                ) { updateFlag("filter", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_filter_udp),
                    checked = bool("filter_udp", false),
                    defaultChecked = false,
                    onReset = { resetFlag("filter_udp", reload = true) },
                ) { updateFlag("filter_udp", it, reload = true) }
                SettingToggleRowWithTooltip(
                    title = stringResource(Res.string.setting_lockdown),
                    tooltip = stringResource(Res.string.tooltip_lockdown),
                    checked = bool("lockdown", false),
                    defaultChecked = false,
                    onReset = {
                        resetFlag(
                            "lockdown",
                            reload = true,
                            updateWidgets = { NetGuardPlatform.widgets.updateLockdown() },
                        )
                    },
                ) {
                    updateFlag(
                        "lockdown",
                        it,
                        reload = true,
                        updateWidgets = { NetGuardPlatform.widgets.updateLockdown() },
                    )
                }
                SettingTogglePairRow(
                    firstTitle = stringResource(Res.string.setting_lockdown_wifi),
                    firstChecked = bool("lockdown_wifi", false),
                    firstDefaultChecked = false,
                    onFirstCheckedChange = { updateFlag("lockdown_wifi", it, reload = true) },
                    onFirstReset = { resetFlag("lockdown_wifi", reload = true) },
                    secondTitle = stringResource(Res.string.setting_lockdown_other),
                    secondChecked = bool("lockdown_other", false),
                    secondDefaultChecked = false,
                    onSecondCheckedChange = { updateFlag("lockdown_other", it, reload = true) },
                    onSecondReset = { resetFlag("lockdown_other", reload = true) },
                )
                SettingToggleRow(
                    title = stringResource(Res.string.setting_malware),
                    checked = bool("malware", false),
                    defaultChecked = false,
                    onReset = { resetFlag("malware", reload = true) },
                ) { updateFlag("malware", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_track_usage),
                    checked = bool("track_usage", false),
                    defaultChecked = false,
                    onReset = { resetFlag("track_usage", reload = true) },
                ) { updateFlag("track_usage", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_stats),
                    checked = bool("show_stats", false),
                    defaultChecked = false,
                    onReset = { resetFlag("show_stats", reloadStats = true) },
                ) { updateFlag("show_stats", it, reloadStats = true) }
                SettingToggleRow(
                    title = stringResource(Res.string.setting_stats_top),
                    checked = bool("show_top", false),
                    defaultChecked = false,
                    onReset = { resetFlag("show_top", reloadStats = true) },
                ) { updateFlag("show_top", it, reloadStats = true) }

                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_stats_frequency,
                        str("stats_frequency", "1000"),
                    ),
                    value = str("stats_frequency", "1000"),
                    defaultValue = "1000",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("stats_frequency") },
                    onValueChange = { preferencesRepository.putString("stats_frequency", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_stats_samples,
                        str("stats_samples", "10"),
                    ),
                    value = str("stats_samples", "10"),
                    defaultValue = "10",
                    keyboardType = KeyboardType.Number,
                    isLast = true,
                    onReset = { preferencesRepository.removeString("stats_samples") },
                    onValueChange = { preferencesRepository.putString("stats_samples", it) },
                )
            }

            // Hosts Section
            val hostsTitle = stringResource(Res.string.setting_section_hosts)
            CollapsibleSettingsSection(title = hostsTitle) {
                SettingToggleRow(
                    title = stringResource(Res.string.setting_use_hosts),
                    checked = bool("use_hosts", false),
                    defaultChecked = false,
                    isFirst = true,
                    onReset = { resetFlag("use_hosts", reload = true) },
                ) { updateFlag("use_hosts", it, reload = true) }

                SettingTextRow(
                    title = stringResource(Res.string.setting_hosts_url),
                    value = str("hosts_url", ""),
                    defaultValue = "",
                    isLast = true,
                    onReset = { preferencesRepository.removeString("hosts_url") },
                    onValueChange = { preferencesRepository.putString("hosts_url", it) },
                )

                // Single button with dropdown for hosts file operations
                Box {
                    var showHostsMenu by remember { mutableStateOf(false) }
                    FilledTonalButton(
                        onClick = { showHostsMenu = true },
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.setting_section_hosts))
                    }
                    DropdownMenu(
                        expanded = showHostsMenu,
                        onDismissRequest = { showHostsMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.setting_hosts)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showHostsMenu = false
                                viewModel.importHosts { showHostImportHint = it }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.setting_hosts_append)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showHostsMenu = false
                                viewModel.importHosts { showHostImportHint = it }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.setting_hosts_download)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showHostsMenu = false
                                NetGuardPlatform.hosts.startHostsDownload()
                            },
                        )
                    }
                }
            }

            // Network Section (DNS + Proxy + VPN)
            val networkSectionTitle = stringResource(Res.string.setting_section_network)
            val dnsTitle = stringResource(Res.string.setting_section_dns)
            val proxyTitle = stringResource(Res.string.setting_section_proxy)
            val vpnTitle = stringResource(Res.string.setting_section_vpn)
            CollapsibleSettingsSection(title = networkSectionTitle) {
                Text(
                    text = dnsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingTextRowWithTooltip(
                    title = stringResource(Res.string.setting_rcode, str("rcode", "3")),
                    tooltip = stringResource(Res.string.tooltip_rcode),
                    value = str("rcode", "3"),
                    defaultValue = "3",
                    keyboardType = KeyboardType.Number,
                    isFirst = true,
                    onReset = { preferencesRepository.removeString("rcode") },
                    onValueChange = { preferencesRepository.putString("rcode", it) },
                )
                SettingTextRowWithTooltip(
                    title = stringResource(Res.string.setting_ttl, str("ttl", "259200")),
                    tooltip = stringResource(Res.string.tooltip_ttl),
                    value = str("ttl", "259200"),
                    defaultValue = "259200",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("ttl") },
                    onValueChange = { preferencesRepository.putString("ttl", it) },
                )
                SettingTextRow(
                    title = stringResource(Res.string.setting_validate, str("validate", "")),
                    value = str("validate", ""),
                    defaultValue = "",
                    onReset = { preferencesRepository.removeString("validate") },
                    onValueChange = { preferencesRepository.putString("validate", it) },
                )
                FilledTonalButton(
                    onClick = onOpenDns,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(Res.string.setting_show_resolved))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.default))

                Text(
                    text = proxyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingToggleRow(
                    title = stringResource(Res.string.setting_socks5_enabled),
                    checked = bool("socks5_enabled", false),
                    defaultChecked = false,
                    onReset = { resetFlag("socks5_enabled") },
                ) { preferencesRepository.putBoolean("socks5_enabled", it) }
                SettingTextRow(
                    title = stringResource(Res.string.setting_socks5_addr, str("socks5_addr", "")),
                    value = str("socks5_addr", ""),
                    defaultValue = "",
                    onReset = { preferencesRepository.removeString("socks5_addr") },
                    onValueChange = { preferencesRepository.putString("socks5_addr", it) },
                )
                SettingTextRow(
                    title = stringResource(Res.string.setting_socks5_port, str("socks5_port", "0")),
                    value = str("socks5_port", "0"),
                    defaultValue = "0",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("socks5_port") },
                    onValueChange = { preferencesRepository.putString("socks5_port", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_socks5_username,
                        str("socks5_username", "")
                    ),
                    value = str("socks5_username", ""),
                    defaultValue = "",
                    onReset = { preferencesRepository.removeString("socks5_username") },
                    onValueChange = { preferencesRepository.putString("socks5_username", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_socks5_password,
                        str("socks5_password", "")
                    ),
                    value = str("socks5_password", ""),
                    defaultValue = "",
                    onReset = { preferencesRepository.removeString("socks5_password") },
                    onValueChange = { preferencesRepository.putString("socks5_password", it) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.default))

                Text(
                    text = vpnTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingTextRow(
                    title = stringResource(Res.string.setting_vpn4, str("vpn4", "10.1.10.1")),
                    value = str("vpn4", "10.1.10.1"),
                    defaultValue = "10.1.10.1",
                    onReset = { preferencesRepository.removeString("vpn4") },
                    onValueChange = { preferencesRepository.putString("vpn4", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_vpn6,
                        str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                    ),
                    value = str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                    defaultValue = "fd00:1:fd00:1:fd00:1:fd00:1",
                    onReset = { preferencesRepository.removeString("vpn6") },
                    onValueChange = { preferencesRepository.putString("vpn6", it) },
                )
                SettingTextRow(
                    title = stringResource(Res.string.setting_dns, str("dns", "")),
                    value = str("dns", ""),
                    defaultValue = "",
                    onReset = { preferencesRepository.removeString("dns") },
                    onValueChange = { preferencesRepository.putString("dns", it) },
                )
                SettingTextRow(
                    title = stringResource(Res.string.setting_dns2, str("dns2", "")),
                    value = str("dns2", ""),
                    defaultValue = "",
                    isLast = true,
                    onReset = { preferencesRepository.removeString("dns2") },
                    onValueChange = { preferencesRepository.putString("dns2", it) },
                )
            }

            // Forwarding Section
            val forwardingTitle = stringResource(Res.string.setting_forwarding)
            CollapsibleSettingsSection(title = forwardingTitle) {
                FilledTonalButton(onClick = onOpenForwarding) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(Res.string.setting_forwarding))
                }
            }

            // Background Section
            val backgroundTitle = stringResource(Res.string.setting_section_background)
            CollapsibleSettingsSection(title = backgroundTitle) {
                SettingTextRow(
                    title = stringResource(Res.string.setting_watchdog, str("watchdog", "0")),
                    value = str("watchdog", "0"),
                    defaultValue = "0",
                    keyboardType = KeyboardType.Number,
                    isFirst = true,
                    onReset = {
                        preferencesRepository.removeString("watchdog")
                        val enabled = preferencesRepository.getBoolean("enabled", false)
                        NetGuardPlatform.workScheduler.scheduleWatchdog(0, enabled)
                    },
                    onValueChange = { value ->
                        preferencesRepository.putString("watchdog", value)
                        val enabled = preferencesRepository.getBoolean("enabled", false)
                        NetGuardPlatform.workScheduler.scheduleWatchdog(value.toIntOrNull() ?: 0, enabled)
                    },
                )
                if (PlatformContext.isAndroid()) {
                    SettingToggleRow(
                        title = stringResource(Res.string.setting_update),
                        checked = bool("update_check", true),
                        defaultChecked = true,
                        onReset = { resetFlag("update_check") },
                    ) { preferencesRepository.putBoolean("update_check", it) }

                    FilledTonalButton(
                        onClick = {
                            updateCheckInProgress = true
                            updateCheckStatus = null
                            updateCheckVersion = null
                            NetGuardPlatform.firewall.checkForUpdateNow("settings")
                        },
                        enabled = !updateCheckInProgress,
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        if (updateCheckInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(text = stringResource(Res.string.setting_update_checking))
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(text = stringResource(Res.string.setting_update_now))
                        }
                    }

                    updateCheckStatus?.let { status ->
                        val message =
                            when (status) {
                                "available" ->
                                    if (!updateCheckVersion.isNullOrBlank()) {
                                        stringResource(
                                            Res.string.setting_update_result_available,
                                            updateCheckVersion!!,
                                        )
                                    } else {
                                        stringResource(Res.string.setting_update_result_available_unknown)
                                    }

                                "upToDate" -> stringResource(Res.string.setting_update_result_up_to_date)
                                "unavailable" -> stringResource(Res.string.setting_update_result_unavailable)
                                else -> stringResource(Res.string.setting_update_result_failed)
                            }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Diagnostics Section
            val diagnosticsTitle = stringResource(Res.string.setting_section_diagnostics)
            CollapsibleSettingsSection(title = diagnosticsTitle) {
                SettingToggleRowWithTooltip(
                    title = stringResource(Res.string.setting_pcap),
                    tooltip = stringResource(Res.string.tooltip_pcap),
                    checked = bool("pcap", false),
                    defaultChecked = false,
                    isFirst = true,
                    onReset = { resetFlag("pcap") },
                ) { preferencesRepository.putBoolean("pcap", it) }
                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_pcap_record_size,
                        str("pcap_record_size", "64")
                    ),
                    value = str("pcap_record_size", "64"),
                    defaultValue = "64",
                    keyboardType = KeyboardType.Number,
                    onReset = { preferencesRepository.removeString("pcap_record_size") },
                    onValueChange = { preferencesRepository.putString("pcap_record_size", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        Res.string.setting_pcap_file_size,
                        str("pcap_file_size", "2")
                    ),
                    value = str("pcap_file_size", "2"),
                    defaultValue = "2",
                    keyboardType = KeyboardType.Number,
                    isLast = true,
                    onReset = { preferencesRepository.removeString("pcap_file_size") },
                    onValueChange = { preferencesRepository.putString("pcap_file_size", it) },
                )
            }

            CollapsibleSettingsSection(title = stringResource(Res.string.menu_about)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(PROJECT_GITHUB_URL) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = "GitHub")
                    }
                }
            }
            }
        }
    } // end Scaffold

    if (showHostImportHint) {
        AlertDialog(
            onDismissRequest = { showHostImportHint = false },
            confirmButton = {
                TextButton(onClick = { showHostImportHint = false }) {
                    Text(text = stringResource(Res.string.menu_ok))
                }
            },
            title = { Text(text = stringResource(Res.string.setting_hosts)) },
            text = { Text(text = stringResource(Res.string.summary_block_domains)) },
        )
    }
}

/**
 * Animated theme swatch with springy selection, soft glow and shape morphing.
 */
@Composable
private fun ThemeSwatch(
    theme: String,
    seedColor: Color?,
    isSelected: Boolean,
    isEnabled: Boolean,
    dynamicColor: Color,
    onClick: () -> Unit,
) {
    val isDynamic = theme == "dynamic"
    val baseColor = seedColor ?: dynamicColor
    val displayColor = if (isEnabled) baseColor else baseColor.copy(alpha = 0.42f)
    val tokenSize = 50.dp
    val glowSize = 56.dp
    val orbSize = 40.dp
    val orbCornerFraction by animateFloatAsState(
        targetValue = if (isSelected) 0.5f else 0.26f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "orbCorner_$theme",
    )
    val orbShape = RoundedCornerShape(percent = (orbCornerFraction * 100).toInt())

    val orbScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 0.86f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 600f),
        label = "orbScale_$theme",
    )
    val orbRotation by animateFloatAsState(
        targetValue = if (isSelected) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 420f),
        label = "orbRotation_$theme",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "glow_$theme",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected || isDynamic) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "icon_$theme",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val overallAlpha = if (isEnabled) 1f else 0.52f

    Box(
        modifier = Modifier
            .size(tokenSize)
            .graphicsLayer { alpha = overallAlpha }
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                role = Role.RadioButton
                contentDescription =
                    "$theme theme: ${if (isSelected) "selected" else "not selected"}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(glowSize)
                .graphicsLayer {
                    alpha = glowAlpha
                }
                .drawBehind {
                    drawCircle(
                        color = displayColor.copy(alpha = 0.44f),
                        radius = size.minDimension * 0.5f,
                    )
                },
        )

        Box(
            modifier = Modifier
                .size(orbSize)
                .graphicsLayer {
                    scaleX = orbScale
                    scaleY = orbScale
                    rotationZ = orbRotation
                }
                .clip(orbShape)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(
                        bounded = true,
                        radius = 18.dp,
                        color = Color.White.copy(alpha = 0.32f),
                    ),
                )
                .background(displayColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(60f, 60f),
                        ),
                    ),
            )

            if (isSelected || isDynamic) {
                val iconSize = if (isSelected) 18.dp else 16.dp
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            alpha = iconAlpha
                            // Counter-rotate to keep glyph upright while orb spins.
                            rotationZ = -orbRotation
                        },
                    tint = Color.White.copy(alpha = if (isSelected) 1f else 0.9f),
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    framed: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.extraSmall),
        )
        if (framed) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(spacing.small),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    content = content,
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                content = content,
            )
        }
    }
}

@Composable
private fun settingItemShape(
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
private fun settingPairTileShape(
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
private fun SettingResetAction(
    title: String,
    defaultValue: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(TouchTargets.minimum),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(
                    Res.string.setting_reset_accessibility,
                    title,
                ),
                modifier = Modifier.size(18.dp),
                tint = tint,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(Res.string.setting_reset_to_default),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = stringResource(Res.string.setting_default_value, defaultValue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onReset()
                },
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    defaultChecked: Boolean? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onReset: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val rowShape =
        settingItemShape(isFirst = isFirst, isLast = isLast, baseShape = MaterialTheme.shapes.small)
    val haptic = LocalHapticFeedback.current
    val onToggle = { newValue: Boolean ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onCheckedChange(newValue)
    }
    val resetAction = onReset?.takeIf { defaultChecked != null && checked != defaultChecked }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = rowShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(rowShape)
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(horizontal = spacing.default, vertical = spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.small),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                resetAction?.let { reset ->
                    SettingResetAction(
                        title = title,
                        defaultValue = stringResource(
                            if (defaultChecked == true) {
                                Res.string.setting_value_on
                            } else {
                                Res.string.setting_value_off
                            },
                        ),
                        onReset = reset,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.semantics {
                        contentDescription = "$title: ${if (checked) "enabled" else "disabled"}"
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRowWithTooltip(
    title: String,
    tooltip: String,
    checked: Boolean,
    defaultChecked: Boolean? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onReset: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val rowShape =
        settingItemShape(isFirst = isFirst, isLast = isLast, baseShape = MaterialTheme.shapes.small)
    val haptic = LocalHapticFeedback.current
    var showTooltip by remember { mutableStateOf(false) }
    val onToggle = { newValue: Boolean ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onCheckedChange(newValue)
    }
    val resetAction = onReset?.takeIf { defaultChecked != null && checked != defaultChecked }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = rowShape,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(rowShape)
                    .toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onToggle,
                    )
                    .padding(horizontal = spacing.default, vertical = spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = spacing.small),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(TouchTargets.minimum),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(
                                Res.string.content_desc_show_info,
                                title,
                            ),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(bottom = 1.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    resetAction?.let { reset ->
                        SettingResetAction(
                            title = title,
                            defaultValue = stringResource(
                                if (defaultChecked == true) {
                                    Res.string.setting_value_on
                                } else {
                                    Res.string.setting_value_off
                                },
                            ),
                            onReset = reset,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = null,
                        modifier = Modifier.semantics {
                            contentDescription = "$title: ${if (checked) "enabled" else "disabled"}"
                        },
                    )
                }
            }
            ExpandableContent(expanded = showTooltip) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(
                        horizontal = spacing.small,
                        vertical = spacing.extraSmall
                    ),
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
}

@Composable
private fun SettingTextRow(
    title: String,
    value: String,
    defaultValue: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    val modifiedDefault = defaultValue?.takeIf { value != it && onReset != null }
    val resetAction = onReset?.takeIf { modifiedDefault != null }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = settingFieldLabel(title, value)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.large,
        singleLine = true,
        trailingIcon = if (modifiedDefault != null && resetAction != null) {
            {
                SettingResetAction(
                    title = settingFieldLabel(title, value),
                    defaultValue = modifiedDefault.ifEmpty {
                        stringResource(Res.string.setting_value_empty)
                    },
                    onReset = resetAction,
                )
            }
        } else {
            trailingIcon
        },
    )
}

private fun settingFieldLabel(title: String, value: String): String {
    val withoutValue =
        if (value.isNotEmpty() && title.endsWith(value)) title.dropLast(value.length) else title
    return withoutValue
        .replace("%s", "")
        .trim()
        .trimEnd(':')
        .trim()
}

@Composable
private fun SettingTextRowWithTooltip(
    title: String,
    tooltip: String,
    value: String,
    defaultValue: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onReset: (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var showTooltip by remember { mutableStateOf(false) }
    val modifiedDefault = defaultValue?.takeIf { value != it && onReset != null }
    val resetAction = onReset?.takeIf { modifiedDefault != null }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = settingFieldLabel(title, value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showTooltip = !showTooltip },
                modifier = Modifier.size(TouchTargets.minimum),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(Res.string.content_desc_show_info, title),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ExpandableContent(expanded = showTooltip) {
            Text(
                text = tooltip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.small),
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            trailingIcon = if (modifiedDefault != null && resetAction != null) {
                {
                    SettingResetAction(
                        title = settingFieldLabel(title, value),
                        defaultValue = modifiedDefault.ifEmpty {
                            stringResource(Res.string.setting_value_empty)
                        },
                        onReset = resetAction,
                    )
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun SettingTogglePairRow(
    firstTitle: String,
    firstChecked: Boolean,
    firstDefaultChecked: Boolean? = null,
    onFirstCheckedChange: (Boolean) -> Unit,
    onFirstReset: (() -> Unit)? = null,
    secondTitle: String,
    secondChecked: Boolean,
    secondDefaultChecked: Boolean? = null,
    onSecondCheckedChange: (Boolean) -> Unit,
    onSecondReset: (() -> Unit)? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        CompactSettingToggleTile(
            modifier = Modifier.weight(1f),
            title = firstTitle,
            checked = firstChecked,
            defaultChecked = firstDefaultChecked,
            onCheckedChange = onFirstCheckedChange,
            onReset = onFirstReset,
            shape = settingPairTileShape(
                isLeadingTile = true,
                isFirstRow = isFirst,
                isLastRow = isLast,
                baseShape = MaterialTheme.shapes.small,
            ),
        )
        CompactSettingToggleTile(
            modifier = Modifier.weight(1f),
            title = secondTitle,
            checked = secondChecked,
            defaultChecked = secondDefaultChecked,
            onCheckedChange = onSecondCheckedChange,
            onReset = onSecondReset,
            shape = settingPairTileShape(
                isLeadingTile = false,
                isFirstRow = isFirst,
                isLastRow = isLast,
                baseShape = MaterialTheme.shapes.small,
            ),
        )
    }
}

@Composable
private fun CompactSettingToggleTile(
    title: String,
    checked: Boolean,
    defaultChecked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit,
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
) {
    val tileShape = shape ?: MaterialTheme.shapes.small
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val resetAction = onReset?.takeIf { defaultChecked != null && checked != defaultChecked }
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheckedChange(!checked)
        },
        modifier = modifier.heightIn(min = 72.dp),
        shape = tileShape,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.default),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.heightIn(min = spacing.small))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                resetAction?.let { reset ->
                    SettingResetAction(
                        title = title,
                        defaultValue = stringResource(
                            if (defaultChecked == true) {
                                Res.string.setting_value_on
                            } else {
                                Res.string.setting_value_off
                            },
                        ),
                        onReset = reset,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                )
            }
        }
    }
}
