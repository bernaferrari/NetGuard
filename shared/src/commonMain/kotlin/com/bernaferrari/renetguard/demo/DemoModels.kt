package com.bernaferrari.renetguard.demo

enum class DemoTab {
    Home,
    Apps,
    Logs,
    Settings,
}

enum class AppsFilter {
    All,
    Blocked,
    Allowed,
}

enum class DemoTheme {
    Teal,
    Blue,
    Purple,
}

enum class LogProtocol {
    TCP,
    UDP,
}

data class AppRule(
    val id: String,
    val name: String,
    val packageName: String,
    val iconColor: String,
    val iconGlyph: String,
    val wifiBlocked: Boolean,
    val mobileBlocked: Boolean,
    val system: Boolean = false,
)

data class LogEntry(
    val id: String,
    val appId: String,
    val appName: String,
    val protocol: LogProtocol,
    val destination: String,
    val port: Int,
    val allowed: Boolean,
    val time: String,
)

data class DemoSettings(
    val darkMode: Boolean = false,
    val theme: DemoTheme = DemoTheme.Teal,
    val filterEnabled: Boolean = false,
    val lockdown: Boolean = false,
    val logTraffic: Boolean = true,
)

data class DemoState(
    val firewallEnabled: Boolean = true,
    val tab: DemoTab = DemoTab.Home,
    val appsFilter: AppsFilter = AppsFilter.All,
    val searchQuery: String = "",
    val selectedAppId: String? = null,
    val apps: List<AppRule> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val settings: DemoSettings = DemoSettings(),
)

data class DemoStats(
    val blocked: Int,
    val allowed: Int,
    val total: Int,
)