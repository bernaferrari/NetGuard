package com.bernaferari.renetguard

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object PendingIntentCompat {
    fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || flags and PendingIntent.FLAG_MUTABLE != 0) {
            PendingIntent.getActivity(context, requestCode, intent, flags)
        } else {
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    fun getService(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || flags and PendingIntent.FLAG_MUTABLE != 0) {
            PendingIntent.getService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(
                context,
                requestCode,
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    fun getForegroundService(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): PendingIntent {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || flags and PendingIntent.FLAG_MUTABLE != 0) {
            PendingIntent.getService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    fun getBroadcast(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int
    ): PendingIntent {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || flags and PendingIntent.FLAG_MUTABLE != 0) {
            PendingIntent.getBroadcast(context, requestCode, intent, flags)
        } else {
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
