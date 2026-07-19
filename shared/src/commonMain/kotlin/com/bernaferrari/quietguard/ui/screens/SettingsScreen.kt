package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols

import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.app_description
import com.bernaferrari.quietguard.generated.resources.app_name
import com.bernaferrari.quietguard.generated.resources.action_back
import com.bernaferrari.quietguard.generated.resources.content_desc_show_info
import com.bernaferrari.quietguard.generated.resources.menu_about
import com.bernaferrari.quietguard.generated.resources.menu_ok
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
import com.bernaferrari.quietguard.generated.resources.title_wifi
import com.bernaferrari.quietguard.generated.resources.tooltip_filter
import com.bernaferrari.quietguard.generated.resources.tooltip_lockdown
import com.bernaferrari.quietguard.generated.resources.tooltip_pcap
import com.bernaferrari.quietguard.generated.resources.tooltip_rcode
import com.bernaferrari.quietguard.generated.resources.tooltip_ttl
import com.bernaferrari.quietguard.generated.resources.ui_settings_title
import com.bernaferrari.quietguard.generated.resources.settings_quick_controls
import com.bernaferrari.quietguard.generated.resources.settings_more
import com.bernaferrari.quietguard.generated.resources.settings_firewall_summary
import com.bernaferrari.quietguard.generated.resources.settings_advanced_summary
import com.bernaferrari.quietguard.generated.resources.settings_hosts_summary
import com.bernaferrari.quietguard.generated.resources.settings_network_summary
import com.bernaferrari.quietguard.generated.resources.settings_background_summary
import com.bernaferrari.quietguard.generated.resources.settings_diagnostics_summary
import com.bernaferrari.quietguard.generated.resources.settings_about_summary
import com.bernaferrari.quietguard.generated.resources.about_tagline
import com.bernaferrari.quietguard.generated.resources.about_source_title
import com.bernaferrari.quietguard.generated.resources.about_source_summary
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.PlatformContext
import com.bernaferrari.quietguard.ui.screens.vm.SettingsViewModel
import com.bernaferrari.quietguard.ui.SettingsSection
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

import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon

private const val PROJECT_GITHUB_URL = "https://github.com/bernaferrari/QuietGuard"

