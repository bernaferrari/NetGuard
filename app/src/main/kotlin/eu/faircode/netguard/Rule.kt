package eu.faircode.netguard

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.database.Cursor
import android.os.Build
import android.os.Process
import android.util.Log
import eu.faircode.netguard.data.Prefs
import org.xmlpull.v1.XmlPullParser
import java.text.Collator
import java.util.Locale

class Rule private constructor(dh: DatabaseHelper, info: PackageInfo, context: Context) {
    @JvmField var uid: Int = 0
    @JvmField var packageName: String? = null
    @JvmField var icon: Int = 0
    @JvmField var name: String? = null
    @JvmField var version: String? = null
    @JvmField var system: Boolean = false
    @JvmField var internet: Boolean = false
    @JvmField var enabled: Boolean = false
    @JvmField var pkg: Boolean = true

    @JvmField var wifi_default: Boolean = false
    @JvmField var other_default: Boolean = false
    @JvmField var screen_wifi_default: Boolean = false
    @JvmField var screen_other_default: Boolean = false
    @JvmField var roaming_default: Boolean = false

    @JvmField var wifi_blocked: Boolean = false
    @JvmField var other_blocked: Boolean = false
    @JvmField var screen_wifi: Boolean = false
    @JvmField var screen_other: Boolean = false
    @JvmField var roaming: Boolean = false
    @JvmField var lockdown: Boolean = false

    @JvmField var apply: Boolean = true
    @JvmField var notify: Boolean = true

    @JvmField var relateduids: Boolean = false
    @JvmField var related: Array<String>? = null

    @JvmField var hosts: Long = 0
    @JvmField var changed: Boolean = false

    @JvmField var expanded: Boolean = false

    init {
        val appInfo = requireNotNull(info.applicationInfo)
        val packageNameValue = info.packageName
        uid = appInfo.uid
        packageName = packageNameValue
        icon = appInfo.icon
        version = info.versionName
        when (appInfo.uid) {
            0 -> {
                name = context.getString(R.string.title_root)
                system = true
                internet = true
                enabled = true
                pkg = false
            }
            1013 -> {
                name = context.getString(R.string.title_mediaserver)
                system = true
                internet = true
                enabled = true
                pkg = false
            }
            1020 -> {
                name = "MulticastDNSResponder"
                system = true
                internet = true
                enabled = true
                pkg = false
            }
            1021 -> {
                name = context.getString(R.string.title_gpsdaemon)
                system = true
                internet = true
                enabled = true
                pkg = false
            }
            1051 -> {
                name = context.getString(R.string.title_dnsdaemon)
                system = true
                internet = true
                enabled = true
                pkg = false
            }
            9999 -> {
                name = context.getString(R.string.title_nobody)
                system = true
                internet = true
                enabled = true
                pkg = false
            }
            else -> {
                var cursor: Cursor? = null
                try {
                    cursor = dh.getApp(packageNameValue)
                    if (cursor.moveToNext()) {
                        val labelColumn = cursor.getColumnIndexOrThrow("label")
                        val systemColumn = cursor.getColumnIndexOrThrow("system")
                        val internetColumn = cursor.getColumnIndexOrThrow("internet")
                        val enabledColumn = cursor.getColumnIndexOrThrow("enabled")
                        name = cursor.getString(labelColumn)
                        system = cursor.getInt(systemColumn) > 0
                        internet = cursor.getInt(internetColumn) > 0
                        enabled = cursor.getInt(enabledColumn) > 0
                    } else {
                        name = getLabel(info, context)
                        system = isSystem(packageNameValue, context)
                        internet = hasInternet(packageNameValue, context)
                        enabled = isEnabled(info, context)

                        dh.addApp(packageNameValue, name, system, internet, enabled)
                    }
                } finally {
                    cursor?.close()
                }
            }
        }
    }

    private fun updateChanged(default_wifi: Boolean, default_other: Boolean, default_roaming: Boolean) {
        changed =
            wifi_blocked != default_wifi ||
                other_blocked != default_other ||
                (wifi_blocked && screen_wifi != screen_wifi_default) ||
                (other_blocked && screen_other != screen_other_default) ||
                ((!other_blocked || screen_other) && roaming != default_roaming) ||
                hosts > 0 ||
                lockdown ||
                !apply
    }

    fun updateChanged(context: Context) {
        val screen_on = Prefs.getBoolean("screen_on", false)
        val default_wifi = Prefs.getBoolean("whitelist_wifi", true) && screen_on
        val default_other = Prefs.getBoolean("whitelist_other", true) && screen_on
        val default_roaming = Prefs.getBoolean("whitelist_roaming", true)
        updateChanged(default_wifi, default_other, default_roaming)
    }

    override fun toString(): String {
        return name ?: ""
    }

