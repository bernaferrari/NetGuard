package com.bernaferari.renetguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

object Notifications {
    const val CHANNEL_FOREGROUND = "foreground"
    const val CHANNEL_NOTIFY = "notify"
    const val CHANNEL_ACCESS = "access"
    const val CHANNEL_MALWARE = "malware"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val foreground = NotificationChannel(
            CHANNEL_FOREGROUND,
            context.getString(R.string.channel_foreground),
            NotificationManager.IMPORTANCE_MIN,
        )
        foreground.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        nm.createNotificationChannel(foreground)

        val notify = NotificationChannel(
            CHANNEL_NOTIFY,
            context.getString(R.string.channel_notify),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notify.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        notify.setBypassDnd(true)
        nm.createNotificationChannel(notify)

        val access = NotificationChannel(
            CHANNEL_ACCESS,
            context.getString(R.string.channel_access),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        access.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        access.setBypassDnd(true)
        nm.createNotificationChannel(access)

        val malware = NotificationChannel(
            CHANNEL_MALWARE,
            context.getString(R.string.setting_malware),
            NotificationManager.IMPORTANCE_HIGH,
        )
        malware.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        malware.setBypassDnd(true)
        nm.createNotificationChannel(malware)
    }
}

private const val NOTIFICATION_ICON_SIZE_DP = 24
private val materialNotificationIconCache = ConcurrentHashMap<String, Bitmap>()

fun Context.securityIcon(): Icon = notificationIcon(Icons.Default.Security)

fun Context.filterListIcon(): Icon = notificationIcon(Icons.Default.FilterList)

fun Context.equalizerIcon(): Icon = notificationIcon(Icons.Default.Equalizer)

fun Context.hourglassIcon(): Icon = notificationIcon(Icons.Default.HourglassEmpty)

fun Context.cloudUploadIcon(): Icon = notificationIcon(Icons.Default.CloudUpload)

fun Context.fileDownloadIcon(): Icon = notificationIcon(Icons.Default.FileDownload)

fun Context.lockIcon(): Icon = notificationIcon(Icons.Default.Lock)

fun Context.errorIcon(): Icon = notificationIcon(Icons.Default.Error)

fun Context.wifiIcon(enabled: Boolean): Icon =
    notificationIcon(if (enabled) Icons.Default.Wifi else Icons.Default.WifiOff)

fun Context.cellularIcon(enabled: Boolean): Icon =
    notificationIcon(if (enabled) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularOff)

private fun Context.notificationIcon(imageVector: ImageVector): Icon {
    val key = imageVector.name + "@" + resources.displayMetrics.densityDpi
    val bitmap = materialNotificationIconCache.getOrPut(key) {
        materialBitmap(imageVector)
    }
    return Icon.createWithBitmap(bitmap)
}

private fun Context.materialBitmap(imageVector: ImageVector): Bitmap {
    val size = max(1, (NOTIFICATION_ICON_SIZE_DP * resources.displayMetrics.density).roundToInt())
    val bitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888)

    if (imageVector.viewportWidth <= 0f || imageVector.viewportHeight <= 0f || size <= 0) {
        return bitmap
    }

    val viewportScaleX = size.toFloat() / imageVector.viewportWidth
    val viewportScaleY = size.toFloat() / imageVector.viewportHeight

    val canvas = Canvas(bitmap)
    bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    return try {
        canvas.save()
        canvas.scale(viewportScaleX, viewportScaleY)
        drawVectorNode(canvas, imageVector.root, Color.White)
        canvas.restore()
        bitmap
    } catch (_: Throwable) {
        bitmap
    }
}

private fun drawVectorNode(canvas: Canvas, node: VectorNode, tint: Color) {
    when (node) {
        is VectorGroup -> drawVectorGroup(canvas, node, tint)
        is VectorPath -> drawVectorPath(canvas, node, tint)
    }
}

private fun drawVectorGroup(
    canvas: Canvas,
    group: VectorGroup,
    tint: Color,
) {
    canvas.save()
    if (
        group.rotation != 0f ||
        group.pivotX != 0f ||
        group.pivotY != 0f ||
        group.scaleX != 1f ||
        group.scaleY != 1f ||
        group.translationX != 0f ||
        group.translationY != 0f
    ) {
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
    }

    val clipNodes: List<PathNode> = group.clipPathData
    if (clipNodes.isNotEmpty()) {
        val clipPath = androidx.compose.ui.graphics.Path().apply {
            fillType = PathFillType.NonZero
            clipNodes.toPath(this)
        }
        canvas.clipPath(clipPath.asAndroidPath())
    }

    repeat(group.size) { index ->
        drawVectorNode(canvas, group[index], tint)
    }

    canvas.restore()
}

private fun drawVectorPath(
    canvas: Canvas,
    pathNode: VectorPath,
    tint: Color,
) {
    drawVectorShape(canvas, pathNode, tint)
}

private fun drawVectorShape(
    canvas: Canvas,
    shape: VectorPath,
    tint: Color,
) {
    val composePath =
        androidx.compose.ui.graphics.Path().apply {
            fillType = shape.pathFillType
            shape.pathData.toPath(this)
        }

    val androidPath = composePath.asAndroidPath()

    createPaint(shape.fill, shape.fillAlpha, tint)
        ?.also { paint ->
            paint.style = Paint.Style.FILL
            paint.color = tintColorFromBrush(shape.fill, tint, shape.fillAlpha)
            canvas.drawPath(androidPath, paint)
        }

    createPaint(shape.stroke, shape.strokeAlpha, tint)
        ?.apply {
            style = Paint.Style.STROKE
            strokeWidth = shape.strokeLineWidth
            strokeCap = when {
                shape.strokeLineCap == StrokeCap.Butt -> Paint.Cap.BUTT
                shape.strokeLineCap == StrokeCap.Round -> Paint.Cap.ROUND
                shape.strokeLineCap == StrokeCap.Square -> Paint.Cap.SQUARE
                else -> Paint.Cap.BUTT
            }
            strokeJoin = when {
                shape.strokeLineJoin == StrokeJoin.Miter -> Paint.Join.MITER
                shape.strokeLineJoin == StrokeJoin.Round -> Paint.Join.ROUND
                shape.strokeLineJoin == StrokeJoin.Bevel -> Paint.Join.BEVEL
                else -> Paint.Join.MITER
            }
            if (shape.strokeLineWidth <= 0f) {
                return@apply
            }
            strokeMiter = shape.strokeLineMiter
            color = tintColorFromBrush(shape.stroke, tint, shape.strokeAlpha)
            canvas.drawPath(androidPath, this)
        }
}

private fun createPaint(
    brush: Brush?,
    alpha: Float,
    tint: Color,
): Paint? {
    if (alpha <= 0f) {
        return null
    }

    val paintColor = tintColorFromBrush(brush, tint, alpha)
    if (paintColor == 0) {
        return null
    }

    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = paintColor
        isAntiAlias = true
        isDither = true
    }
}

private fun tintColorFromBrush(
    brush: Brush?,
    tint: Color,
    opacity: Float,
): Int {
    if (opacity <= 0f) {
        return 0
    }

    val brushAlpha =
        when (brush) {
            is SolidColor -> brush.value.alpha
            else -> tint.alpha
        }

    return if (brushAlpha <= 0f || opacity <= 0f) {
        0
    } else {
        tint.copy(alpha = tint.alpha * brushAlpha * opacity).toArgb()
    }
}
