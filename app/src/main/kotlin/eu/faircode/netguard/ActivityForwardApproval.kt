package eu.faircode.netguard

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.faircode.netguard.ui.theme.NetGuardThemeFromPrefs
import java.net.InetAddress

class ActivityForwardApproval : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val protocol = intent.getIntExtra("protocol", 0)
        val dport = intent.getIntExtra("dport", 0)
        val addr = intent.getStringExtra("raddr")
        val rport = intent.getIntExtra("rport", 0)
        val ruid = intent.getIntExtra("ruid", 0)
        val raddr = addr ?: "127.0.0.1"

        try {
            val iraddr = InetAddress.getByName(raddr)
            if (rport < 1024 && (iraddr.isLoopbackAddress || iraddr.isAnyLocalAddress)) {
                throw IllegalArgumentException("Port forwarding to privileged port on local address not possible")
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            finish()
            return
        }

        val pname =
            when (protocol) {
                6 -> getString(R.string.menu_protocol_tcp)
                17 -> getString(R.string.menu_protocol_udp)
                else -> protocol.toString()
            }

        setContent {
            NetGuardThemeFromPrefs {
                ForwardApprovalContent(
                    text =
                        if (ACTION_START_PORT_FORWARD == intent.action) {
                            getString(
                                R.string.msg_start_forward,
                                pname,
                                dport,
                                raddr,
                                rport,
                                TextUtils.join(", ", Util.getApplicationNames(ruid, this)),
                            )
                        } else {
                            getString(R.string.msg_stop_forward, pname, dport)
                        },
                    onApprove = {
                        if (ACTION_START_PORT_FORWARD == intent.action) {
                            Log.i(TAG, "Start forwarding protocol $protocol port $dport to $raddr/$rport uid $ruid")
                            val dh = DatabaseHelper.getInstance(this)
                            dh.deleteForward(protocol, dport)
                            dh.addForward(protocol, dport, raddr, rport, ruid)
                        } else if (ACTION_STOP_PORT_FORWARD == intent.action) {
                            Log.i(TAG, "Stop forwarding protocol $protocol port $dport")
                            DatabaseHelper.getInstance(this).deleteForward(protocol, dport)
                        }

                        ServiceSinkhole.reload("forwarding", this, false)
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }

    companion object {
        private const val TAG = "NetGuard.Forward"
        private const val ACTION_START_PORT_FORWARD = "eu.faircode.netguard.START_PORT_FORWARD"
        private const val ACTION_STOP_PORT_FORWARD = "eu.faircode.netguard.STOP_PORT_FORWARD"

        init {
            try {
                System.loadLibrary("netguard")
            } catch (ignored: UnsatisfiedLinkError) {
                System.exit(1)
            }
        }
    }
}

@Composable
private fun ForwardApprovalContent(
    text: String,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.menu_cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onApprove) {
                Text(text = stringResource(android.R.string.ok))
            }
        }
    }
}
