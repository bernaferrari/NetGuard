package eu.faircode.netguard.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import eu.faircode.netguard.R
import eu.faircode.netguard.ui.main.AppsScreen
import eu.faircode.netguard.ui.main.HomeScreen
import eu.faircode.netguard.ui.main.MainViewModel
import eu.faircode.netguard.ui.screens.DnsScreen
import eu.faircode.netguard.ui.screens.ForwardingScreen
import eu.faircode.netguard.ui.screens.LogsScreen
import eu.faircode.netguard.ui.screens.ProScreen
import eu.faircode.netguard.ui.screens.SettingsScreen

private enum class NavDestination(
    val key: AppNavKey,
    @param:StringRes val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HomeTab(Home, R.string.app_name, Icons.Default.Security),
    AppsTab(Apps, R.string.menu_firewall, Icons.Default.Tune),
    LogsTab(Logs, R.string.menu_log, Icons.AutoMirrored.Filled.List),
    SettingsTab(Settings, R.string.menu_settings, Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    fun setStack(vararg keys: AppNavKey) {
        backStack.clear()
        backStack.addAll(keys.toList())
    }

    fun navigateTo(destination: AppNavKey) {
        when (destination) {
            Logs -> setStack(Apps, Logs)
            Dns -> setStack(Settings, Dns)
            Forwarding -> setStack(Settings, Forwarding)
            Pro -> setStack(Settings, Pro)
            else -> setStack(destination)
        }
    }

    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            navigateTo(NavRoutes.fromRoute(pendingRoute))
            onRouteNavigated()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentKey = backStack.lastOrNull()
                NavDestination.entries.forEach { destination ->
                    val selected = currentKey == destination.key
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateTo(destination.key) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.label),
                            )
                        },
                        label = {
                            Text(stringResource(destination.label))
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            sceneStrategy = listDetailStrategy,
            onBack = { backStack.removeLastOrNull() },
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
                            onNavigateToApps = { navigateTo(Apps) },
                            onNavigateToLogs = { navigateTo(Logs) },
                            onNavigateToSettings = { navigateTo(Settings) },
                        )
                    }
                    entry<Apps>(
                        metadata =
                            ListDetailSceneStrategy.listPane(
                                detailPlaceholder = {
                                    Text(
                                        text = stringResource(R.string.home_logs_hint),
                                        modifier = Modifier.padding(24.dp),
                                    )
                                },
                            ),
                    ) {
                        AppsScreen()
                    }
                    entry<Logs>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) {
                        LogsScreen()
                    }
                    entry<Settings> {
                        SettingsScreen(
                            onOpenDns = { navigateTo(Dns) },
                            onOpenForwarding = { navigateTo(Forwarding) },
                            onOpenPro = { navigateTo(Pro) },
                        )
                    }
                    entry<Dns> {
                        DnsScreen()
                    }
                    entry<Forwarding> {
                        ForwardingScreen()
                    }
                    entry<Pro> {
                        ProScreen()
                    }
                },
        )
    }
}
