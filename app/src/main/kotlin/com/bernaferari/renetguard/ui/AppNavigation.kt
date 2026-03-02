package com.bernaferari.renetguard.ui

import androidx.annotation.StringRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.bernaferari.renetguard.R
import com.bernaferari.renetguard.ui.main.AppsFilter
import com.bernaferari.renetguard.ui.main.AppsScreen
import com.bernaferari.renetguard.ui.main.HomeScreen
import com.bernaferari.renetguard.ui.main.MainViewModel
import com.bernaferari.renetguard.ui.screens.AppRuleDetailScreen
import com.bernaferari.renetguard.ui.screens.DnsScreen
import com.bernaferari.renetguard.ui.screens.ForwardingScreen
import com.bernaferari.renetguard.ui.screens.LogsScreen
import com.bernaferari.renetguard.ui.screens.ProScreen
import com.bernaferari.renetguard.ui.screens.SettingsScreen
import com.bernaferari.renetguard.ui.util.StatePlaceholder

private enum class NavDestination(
    val key: AppNavKey,
    @param:StringRes val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HomeTab(Home, R.string.menu_home, Icons.Default.Security),
    AppsTab(Apps, R.string.menu_firewall, Icons.Default.Tune),
    LogsTab(Logs, R.string.menu_log, Icons.AutoMirrored.Filled.List),
    SettingsTab(Settings, R.string.menu_settings, Icons.Default.Settings),
}

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
    startRoute: String,
    pendingRoute: String? = null,
    onRouteNavigated: () -> Unit = {},
) {
    val startKey = remember(startRoute) { NavRoutes.fromRoute(startRoute) }
    val backStack = rememberNavBackStack(startKey)
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val showDetailBackButton = isWideScreen.not()
    var appsLaunchFilter by remember { mutableStateOf(AppsFilter.All) }
    var appsLaunchFilterVersion by remember { mutableIntStateOf(0) }
    var selectedFirewallUid by remember { mutableStateOf<Int?>(null) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    fun Scene<NavKey>.hasAppDetailEntry(): Boolean {
        return entries.any { entry ->
            entry.contentKey.toString().startsWith("AppRuleDetail(")
        }
    }

    fun popBackStack() {
        val current = backStack.lastOrNull() as? AppNavKey ?: return

        when {
            // Keep detail navigation behavior: detail -> list.
            current is AppRuleDetail && backStack.size > 1 -> {
                backStack.removeAt(backStack.lastIndex)
                selectedFirewallUid = null
            }
            // From any non-home top-level destination, go to Home first.
            current != Home -> {
                backStack.clear()
                backStack.add(Home)
            }
            // Already at Home, allow the activity to close on next back.
            else -> {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }

    fun setStack(vararg keys: AppNavKey) {
        backStack.clear()
        backStack.addAll(keys.toList())
    }

    fun navigateTo(destination: AppNavKey) {
        if (selectedFirewallUid != null && destination !is Apps && destination !is AppRuleDetail) {
            selectedFirewallUid = null
        }
        when (destination) {
            Home -> setStack(Home)
            Apps -> setStack(Home, Apps)
            Logs -> setStack(Home, Logs)
            Settings -> setStack(Home, Settings)
            Dns -> setStack(Home, Settings, Dns)
            Forwarding -> setStack(Home, Settings, Forwarding)
            Pro -> setStack(Home, Settings, Pro)
            else -> setStack(destination)
        }
    }

    fun openFirewallDetail(uid: Int) {
        selectedFirewallUid = uid
        if (isWideScreen) {
            while (backStack.lastOrNull() is AppRuleDetail) {
                backStack.removeAt(backStack.lastIndex)
            }
            if (backStack.isEmpty() || backStack.last() !is Apps) {
                backStack.clear()
                backStack.add(Home)
                backStack.add(Apps)
            }
            backStack.add(AppRuleDetail(uid))
        } else {
            setStack(Home, Apps, AppRuleDetail(uid))
        }
    }

    fun selectedTabFor(current: NavKey?): AppNavKey? {
        val appKey = current as? AppNavKey ?: return null
        return when (appKey) {
            is AppRuleDetail -> Apps
            Dns, Forwarding, Pro -> Settings
            else -> appKey
        }
    }

    @Composable
    fun HalfScreen(content: @Composable () -> Unit) {
        if (isWideScreen) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                    content()
                }
            }
        } else {
            content()
        }
    }

    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            navigateTo(NavRoutes.fromRoute(pendingRoute))
            onRouteNavigated()
        }
    }

    LaunchedEffect(isWideScreen) {
        if (!isWideScreen) {
            selectedFirewallUid = null
        }
    }

    LaunchedEffect(isWideScreen, backStack.lastOrNull()) {
        if (!isWideScreen) {
            val activeDetail = backStack.lastOrNull { it is AppRuleDetail } as? AppRuleDetail
            if (activeDetail != null) {
                backStack.clear()
                backStack.add(Home)
                backStack.add(Apps)
                backStack.add(activeDetail)
                selectedFirewallUid = activeDetail.uid
            }
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            val currentKey = backStack.lastOrNull()
            val selectedTab = selectedTabFor(currentKey)
            NavDestination.entries.forEach { destination ->
                item(
                    selected = selectedTab == destination.key,
                    onClick = { navigateTo(destination.key) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.label),
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
    ) {
        val activeDetail = backStack.lastOrNull()
        val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()

        if (activeDetail is AppRuleDetail) {
            LaunchedEffect(activeDetail.uid) {
                viewModel.ensureRulesLoaded()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            NavDisplay(
                backStack = backStack.toList(),
                modifier = Modifier.fillMaxSize(),
                sceneStrategy = listDetailStrategy,
                onBack = { popBackStack() },
                transitionSpec = {
                    val isAppDetailTransition =
                        initialState.hasAppDetailEntry() || targetState.hasAppDetailEntry()
                    if (isAppDetailTransition) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        defaultTransitionSpec<NavKey>()(this)
                    }
                },
                popTransitionSpec = {
                    val isAppDetailTransition =
                        initialState.hasAppDetailEntry() || targetState.hasAppDetailEntry()
                    if (isAppDetailTransition) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        defaultPopTransitionSpec<NavKey>()(this)
                    }
                },
                predictivePopTransitionSpec = { swipeEdge ->
                    val isAppDetailTransition =
                        initialState.hasAppDetailEntry() || targetState.hasAppDetailEntry()
                    if (isAppDetailTransition) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        defaultPredictivePopTransitionSpec<NavKey>()(this, swipeEdge)
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider =
                    entryProvider {
                        entry<Home> {
                            HalfScreen {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onToggleEnabled = onToggleEnabled,
                                    onOpenFirewall = { filter ->
                                        appsLaunchFilter = filter
                                        appsLaunchFilterVersion++
                                        navigateTo(Apps)
                                    },
                                    onOpenLogs = { navigateTo(Logs) },
                                    onOpenSettings = { navigateTo(Settings) },
                                )
                            }
                        }
                        entry<Apps>(
                            metadata =
                                ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        StatePlaceholder(
                                            title = stringResource(R.string.ui_apps_title),
                                            message = stringResource(R.string.home_apps_hint),
                                            icon = Icons.Default.Tune,
                                        )
                                    },
                                ),
                        ) {
                            AppsScreen(
                                viewModel = viewModel,
                                selectedRuleUid = selectedFirewallUid.takeIf { isWideScreen },
                                onNavigateToDetail = { rule ->
                                    openFirewallDetail(rule.uid)
                                },
                                initialFilter = appsLaunchFilter,
                                initialFilterVersion = appsLaunchFilterVersion,
                            )
                        }
                        entry<AppRuleDetail>(
                            metadata = ListDetailSceneStrategy.detailPane(),
                        ) { key ->
                            // Kept for compatibility if route stack explicitly resolves to detail scene.
                            val targetRule = rulesUiState.rules.firstOrNull { it.uid == key.uid }
                            if (targetRule != null) {
                                AppRuleDetailScreen(
                                    rule = targetRule,
                                    allRules = rulesUiState.rules,
                                    showBackButton = showDetailBackButton,
                                    enableSlideTransition = isWideScreen.not(),
                                    onRuleChanged = { viewModel.notifyRulesChanged() },
                                    onBack = { popBackStack() },
                                )
                            }
                        }
                        entry<Logs>(
                            metadata = ListDetailSceneStrategy.detailPane(),
                        ) {
                            HalfScreen {
                                LogsScreen()
                            }
                        }
                        entry<Settings> {
                            HalfScreen {
                                SettingsScreen(
                                    onOpenDns = { navigateTo(Dns) },
                                    onOpenForwarding = { navigateTo(Forwarding) },
                                    onOpenPro = { navigateTo(Pro) },
                                )
                            }
                        }
                        entry<Dns> {
                            HalfScreen {
                                DnsScreen()
                            }
                        }
                        entry<Forwarding> {
                            HalfScreen {
                                ForwardingScreen()
                            }
                        }
                        entry<Pro> {
                            HalfScreen {
                                ProScreen()
                            }
                        }
                    },
            )

        }
    }
}
