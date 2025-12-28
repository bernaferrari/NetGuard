package eu.faircode.netguard

import android.app.Activity
import android.os.Bundle
import eu.faircode.netguard.ui.Forwarding

class ActivityForwarding : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(ActivityMain.createRouteIntent(this, Forwarding.route))
        finish()
    }
}
