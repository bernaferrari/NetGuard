package com.bernaferrari.renetguard.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DemoStateHolder(
    initialState: DemoState = createDefaultState(),
) {
    var state by mutableStateOf(initialState)
        private set

    val stats: DemoStats
        get() {
            val blocked = state.apps.count(::isBlocked)
            val total = state.apps.size
            return DemoStats(
                blocked = blocked,
                allowed = total - blocked,
                total = total,
            )
        }

    val filteredApps: List<AppRule>
        get() {
            var list = state.apps
            list = when (state.appsFilter) {
                AppsFilter.Blocked -> list.filter(::isBlocked)
                AppsFilter.Allowed -> list.filter { !isBlocked(it) }
                AppsFilter.All -> list
            }
            val query = state.searchQuery.trim().lowercase()
            if (query.isNotEmpty()) {
                list = list.filter { app ->
                    app.name.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
                }
            }
            return list.sortedBy { it.name }
        }

    val selectedApp: AppRule?
        get() = state.selectedAppId?.let { id -> state.apps.find { it.id == id } }

    fun setTab(tab: DemoTab) {
        state = state.copy(
            tab = tab,
            selectedAppId = if (tab == DemoTab.Apps) state.selectedAppId else null,
        )
    }

    fun setFirewall(enabled: Boolean) {
        state = state.copy(firewallEnabled = enabled)
    }

    fun setFilter(filter: AppsFilter) {
        state = state.copy(appsFilter = filter)
    }

    fun setSearch(query: String) {
        state = state.copy(searchQuery = query)
    }

    fun selectApp(appId: String?) {
        state = state.copy(selectedAppId = appId)
    }

    fun toggleWifi(appId: String) {
        state = state.copy(
            apps = state.apps.map { app ->
                if (app.id == appId) app.copy(wifiBlocked = !app.wifiBlocked) else app
            },
        )
    }

    fun toggleMobile(appId: String) {
        state = state.copy(
            apps = state.apps.map { app ->
                if (app.id == appId) app.copy(mobileBlocked = !app.mobileBlocked) else app
            },
        )
    }

    fun updateSetting(key: DemoSettingKey, value: Boolean) {
        val settings = when (key) {
            DemoSettingKey.DarkMode -> state.settings.copy(darkMode = value)
            DemoSettingKey.FilterEnabled -> state.settings.copy(filterEnabled = value)
            DemoSettingKey.Lockdown -> state.settings.copy(lockdown = value)
            DemoSettingKey.LogTraffic -> state.settings.copy(logTraffic = value)
            DemoSettingKey.Theme -> return
        }
        state = state.copy(settings = settings)
    }

    fun updateSetting(key: DemoSettingKey, value: DemoTheme) {
        if (key != DemoSettingKey.Theme) return
        state = state.copy(settings = state.settings.copy(theme = value))
    }

    private fun isBlocked(app: AppRule): Boolean = app.wifiBlocked || app.mobileBlocked
}

enum class DemoSettingKey {
    DarkMode,
    Theme,
    FilterEnabled,
    Lockdown,
    LogTraffic,
}