// M3 Expressive grouped-list corners: large radius on group edges, small between items.
private val GroupOuterCorner = 24.dp
private val GroupInnerCorner = 6.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    section: SettingsSection? = null,
    onBack: () -> Unit = {},
    onOpenSection: (SettingsSection) -> Unit = {},
    onOpenDns: () -> Unit,
    onOpenForwarding: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val viewModel: SettingsViewModel = koinViewModel()
    val preferencesRepository = viewModel.preferencesRepository
    val prefs by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

    val screenTitle =
        when (section) {
            null -> stringResource(Res.string.ui_settings_title)
            SettingsSection.Firewall -> stringResource(Res.string.setting_section_firewall)
            SettingsSection.Advanced -> stringResource(Res.string.setting_section_advanced)
            SettingsSection.Hosts -> stringResource(Res.string.setting_section_hosts)
            SettingsSection.Network -> stringResource(Res.string.setting_section_network)
            SettingsSection.Background -> stringResource(Res.string.setting_section_background)
            SettingsSection.Diagnostics -> stringResource(Res.string.setting_section_diagnostics)
            SettingsSection.About -> stringResource(Res.string.menu_about)
        }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (section == null) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                MediumFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                icon = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back),
                            )
                        }
                    },
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.large, vertical = spacing.default),
            verticalArrangement = Arrangement.spacedBy(spacing.extraLarge),
        ) {

            if (section == null) {
                // Appearance
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    SettingsSubheader(stringResource(Res.string.setting_section_appearance))
                    val currentTheme = str("theme", "teal")
                    val dynamicSwatchColor = Teal500
                    val modeOptions = listOf(
                        Triple(
                            "light",
                            stringResource(Res.string.setting_appearance_light),
                            Pair(MaterialSymbols.Filled.LightMode, MaterialSymbols.Outlined.LightMode),
                        ),
                        Triple(
                            "dark",
                            stringResource(Res.string.setting_appearance_dark),
                            Pair(MaterialSymbols.Filled.DarkMode, MaterialSymbols.Outlined.DarkMode),
                        ),
                        Triple(
                            "auto",
                            stringResource(Res.string.setting_appearance_auto),
                            Pair(
                                MaterialSymbols.Filled.BrightnessAuto,
                                MaterialSymbols.Outlined.BrightnessAuto,
                            ),
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
                                    icon = if (selected) MaterialSymbols.Filled.Check else icons.second,
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
                    }
                }

                QuickFirewallControls(
                    wifiAllowed = !bool("whitelist_wifi", true),
                    mobileAllowed = !bool("whitelist_other", true),
                    onToggleWifi = {
                        updateFlag(
                            "whitelist_wifi",
                            !bool("whitelist_wifi", true),
                            reload = true,
                        )
                    },
                    onToggleMobile = {
                        updateFlag(
                            "whitelist_other",
                            !bool("whitelist_other", true),
                            reload = true,
                        )
                    },
                )

                SettingsDirectory(onOpenSection = onOpenSection)
            }

            // Firewall Section
            if (section == SettingsSection.Firewall) {
                SettingsGroup {
                    item { first, last ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            FirewallTile(
                                allowedIcon = MaterialSymbols.Filled.Wifi,
                                blockedIcon = MaterialSymbols.Filled.WifiOff,
                                label = stringResource(Res.string.title_wifi),
                                allowed = !bool("whitelist_wifi", true),
                                onToggle = {
                                    updateFlag(
                                        "whitelist_wifi",
                                        !bool("whitelist_wifi", true),
                                        reload = true,
                                    )
                                },
                                shape = settingPairTileShape(
                                    isLeadingTile = true,
                                    isFirst = first,
                                    isLast = last,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            FirewallTile(
                                allowedIcon = MaterialSymbols.Filled.PhoneAndroid,
                                blockedIcon = MaterialSymbols.Filled.MobileOff,
                                label = stringResource(Res.string.title_mobile),
                                allowed = !bool("whitelist_other", true),
                                onToggle = {
                                    updateFlag(
                                        "whitelist_other",
                                        !bool("whitelist_other", true),
                                        reload = true,
                                    )
                                },
                                shape = settingPairTileShape(
                                    isLeadingTile = false,
                                    isFirst = first,
                                    isLast = last,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_whitelist_roaming),
                            checked = bool("whitelist_roaming", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("whitelist_roaming", it, reload = true) }
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_screen_on),
                            checked = bool("screen_on", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("screen_on", it, reload = true) }
                    }
                    item { first, last ->
                        SettingTogglePairRow(
                            firstTitle = stringResource(Res.string.setting_screen_wifi),
                            firstChecked = bool("screen_wifi", false),
                            onFirstCheckedChange = { updateFlag("screen_wifi", it, reload = true) },
                            secondTitle = stringResource(Res.string.setting_screen_other),
                            secondChecked = bool("screen_other", false),
                            onSecondCheckedChange = { updateFlag("screen_other", it, reload = true) },
                            isFirst = first,
                            isLast = last,
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_delay, "0"),
                            value = str("screen_delay", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("screen_delay", it) },
                        )
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_subnet),
                            checked = bool("subnet", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("subnet", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_tethering),
                            checked = bool("tethering", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("tethering", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_lan),
                            checked = bool("lan", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("lan", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_ip6),
                            checked = bool("ip6", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("ip6", it, reload = true) }
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_auto, "0"),
                            value = str("auto_enable", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("auto_enable", it) },
                        )
                    }
                    item { first, last ->
                        val homes = strSet("wifi_homes", emptySet()).joinToString(",")
                        SettingTextRow(
                            title = stringResource(Res.string.setting_wifi_home, homes.ifEmpty { "-" }),
                            value = homes,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { value ->
                                val set =
                                    value.split(",").map { it.trim() }
                                        .filter { it.isNotEmpty() }.toSet()
                                preferencesRepository.putStringSet("wifi_homes", set)
                            },
                        )
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_metered),
                            checked = bool("use_metered", true),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("use_metered", it, reload = true) }
                    }
                    item { first, last ->
                        SettingTogglePairRow(
                            firstTitle = stringResource(Res.string.setting_metered_2g),
                            firstChecked = bool("unmetered_2g", false),
                            onFirstCheckedChange = { updateFlag("unmetered_2g", it, reload = true) },
                            secondTitle = stringResource(Res.string.setting_metered_3g),
                            secondChecked = bool("unmetered_3g", false),
                            onSecondCheckedChange = { updateFlag("unmetered_3g", it, reload = true) },
                            isFirst = first,
                            isLast = last,
                        )
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_metered_4g),
                            checked = bool("unmetered_4g", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("unmetered_4g", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_national_roaming),
                            checked = bool("national_roaming", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("national_roaming", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_eu_roaming),
                            checked = bool("eu_roaming", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("eu_roaming", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_call),
                            checked = bool("disable_on_call", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("disable_on_call", it, reload = true) }
                    }
                }
            }

            // Advanced Section
            if (section == SettingsSection.Advanced) {
                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_system),
                            checked = bool("manage_system", false),
                            isFirst = first,
                            isLast = last,
                        ) { enabled ->
                            preferencesRepository.putBoolean("manage_system", enabled)
                            preferencesRepository.putBoolean("show_system", enabled)
                            NetGuardPlatform.firewall.reload("settings", false)
                        }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_log_app),
                            checked = bool("log", false),
                            isFirst = first,
                            isLast = last,
                        ) { enabled ->
                            preferencesRepository.putBoolean("log", enabled)
                            NetGuardPlatform.firewall.reload("settings", false)
                        }
                    }
                    item { first, last ->
                        SettingTextRowWithTooltip(
                            title = stringResource(Res.string.setting_log_retention_days),
                            tooltip = stringResource(Res.string.summary_log_retention_days),
                            value = str("log_retention_days", "3"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                        ) { input ->
                            val numeric = input.filter(Char::isDigit).take(3)
                            val normalized =
                                numeric.toIntOrNull()?.coerceIn(0, 365)?.toString() ?: numeric
                            preferencesRepository.putString("log_retention_days", normalized)
                        }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_access),
                            checked = bool("notify_access", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("notify_access", it) }
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingToggleRowWithTooltip(
                            title = stringResource(Res.string.setting_filter),
                            tooltip = stringResource(Res.string.tooltip_filter),
                            checked = bool("filter", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("filter", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_filter_udp),
                            checked = bool("filter_udp", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("filter_udp", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRowWithTooltip(
                            title = stringResource(Res.string.setting_lockdown),
                            tooltip = stringResource(Res.string.tooltip_lockdown),
                            checked = bool("lockdown", false),
                            isFirst = first,
                            isLast = last,
                        ) {
                            updateFlag(
                                "lockdown",
                                it,
                                reload = true,
                                updateWidgets = { NetGuardPlatform.widgets.updateLockdown() },
                            )
                        }
                    }
                    item { first, last ->
                        SettingTogglePairRow(
                            firstTitle = stringResource(Res.string.setting_lockdown_wifi),
                            firstChecked = bool("lockdown_wifi", false),
                            onFirstCheckedChange = { updateFlag("lockdown_wifi", it, reload = true) },
                            secondTitle = stringResource(Res.string.setting_lockdown_other),
                            secondChecked = bool("lockdown_other", false),
                            onSecondCheckedChange = {
                                updateFlag("lockdown_other", it, reload = true)
                            },
                            isFirst = first,
                            isLast = last,
                        )
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_malware),
                            checked = bool("malware", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("malware", it, reload = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_track_usage),
                            checked = bool("track_usage", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("track_usage", it, reload = true) }
                    }
                }

                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_stats),
                            checked = bool("show_stats", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("show_stats", it, reloadStats = true) }
                    }
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_stats_top),
                            checked = bool("show_top", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("show_top", it, reloadStats = true) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_stats_frequency,
                                str("stats_frequency", "1000"),
                            ),
                            value = str("stats_frequency", "1000"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                preferencesRepository.putString("stats_frequency", it)
                            },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_stats_samples,
                                str("stats_samples", "10"),
                            ),
                            value = str("stats_samples", "10"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("stats_samples", it) },
                        )
                    }
                }
            }

            // Hosts Section
            if (section == SettingsSection.Hosts) {
                SettingsGroup {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_use_hosts),
                            checked = bool("use_hosts", false),
                            isFirst = first,
                            isLast = last,
                        ) { updateFlag("use_hosts", it, reload = true) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_hosts_url),
                            value = str("hosts_url", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("hosts_url", it) },
                        )
                    }
                }

                // Single button with dropdown for hosts file operations
                Box {
                    var showHostsMenu by remember { mutableStateOf(false) }
                    FilledTonalButton(
                        onClick = { showHostsMenu = true },
                    ) {
                        Icon(icon = MaterialSymbols.Filled.Download, contentDescription = null)
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
                                    MaterialSymbols.Filled.Download,
                                    contentDescription = null,
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
                                    MaterialSymbols.Filled.Download,
                                    contentDescription = null,
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
                                    MaterialSymbols.Filled.Download,
                                    contentDescription = null,
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
            if (section == SettingsSection.Network) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    SettingsGroup(title = stringResource(Res.string.setting_section_dns)) {
                        item { first, last ->
                            SettingTextRowWithTooltip(
                                title = stringResource(Res.string.setting_rcode, str("rcode", "3")),
                                tooltip = stringResource(Res.string.tooltip_rcode),
                                value = str("rcode", "3"),
                                keyboardType = KeyboardType.Number,
                                isFirst = first,
                                isLast = last,
                            ) { preferencesRepository.putString("rcode", it) }
                        }
                        item { first, last ->
                            SettingTextRowWithTooltip(
                                title = stringResource(Res.string.setting_ttl, str("ttl", "259200")),
                                tooltip = stringResource(Res.string.tooltip_ttl),
                                value = str("ttl", "259200"),
                                keyboardType = KeyboardType.Number,
                                isFirst = first,
                                isLast = last,
                            ) { preferencesRepository.putString("ttl", it) }
                        }
                        item { first, last ->
                            SettingTextRow(
                                title = stringResource(Res.string.setting_validate, str("validate", "")),
                                value = str("validate", ""),
                                isFirst = first,
                                isLast = last,
                                onValueChange = { preferencesRepository.putString("validate", it) },
                            )
                        }
                    }
                    FilledTonalButton(onClick = onOpenDns) {
                        Icon(icon = MaterialSymbols.Filled.Dns, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.setting_show_resolved))
                    }
                }

                SettingsGroup(title = stringResource(Res.string.setting_section_proxy)) {
                    item { first, last ->
                        SettingToggleRow(
                            title = stringResource(Res.string.setting_socks5_enabled),
                            checked = bool("socks5_enabled", false),
                            isFirst = first,
                            isLast = last,
                        ) { preferencesRepository.putBoolean("socks5_enabled", it) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_socks5_addr,
                                str("socks5_addr", ""),
                            ),
                            value = str("socks5_addr", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("socks5_addr", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_socks5_port,
                                str("socks5_port", "0"),
                            ),
                            value = str("socks5_port", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("socks5_port", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_socks5_username,
                                str("socks5_username", ""),
                            ),
                            value = str("socks5_username", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                preferencesRepository.putString("socks5_username", it)
                            },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_socks5_password,
                                str("socks5_password", ""),
                            ),
                            value = str("socks5_password", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                preferencesRepository.putString("socks5_password", it)
                            },
                        )
                    }
                }

                SettingsGroup(title = stringResource(Res.string.setting_section_vpn)) {
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_vpn4, str("vpn4", "10.1.10.1")),
                            value = str("vpn4", "10.1.10.1"),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("vpn4", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_vpn6,
                                str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                            ),
                            value = str("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("vpn6", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_dns, str("dns", "")),
                            value = str("dns", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("dns", it) },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_dns2, str("dns2", "")),
                            value = str("dns2", ""),
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("dns2", it) },
                        )
                    }
                }

                // Forwarding
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    SettingsSubheader(stringResource(Res.string.setting_forwarding))
                    FilledTonalButton(onClick = onOpenForwarding) {
                        Icon(
                            icon = MaterialSymbols.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(Res.string.setting_forwarding))
                    }
                }
            }

            // Background Section
            if (section == SettingsSection.Background) {
                SettingsGroup {
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(Res.string.setting_watchdog, str("watchdog", "0")),
                            value = str("watchdog", "0"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { value ->
                                preferencesRepository.putString("watchdog", value)
                                val enabled = preferencesRepository.getBoolean("enabled", false)
                                NetGuardPlatform.workScheduler.scheduleWatchdog(
                                    value.toIntOrNull() ?: 0,
                                    enabled,
                                )
                            },
                        )
                    }
                    if (PlatformContext.isAndroid()) {
                        item { first, last ->
                            SettingToggleRow(
                                title = stringResource(Res.string.setting_update),
                                checked = bool("update_check", true),
                                isFirst = first,
                                isLast = last,
                            ) { preferencesRepository.putBoolean("update_check", it) }
                        }
                    }
                }

                if (PlatformContext.isAndroid()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                        FilledTonalButton(
                            onClick = {
                                updateCheckInProgress = true
                                updateCheckStatus = null
                                updateCheckVersion = null
                                NetGuardPlatform.firewall.checkForUpdateNow("settings")
                            },
                            enabled = !updateCheckInProgress,
                        ) {
                            if (updateCheckInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text(text = stringResource(Res.string.setting_update_checking))
                            } else {
                                Icon(icon = MaterialSymbols.Filled.Refresh, contentDescription = null)
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
                                            stringResource(
                                                Res.string.setting_update_result_available_unknown,
                                            )
                                        }

                                    "upToDate" ->
                                        stringResource(Res.string.setting_update_result_up_to_date)

                                    "unavailable" ->
                                        stringResource(Res.string.setting_update_result_unavailable)

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
            }

            // Diagnostics Section
            if (section == SettingsSection.Diagnostics) {
                SettingsGroup {
                    item { first, last ->
                        SettingToggleRowWithTooltip(
                            title = stringResource(Res.string.setting_pcap),
                            tooltip = stringResource(Res.string.tooltip_pcap),
                            checked = bool("pcap", false),
                            isFirst = first,
                            isLast = last,
                        ) { preferencesRepository.putBoolean("pcap", it) }
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_pcap_record_size,
                                str("pcap_record_size", "64"),
                            ),
                            value = str("pcap_record_size", "64"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = {
                                preferencesRepository.putString("pcap_record_size", it)
                            },
                        )
                    }
                    item { first, last ->
                        SettingTextRow(
                            title = stringResource(
                                Res.string.setting_pcap_file_size,
                                str("pcap_file_size", "2"),
                            ),
                            value = str("pcap_file_size", "2"),
                            keyboardType = KeyboardType.Number,
                            isFirst = first,
                            isLast = last,
                            onValueChange = { preferencesRepository.putString("pcap_file_size", it) },
                        )
                    }
                }
            }

            if (section == SettingsSection.About) {
                AboutContent(onOpenGitHub = { uriHandler.openUri(PROJECT_GITHUB_URL) })
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
 * M3 Expressive grouped list: items are collected and rendered as individual filled
 * surfaces separated by 2dp gaps; the first and last items get large outer corners.
 */
private class SettingsGroupScope {
    val items = mutableListOf<@Composable (isFirst: Boolean, isLast: Boolean) -> Unit>()

    fun item(content: @Composable (isFirst: Boolean, isLast: Boolean) -> Unit) {
        items += content
    }
}

@Composable
private fun SettingsGroup(
    title: String? = null,
    builder: SettingsGroupScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val scope = SettingsGroupScope().apply(builder)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        title?.let { SettingsSubheader(it) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            scope.items.forEachIndexed { index, item ->
                item(index == 0, index == scope.items.lastIndex)
            }
        }
    }
}

