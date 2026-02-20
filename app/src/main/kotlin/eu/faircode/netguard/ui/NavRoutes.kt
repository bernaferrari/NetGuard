package eu.faircode.netguard.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppNavKey : NavKey {
    val route: String
}

@Serializable
data object Home : AppNavKey {
    override val route = "home"
}

@Serializable
data object Apps : AppNavKey {
    override val route = "apps"
}

@Serializable
data object Logs : AppNavKey {
    override val route = "logs"
}

@Serializable
data object Settings : AppNavKey {
    override val route = "settings"
}

@Serializable
data object Dns : AppNavKey {
    override val route = "dns"
}

@Serializable
data object Forwarding : AppNavKey {
    override val route = "forwarding"
}

@Serializable
data object Pro : AppNavKey {
    override val route = "pro"
}

@Serializable
data class AppRuleDetail(val uid: Int) : AppNavKey {
    override val route = "app_rule_detail"
}

object NavRoutes {
    fun fromRoute(route: String?): AppNavKey =
        when (route) {
            Apps.route -> Apps
            Logs.route -> Logs
            Settings.route -> Settings
            Dns.route -> Dns
            Forwarding.route -> Forwarding
            Pro.route -> Pro
            else -> Home
        }
}
