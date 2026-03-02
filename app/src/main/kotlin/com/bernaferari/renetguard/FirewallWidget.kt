package com.bernaferari.renetguard

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import com.bernaferari.renetguard.data.Prefs

class FirewallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            FirewallWidgetContent()
        }
    }
}

@Composable
private fun FirewallWidgetContent() {
    val context = LocalContext.current
    val enabled = Prefs.getBoolean("enabled", false)
    val label =
        if (enabled) {
            context.getString(R.string.widget_firewall_enabled)
        } else {
            context.getString(R.string.widget_firewall_disabled)
        }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionRunCallback<ToggleFirewallAction>()),
    ) {
        Text(
            text = label,
            style = TextStyle(),
        )
    }
}

class FirewallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FirewallWidget()
}

class ToggleFirewallAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val enabled = Prefs.getBoolean("enabled", false)
        Prefs.putBoolean("enabled", !enabled)
        if (enabled) {
            ServiceSinkhole.stop("widget", context, false)
        } else {
            ServiceSinkhole.start("widget", context)
        }
        Widgets.updateFirewall(context)
    }
}