@Composable
private fun SettingsSubheader(title: String) {
    val spacing = MaterialTheme.spacing
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = spacing.default),
    )
}

private fun settingItemShape(isFirst: Boolean, isLast: Boolean): RoundedCornerShape =
    RoundedCornerShape(
        topStart = if (isFirst) GroupOuterCorner else GroupInnerCorner,
        topEnd = if (isFirst) GroupOuterCorner else GroupInnerCorner,
        bottomEnd = if (isLast) GroupOuterCorner else GroupInnerCorner,
        bottomStart = if (isLast) GroupOuterCorner else GroupInnerCorner,
    )

private fun settingPairTileShape(
    isLeadingTile: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
): RoundedCornerShape =
    RoundedCornerShape(
        topStart = if (isLeadingTile && isFirst) GroupOuterCorner else GroupInnerCorner,
        topEnd = if (!isLeadingTile && isFirst) GroupOuterCorner else GroupInnerCorner,
        bottomEnd = if (!isLeadingTile && isLast) GroupOuterCorner else GroupInnerCorner,
        bottomStart = if (isLeadingTile && isLast) GroupOuterCorner else GroupInnerCorner,
    )

@Composable
private fun QuickFirewallControls(
    wifiAllowed: Boolean,
    mobileAllowed: Boolean,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SettingsSubheader(stringResource(Res.string.settings_quick_controls))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FirewallTile(
                allowedIcon = MaterialSymbols.Filled.Wifi,
                blockedIcon = MaterialSymbols.Filled.WifiOff,
                label = stringResource(Res.string.title_wifi),
                allowed = wifiAllowed,
                onToggle = onToggleWifi,
                shape = settingPairTileShape(
                    isLeadingTile = true,
                    isFirst = true,
                    isLast = true,
                ),
                modifier = Modifier.weight(1f),
            )
            FirewallTile(
                allowedIcon = MaterialSymbols.Filled.PhoneAndroid,
                blockedIcon = MaterialSymbols.Filled.MobileOff,
                label = stringResource(Res.string.title_mobile),
                allowed = mobileAllowed,
                onToggle = onToggleMobile,
                shape = settingPairTileShape(
                    isLeadingTile = false,
                    isFirst = true,
                    isLast = true,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingsDirectory(onOpenSection: (SettingsSection) -> Unit) {
    SettingsGroup(title = stringResource(Res.string.settings_more)) {
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.setting_section_firewall),
                subtitle = stringResource(Res.string.settings_firewall_summary),
                icon = MaterialSymbols.Outlined.Shield,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.Firewall) },
            )
        }
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.setting_section_advanced),
                subtitle = stringResource(Res.string.settings_advanced_summary),
                icon = MaterialSymbols.Filled.Tune,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.Advanced) },
            )
        }
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.setting_section_hosts),
                subtitle = stringResource(Res.string.settings_hosts_summary),
                icon = MaterialSymbols.Filled.Download,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.Hosts) },
            )
        }
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.setting_section_network),
                subtitle = stringResource(Res.string.settings_network_summary),
                icon = MaterialSymbols.Filled.Public,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.Network) },
            )
        }
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.setting_section_background),
                subtitle = stringResource(Res.string.settings_background_summary),
                icon = MaterialSymbols.Filled.Refresh,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.Background) },
            )
        }
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.setting_section_diagnostics),
                subtitle = stringResource(Res.string.settings_diagnostics_summary),
                icon = MaterialSymbols.Outlined.Info,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.Diagnostics) },
            )
        }
        item { first, last ->
            SettingsDestinationRow(
                title = stringResource(Res.string.menu_about),
                subtitle = stringResource(Res.string.settings_about_summary),
                icon = MaterialSymbols.Filled.Code,
                shape = settingItemShape(first, last),
                onClick = { onOpenSection(SettingsSection.About) },
            )
        }
    }
}

