package eu.faircode.netguard

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import eu.faircode.netguard.ui.Home
import eu.faircode.netguard.ui.AppNavigation
import eu.faircode.netguard.ui.theme.NetGuardThemeFromPrefs
import eu.faircode.netguard.ui.util.InfoScreen
import eu.faircode.netguard.ui.main.MainViewModel

@AndroidEntryPoint
class ActivityMain : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isUnsupported = Build.VERSION.SDK_INT < MIN_SDK
        val isXposed = Util.hasXposed(this)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)

        setContent {
            NetGuardThemeFromPrefs {
                if (isUnsupported) {
                    InfoScreen(
                        title = stringResource(R.string.app_name),
                        body = stringResource(R.string.app_android),
                    )
                    return@NetGuardThemeFromPrefs
                }
                if (isXposed) {
                    InfoScreen(
                        title = stringResource(R.string.app_name),
                        body = stringResource(R.string.app_xposed),
                    )
                    return@NetGuardThemeFromPrefs
                }

                val vpnLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            viewModel.setEnabled(true)
                            ServiceSinkhole.start("UI", this)
                        } else {
                            viewModel.setEnabled(false)
                        }
                    }

                AppNavigation(
                    viewModel = viewModel,
                    onToggleEnabled = { enable ->
                        if (enable) {
                            val intent = VpnService.prepare(this)
                            if (intent == null) {
                                viewModel.setEnabled(true)
                                ServiceSinkhole.start("UI", this)
                            } else {
                                vpnLauncher.launch(intent)
                            }
                        } else {
                            viewModel.setEnabled(false)
                            ServiceSinkhole.stop("UI", this, false)
                        }
                    },
                    startRoute = pendingRoute.value ?: Home.route,
                    pendingRoute = pendingRoute.value,
                    onRouteNavigated = { pendingRoute.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        private const val MIN_SDK = Build.VERSION_CODES.M

        const val EXTRA_ROUTE = "Route"
        const val ACTION_RULES_CHANGED = "eu.faircode.netguard.ACTION_RULES_CHANGED"
        const val ACTION_QUEUE_CHANGED = "eu.faircode.netguard.ACTION_QUEUE_CHANGED"
        const val EXTRA_REFRESH = "Refresh"
        const val EXTRA_SEARCH = "Search"
        const val EXTRA_RELATED = "Related"
        const val EXTRA_APPROVE = "Approve"
        const val EXTRA_CONNECTED = "Connected"
        const val EXTRA_METERED = "Metered"
        const val EXTRA_SIZE = "Size"

        fun createRouteIntent(context: android.content.Context, route: String): Intent {
            return Intent(context, ActivityMain::class.java).apply {
                putExtra(EXTRA_ROUTE, route)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}
