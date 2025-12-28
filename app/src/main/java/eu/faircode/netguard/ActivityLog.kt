package eu.faircode.netguard

import android.app.Activity
import android.os.Bundle
import eu.faircode.netguard.ui.Logs

class ActivityLog : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(ActivityMain.createRouteIntent(this, Logs.route))
        finish()
    }
}