@Composable
private fun SettingsDestinationRow(
    title: String,
    subtitle: String,
    icon: MaterialIcon,
    trailingIcon: MaterialIcon = MaterialSymbols.Filled.ChevronRight,
    shape: Shape = RoundedCornerShape(GroupOuterCorner),
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(
                start = spacing.default,
                top = spacing.medium,
                end = spacing.small,
                bottom = spacing.medium,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon = icon, contentDescription = null)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                icon = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutContent(onOpenGitHub: () -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = spacing.extraLarge,
                    vertical = spacing.xxLarge,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon = MaterialSymbols.Filled.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.about_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = stringResource(Res.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
        )

        SettingsDestinationRow(
            title = stringResource(Res.string.about_source_title),
            subtitle = stringResource(Res.string.about_source_summary),
            icon = MaterialSymbols.Filled.Code,
            trailingIcon = MaterialSymbols.AutoMirrored.Filled.OpenInNew,
            onClick = onOpenGitHub,
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
                    icon = if (isSelected) MaterialSymbols.Filled.Check else MaterialSymbols.Filled.Palette,
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
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val localizedState = stringResource(
        if (checked) Res.string.setting_value_on else Res.string.setting_value_off,
    )
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val onToggle = { newValue: Boolean ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onCheckedChange(newValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = settingItemShape(isFirst, isLast),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(horizontal = spacing.large, vertical = spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.default),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
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
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.semantics {
                    contentDescription = "$title: $localizedState"
                },
            )
        }
    }
}

@Composable
private fun SettingToggleRowWithTooltip(
    title: String,
    tooltip: String,
    checked: Boolean,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val localizedState = stringResource(
        if (checked) Res.string.setting_value_on else Res.string.setting_value_off,
    )
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    var showTooltip by remember { mutableStateOf(false) }
    val onToggle = { newValue: Boolean ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onCheckedChange(newValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = settingItemShape(isFirst, isLast),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onToggle,
                    )
                    .padding(horizontal = spacing.large, vertical = spacing.small),
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(TouchTargets.minimum),
                    ) {
                        Icon(
                            icon = MaterialSymbols.Outlined.Info,
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
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.semantics {
                        contentDescription = "$title: $localizedState"
                    },
                )
            }
            ExpandableContent(expanded = showTooltip) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = spacing.large,
                        end = spacing.large,
                        bottom = spacing.default,
                    ),
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
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = settingFieldLabel(title, value)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = settingItemShape(isFirst, isLast),
        singleLine = true,
        colors = settingTextFieldColors(),
    )
}

