package com.bernaferari.renetguard.ui.screens

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.bernaferari.renetguard.R
import com.bernaferari.renetguard.ServiceExternal
import com.bernaferari.renetguard.ServiceSinkhole
import com.bernaferari.renetguard.Widgets
import com.bernaferari.renetguard.WorkScheduler
import com.bernaferari.renetguard.data.Prefs
import com.bernaferari.renetguard.ui.components.ExpandableContent
import com.bernaferari.renetguard.ui.components.FirewallTile
import com.bernaferari.renetguard.ui.theme.AmberPrimary
import com.bernaferari.renetguard.ui.theme.BluePrimary
import com.bernaferari.renetguard.ui.theme.CyanPrimary
import com.bernaferari.renetguard.ui.theme.GreenPrimary
import com.bernaferari.renetguard.ui.theme.IndigoPrimary
import com.bernaferari.renetguard.ui.theme.LimePrimary
import com.bernaferari.renetguard.ui.theme.OrangePrimary
import com.bernaferari.renetguard.ui.theme.PinkPrimary
import com.bernaferari.renetguard.ui.theme.PurplePrimary
import com.bernaferari.renetguard.ui.theme.Teal500
import com.bernaferari.renetguard.ui.theme.TouchTargets
import com.bernaferari.renetguard.ui.theme.spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    var updateCheckInProgress by remember { mutableStateOf(false) }
    var updateCheckStatus by remember { mutableStateOf<String?>(null) }
    var updateCheckVersion by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: android.content.Context?, intent: Intent?) {
                    if (intent?.action != ServiceSinkhole.ACTION_UPDATE_CHECK_RESULT) return

                    updateCheckInProgress = false

                    val status = intent.getStringExtra(ServiceSinkhole.EXTRA_UPDATE_CHECK_STATUS)
                    val version = intent.getStringExtra(ServiceSinkhole.EXTRA_UPDATE_CHECK_VERSION)
                    updateCheckStatus = status
                    updateCheckVersion = version
                }
            }

        val filter = IntentFilter(ServiceSinkhole.ACTION_UPDATE_CHECK_RESULT)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

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
        Prefs.putString("appearance", mode)
        when (mode) {
            "auto" -> Prefs.remove("dark_theme")
            "dark" -> Prefs.putBoolean("dark_theme", true)
            "light" -> Prefs.putBoolean("dark_theme", false)
        }
        Widgets.updateAll(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ui_settings_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding)
                .verticalScroll(scrollState)
                .padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.default),
        ) {


            // Appearance Section
            val appearanceTitle = stringResource(R.string.setting_section_appearance)
            CollapsibleSettingsSection(title = appearanceTitle) {
                val currentTheme = str("theme", "teal")
                val dynamicThemeEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                val useDarkDynamicPreview = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val dynamicSwatchColor =
                    remember(dynamicThemeEnabled, useDarkDynamicPreview, context) {
                        if (!dynamicThemeEnabled) {
                            Teal500
                        } else if (useDarkDynamicPreview) {
                            dynamicDarkColorScheme(context).primary
                        } else {
                            dynamicLightColorScheme(context).primary
                        }
                    }
                val modeOptions = listOf(
                    Triple(
                        "light",
                        stringResource(R.string.setting_appearance_light),
                        Pair(Icons.Default.LightMode, Icons.Outlined.LightMode),
                    ),
                    Triple(
                        "dark",
                        stringResource(R.string.setting_appearance_dark),
                        Pair(Icons.Default.DarkMode, Icons.Outlined.DarkMode),
                    ),
                    Triple(
                        "auto",
                        stringResource(R.string.setting_appearance_auto),
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
                val themeChoices = listOf(
                    Pair("dynamic", null as Color?),
                    Pair("teal", Teal500 as Color?),
                    Pair("blue", BluePrimary as Color?),
                    Pair("purple", PurplePrimary as Color?),
                    Pair("amber", AmberPrimary as Color?),
                    Pair("orange", OrangePrimary as Color?),
                    Pair("green", GreenPrimary as Color?),
                    Pair("cyan", CyanPrimary as Color?),
                    Pair("indigo", IndigoPrimary as Color?),
                    Pair("pink", PinkPrimary as Color?),
                    Pair("lime", LimePrimary as Color?),
                )

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
                            isEnabled = theme != "dynamic" || dynamicThemeEnabled,
                            dynamicColor = dynamicSwatchColor,
                            onClick = {
                                Prefs.putString("theme", theme)
                                Widgets.updateAll(context)
                            },
                        )
                    }
                }

                if (!dynamicThemeEnabled) {
                    Text(
                        text = stringResource(R.string.setting_dynamic_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Firewall Section
            val firewallTitle = stringResource(R.string.setting_section_firewall)
            CollapsibleSettingsSection(title = firewallTitle) {
                // Main allow/block toggles — uses the same FirewallTile as per-app detail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    FirewallTile(
                        allowedIcon = Icons.Default.Wifi,
                        blockedIcon = Icons.Default.WifiOff,
                        label = stringResource(R.string.title_wifi),
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
                        label = stringResource(R.string.title_mobile),
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
                SettingToggleRow(
                    title = stringResource(R.string.setting_whitelist_roaming),
                    checked = bool("whitelist_roaming", true),
                ) { updateFlag("whitelist_roaming", it, reload = true) }
                SettingToggleRow(
                    title = stringResource(R.string.setting_screen_on),
                    checked = bool("screen_on", true),
                ) { updateFlag("screen_on", it, reload = true) }
                SettingTogglePairRow(
                    firstTitle = stringResource(R.string.setting_screen_wifi),
                    firstChecked = bool("screen_wifi", false),
                    onFirstCheckedChange = { updateFlag("screen_wifi", it, reload = true) },
                    secondTitle = stringResource(R.string.setting_screen_other),
                    secondChecked = bool("screen_other", false),
                    onSecondCheckedChange = { updateFlag("screen_other", it, reload = true) },
                )
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
                        val set =
                            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        Prefs.putStringSet("wifi_homes", set)
                    },
                )

                SettingToggleRow(
                    title = stringResource(R.string.setting_metered),
                    checked = bool("use_metered", true),
                ) { updateFlag("use_metered", it, reload = true) }
                SettingTogglePairRow(
                    firstTitle = stringResource(R.string.setting_metered_2g),
                    firstChecked = bool("unmetered_2g", false),
                    onFirstCheckedChange = { updateFlag("unmetered_2g", it, reload = true) },
                    secondTitle = stringResource(R.string.setting_metered_3g),
                    secondChecked = bool("unmetered_3g", false),
                    onSecondCheckedChange = { updateFlag("unmetered_3g", it, reload = true) },
                )
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
                    isLast = true,
                ) { updateFlag("disable_on_call", it, reload = true) }
            }

            // Advanced Section
            val advancedTitle = stringResource(R.string.setting_section_advanced)
            CollapsibleSettingsSection(title = advancedTitle) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_system),
                    checked = bool("manage_system", false),
                    isFirst = true,
                ) { enabled ->
                    Prefs.putBoolean("manage_system", enabled)
                    Prefs.putBoolean("show_system", enabled)
                    ServiceSinkhole.reload("settings", context, false)
                }
                SettingToggleRow(
                    title = stringResource(R.string.setting_log_app),
                    checked = bool("log", false),
                ) { enabled ->
                    Prefs.putBoolean("log", enabled)
                    ServiceSinkhole.reload("settings", context, false)
                }
                SettingTextRowWithTooltip(
                    title = stringResource(R.string.setting_log_retention_days),
                    tooltip = stringResource(R.string.summary_log_retention_days),
                    value = str("log_retention_days", "3"),
                    keyboardType = KeyboardType.Number,
                ) { input ->
                    val numeric = input.filter(Char::isDigit).take(3)
                    val normalized = numeric.toIntOrNull()?.coerceIn(0, 365)?.toString() ?: numeric
                    Prefs.putString("log_retention_days", normalized)
                }
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
                SettingTogglePairRow(
                    firstTitle = stringResource(R.string.setting_lockdown_wifi),
                    firstChecked = bool("lockdown_wifi", false),
                    onFirstCheckedChange = { updateFlag("lockdown_wifi", it, reload = true) },
                    secondTitle = stringResource(R.string.setting_lockdown_other),
                    secondChecked = bool("lockdown_other", false),
                    onSecondCheckedChange = { updateFlag("lockdown_other", it, reload = true) },
                )
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
                    title = stringResource(
                        R.string.setting_stats_frequency,
                        str("stats_frequency", "1000")
                    ),
                    value = str("stats_frequency", "1000"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("stats_frequency", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        R.string.setting_stats_samples,
                        str("stats_samples", "10")
                    ),
                    value = str("stats_samples", "10"),
                    keyboardType = KeyboardType.Number,
                    isLast = true,
                    onValueChange = { Prefs.putString("stats_samples", it) },
                )
            }

            // Hosts Section
            val hostsTitle = stringResource(R.string.setting_section_hosts)
            CollapsibleSettingsSection(title = hostsTitle) {
                SettingToggleRow(
                    title = stringResource(R.string.setting_use_hosts),
                    checked = bool("use_hosts", false),
                    isFirst = true,
                ) { updateFlag("use_hosts", it, reload = true) }

                SettingTextRow(
                    title = stringResource(R.string.setting_hosts_url),
                    value = str("hosts_url", ""),
                    isLast = true,
                    onValueChange = { Prefs.putString("hosts_url", it) },
                )

                // Single button with dropdown for hosts file operations
                Box {
                    var showHostsMenu by remember { mutableStateOf(false) }
                    FilledTonalButton(
                        onClick = { showHostsMenu = true },
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(R.string.setting_section_hosts))
                    }
                    DropdownMenu(
                        expanded = showHostsMenu,
                        onDismissRequest = { showHostsMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.setting_hosts)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showHostsMenu = false
                                importHostsLauncher.launch(arrayOf("*/*"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.setting_hosts_append)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showHostsMenu = false
                                appendHostsLauncher.launch(arrayOf("*/*"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.setting_hosts_download)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showHostsMenu = false
                                val intent =
                                    android.content.Intent(context, ServiceExternal::class.java)
                                intent.action = ServiceExternal.ACTION_DOWNLOAD_HOSTS_FILE
                                ContextCompat.startForegroundService(context, intent)
                            },
                        )
                    }
                }
            }

            // Network Section (DNS + Proxy + VPN)
            val networkSectionTitle = stringResource(R.string.setting_section_network)
            val dnsTitle = stringResource(R.string.setting_section_dns)
            val proxyTitle = stringResource(R.string.setting_section_proxy)
            val vpnTitle = stringResource(R.string.setting_section_vpn)
            CollapsibleSettingsSection(title = networkSectionTitle) {
                Text(
                    text = dnsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingTextRowWithTooltip(
                    title = stringResource(R.string.setting_rcode, str("rcode", "3")),
                    tooltip = stringResource(R.string.tooltip_rcode),
                    value = str("rcode", "3"),
                    keyboardType = KeyboardType.Number,
                    isFirst = true,
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
                FilledTonalButton(
                    onClick = onOpenDns,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.setting_show_resolved))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.default))

                Text(
                    text = proxyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    title = stringResource(
                        R.string.setting_socks5_username,
                        str("socks5_username", "")
                    ),
                    value = str("socks5_username", ""),
                    onValueChange = { Prefs.putString("socks5_username", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        R.string.setting_socks5_password,
                        str("socks5_password", "")
                    ),
                    value = str("socks5_password", ""),
                    onValueChange = { Prefs.putString("socks5_password", it) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.default))

                Text(
                    text = vpnTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    isLast = true,
                    onValueChange = { Prefs.putString("dns2", it) },
                )
            }

            // Forwarding Section
            val forwardingTitle = stringResource(R.string.setting_forwarding)
            CollapsibleSettingsSection(title = forwardingTitle) {
                FilledTonalButton(onClick = onOpenForwarding) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.setting_forwarding))
                }
            }

            // Background Section
            val backgroundTitle = stringResource(R.string.setting_section_background)
            CollapsibleSettingsSection(title = backgroundTitle) {
                SettingTextRow(
                    title = stringResource(R.string.setting_watchdog, str("watchdog", "0")),
                    value = str("watchdog", "0"),
                    keyboardType = KeyboardType.Number,
                    isFirst = true,
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

                FilledTonalButton(
                    onClick = {
                        updateCheckInProgress = true
                        updateCheckStatus = null
                        updateCheckVersion = null
                        ServiceSinkhole.checkForUpdateNow("settings", context)
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
                        Text(text = stringResource(R.string.setting_update_checking))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text(text = stringResource(R.string.setting_update_now))
                    }
                }

                updateCheckStatus?.let { status ->
                    val message =
                        when (status) {
                            "available" ->
                                if (!updateCheckVersion.isNullOrBlank()) {
                                    stringResource(
                                        R.string.setting_update_result_available,
                                        updateCheckVersion!!
                                    )
                                } else {
                                    stringResource(R.string.setting_update_result_available_unknown)
                                }

                            "upToDate" -> stringResource(R.string.setting_update_result_up_to_date)
                            "unavailable" -> stringResource(R.string.setting_update_result_unavailable)
                            else -> stringResource(R.string.setting_update_result_failed)
                        }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Diagnostics Section
            val diagnosticsTitle = stringResource(R.string.setting_section_diagnostics)
            CollapsibleSettingsSection(title = diagnosticsTitle) {
                SettingToggleRowWithTooltip(
                    title = stringResource(R.string.setting_pcap),
                    tooltip = stringResource(R.string.tooltip_pcap),
                    checked = bool("pcap", false),
                    isFirst = true,
                ) { Prefs.putBoolean("pcap", it) }
                SettingTextRow(
                    title = stringResource(
                        R.string.setting_pcap_record_size,
                        str("pcap_record_size", "64")
                    ),
                    value = str("pcap_record_size", "64"),
                    keyboardType = KeyboardType.Number,
                    onValueChange = { Prefs.putString("pcap_record_size", it) },
                )
                SettingTextRow(
                    title = stringResource(
                        R.string.setting_pcap_file_size,
                        str("pcap_file_size", "2")
                    ),
                    value = str("pcap_file_size", "2"),
                    keyboardType = KeyboardType.Number,
                    isLast = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                Prefs.putBoolean("pcap", false)
                                Prefs.putString("pcap_record_size", "64")
                                Prefs.putString("pcap_file_size", "2")
                            },
                            modifier = Modifier.size(TouchTargets.minimum),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.menu_reset),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onValueChange = { Prefs.putString("pcap_file_size", it) },
                )
            }

            // Support Section
            val supportTitle = stringResource(R.string.setting_section_support)
            CollapsibleSettingsSection(title = supportTitle) {
                FilledTonalButton(onClick = onOpenPro) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(text = stringResource(R.string.title_pro))
                }
            }
        }
    } // end Scaffold

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
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.extraSmall),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            content = content,
        )
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
private fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
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
    val isHighlighted = checked
    val containerColor =
        if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    val titleColor =
        if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    val subtitleColor =
        if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                    )
                }
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

