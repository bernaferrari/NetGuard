package eu.faircode.netguard

import android.content.Context
import android.content.res.TypedArray
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import java.net.InetAddress
import java.net.UnknownHostException
import java.text.SimpleDateFormat

class AdapterAccess(context: Context, cursor: Cursor) : CursorAdapter(context, cursor, 0) {
    private val colVersion = cursor.getColumnIndex("version")
    private val colProtocol = cursor.getColumnIndex("protocol")
    private val colDaddr = cursor.getColumnIndex("daddr")
    private val colDPort = cursor.getColumnIndex("dport")
    private val colTime = cursor.getColumnIndex("time")
    private val colAllowed = cursor.getColumnIndex("allowed")
    private val colBlock = cursor.getColumnIndex("block")
    private val colCount = cursor.getColumnIndex("count")
    private val colSent = cursor.getColumnIndex("sent")
    private val colReceived = cursor.getColumnIndex("received")
    private val colConnections = cursor.getColumnIndex("connections")

    private val colorText: Int
    private val colorOn: Int
    private val colorOff: Int

    init {
        val ta: TypedArray = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorSecondary))
        colorText =
            try {
                ta.getColor(0, 0)
            } finally {
                ta.recycle()
            }

        val tv = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOn, tv, true)
        colorOn = tv.data
        context.theme.resolveAttribute(R.attr.colorOff, tv, true)
        colorOff = tv.data
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.access, parent, false)
    }

    @Suppress("DEPRECATION")
    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val version = cursor.getInt(colVersion)
        val protocol = cursor.getInt(colProtocol)
        val daddr = cursor.getString(colDaddr)
        val dport = cursor.getInt(colDPort)
        val time = cursor.getLong(colTime)
        val allowed = cursor.getInt(colAllowed)
        val block = cursor.getInt(colBlock)
        val count = cursor.getInt(colCount)
        val sent = if (cursor.isNull(colSent)) -1 else cursor.getLong(colSent)
        val received = if (cursor.isNull(colReceived)) -1 else cursor.getLong(colReceived)
        val connections = if (cursor.isNull(colConnections)) -1 else cursor.getInt(colConnections)

        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val ivBlock = view.findViewById<ImageView>(R.id.ivBlock)
        val tvDest = view.findViewById<TextView>(R.id.tvDest)
        val llTraffic = view.findViewById<LinearLayout>(R.id.llTraffic)
        val tvConnections = view.findViewById<TextView>(R.id.tvConnections)
        val tvTraffic = view.findViewById<TextView>(R.id.tvTraffic)

        tvTime.text = SimpleDateFormat("dd HH:mm").format(time)
        if (block < 0) {
            ivBlock.setImageDrawable(null)
        } else {
            ivBlock.setImageResource(if (block > 0) R.drawable.host_blocked else R.drawable.host_allowed)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val wrap = DrawableCompat.wrap(ivBlock.drawable)
                DrawableCompat.setTint(wrap, if (block > 0) colorOff else colorOn)
            }
        }

        val dest = Util.getProtocolName(protocol, version, true) +
            " " + daddr + (if (dport > 0) "/$dport" else "") + (if (count > 1) " ?$count" else "")
        val span = SpannableString(dest)
        span.setSpan(UnderlineSpan(), 0, dest.length, 0)
        tvDest.text = span

        if (Util.isNumericAddress(daddr)) {
            object : AsyncTask<String, Any, String>() {
                override fun onPreExecute() {
                    ViewCompat.setHasTransientState(tvDest, true)
                }

                override fun doInBackground(vararg args: String): String {
                    return try {
                        InetAddress.getByName(args[0]).hostName
                    } catch (ignored: UnknownHostException) {
                        args[0]
                    }
                }

                override fun onPostExecute(addr: String) {
                    tvDest.text =
                        Util.getProtocolName(protocol, version, true) +
                            " >" + addr + (if (dport > 0) "/$dport" else "")
                    ViewCompat.setHasTransientState(tvDest, false)
                }
            }.execute(daddr)
        }

        tvDest.setTextColor(
            when {
                allowed < 0 -> colorText
                allowed > 0 -> colorOn
                else -> colorOff
            },
        )

        llTraffic.visibility =
            if (connections > 0 || sent > 0 || received > 0) View.VISIBLE else View.GONE
        if (connections > 0) {
            tvConnections.text = context.getString(R.string.msg_count, connections)
        }

        tvTraffic.text =
            if (sent > 1024 * 1204 * 1024L || received > 1024 * 1024 * 1024L) {
                context.getString(
                    R.string.msg_gb,
                    if (sent > 0) sent / (1024 * 1024 * 1024f) else 0f,
                    if (received > 0) received / (1024 * 1024 * 1024f) else 0f,
                )
            } else if (sent > 1204 * 1024L || received > 1024 * 1024L) {
                context.getString(
                    R.string.msg_mb,
                    if (sent > 0) sent / (1024 * 1024f) else 0f,
                    if (received > 0) received / (1024 * 1024f) else 0f,
                )
            } else {
                context.getString(
                    R.string.msg_kb,
                    if (sent > 0) sent / 1024f else 0f,
                    if (received > 0) received / 1024f else 0f,
                )
            }
    }
}
