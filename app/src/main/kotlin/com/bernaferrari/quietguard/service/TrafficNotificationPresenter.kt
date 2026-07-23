package com.bernaferrari.quietguard.service

import com.bernaferrari.quietguard.*

import com.bernaferrari.quietguard.shared.R

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.ui.theme.THEME_DEFAULT
import com.bernaferrari.quietguard.ui.theme.themePrimaryColor

internal data class TrafficNotificationContent(
    val whenMs: Long,
    val graph: Bitmap,
    val summary: String,
    val extraLines: List<String>,
)

/** Renders traffic statistics notifications independently from sampling and service state transitions. */
internal class TrafficNotificationPresenter(
    private val context: Context,
    private val prefs: PreferencesRepository,
) {
    fun build(content: TrafficNotificationContent): Notification {
        val pendingIntent = PendingIntentCompat.getActivity(
            context,
            0,
            Intent(context, ActivityMain::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val headline = content.extraLines.firstOrNull() ?: content.summary
        return Notification.Builder(context, Notifications.CHANNEL_NOTIFY)
            .setWhen(content.whenMs)
            .setSmallIcon(context.equalizerIcon())
            .setContentTitle(context.getString(R.string.notify_traffic_title))
            .setContentText(headline)
            .setSubText(content.summary.takeIf { content.extraLines.isNotEmpty() })
            .setContentIntent(pendingIntent)
            .setColor(themePrimaryColor(prefs.getString("theme", THEME_DEFAULT)))
            .setOngoing(true)
            .setAutoCancel(false)
            .setLargeIcon(content.graph)
            .setStyle(
                Notification.BigPictureStyle()
                    .bigPicture(content.graph)
                    .bigLargeIcon(null as Bitmap?)
                    .setBigContentTitle(context.getString(R.string.notify_traffic_title))
                    .setSummaryText(content.summary),
            )
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }
}
