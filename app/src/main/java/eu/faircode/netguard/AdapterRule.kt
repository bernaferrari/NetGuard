package eu.faircode.netguard

import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CursorAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
class AdapterRule(context: Context, private val anchor: View) :
    RecyclerView.Adapter<AdapterRule.ViewHolder>(),
    Filterable {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var rv: RecyclerView? = null
    private val colorText: Int
    private val colorChanged: Int
    private val colorOn: Int
    private val colorOff: Int
    private val colorGrayed: Int
    private val iconSize: Int
    private var wifiActive = true
    private var otherActive = true
    private var live = true
    private var listAll: MutableList<Rule> = mutableListOf()
    private val listFiltered: MutableList<Rule> = mutableListOf()

    private val messaging =
        listOf(
            "com.discord",
            "com.facebook.mlite",
            "com.facebook.orca",
            "com.instagram.android",
            "com.Slack",
            "com.skype.raider",
            "com.snapchat.android",
            "com.whatsapp",
            "com.whatsapp.w4b",
        )

    private val download = listOf("com.google.android.youtube")

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llApplication: LinearLayout = itemView.findViewById(R.id.llApplication)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        val ivExpander: ImageView = itemView.findViewById(R.id.ivExpander)
        val tvName: TextView = itemView.findViewById(R.id.tvName)

        val tvHosts: TextView = itemView.findViewById(R.id.tvHosts)

        val rlLockdown: RelativeLayout = itemView.findViewById(R.id.rlLockdown)
        val ivLockdown: ImageView = itemView.findViewById(R.id.ivLockdown)

        val cbWifi: CheckBox = itemView.findViewById(R.id.cbWifi)
        val ivScreenWifi: ImageView = itemView.findViewById(R.id.ivScreenWifi)

        val cbOther: CheckBox = itemView.findViewById(R.id.cbOther)
        val ivScreenOther: ImageView = itemView.findViewById(R.id.ivScreenOther)
        val tvRoaming: TextView = itemView.findViewById(R.id.tvRoaming)

        val tvRemarkMessaging: TextView = itemView.findViewById(R.id.tvRemarkMessaging)
        val tvRemarkDownload: TextView = itemView.findViewById(R.id.tvRemarkDownload)

        val llConfiguration: LinearLayout = itemView.findViewById(R.id.llConfiguration)
        val tvUid: TextView = itemView.findViewById(R.id.tvUid)
        val tvPackage: TextView = itemView.findViewById(R.id.tvPackage)
        val tvVersion: TextView = itemView.findViewById(R.id.tvVersion)
        val tvInternet: TextView = itemView.findViewById(R.id.tvInternet)
        val tvDisabled: TextView = itemView.findViewById(R.id.tvDisabled)

        val btnRelated: Button = itemView.findViewById(R.id.btnRelated)
        val ibSettings: ImageButton = itemView.findViewById(R.id.ibSettings)
        val ibLaunch: ImageButton = itemView.findViewById(R.id.ibLaunch)

        val cbApply: CheckBox = itemView.findViewById(R.id.cbApply)

        val llScreenWifi: LinearLayout = itemView.findViewById(R.id.llScreenWifi)
        val ivWifiLegend: ImageView = itemView.findViewById(R.id.ivWifiLegend)
        val cbScreenWifi: CheckBox = itemView.findViewById(R.id.cbScreenWifi)

        val llScreenOther: LinearLayout = itemView.findViewById(R.id.llScreenOther)
        val ivOtherLegend: ImageView = itemView.findViewById(R.id.ivOtherLegend)
        val cbScreenOther: CheckBox = itemView.findViewById(R.id.cbScreenOther)

        val cbRoaming: CheckBox = itemView.findViewById(R.id.cbRoaming)

        val cbLockdown: CheckBox = itemView.findViewById(R.id.cbLockdown)
        val ivLockdownLegend: ImageView = itemView.findViewById(R.id.ivLockdownLegend)

        val btnClear: ImageButton = itemView.findViewById(R.id.btnClear)

        val llFilter: LinearLayout = itemView.findViewById(R.id.llFilter)
        val ivLive: ImageView = itemView.findViewById(R.id.ivLive)
        val tvLogging: TextView = itemView.findViewById(R.id.tvLogging)
        val btnLogging: Button = itemView.findViewById(R.id.btnLogging)
        val lvAccess: ListView = itemView.findViewById(R.id.lvAccess)
        val btnClearAccess: ImageButton = itemView.findViewById(R.id.btnClearAccess)
        val cbNotify: CheckBox = itemView.findViewById(R.id.cbNotify)

        init {
            val wifiParent = cbWifi.parent as View
            wifiParent.post {
                val rect = Rect()
                cbWifi.getHitRect(rect)
                rect.bottom += rect.top
                rect.right += rect.left
                rect.top = 0
                rect.left = 0
                wifiParent.touchDelegate = TouchDelegate(rect, cbWifi)
            }

            val otherParent = cbOther.parent as View
            otherParent.post {
                val rect = Rect()
                cbOther.getHitRect(rect)
                rect.bottom += rect.top
                rect.right += rect.left
                rect.top = 0
                rect.left = 0
                otherParent.touchDelegate = TouchDelegate(rect, cbOther)
            }
        }
    }

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        colorChanged =
            if (prefs.getBoolean("dark_theme", false)) {
                Color.argb(128, Color.red(Color.DKGRAY), Color.green(Color.DKGRAY), Color.blue(Color.DKGRAY))
            } else {
                Color.argb(128, Color.red(Color.LTGRAY), Color.green(Color.LTGRAY), Color.blue(Color.LTGRAY))
            }

        val ta: TypedArray = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
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

        colorGrayed = ContextCompat.getColor(context, R.color.colorGrayed)

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.listPreferredItemHeight, typedValue, true)
        val height =
            TypedValue.complexToDimensionPixelSize(typedValue.data, context.resources.displayMetrics)
        iconSize = Math.round(height * context.resources.displayMetrics.density + 0.5f)

        setHasStableIds(true)
    }

    fun set(listRule: List<Rule>) {
        listAll = listRule.toMutableList()
        listFiltered.clear()
        listFiltered.addAll(listRule)
        notifyDataSetChanged()
    }

    fun setWifiActive() {
        wifiActive = true
        otherActive = false
        notifyDataSetChanged()
    }

    fun setMobileActive() {
        wifiActive = false
        otherActive = true
        notifyDataSetChanged()
    }

    fun setDisconnected() {
        wifiActive = false
        otherActive = false
        notifyDataSetChanged()
    }

    fun isLive(): Boolean = live

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        rv = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        rv = null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val logApp = prefs.getBoolean("log_app", false)
        val filter = prefs.getBoolean("filter", false)
        val notifyAccess = prefs.getBoolean("notify_access", false)

        val rule = listFiltered[position]

        holder.llApplication.setOnClickListener {
            rule.expanded = !rule.expanded
            notifyItemChanged(holder.adapterPosition)
        }

        holder.itemView.setBackgroundColor(if (rule.changed) colorChanged else Color.TRANSPARENT)
        holder.ivExpander.setImageLevel(if (rule.expanded) 1 else 0)

        if (rule.icon <= 0) {
            holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        } else {
            val pkg = rule.packageName ?: ""
            val uri = Uri.parse("android.resource://$pkg/${rule.icon}")
            GlideApp.with(holder.itemView.context)
                .applyDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_RGB_565))
                .load(uri)
                .override(iconSize, iconSize)
                .into(holder.ivIcon)
        }

        holder.tvName.text = rule.name

        var color = if (rule.system) colorOff else colorText
        if (!rule.internet || !rule.enabled) {
            color = Color.argb(128, Color.red(color), Color.green(color), Color.blue(color))
        }
        holder.tvName.setTextColor(color)

        holder.tvHosts.visibility = if (rule.hosts > 0) View.VISIBLE else View.GONE
        holder.tvHosts.text = rule.hosts.toString()

        var lockdown = prefs.getBoolean("lockdown", false)
        val lockdownWifi = prefs.getBoolean("lockdown_wifi", true)
        val lockdownOther = prefs.getBoolean("lockdown_other", true)
        if ((otherActive && !lockdownOther) || (wifiActive && !lockdownWifi)) {
            lockdown = false
        }

        holder.rlLockdown.visibility = if (lockdown && !rule.lockdown) View.VISIBLE else View.GONE
        holder.ivLockdown.isEnabled = rule.apply
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(holder.ivLockdown.drawable)
            DrawableCompat.setTint(wrap, if (rule.apply) colorOff else colorGrayed)
        }

        val screenOn = prefs.getBoolean("screen_on", true)

        holder.cbWifi.isEnabled = rule.apply
        holder.cbWifi.alpha = if (wifiActive) 1f else 0.5f
        holder.cbWifi.setOnCheckedChangeListener(null)
        holder.cbWifi.isChecked = rule.wifi_blocked
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val buttonDrawable = CompoundButtonCompat.getButtonDrawable(holder.cbWifi)
            if (buttonDrawable != null) {
                val wrap = DrawableCompat.wrap(buttonDrawable)
                DrawableCompat.setTint(wrap, if (rule.apply) (if (rule.wifi_blocked) colorOff else colorOn) else colorGrayed)
            }
        }
        holder.cbWifi.setOnCheckedChangeListener { _, isChecked ->
            rule.wifi_blocked = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.ivScreenWifi.isEnabled = rule.apply
        holder.ivScreenWifi.alpha = if (wifiActive) 1f else 0.5f
        holder.ivScreenWifi.visibility =
            if (rule.screen_wifi && rule.wifi_blocked) View.VISIBLE else View.INVISIBLE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(holder.ivScreenWifi.drawable)
            DrawableCompat.setTint(wrap, if (rule.apply) colorOn else colorGrayed)
        }

        holder.cbOther.isEnabled = rule.apply
        holder.cbOther.alpha = if (otherActive) 1f else 0.5f
        holder.cbOther.setOnCheckedChangeListener(null)
        holder.cbOther.isChecked = rule.other_blocked
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val buttonDrawable = CompoundButtonCompat.getButtonDrawable(holder.cbOther)
            if (buttonDrawable != null) {
                val wrap = DrawableCompat.wrap(buttonDrawable)
                DrawableCompat.setTint(wrap, if (rule.apply) (if (rule.other_blocked) colorOff else colorOn) else colorGrayed)
            }
        }
        holder.cbOther.setOnCheckedChangeListener { _, isChecked ->
            rule.other_blocked = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.ivScreenOther.isEnabled = rule.apply
        holder.ivScreenOther.alpha = if (otherActive) 1f else 0.5f
        holder.ivScreenOther.visibility =
            if (rule.screen_other && rule.other_blocked) View.VISIBLE else View.INVISIBLE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(holder.ivScreenOther.drawable)
            DrawableCompat.setTint(wrap, if (rule.apply) colorOn else colorGrayed)
        }

        holder.tvRoaming.setTextColor(if (rule.apply) colorOff else colorGrayed)
        holder.tvRoaming.alpha = if (otherActive) 1f else 0.5f
        holder.tvRoaming.visibility =
            if (rule.roaming && (!rule.other_blocked || rule.screen_other)) View.VISIBLE else View.INVISIBLE

        val pkgName = rule.packageName ?: ""
        holder.tvRemarkMessaging.visibility = if (messaging.contains(pkgName)) View.VISIBLE else View.GONE
        holder.tvRemarkDownload.visibility = if (download.contains(pkgName)) View.VISIBLE else View.GONE

        holder.llConfiguration.visibility = if (rule.expanded) View.VISIBLE else View.GONE

        holder.tvUid.text = rule.uid.toString()
        holder.tvPackage.text = rule.packageName
        holder.tvVersion.text = rule.version

        holder.tvInternet.visibility = if (rule.internet) View.GONE else View.VISIBLE
        holder.tvDisabled.visibility = if (rule.enabled) View.GONE else View.VISIBLE

        holder.btnRelated.visibility = if (rule.relateduids) View.VISIBLE else View.GONE
        holder.btnRelated.setOnClickListener {
            val main = Intent(context, ActivityMain::class.java)
            main.putExtra(ActivityMain.EXTRA_SEARCH, rule.uid.toString())
            main.putExtra(ActivityMain.EXTRA_RELATED, true)
            context.startActivity(main)
        }

        if (rule.expanded) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$pkgName")
            val settings = if (intent.resolveActivity(context.packageManager) == null) null else intent

            holder.ibSettings.visibility = if (settings == null) View.GONE else View.VISIBLE
            holder.ibSettings.setOnClickListener {
                if (settings != null) {
                    context.startActivity(settings)
                }
            }
        } else {
            holder.ibSettings.visibility = View.GONE
        }

        if (rule.expanded) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkgName)
            val launch = if (intent == null || intent.resolveActivity(context.packageManager) == null) null else intent

            holder.ibLaunch.visibility = if (launch == null) View.GONE else View.VISIBLE
            holder.ibLaunch.setOnClickListener {
                if (launch != null) {
                    context.startActivity(launch)
                }
            }
        } else {
            holder.ibLaunch.visibility = View.GONE
        }

        holder.cbApply.isEnabled = rule.pkg && filter
        holder.cbApply.setOnCheckedChangeListener(null)
        holder.cbApply.isChecked = rule.apply
        holder.cbApply.setOnCheckedChangeListener { _, isChecked ->
            rule.apply = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.llScreenWifi.visibility = if (screenOn) View.VISIBLE else View.GONE
        holder.cbScreenWifi.isEnabled = rule.wifi_blocked && rule.apply
        holder.cbScreenWifi.setOnCheckedChangeListener(null)
        holder.cbScreenWifi.isChecked = rule.screen_wifi

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(holder.ivWifiLegend.drawable)
            DrawableCompat.setTint(wrap, colorOn)
        }

        holder.cbScreenWifi.setOnCheckedChangeListener { _, isChecked ->
            rule.screen_wifi = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.llScreenOther.visibility = if (screenOn) View.VISIBLE else View.GONE
        holder.cbScreenOther.isEnabled = rule.other_blocked && rule.apply
        holder.cbScreenOther.setOnCheckedChangeListener(null)
        holder.cbScreenOther.isChecked = rule.screen_other

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(holder.ivOtherLegend.drawable)
            DrawableCompat.setTint(wrap, colorOn)
        }

        holder.cbScreenOther.setOnCheckedChangeListener { _, isChecked ->
            rule.screen_other = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.cbRoaming.isEnabled = (!rule.other_blocked || rule.screen_other) && rule.apply
        holder.cbRoaming.setOnCheckedChangeListener(null)
        holder.cbRoaming.isChecked = rule.roaming
        holder.cbRoaming.setOnCheckedChangeListener { _, isChecked ->
            rule.roaming = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.cbLockdown.isEnabled = rule.apply
        holder.cbLockdown.setOnCheckedChangeListener(null)
        holder.cbLockdown.isChecked = rule.lockdown

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val wrap = DrawableCompat.wrap(holder.ivLockdownLegend.drawable)
            DrawableCompat.setTint(wrap, colorOn)
        }

        holder.cbLockdown.setOnCheckedChangeListener { _, isChecked ->
            rule.lockdown = isChecked
            updateRule(context, rule, true, listAll)
        }

        holder.btnClear.setOnClickListener { view ->
            Util.areYouSure(view.context, R.string.msg_clear_rules) {
                holder.cbApply.isChecked = true
                holder.cbWifi.isChecked = rule.wifi_default
                holder.cbOther.isChecked = rule.other_default
                holder.cbScreenWifi.isChecked = rule.screen_wifi_default
                holder.cbScreenOther.isChecked = rule.screen_other_default
                holder.cbRoaming.isChecked = rule.roaming_default
                holder.cbLockdown.isChecked = false
            }
        }

        holder.llFilter.visibility = if (Util.canFilter(context)) View.VISIBLE else View.GONE

        holder.ivLive.setOnClickListener { view ->
            live = !live
            val tv = TypedValue()
            view.context.theme.resolveAttribute(if (live) R.attr.iconPause else R.attr.iconPlay, tv, true)
            holder.ivLive.setImageResource(tv.resourceId)
            if (live) {
                notifyDataSetChanged()
            }
        }

        holder.tvLogging.setText(if (logApp && filter) R.string.title_logging_enabled else R.string.title_logging_disabled)
        holder.btnLogging.setOnClickListener {
            val view = inflater.inflate(R.layout.enable, null, false)

            val cbLogging = view.findViewById<CheckBox>(R.id.cbLogging)
            val cbFiltering = view.findViewById<CheckBox>(R.id.cbFiltering)
            val cbNotify = view.findViewById<CheckBox>(R.id.cbNotify)
            val tvFilter4 = view.findViewById<TextView>(R.id.tvFilter4)

            cbLogging.isChecked = logApp
            cbFiltering.isChecked = filter
            cbFiltering.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            tvFilter4.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) View.GONE else View.VISIBLE
            cbNotify.isChecked = notifyAccess
            cbNotify.isEnabled = logApp

            cbLogging.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("log_app", checked).apply()
                cbNotify.isEnabled = checked
                if (!checked) {
                    cbNotify.isChecked = false
                    prefs.edit().putBoolean("notify_access", false).apply()
                }
                ServiceSinkhole.reload("changed notify", context, false)
                notifyDataSetChanged()
            }

            cbFiltering.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    cbLogging.isChecked = true
                }
                prefs.edit().putBoolean("filter", checked).apply()
                ServiceSinkhole.reload("changed filter", context, false)
                notifyDataSetChanged()
            }

            cbNotify.setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("notify_access", checked).apply()
                ServiceSinkhole.reload("changed notify", context, false)
                notifyDataSetChanged()
            }

            AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create()
                .show()
        }

        if (rule.expanded) {
            val badapter = AdapterAccess(context, DatabaseHelper.getInstance(context).getAccess(rule.uid))
            holder.lvAccess.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, bposition, _ ->
                    val pm = context.packageManager
                    val cursor = badapter.getItem(bposition) as Cursor
                    val id = cursor.getLong(cursor.getColumnIndex("ID"))
                    val version = cursor.getInt(cursor.getColumnIndex("version"))
                    val protocol = cursor.getInt(cursor.getColumnIndex("protocol"))
                    val daddr = cursor.getString(cursor.getColumnIndex("daddr"))
                    val dport = cursor.getInt(cursor.getColumnIndex("dport"))
                    val time = cursor.getLong(cursor.getColumnIndex("time"))
                    val block = cursor.getInt(cursor.getColumnIndex("block"))

                    val popup = PopupMenu(context, anchor)
                    popup.inflate(R.menu.access)

                    popup.menu
                        .findItem(R.id.menu_host)
                        .title =
                        Util.getProtocolName(protocol, version, false) +
                        " " +
                        daddr +
                        if (dport > 0) "/$dport" else ""

                    val sub = popup.menu.findItem(R.id.menu_host).subMenu ?: return@OnItemClickListener
                    var multiple = false
                    var alt: Cursor? = null
                    try {
                        alt = DatabaseHelper.getInstance(context).getAlternateQNames(daddr)
                        while (alt.moveToNext()) {
                            multiple = true
                            sub.add(Menu.NONE, Menu.NONE, 0, alt.getString(0)).isEnabled = false
                        }
                    } finally {
                        alt?.close()
                    }
                    popup.menu.findItem(R.id.menu_host).isEnabled = multiple

                    markPro(context, popup.menu.findItem(R.id.menu_allow), ActivityPro.SKU_FILTER)
                    markPro(context, popup.menu.findItem(R.id.menu_block), ActivityPro.SKU_FILTER)

                    val lookupIP =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.dnslytics.com/whois-lookup/$daddr"),
                        )
                    if (pm.resolveActivity(lookupIP, 0) == null) {
                        popup.menu.removeItem(R.id.menu_whois)
                    } else {
                        popup.menu.findItem(R.id.menu_whois).title =
                            context.getString(R.string.title_log_whois, daddr)
                    }

                    val lookupPort =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.speedguide.net/port.php?port=$dport"),
                        )
                    if (dport <= 0 || pm.resolveActivity(lookupPort, 0) == null) {
                        popup.menu.removeItem(R.id.menu_port)
                    } else {
                        popup.menu.findItem(R.id.menu_port).title =
                            context.getString(R.string.title_log_port, dport)
                    }

                    popup.menu.findItem(R.id.menu_time).title =
                        SimpleDateFormat.getDateTimeInstance().format(time)

                    popup.setOnMenuItemClickListener { menuItem ->
                        val menu = menuItem.itemId
                        var result = false
                        when (menu) {
                            R.id.menu_whois -> {
                                context.startActivity(lookupIP)
                                result = true
                            }
                            R.id.menu_port -> {
                                context.startActivity(lookupPort)
                                result = true
                            }
                            R.id.menu_allow -> {
                                if (IAB.isPurchased(ActivityPro.SKU_FILTER, context)) {
                                    DatabaseHelper.getInstance(context).setAccess(id, 0)
                                    ServiceSinkhole.reload("allow host", context, false)
                                } else {
                                    context.startActivity(Intent(context, ActivityPro::class.java))
                                }
                                result = true
                            }
                            R.id.menu_block -> {
                                if (IAB.isPurchased(ActivityPro.SKU_FILTER, context)) {
                                    DatabaseHelper.getInstance(context).setAccess(id, 1)
                                    ServiceSinkhole.reload("block host", context, false)
                                } else {
                                    context.startActivity(Intent(context, ActivityPro::class.java))
                                }
                                result = true
                            }
                            R.id.menu_reset -> {
                                DatabaseHelper.getInstance(context).setAccess(id, -1)
                                ServiceSinkhole.reload("reset host", context, false)
                                result = true
                            }
                            R.id.menu_copy -> {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("netguard", daddr)
                                clipboard.setPrimaryClip(clip)
                                return@setOnMenuItemClickListener true
                            }
                        }

                        if (menu == R.id.menu_allow || menu == R.id.menu_block || menu == R.id.menu_reset) {
                            object : AsyncTask<Any, Any, Long>() {
                                override fun doInBackground(vararg params: Any): Long {
                                    return DatabaseHelper.getInstance(context).getHostCount(rule.uid, false)
                                }

                                override fun onPostExecute(hosts: Long) {
                                    rule.hosts = hosts
                                    notifyDataSetChanged()
                                }
                            }.execute()
                        }

                        result
                    }

                    if (block == 0) {
                        popup.menu.removeItem(R.id.menu_allow)
                    } else if (block == 1) {
                        popup.menu.removeItem(R.id.menu_block)
                    }

                    popup.show()
                }

            holder.lvAccess.adapter = badapter
        } else {
            holder.lvAccess.adapter = null
            holder.lvAccess.onItemClickListener = null
        }

        holder.btnClearAccess.setOnClickListener { view ->
            Util.areYouSure(view.context, R.string.msg_reset_access) {
                DatabaseHelper.getInstance(context).clearAccess(rule.uid, true)
                if (!live) {
                    notifyDataSetChanged()
                }
                rv?.scrollToPosition(holder.adapterPosition)
            }
        }

        holder.cbNotify.isEnabled = prefs.getBoolean("notify_access", false) && rule.apply
        holder.cbNotify.setOnCheckedChangeListener(null)
        holder.cbNotify.isChecked = rule.notify
        holder.cbNotify.setOnCheckedChangeListener { _, isChecked ->
            rule.notify = isChecked
            updateRule(context, rule, true, listAll)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)

        val adapter = holder.lvAccess.adapter as? CursorAdapter
        if (adapter != null) {
            Log.i(TAG, "Closing access cursor")
            adapter.changeCursor(null)
            holder.lvAccess.adapter = null
        }
    }

    private fun markPro(context: Context, menu: MenuItem, sku: String?) {
        if (sku == null || !IAB.isPurchased(sku, context)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val dark = prefs.getBoolean("dark_theme", false)
            val ssb = SpannableStringBuilder("  " + menu.title)
            ssb.setSpan(
                ImageSpan(
                    context,
                    if (dark) R.drawable.ic_shopping_cart_white_24dp else R.drawable.ic_shopping_cart_black_24dp,
                ),
                0,
                1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            menu.title = ssb
        }
    }

    private fun updateRule(context: Context, rule: Rule, root: Boolean, listAll: MutableList<Rule>) {
        val wifi = context.getSharedPreferences("wifi", Context.MODE_PRIVATE)
        val other = context.getSharedPreferences("other", Context.MODE_PRIVATE)
        val apply = context.getSharedPreferences("apply", Context.MODE_PRIVATE)
        val screenWifi = context.getSharedPreferences("screen_wifi", Context.MODE_PRIVATE)
        val screenOther = context.getSharedPreferences("screen_other", Context.MODE_PRIVATE)
        val roaming = context.getSharedPreferences("roaming", Context.MODE_PRIVATE)
        val lockdown = context.getSharedPreferences("lockdown", Context.MODE_PRIVATE)
        val notify = context.getSharedPreferences("notify", Context.MODE_PRIVATE)

        val packageName = rule.packageName ?: ""

        if (rule.wifi_blocked == rule.wifi_default) {
            wifi.edit().remove(packageName).apply()
        } else {
            wifi.edit().putBoolean(packageName, rule.wifi_blocked).apply()
        }

        if (rule.other_blocked == rule.other_default) {
            other.edit().remove(packageName).apply()
        } else {
            other.edit().putBoolean(packageName, rule.other_blocked).apply()
        }

        if (rule.apply) {
            apply.edit().remove(packageName).apply()
        } else {
            apply.edit().putBoolean(packageName, rule.apply).apply()
        }

        if (rule.screen_wifi == rule.screen_wifi_default) {
            screenWifi.edit().remove(packageName).apply()
        } else {
            screenWifi.edit().putBoolean(packageName, rule.screen_wifi).apply()
        }

        if (rule.screen_other == rule.screen_other_default) {
            screenOther.edit().remove(packageName).apply()
        } else {
            screenOther.edit().putBoolean(packageName, rule.screen_other).apply()
        }

        if (rule.roaming == rule.roaming_default) {
            roaming.edit().remove(packageName).apply()
        } else {
            roaming.edit().putBoolean(packageName, rule.roaming).apply()
        }

        if (rule.lockdown) {
            lockdown.edit().putBoolean(packageName, rule.lockdown).apply()
        } else {
            lockdown.edit().remove(packageName).apply()
        }

        if (rule.notify) {
            notify.edit().remove(packageName).apply()
        } else {
            notify.edit().putBoolean(packageName, rule.notify).apply()
        }

        rule.updateChanged(context)
        Log.i(TAG, "Updated $rule")

        val listModified = mutableListOf<Rule>()
        for (pkg in rule.related ?: emptyArray()) {
            for (related in listAll) {
                if (related.packageName == pkg) {
                    related.wifi_blocked = rule.wifi_blocked
                    related.other_blocked = rule.other_blocked
                    related.apply = rule.apply
                    related.screen_wifi = rule.screen_wifi
                    related.screen_other = rule.screen_other
                    related.roaming = rule.roaming
                    related.lockdown = rule.lockdown
                    related.notify = rule.notify
                    listModified.add(related)
                }
            }
        }

        val listSearch = if (root) ArrayList(listAll) else listAll
        listSearch.remove(rule)
        for (modified in listModified) {
            listSearch.remove(modified)
        }
        for (modified in listModified) {
            updateRule(context, modified, false, listSearch)
        }

        if (root) {
            notifyDataSetChanged()
            NotificationManagerCompat.from(context).cancel(rule.uid)
            ServiceSinkhole.reload("rule changed", context, false)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val listResult = mutableListOf<Rule>()
                if (query == null) {
                    listResult.addAll(listAll)
                } else {
                    val q = query.toString().lowercase(Locale.ROOT).trim()
                    val uid = q.toIntOrNull() ?: -1
                    for (rule in listAll) {
                        if (
                            rule.uid == uid ||
                                (rule.packageName ?: "").lowercase(Locale.ROOT).contains(q) ||
                                (rule.name?.lowercase(Locale.ROOT)?.contains(q) == true)
                        ) {
                            listResult.add(rule)
                        }
                    }
                }

                val result = FilterResults()
                result.values = listResult
                result.count = listResult.size
                return result
            }

            override fun publishResults(query: CharSequence?, result: FilterResults?) {
                listFiltered.clear()
                if (result == null) {
                    listFiltered.addAll(listAll)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val values = result.values as? List<Rule> ?: emptyList()
                    listFiltered.addAll(values)
                    if (listFiltered.size == 1) {
                        listFiltered[0].expanded = true
                    }
                }
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.rule, parent, false))
    }

    override fun getItemId(position: Int): Long {
        val rule = listFiltered[position]
        val hash = rule.packageName?.hashCode() ?: 0
        return hash * 100000L + rule.uid
    }

    override fun getItemCount(): Int = listFiltered.size

    companion object {
        private const val TAG = "NetGuard.Adapter"
    }
}
