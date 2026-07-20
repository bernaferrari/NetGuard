package com.bernaferrari.quietguard.ui

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols




import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.quietguard.ui.main.AppsScreen
import com.bernaferrari.quietguard.ui.main.HomeScreen
import com.bernaferrari.quietguard.ui.main.MainViewModel
import com.bernaferrari.quietguard.ui.screens.AppRuleDetailScreen
import com.bernaferrari.quietguard.ui.screens.DnsScreen
import com.bernaferrari.quietguard.ui.screens.ForwardingScreen
import com.bernaferrari.quietguard.ui.screens.LogsScreen
import com.bernaferrari.quietguard.ui.screens.ProScreen
import com.bernaferrari.quietguard.ui.screens.SettingsScreen
import com.bernaferrari.quietguard.ui.screens.vm.LogsViewModel
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import com.bernaferrari.quietguard.ui.util.LoadErrorPlaceholder
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.home_apps_hint
import com.bernaferrari.quietguard.generated.resources.menu_firewall
import com.bernaferrari.quietguard.generated.resources.menu_home
import com.bernaferrari.quietguard.generated.resources.menu_log
import com.bernaferrari.quietguard.generated.resources.menu_settings
import com.bernaferrari.quietguard.generated.resources.ui_apps_title
import com.bernaferrari.quietguard.generated.resources.ui_loading
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
private enum class NavDestination(
    val key: AppNavKey,
    val labelRes: StringResource,
    val selectedIcon: MaterialIcon,
    val unselectedIcon: MaterialIcon = selectedIcon,
) {
    HomeTab(Home, Res.string.menu_home, MaterialSymbols.Filled.Shield, MaterialSymbols.Outlined.Shield),
    AppsTab(Apps, Res.string.menu_firewall, MaterialSymbols.Filled.Tune),
    LogsTab(Logs, Res.string.menu_log, MaterialSymbols.AutoMirrored.Filled.List),
    SettingsTab(
        Settings,
        Res.string.menu_settings,
        MaterialSymbols.Filled.Settings,
        MaterialSymbols.Outlined.Settings,
    ),
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
    // Root tabs are removed from the Navigation 3 back stack when switching tabs.
    // Keep logs at the app-navigation scope so a revisit can reuse its last real state.
    val logsViewModel: LogsViewModel = koinViewModel()
    val startStack = remember(startRoute) {
        canonicalStackFor(NavRoutes.fromRoute(startRoute))
    }
    val backStack = rememberNavBackStack(
        appNavSavedStateConfiguration,
        *startStack.toTypedArray(),
    )
    val paneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    val singlePaneLayout = paneScaffoldDirective.maxHorizontalPartitions == 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = paneScaffoldDirective,
    )
    val motion = LocalMotion.current
    val detailTransitionMetadata = remember(motion) {
        NavDisplay.transitionSpec {
            ContentTransform(
                targetContentEnter = slideInHorizontally(
                    animationSpec = tween(
                        durationMillis = if (motion.reducedMotion) 0 else 220,
                        easing = motion.easingDecelerate,
                    ),
                    initialOffsetX = { fullWidth -> fullWidth },
                ),
                initialContentExit = ExitTransition.None,
            )
        } +
            NavDisplay.popTransitionSpec {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = if (motion.reducedMotion) 0 else 180,
                            easing = motion.easingAccelerate,
                        ),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ),
                )
            }
    }
    val appDetailMetadata = remember(singlePaneLayout, detailTransitionMetadata) {
        val paneMetadata = ListDetailSceneStrategy.detailPane()
        if (!singlePaneLayout) {
            paneMetadata
        } else {
            paneMetadata + detailTransitionMetadata
        }
    }

    fun popBackStack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun setStack(vararg keys: AppNavKey) {
        backStack.clear()
        backStack.addAll(keys.toList())
    }

    fun selectRoot(destination: AppNavKey) {
        setStack(*canonicalStackFor(destination).toTypedArray())
    }

    fun push(destination: AppNavKey) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    fun openFirewallDetail(uid: Int) {
        val currentDetail = backStack.lastOrNull() as? AppRuleDetail
        if (currentDetail?.uid == uid) {
            return
        }
        if (currentDetail != null) {
            backStack[backStack.lastIndex] = AppRuleDetail(uid)
            return
        }
        if (backStack.lastOrNull() !is Apps) {
            setStack(Home, Apps)
        }
        backStack.add(AppRuleDetail(uid))
    }

    fun selectedTabFor(current: NavKey?): AppNavKey? {
        val appKey = current as? AppNavKey ?: return null
        return when (appKey) {
            is AppRuleDetail -> Apps
            Dns, Forwarding, Pro, is SettingsDetail -> Settings
            else -> appKey
        }
    }

    @Composable
    fun CenteredScreen(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
            ) {
                content()
            }
        }
    }

    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            selectRoot(NavRoutes.fromRoute(pendingRoute))
            onRouteNavigated()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            val currentKey = backStack.lastOrNull()
            val selectedTab = selectedTabFor(currentKey)
            NavDestination.entries.forEach { destination ->
                val selected = selectedTab == destination.key
                item(
                    selected = selected,
                    onClick = { selectRoot(destination.key) },
                    icon = {
                        Icon(
                            icon =
                                if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
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
            LaunchedEffect(activeDetail.uid, rulesUiState.hasReceived, rulesUiState.data) {
                if (
                    rulesUiState.hasReceived &&
                    !rulesUiState.hasFailed &&
                    rulesUiState.data.none { it.uid == activeDetail.uid }
                ) {
                    popBackStack()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                sceneStrategies = listOf(listDetailStrategy),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider =
                    entryProvider {
                        entry<Home> {
                            HomeScreen(
                                viewModel = viewModel,
                                onToggleEnabled = onToggleEnabled,
                            )
                        }
                        entry<Apps>(
                            metadata =
                                ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        StatePlaceholder(
                                            title = stringResource(Res.string.ui_apps_title),
                                            message = stringResource(Res.string.home_apps_hint),
                                            icon = MaterialSymbols.Filled.Tune,
                                        )
                                    },
                                ),
                        ) {
                            AppsScreen(
                                viewModel = viewModel,
                                selectedRuleUid =
                                    (backStack.lastOrNull() as? AppRuleDetail)
                                        ?.uid,
                                onNavigateToDetail = { rule ->
                                    openFirewallDetail(rule.uid)
                                },
                            )
                        }
                        entry<AppRuleDetail>(
                            metadata = appDetailMetadata,
                        ) { key ->
                            val targetRule = rulesUiState.data.firstOrNull { it.uid == key.uid }
                            when {
                                targetRule != null -> {
                                    AppRuleDetailScreen(
                                        rule = targetRule,
                                        showBackButton = singlePaneLayout,
                                        onUpdateRule = { transform ->
                                            viewModel.updateRule(key.uid, transform)
                                        },
                                        onBack = { popBackStack() },
                                    )
                                }

                                rulesUiState.hasFailed -> {
                                    LoadErrorPlaceholder(
                                        icon = MaterialSymbols.Filled.Apps,
                                        onRetry = viewModel::refreshRules,
                                    )
                                }

                                else -> {
                                    StatePlaceholder(
                                        title = stringResource(Res.string.ui_loading),
                                        message = stringResource(Res.string.home_apps_hint),
                                        icon = MaterialSymbols.Filled.Apps,
                                        isLoading = true,
                                    )
                                }
                            }
                        }
                        entry<Logs> {
                            CenteredScreen {
                                LogsScreen(viewModel = logsViewModel)
                            }
                        }
                        entry<Settings> {
                            CenteredScreen {
                                SettingsScreen(
                                    section = null,
                                    onOpenSection = { push(SettingsDetail(it)) },
                                    onOpenDns = { push(Dns) },
                                    onOpenForwarding = { push(Forwarding) },
                                )
                            }
                        }
                        entry<SettingsDetail>(metadata = detailTransitionMetadata) { key ->
                            CenteredScreen {
                                SettingsScreen(
                                    section = key.section,
                                    onBack = { popBackStack() },
                                    onOpenSection = { push(SettingsDetail(it)) },
                                    onOpenDns = { push(Dns) },
                                    onOpenForwarding = { push(Forwarding) },
                                )
                            }
                        }
                        entry<Dns>(metadata = detailTransitionMetadata) {
                            CenteredScreen {
                                DnsScreen(onBack = { popBackStack() })
                            }
                        }
                        entry<Forwarding>(metadata = detailTransitionMetadata) {
                            CenteredScreen {
                                ForwardingScreen(onBack = { popBackStack() })
                            }
                        }
                        entry<Pro>(metadata = detailTransitionMetadata) {
                            CenteredScreen {
                                ProScreen(onBack = { popBackStack() })
                            }
                        }
                    },
            )
        }
    }
}

private fun canonicalStackFor(destination: AppNavKey): List<AppNavKey> =
    when (destination) {
        Home -> listOf(Home)
        Apps -> listOf(Home, Apps)
        Logs -> listOf(Home, Logs)
        Settings -> listOf(Home, Settings)
        is SettingsDetail -> listOf(Home, Settings, destination)
        Dns -> listOf(Home, Settings, Dns)
        Forwarding -> listOf(Home, Settings, Forwarding)
        Pro -> listOf(Home, Settings, Pro)
        is AppRuleDetail -> listOf(Home, Apps, destination)
    }
