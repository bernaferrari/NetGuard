package eu.faircode.netguard

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import eu.faircode.netguard.data.Prefs
import eu.faircode.netguard.ui.theme.themeOffColor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadTask(
    private val context: Activity,
    private val url: URL,
    private val file: File,
    private val listener: Listener,
) {
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    interface Listener {
        fun onCompleted()
        fun onCancelled()
        fun onException(ex: Throwable)
    }

    fun start() {
        if (job != null) return
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock?.acquire()
        showNotification(0)
        if (!Util.isPlayStoreInstall(context)) {
            Toast.makeText(context, context.getString(R.string.msg_downloading, url.toString()), Toast.LENGTH_SHORT).show()
        }

        job = scope.launch {
            try {
                download()
                finishSuccess()
            } catch (ex: CancellationException) {
                finishCancelled()
            } catch (ex: Throwable) {
                finishException(ex)
            } finally {
                job = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun download() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Downloading $url into $file")

        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: URLConnection? = null
        try {
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
            output = FileOutputStream(file)

            var size = 0L
            val buffer = ByteArray(4096)
            var bytes = input.read(buffer)
            while (bytes != -1 && currentCoroutineContext().isActive) {
                output.write(buffer, 0, bytes)
                size += bytes
                if (contentLength > 0) {
                    val progress = (size * 100 / contentLength).toInt()
                    withContext(Dispatchers.Main.immediate) {
                        showNotification(progress)
                    }
                }
                bytes = input.read(buffer)
            }
            if (!currentCoroutineContext().isActive) {
                throw CancellationException()
            }
            Log.i(TAG, "Downloaded size=$size")
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

    private fun finishSuccess() {
        wakeLock?.release()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ServiceSinkhole.NOTIFY_DOWNLOAD)
        listener.onCompleted()
    }

    private fun finishCancelled() {
        Log.i(TAG, "Cancelled")
        wakeLock?.release()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ServiceSinkhole.NOTIFY_DOWNLOAD)
        listener.onCancelled()
    }

    private fun finishException(ex: Throwable) {
        wakeLock?.release()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ServiceSinkhole.NOTIFY_DOWNLOAD)
        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        listener.onException(ex)
    }

    private fun showNotification(progress: Int) {
        val main = Intent(context, ActivitySettings::class.java)
        val pi = PendingIntentCompat.getActivity(
            context,
            ServiceSinkhole.NOTIFY_DOWNLOAD,
            main,
            PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificationColor = themeOffColor(Prefs.getString("theme", eu.faircode.netguard.ui.theme.THEME_DEFAULT))
        val builder = Notification.Builder(context, Notifications.CHANNEL_NOTIFY)
            .setSmallIcon(context.fileDownloadIcon())
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.msg_downloading, url.toString()))
            .setContentIntent(pi)
            .setProgress(100, progress, false)
            .setColor(notificationColor)
            .setOngoing(true)
            .setAutoCancel(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        if (Util.canNotify(context)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(ServiceSinkhole.NOTIFY_DOWNLOAD, builder.build())
        }
    }

    companion object {
        private const val TAG = "NetGuard.Download"
    }
}