@Composable
private fun settingTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

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
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var showTooltip by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = settingItemShape(isFirst, isLast),
    ) {
        Column {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(text = settingFieldLabel(title, value)) },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = settingTextFieldColors(),
                trailingIcon = {
                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(TouchTargets.minimum),
                    ) {
                        Icon(
                            icon = MaterialSymbols.Outlined.Info,
                            contentDescription = stringResource(
                                Res.string.content_desc_show_info,
                                title,
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            ExpandableContent(expanded = showTooltip) {
                Text(
                    text = tooltip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = spacing.default,
                        end = spacing.default,
                        top = spacing.extraSmall,
                        bottom = spacing.medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SettingTogglePairRow(
    firstTitle: String,
    firstChecked: Boolean,
    onFirstCheckedChange: (Boolean) -> Unit,
    secondTitle: String,
    secondChecked: Boolean,
    onSecondCheckedChange: (Boolean) -> Unit,
    isFirst: Boolean = true,
    isLast: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CompactSettingToggleTile(
            modifier = Modifier.weight(1f),
            title = firstTitle,
            checked = firstChecked,
            onCheckedChange = onFirstCheckedChange,
            shape = settingPairTileShape(
                isLeadingTile = true,
                isFirst = isFirst,
                isLast = isLast,
            ),
        )
        CompactSettingToggleTile(
            modifier = Modifier.weight(1f),
            title = secondTitle,
            checked = secondChecked,
            onCheckedChange = onSecondCheckedChange,
            shape = settingPairTileShape(
                isLeadingTile = false,
                isFirst = isFirst,
                isLast = isLast,
            ),
        )
    }
}

@Composable
private fun CompactSettingToggleTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheckedChange(!checked)
        },
        modifier = modifier.heightIn(min = 72.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.default),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.heightIn(min = spacing.small))
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
