package eu.faircode.netguard

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

@Suppress("DEPRECATION")
class DownloadTask(
    private val context: Activity,
    private val url: URL,
    private val file: File,
    private val listener: Listener,
) : AsyncTask<Any, Int, Any?>() {
    private var wakeLock: PowerManager.WakeLock? = null

    interface Listener {
        fun onCompleted()
        fun onCancelled()
        fun onException(ex: Throwable)
    }

    override fun onPreExecute() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock?.acquire()
        showNotification(0)
        if (!Util.isPlayStoreInstall(context)) {
            Toast.makeText(context, context.getString(R.string.msg_downloading, url.toString()), Toast.LENGTH_SHORT).show()
        }
    }

    override fun doInBackground(vararg args: Any): Any? {
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
            while (!isCancelled && bytes != -1) {
                output.write(buffer, 0, bytes)
                size += bytes
                if (contentLength > 0) {
                    publishProgress((size * 100 / contentLength).toInt())
                }
                bytes = input.read(buffer)
            }
            Log.i(TAG, "Downloaded size=$size")
            return null
        } catch (ex: Throwable) {
            return ex
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

    override fun onProgressUpdate(vararg values: Int?) {
        showNotification(values.firstOrNull() ?: 0)
    }

    override fun onCancelled() {
        super.onCancelled()
        Log.i(TAG, "Cancelled")
        listener.onCancelled()
    }

    override fun onPostExecute(result: Any?) {
        wakeLock?.release()
        NotificationManagerCompat.from(context).cancel(ServiceSinkhole.NOTIFY_DOWNLOAD)
        if (result is Throwable) {
            Log.e(TAG, result.toString() + "\n" + Log.getStackTraceString(result))
            listener.onException(result)
        } else {
            listener.onCompleted()
        }
    }

    private fun showNotification(progress: Int) {
        val main = Intent(context, ActivitySettings::class.java)
        val pi = PendingIntentCompat.getActivity(
            context,
            ServiceSinkhole.NOTIFY_DOWNLOAD,
            main,
            PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val tv = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOff, tv, true)
        val builder = NotificationCompat.Builder(context, "notify")
            .setSmallIcon(R.drawable.ic_file_download_white_24dp)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.msg_downloading, url.toString()))
            .setContentIntent(pi)
            .setProgress(100, progress, false)
            .setColor(tv.data)
            .setOngoing(true)
            .setAutoCancel(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        }

        if (Util.canNotify(context)) {
            NotificationManagerCompat.from(context).notify(ServiceSinkhole.NOTIFY_DOWNLOAD, builder.build())
        }
    }

    companion object {
        private const val TAG = "NetGuard.Download"
    }
}
