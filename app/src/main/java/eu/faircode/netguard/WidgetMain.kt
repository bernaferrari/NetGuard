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

class WidgetMain : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(appWidgetIds, appWidgetManager, context)
    }

    companion object {
        private const val TAG = "NetGuard.Widget"

        private fun update(appWidgetIds: IntArray, appWidgetManager: AppWidgetManager, context: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val enabled = prefs.getBoolean("enabled", false)
            try {
                val intent = Intent(if (enabled) WidgetAdmin.INTENT_OFF else WidgetAdmin.INTENT_ON)
                intent.setPackage(context.packageName)
                val pi = PendingIntentCompat.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widgetmain)
                    views.setOnClickPendingIntent(R.id.ivEnabled, pi)
                    views.setImageViewResource(
                        R.id.ivEnabled,
                        if (enabled) R.drawable.ic_security_color_24dp else R.drawable.ic_security_white_24dp_60,
                    )
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, WidgetMain::class.java))
            update(appWidgetIds, appWidgetManager, context)
        }
    }
}