    companion object {
        private const val TAG = "NetGuard.Rule"

        private var cachePackageInfo: List<PackageInfo>? = null
        private val cacheLabel = HashMap<PackageInfo, String>()
        private val cacheSystem = HashMap<String, Boolean>()
        private val cacheInternet = HashMap<String, Boolean>()
        private val cacheEnabled = HashMap<PackageInfo, Boolean>()

        private fun getPackages(context: Context): List<PackageInfo> {
            if (cachePackageInfo == null) {
                val pm = context.packageManager
                cachePackageInfo = pm.getInstalledPackages(0)
            }
            return ArrayList(cachePackageInfo ?: emptyList())
        }

        private fun getLabel(info: PackageInfo, context: Context): String {
            if (!cacheLabel.containsKey(info)) {
                val pm = context.packageManager
                val label = info.applicationInfo?.loadLabel(pm)?.toString() ?: ""
                cacheLabel[info] = label
            }
            return cacheLabel[info] ?: ""
        }

        private fun isSystem(packageName: String, context: Context): Boolean {
            if (!cacheSystem.containsKey(packageName)) {
                cacheSystem[packageName] = Util.isSystem(packageName, context)
            }
            return cacheSystem[packageName] ?: false
        }

        private fun hasInternet(packageName: String, context: Context): Boolean {
            if (!cacheInternet.containsKey(packageName)) {
                cacheInternet[packageName] = Util.hasInternet(packageName, context)
            }
            return cacheInternet[packageName] ?: false
        }

        private fun isEnabled(info: PackageInfo, context: Context): Boolean {
            if (!cacheEnabled.containsKey(info)) {
                cacheEnabled[info] = Util.isEnabled(info, context)
            }
            return cacheEnabled[info] ?: false
        }

        @JvmStatic
        fun clearCache(context: Context) {
            Log.i(TAG, "Clearing cache")
            synchronized(context.applicationContext) {
                cachePackageInfo = null
                cacheLabel.clear()
                cacheSystem.clear()
                cacheInternet.clear()
                cacheEnabled.clear()
            }
            val dh = DatabaseHelper.getInstance(context)
            dh.clearApps()
        }

        @JvmStatic
        fun getRules(all: Boolean, context: Context): List<Rule> {
            synchronized(context.applicationContext) {
                var default_wifi = Prefs.getBoolean("whitelist_wifi", true)
                var default_other = Prefs.getBoolean("whitelist_other", true)
                var default_screen_wifi = Prefs.getBoolean("screen_wifi", false)
                var default_screen_other = Prefs.getBoolean("screen_other", false)
                val default_roaming = Prefs.getBoolean("whitelist_roaming", true)

                val manage_system = Prefs.getBoolean("manage_system", false)
                val screen_on = Prefs.getBoolean("screen_on", true)
                val show_user = Prefs.getBoolean("show_user", true)
                val show_system = Prefs.getBoolean("show_system", false)
                val show_nointernet = Prefs.getBoolean("show_nointernet", true)
                val show_disabled = Prefs.getBoolean("show_disabled", true)

                default_screen_wifi = default_screen_wifi && screen_on
                default_screen_other = default_screen_other && screen_on

                val pre_wifi_blocked = HashMap<String, Boolean>()
                val pre_other_blocked = HashMap<String, Boolean>()
                val pre_roaming = HashMap<String, Boolean>()
                val pre_related = HashMap<String, Array<String>>()
                val pre_system = HashMap<String, Boolean>()
                try {
                    val xml: XmlResourceParser = context.resources.getXml(R.xml.predefined)
                    var eventType = xml.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (xml.name) {
                                "wifi" -> {
                                    val pkg = xml.getAttributeValue(null, "package")
                                    val pblocked = xml.getAttributeBooleanValue(null, "blocked", false)
                                    pre_wifi_blocked[pkg] = pblocked
                                }
                                "other" -> {
                                    val pkg = xml.getAttributeValue(null, "package")
                                    val pblocked = xml.getAttributeBooleanValue(null, "blocked", false)
                                    val proaming = xml.getAttributeBooleanValue(null, "roaming", default_roaming)
                                    pre_other_blocked[pkg] = pblocked
                                    pre_roaming[pkg] = proaming
                                }
                                "relation" -> {
                                    val pkg = xml.getAttributeValue(null, "package")
                                    val rel = xml.getAttributeValue(null, "related").split(",").toTypedArray()
                                    pre_related[pkg] = rel
                                }
                                "type" -> {
                                    val pkg = xml.getAttributeValue(null, "package")
                                    val system = xml.getAttributeBooleanValue(null, "system", true)
                                    pre_system[pkg] = system
                                }
                            }
                        }
                        eventType = xml.next()
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }

                val listRules = ArrayList<Rule>()
                val listPI = getPackages(context).toMutableList()

                val userId = Process.myUid() / 100000

                val root = PackageInfo().apply {
                    packageName = "root"
                    versionName = Build.VERSION.RELEASE
                    applicationInfo = ApplicationInfo().apply {
                        uid = 0
                        icon = 0
                    }
                }
                listPI.add(root)

                val media = PackageInfo().apply {
                    packageName = "android.media"
                    versionName = Build.VERSION.RELEASE
                    applicationInfo = ApplicationInfo().apply {
                        uid = 1013 + userId * 100000
                        icon = 0
                    }
                }
                listPI.add(media)

                val mdr = PackageInfo().apply {
                    packageName = "android.multicast"
                    versionName = Build.VERSION.RELEASE
                    applicationInfo = ApplicationInfo().apply {
                        uid = 1020 + userId * 100000
                        icon = 0
                    }
                }
                listPI.add(mdr)

                val gps = PackageInfo().apply {
                    packageName = "android.gps"
                    versionName = Build.VERSION.RELEASE
                    applicationInfo = ApplicationInfo().apply {
                        uid = 1021 + userId * 100000
                        icon = 0
                    }
                }
                listPI.add(gps)

                val dns = PackageInfo().apply {
                    packageName = "android.dns"
                    versionName = Build.VERSION.RELEASE
                    applicationInfo = ApplicationInfo().apply {
                        uid = 1051 + userId * 100000
                        icon = 0
                    }
                }
                listPI.add(dns)

                val nobody = PackageInfo().apply {
                    packageName = "nobody"
                    versionName = Build.VERSION.RELEASE
                    applicationInfo = ApplicationInfo().apply {
                        uid = 9999
                        icon = 0
                    }
                }
                listPI.add(nobody)

                val dh = DatabaseHelper.getInstance(context)
                for (info in listPI) {
                    try {
                        val appInfo = info.applicationInfo ?: continue
                        if (appInfo.uid == Process.myUid()) continue

                        val rule = Rule(dh, info, context)

                        if (pre_system.containsKey(info.packageName)) {
                            rule.system = pre_system[info.packageName] == true
                        }
                        if (appInfo.uid == Process.myUid()) rule.system = true

                        if (all ||
                            ((if (rule.system) show_system else show_user) &&
                                (show_nointernet || rule.internet) &&
                                (show_disabled || rule.enabled))
                        ) {
                            rule.wifi_default = pre_wifi_blocked[info.packageName] ?: default_wifi
                            rule.other_default = pre_other_blocked[info.packageName] ?: default_other
                            rule.screen_wifi_default = default_screen_wifi
                            rule.screen_other_default = default_screen_other
                            rule.roaming_default = pre_roaming[info.packageName] ?: default_roaming

                            val packageName = info.packageName
                            rule.wifi_blocked =
                                (!(rule.system && !manage_system) &&
                                    Prefs.getBoolean(Prefs.namespaced("wifi", packageName), rule.wifi_default))
                            rule.other_blocked =
                                (!(rule.system && !manage_system) &&
                                    Prefs.getBoolean(Prefs.namespaced("other", packageName), rule.other_default))
                            rule.screen_wifi =
                                Prefs.getBoolean(Prefs.namespaced("screen_wifi", packageName), rule.screen_wifi_default) &&
                                    screen_on
                            rule.screen_other =
                                Prefs.getBoolean(Prefs.namespaced("screen_other", packageName), rule.screen_other_default) &&
                                    screen_on
                            rule.roaming =
                                Prefs.getBoolean(Prefs.namespaced("roaming", packageName), rule.roaming_default)
                            rule.lockdown =
                                Prefs.getBoolean(Prefs.namespaced("lockdown", packageName), false)

                            rule.apply = Prefs.getBoolean(Prefs.namespaced("apply", packageName), true)
                            rule.notify = Prefs.getBoolean(Prefs.namespaced("notify", packageName), true)

                            val listPkg = ArrayList<String>()
                            if (pre_related.containsKey(info.packageName)) {
                                listPkg.addAll(pre_related[info.packageName]?.toList() ?: emptyList())
                            }
                            for (pi in listPI) {
                                val piAppInfo = pi.applicationInfo ?: continue
                                if (piAppInfo.uid == rule.uid && pi.packageName != rule.packageName) {
                                    rule.relateduids = true
                                    listPkg.add(pi.packageName)
                                }
                            }
                            rule.related = listPkg.toTypedArray()

                            rule.hosts = dh.getHostCount(rule.uid, true)
                            rule.updateChanged(default_wifi, default_other, default_roaming)

                            listRules.add(rule)
                        }
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                    }
                }

                val collator = Collator.getInstance(Locale.getDefault()).apply {
                    strength = Collator.SECONDARY
                }

                val sort = Prefs.getString("sort", "name")
                if (sort == "uid") {
                    listRules.sortWith { rule, other ->
                        when {
                            rule.uid < other.uid -> -1
                            rule.uid > other.uid -> 1
                            else -> {
                                val i = collator.compare(rule.name, other.name)
                                if (i == 0) {
                                    (rule.packageName ?: "").compareTo(other.packageName ?: "")
                                } else {
                                    i
                                }
                            }
                        }
                    }
                } else {
                    listRules.sortWith { rule, other ->
                        if (all || rule.changed == other.changed) {
                            val i = collator.compare(rule.name, other.name)
                            if (i == 0) {
                                (rule.packageName ?: "").compareTo(other.packageName ?: "")
                            } else {
                                i
                            }
                        } else {
                            if (rule.changed) -1 else 1
                        }
                    }
                }

                return listRules
            }
        }
    }
}
