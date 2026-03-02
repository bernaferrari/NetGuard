package com.bernaferari.renetguard

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.bernaferari.renetguard.data.Prefs
import com.bernaferari.renetguard.ui.theme.themeOffColor
import com.bernaferari.renetguard.ui.theme.themeOnColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun updateAll(context: Context) {
        scope.launch {
            FirewallWidget().updateAll(context)
            LockdownWidget().updateAll(context)
        }
    }

    fun updateFirewall(context: Context) {
        scope.launch {
            FirewallWidget().updateAll(context)
        }
    }

    fun updateLockdown(context: Context) {
        scope.launch {
            LockdownWidget().updateAll(context)
        }
    }

    fun themeColorRes(enabled: Boolean): Int {
        val theme = Prefs.getString("theme", "teal")
        return if (enabled) themeOnColor(theme) else themeOffColor(theme)
    }
}
