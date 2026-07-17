package com.bernaferari.renetguard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferari.renetguard.platform.PlatformContext
import com.bernaferari.renetguard.platform.PlatformToast
import com.bernaferari.renetguard.platform.onDemoFirewallToggled
import com.bernaferari.renetguard.ui.AppNavigation
import com.bernaferari.renetguard.ui.Home
import com.bernaferari.renetguard.ui.main.MainViewModel
import com.bernaferari.renetguard.ui.theme.NetGuardAppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppContent(
    onToggleEnabled: (Boolean) -> Unit,
    startRoute: String = Home.route,
    pendingRoute: String? = null,
    onRouteNavigated: () -> Unit = {},
    viewModel: MainViewModel = koinViewModel(),
) {
    NetGuardAppTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val toastMessage by PlatformToast.message.collectAsState()

        LaunchedEffect(toastMessage) {
            val message = toastMessage ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            PlatformToast.dismiss()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val handleToggleEnabled: (Boolean) -> Unit = { enabled ->
                    if (PlatformContext.isDemoMode()) {
                        viewModel.setEnabled(enabled)
                        onDemoFirewallToggled(enabled)
                    } else {
                        onToggleEnabled(enabled)
                    }
                }
                AppNavigation(
                    viewModel = viewModel,
                    onToggleEnabled = handleToggleEnabled,
                    startRoute = startRoute,
                    pendingRoute = pendingRoute,
                    onRouteNavigated = onRouteNavigated,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) { data ->
                Snackbar(snackbarData = data)
            }
        }
    }
}
