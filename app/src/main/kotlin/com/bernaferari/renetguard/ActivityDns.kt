package com.bernaferari.renetguard

import android.app.Activity
import android.os.Bundle
import com.bernaferari.renetguard.ui.Dns

class ActivityDns : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(ActivityMain.createRouteIntent(this, Dns.route))
        finish()
    }
}
