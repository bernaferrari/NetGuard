package eu.faircode.netguard

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceManager
import java.net.InetAddress
import java.net.UnknownHostException
import java.text.SimpleDateFormat

class AdapterLog(
    context: Context,
    cursor: Cursor,
    private var resolve: Boolean,
    private var organization: Boolean,
) : CursorAdapter(context, cursor, 0) {
    private val colTime = cursor.getColumnIndex("time")
    private val colVersion = cursor.getColumnIndex("version")
    private val colProtocol = cursor.getColumnIndex("protocol")
    private val colFlags = cursor.getColumnIndex("flags")
    private val colSAddr = cursor.getColumnIndex("saddr")
    private val colSPort = cursor.getColumnIndex("sport")
    private val colDAddr = cursor.getColumnIndex("daddr")
    private val colDPort = cursor.getColumnIndex("dport")
    private val colDName = cursor.getColumnIndex("dname")
    private val colUid = cursor.getColumnIndex("uid")
    private val colData = cursor.getColumnIndex("data")
    private val colAllowed = cursor.getColumnIndex("allowed")
    private val colConnection = cursor.getColumnIndex("connection")
    private val colInteractive = cursor.getColumnIndex("interactive")
    private val colorOn: Int
    private val colorOff: Int
    private val iconSize: Int
    private var dns1: InetAddress? = null
    private var dns2: InetAddress? = null
    private var vpn4: InetAddress? = null
    private var vpn6: InetAddress? = null

    init {
        val tv = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOn, tv, true)
        colorOn = tv.data
        context.theme.resolveAttribute(R.attr.colorOff, tv, true)
        colorOff = tv.data

        iconSize = Util.dips2pixels(24, context)

        try {
            val lstDns = ServiceSinkhole.getDns(context)
            dns1 = if (lstDns.isNotEmpty()) lstDns[0] else null
            dns2 = if (lstDns.size > 1) lstDns[1] else null
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            vpn4 = InetAddress.getByName(prefs.getString("vpn4", "10.1.10.1"))
            vpn6 = InetAddress.getByName(prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1"))
        } catch (ex: UnknownHostException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    fun setResolve(resolve: Boolean) {
        this.resolve = resolve
    }

    fun setOrganization(organization: Boolean) {
        this.organization = organization
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.log, parent, false)
    }

    @Suppress("DEPRECATION")
    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val time = cursor.getLong(colTime)
        val version = if (cursor.isNull(colVersion)) -1 else cursor.getInt(colVersion)
        val protocol = if (cursor.isNull(colProtocol)) -1 else cursor.getInt(colProtocol)
        val flags = cursor.getString(colFlags)
        val saddr = cursor.getString(colSAddr)
        val sport = if (cursor.isNull(colSPort)) -1 else cursor.getInt(colSPort)
        val daddr = cursor.getString(colDAddr)
        val dport = if (cursor.isNull(colDPort)) -1 else cursor.getInt(colDPort)
        val dname = if (cursor.isNull(colDName)) null else cursor.getString(colDName)
        var uid = if (cursor.isNull(colUid)) -1 else cursor.getInt(colUid)
        val data = cursor.getString(colData)
        val allowed = if (cursor.isNull(colAllowed)) -1 else cursor.getInt(colAllowed)
        val connection = if (cursor.isNull(colConnection)) -1 else cursor.getInt(colConnection)
        val interactive = if (cursor.isNull(colInteractive)) -1 else cursor.getInt(colInteractive)

        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvProtocol = view.findViewById<TextView>(R.id.tvProtocol)
        val tvFlags = view.findViewById<TextView>(R.id.tvFlags)
        val tvSAddr = view.findViewById<TextView>(R.id.tvSAddr)
        val tvSPort = view.findViewById<TextView>(R.id.tvSPort)
        val tvDaddr = view.findViewById<TextView>(R.id.tvDAddr)
        val tvDPort = view.findViewById<TextView>(R.id.tvDPort)
        val tvOrganization = view.findViewById<TextView>(R.id.tvOrganization)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
        val tvUid = view.findViewById<TextView>(R.id.tvUid)
        val tvData = view.findViewById<TextView>(R.id.tvData)
        val ivConnection = view.findViewById<ImageView>(R.id.ivConnection)
        val ivInteractive = view.findViewById<ImageView>(R.id.ivInteractive)

        tvTime.text = SimpleDateFormat("HH:mm:ss").format(time)

        if (connection <= 0) {
            ivConnection.setImageResource(if (allowed > 0) R.drawable.host_allowed else R.drawable.host_blocked)
        } else {
            if (allowed > 0) {
                ivConnection.setImageResource(if (connection == 1) R.drawable.wifi_on else R.drawable.other_on)
            } else {
                ivConnection.setImageResource(if (connection == 1) R.drawable.wifi_off else R.drawable.other_off)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(ivConnection.drawable)
            DrawableCompat.setTint(wrap, if (allowed > 0) colorOn else colorOff)
        }

        if (interactive <= 0) {
            ivInteractive.setImageDrawable(null)
        } else {
            ivInteractive.setImageResource(R.drawable.screen_on)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val wrap = DrawableCompat.wrap(ivInteractive.drawable)
                DrawableCompat.setTint(wrap, colorOn)
            }
        }

        tvProtocol.text = Util.getProtocolName(protocol, version, false)

        tvFlags.text = flags
        tvFlags.visibility = if (TextUtils.isEmpty(flags)) View.GONE else View.VISIBLE

        if (protocol == 6 || protocol == 17) {
            tvSPort.text = if (sport < 0) "" else getKnownPort(sport)
            tvDPort.text = if (dport < 0) "" else getKnownPort(dport)
        } else {
            tvSPort.text = if (sport < 0) "" else sport.toString()
            tvDPort.text = if (dport < 0) "" else dport.toString()
        }

        val pm = context.packageManager
        var info: ApplicationInfo? = null
        var pkg: Array<String>? = null
        try {
            pkg = pm.getPackagesForUid(uid)
        } catch (ignored: SecurityException) {
        }

        if (pkg != null && pkg.isNotEmpty()) {
            try {
                info = pm.getApplicationInfo(pkg[0], 0)
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
        }

        if (info == null) {
            ivIcon.setImageDrawable(null)
        } else {
            if (info.icon <= 0) {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            } else {
                val uri = Uri.parse("android.resource://" + info.packageName + "/" + info.icon)
                GlideApp.with(context)
                    .load(uri)
                    .override(iconSize, iconSize)
                    .into(ivIcon)
            }
        }

        val we = android.os.Process.myUid() == uid
        uid %= 100000
        tvUid.text =
            when (uid) {
                -1 -> ""
                0 -> context.getString(R.string.title_root)
                9999 -> "-"
                else -> uid.toString()
            }

        tvSAddr.text = getKnownAddress(saddr)

        if (!we && resolve && !isKnownAddress(daddr)) {
            if (dname == null) {
                tvDaddr.text = daddr
                object : AsyncTask<String, Any, String>() {
                    override fun onPreExecute() {
                        ViewCompat.setHasTransientState(tvDaddr, true)
                    }

                    override fun doInBackground(vararg args: String): String {
                        return try {
                            InetAddress.getByName(args[0]).hostName
                        } catch (ignored: UnknownHostException) {
                            args[0]
                        }
                    }

                    override fun onPostExecute(name: String) {
                        tvDaddr.text = ">$name"
                        ViewCompat.setHasTransientState(tvDaddr, false)
                    }
                }.execute(daddr)
            } else {
                tvDaddr.text = dname
            }
        } else {
            tvDaddr.text = getKnownAddress(daddr)
        }

        tvOrganization.visibility = View.GONE
        if (!we && organization) {
            if (!isKnownAddress(daddr)) {
                object : AsyncTask<String, Any, String?>() {
                    override fun onPreExecute() {
                        ViewCompat.setHasTransientState(tvOrganization, true)
                    }

                    override fun doInBackground(vararg args: String): String? {
                        return try {
                            Util.getOrganization(args[0])
                        } catch (ex: Throwable) {
                            Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                            null
                        }
                    }

                    override fun onPostExecute(org: String?) {
                        if (org != null) {
                            tvOrganization.text = org
                            tvOrganization.visibility = View.VISIBLE
                        }
                        ViewCompat.setHasTransientState(tvOrganization, false)
                    }
                }.execute(daddr)
            }
        }

        if (TextUtils.isEmpty(data)) {
            tvData.text = ""
            tvData.visibility = View.GONE
        } else {
            tvData.text = data
            tvData.visibility = View.VISIBLE
        }
    }

    fun isKnownAddress(addr: String): Boolean {
        return try {
            val a = InetAddress.getByName(addr)
            a == dns1 || a == dns2 || a == vpn4 || a == vpn6
        } catch (ignored: UnknownHostException) {
            false
        }
    }

    private fun getKnownAddress(addr: String): String {
        return try {
            val a = InetAddress.getByName(addr)
            when {
                a == dns1 -> "dns1"
                a == dns2 -> "dns2"
                a == vpn4 || a == vpn6 -> "vpn"
                else -> addr
            }
        } catch (ignored: UnknownHostException) {
            addr
        }
    }

    private fun getKnownPort(port: Int): String {
        return when (port) {
            7 -> "echo"
            25 -> "smtp"
            53 -> "dns"
            80 -> "http"
            110 -> "pop3"
            143 -> "imap"
            443 -> "https"
            465 -> "smtps"
            993 -> "imaps"
            995 -> "pop3s"
            else -> port.toString()
        }
    }

    companion object {
        private const val TAG = "NetGuard.Log"
    }
}
