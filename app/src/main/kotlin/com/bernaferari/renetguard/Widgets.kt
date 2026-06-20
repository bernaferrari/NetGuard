package com.bernaferari.renetguard

import android.content.Context
import androidx.glance.appwidget.updateAll

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

}
