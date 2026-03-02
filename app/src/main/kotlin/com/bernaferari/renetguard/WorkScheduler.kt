package com.bernaferari.renetguard

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_WATCHDOG = "netguard_watchdog"
    private const val WORK_HOUSEKEEPING = "netguard_housekeeping"

    fun scheduleWatchdog(context: Context, minutes: Int, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled || minutes <= 0) {
            wm.cancelUniqueWork(WORK_WATCHDOG)
            return
        }

        val interval = maxOf(minutes.toLong(), 15L)
        val request =
            PeriodicWorkRequestBuilder<WatchdogWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()

        wm.enqueueUniquePeriodicWork(WORK_WATCHDOG, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleHousekeeping(context: Context) {
        val wm = WorkManager.getInstance(context)
        val request =
            PeriodicWorkRequestBuilder<HousekeepingWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.NONE)
                .build()

        wm.enqueueUniquePeriodicWork(WORK_HOUSEKEEPING, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
