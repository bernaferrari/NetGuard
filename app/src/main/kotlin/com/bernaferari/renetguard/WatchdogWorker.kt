package com.bernaferari.renetguard

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WatchdogWorker(
    appContext: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val intent =
            Intent(applicationContext, ServiceSinkhole::class.java).apply {
                action = ServiceSinkhole.ACTION_WATCHDOG
            }
        ContextCompat.startForegroundService(applicationContext, intent)
        return Result.success()
    }
}
