package eu.faircode.netguard

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.preference.PreferenceManager

class WidgetLockdown : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(appWidgetIds, appWidgetManager, context)
    }

    companion object {
        private const val TAG = "NetGuard.WidgetLock"

        private fun update(appWidgetIds: IntArray, appWidgetManager: AppWidgetManager, context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val lockdown = prefs.getBoolean("lockdown", false)
            try {
                val intent = Intent(if (lockdown) WidgetAdmin.INTENT_LOCKDOWN_OFF else WidgetAdmin.INTENT_LOCKDOWN_ON)
                intent.setPackage(context.packageName)
                val pi = PendingIntentCompat.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widgetlockdown)
                    views.setOnClickPendingIntent(R.id.ivEnabled, pi)
                    views.setImageViewResource(
                        R.id.ivEnabled,
                        if (lockdown) R.drawable.ic_lock_outline_white_24dp else R.drawable.ic_lock_open_white_24dp,
                    )
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, WidgetLockdown::class.java))
            update(appWidgetIds, appWidgetManager, context)
        }
    }
}
