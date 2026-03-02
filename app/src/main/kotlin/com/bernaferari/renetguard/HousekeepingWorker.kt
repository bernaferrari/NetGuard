package com.bernaferari.renetguard

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HousekeepingWorker(
    appContext: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val intent =
            Intent(applicationContext, ServiceSinkhole::class.java).apply {
                action = ServiceSinkhole.ACTION_HOUSE_HOLDING
            }
        ContextCompat.startForegroundService(applicationContext, intent)
        return Result.success()
    }
}