@Composable
private fun SettingToggleRowWithTooltip(
    title: String,
    tooltip: String,
    checked: Boolean,
    isFirst: Boolean = false,
    isLast: Boolean = false,
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
    val isHighlighted = checked
    val containerColor =
        if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    val titleColor =
        if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    val infoTint =
        if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = titleColor,
                    )
                    IconButton(
                        onClick = { showTooltip = !showTooltip },
                        modifier = Modifier.size(TouchTargets.minimum),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(
                                R.string.content_desc_show_info,
                                title
                            ),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(bottom = 1.dp),
                            tint = infoTint,
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.semantics {
                        contentDescription = "$title: ${if (checked) "enabled" else "disabled"}"
                    },
                )
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
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val rowShape =
        settingItemShape(isFirst = isFirst, isLast = isLast, baseShape = MaterialTheme.shapes.small)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = rowShape,
    ) {
        Column(
            modifier = Modifier.padding(spacing.default),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = trailingIcon,
            )
        }
    }
}

@Composable
private fun SettingTextRowWithTooltip(
    title: String,
    tooltip: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val rowShape =
        settingItemShape(isFirst = isFirst, isLast = isLast, baseShape = MaterialTheme.shapes.small)
    var showTooltip by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = rowShape,
    ) {
        Column(
            modifier = Modifier.padding(spacing.default),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                IconButton(
                    onClick = { showTooltip = !showTooltip },
                    modifier = Modifier.size(TouchTargets.minimum),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.content_desc_show_info, title),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(bottom = 1.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ExpandableContent(expanded = showTooltip) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
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
}

@Composable
private fun SettingTogglePairRow(
    firstTitle: String,
    firstChecked: Boolean,
    onFirstCheckedChange: (Boolean) -> Unit,
    secondTitle: String,
    secondChecked: Boolean,
    onSecondCheckedChange: (Boolean) -> Unit,
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
            onCheckedChange = onFirstCheckedChange,
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
            onCheckedChange = onSecondCheckedChange,
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
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
) {
    val tileShape = shape ?: MaterialTheme.shapes.small
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheckedChange(!checked)
        },
        modifier = modifier.heightIn(min = 72.dp),
        shape = tileShape,
        color = if (checked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
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
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
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
