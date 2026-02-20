package eu.faircode.netguard

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import eu.faircode.netguard.data.Prefs
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ServiceExternal : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ServiceSinkhole.NOTIFY_EXTERNAL, getForegroundNotification(this))
        job?.cancel()
        job =
            scope.launch {
                try {
                    handleIntent(intent)
                } finally {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        if (ACTION_DOWNLOAD_HOSTS_FILE == intent?.action) {
            var hostsUrl = Prefs.getString("hosts_url", null)
            if ("https://www.netguard.me/hosts" == hostsUrl) {
                hostsUrl = BuildConfig.HOSTS_FILE_URI
            }

            val tmp = File(filesDir, "hosts.tmp")
            val hosts = File(filesDir, "hosts.txt")

            var input: InputStream? = null
            var output: OutputStream? = null
            var connection: URLConnection? = null
            try {
                val url = URL(hostsUrl)
                connection = url.openConnection()
                connection.connect()

                if (connection is HttpURLConnection) {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IOException("${connection.responseCode} ${connection.responseMessage}")
                    }
                }

                val contentLength = connection.contentLength
                Log.i(TAG, "Content length=$contentLength")
                input = connection.getInputStream()
                output = FileOutputStream(tmp)

                var size = 0L
                val buffer = ByteArray(4096)
                var bytes = input.read(buffer)
                while (bytes != -1) {
                    output.write(buffer, 0, bytes)
                    size += bytes
                    bytes = input.read(buffer)
                }

                Log.i(TAG, "Downloaded size=$size")

                if (hosts.exists()) hosts.delete()
                tmp.renameTo(hosts)

                val last = SimpleDateFormat.getDateTimeInstance().format(Date().time)
                Prefs.putString("hosts_last_download", last)

                ServiceSinkhole.reload("hosts file download", this, false)
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                if (tmp.exists()) tmp.delete()
            } finally {
                try {
                    output?.close()
                } catch (ex: IOException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
                try {
                    input?.close()
                } catch (ex: IOException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
                if (connection is HttpURLConnection) {
                    connection.disconnect()
                }
            }
        }
    }

    companion object {
        private const val TAG = "NetGuard.External"
        const val ACTION_DOWNLOAD_HOSTS_FILE = "eu.faircode.netguard.DOWNLOAD_HOSTS_FILE"

        private fun getForegroundNotification(context: Context): Notification {
            val builder = Notification.Builder(context, Notifications.CHANNEL_FOREGROUND)
            builder.setSmallIcon(context.hourglassIcon())
            builder.setPriority(Notification.PRIORITY_MIN)
            builder.setCategory(Notification.CATEGORY_STATUS)
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
            builder.setContentTitle(context.getString(R.string.app_name))
            return builder.build()
        }
    }
}
