package eu.faircode.netguard

import android.app.IntentService
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
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

@Suppress("DEPRECATION")
class ServiceExternal : IntentService(TAG) {
    override fun onHandleIntent(intent: Intent?) {
        try {
            startForeground(ServiceSinkhole.NOTIFY_EXTERNAL, getForegroundNotification(this))

            Log.i(TAG, "Received $intent")
            Util.logExtras(intent)

            if (ACTION_DOWNLOAD_HOSTS_FILE == intent?.action) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)

                var hostsUrl = prefs.getString("hosts_url", null)
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
                    prefs.edit().putString("hosts_last_download", last).apply()

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
        } finally {
            stopForeground(true)
        }
    }

    companion object {
        private const val TAG = "NetGuard.External"
        private const val ACTION_DOWNLOAD_HOSTS_FILE = "eu.faircode.netguard.DOWNLOAD_HOSTS_FILE"

        private fun getForegroundNotification(context: Context): Notification {
            val builder = NotificationCompat.Builder(context, "foreground")
            builder.setSmallIcon(R.drawable.ic_hourglass_empty_white_24dp)
            builder.priority = NotificationCompat.PRIORITY_MIN
            builder.setCategory(NotificationCompat.CATEGORY_STATUS)
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.setContentTitle(context.getString(R.string.app_name))
            return builder.build()
        }
    }
}
