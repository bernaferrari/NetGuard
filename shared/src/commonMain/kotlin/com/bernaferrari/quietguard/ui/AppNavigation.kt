package com.bernaferrari.quietguard.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.home_apps_hint
import com.bernaferrari.quietguard.generated.resources.menu_firewall
import com.bernaferrari.quietguard.generated.resources.menu_home
import com.bernaferrari.quietguard.generated.resources.menu_log
import com.bernaferrari.quietguard.generated.resources.menu_settings
import com.bernaferrari.quietguard.generated.resources.ui_apps_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class NavDestination(
    val key: AppNavKey,
    val labelRes: StringResource,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector = selectedIcon,
) {
    HomeTab(Home, Res.string.menu_home, Icons.Filled.Shield, Icons.Outlined.Shield),
    AppsTab(Apps, Res.string.menu_firewall, Icons.Default.Tune),
    LogsTab(Logs, Res.string.menu_log, Icons.AutoMirrored.Filled.List),
    SettingsTab(Settings, Res.string.menu_settings, Icons.Default.Settings),
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
    val backStack = rememberNavBackStack(appNavSavedStateConfiguration, startKey)
    val paneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    val singlePaneLayout = paneScaffoldDirective.maxHorizontalPartitions == 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = paneScaffoldDirective,
    )
    val appDetailMetadata = remember(singlePaneLayout) {
        val paneMetadata = ListDetailSceneStrategy.detailPane()
        if (!singlePaneLayout) {
            paneMetadata
        } else {
            paneMetadata +
                NavDisplay.transitionSpec {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = LinearOutSlowInEasing,
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
                                durationMillis = 180,
                                easing = LinearOutSlowInEasing,
                            ),
                            targetOffsetX = { fullWidth -> fullWidth },
                        ),
                    )
                }
        }
    }

    fun popBackStack() {
        val current = backStack.lastOrNull() as? AppNavKey ?: return

        when {
            current is AppRuleDetail && backStack.size > 1 -> {
                backStack.removeAt(backStack.lastIndex)
            }
            current != Home -> {
                backStack.clear()
                backStack.add(Home)
            }
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
            Dns, Forwarding, Pro -> Settings
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
            navigateTo(NavRoutes.fromRoute(pendingRoute))
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
                    onClick = { navigateTo(destination.key) },
                    icon = {
                        Icon(
                            imageVector =
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
            LaunchedEffect(activeDetail.uid, rulesUiState.hasLoaded, rulesUiState.rules) {
                if (
                    rulesUiState.hasLoaded &&
                    rulesUiState.rules.none { it.uid == activeDetail.uid }
                ) {
                    popBackStack()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack.toList(),
                modifier = Modifier.fillMaxSize(),
                sceneStrategies = listOf(listDetailStrategy),
                onBack = { popBackStack() },
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
                                            icon = Icons.Default.Tune,
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
                            val targetRule = rulesUiState.rules.firstOrNull { it.uid == key.uid }
                            if (targetRule != null) {
                                AppRuleDetailScreen(
                                    rule = targetRule,
                                    showBackButton = singlePaneLayout,
                                    onUpdateRule = { transform ->
                                        viewModel.updateRule(key.uid, transform)
                                    },
                                    onBack = { popBackStack() },
                                )
                            }
                        }
                        entry<Logs> {
                            CenteredScreen {
                                LogsScreen()
                            }
                        }
                        entry<Settings> {
                            CenteredScreen {
                                SettingsScreen(
                                    onOpenDns = { navigateTo(Dns) },
                                    onOpenForwarding = { navigateTo(Forwarding) },
                                )
                            }
                        }
                        entry<Dns> {
                            CenteredScreen {
                                DnsScreen()
                            }
                        }
                        entry<Forwarding> {
                            CenteredScreen {
                                ForwardingScreen()
                            }
                        }
                        entry<Pro> {
                            CenteredScreen {
                                ProScreen()
                            }
                        }
                    },
            )
        }
    }
}
