package eu.faircode.netguard

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import eu.faircode.netguard.data.Prefs

class LockdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LockdownWidgetContent()
        }
    }
}

@Composable
private fun LockdownWidgetContent() {
    val context = LocalContext.current
    val lockdown = Prefs.getBoolean("lockdown", false)
    val label =
        if (lockdown) {
            context.getString(R.string.widget_lockdown_enabled)
        } else {
            context.getString(R.string.widget_lockdown_disabled)
        }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionRunCallback<ToggleLockdownAction>()),
    ) {
        Text(
            text = label,
            style = TextStyle(),
        )
    }
}

class LockdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LockdownWidget()
}

class ToggleLockdownAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val lockdown = Prefs.getBoolean("lockdown", false)
        Prefs.putBoolean("lockdown", !lockdown)
        ServiceSinkhole.reload("widget", context, false)
        Widgets.updateLockdown(context)
    }
}
