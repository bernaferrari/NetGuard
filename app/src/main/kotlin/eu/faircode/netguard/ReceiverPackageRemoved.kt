package eu.faircode.netguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.util.Log

class ReceiverPackageRemoved : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        val action = intent?.action
        if (Intent.ACTION_PACKAGE_FULLY_REMOVED == action) {
            val uid = intent.getIntExtra(Intent.EXTRA_UID, 0)
            if (uid > 0) {
                val dh = DatabaseHelper.getInstance(context)
                dh.clearLog(uid)
                dh.clearAccess(uid, false)

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(uid)
                nm.cancel(uid + 10000)
            }
        }
    }

    companion object {
        private const val TAG = "NetGuard.Receiver"
    }
}
