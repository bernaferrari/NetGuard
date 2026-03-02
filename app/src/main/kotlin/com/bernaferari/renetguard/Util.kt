package com.bernaferari.renetguard

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.ApplicationErrorReport
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.PackageInfoCompat
import com.bernaferari.renetguard.data.Prefs
import com.bernaferari.renetguard.ui.theme.THEME_DEFAULT
import com.bernaferari.renetguard.ui.theme.themePrimaryColor
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object Util {
    private const val TAG = "NetGuard.Util"

    // Roam like at home
    private val listEU =
        listOf(
            "AT", // Austria
            "BE", // Belgium
            "BG", // Bulgaria
            "HR", // Croatia
            "CY", // Cyprus
            "CZ", // Czech Republic
            "DK", // Denmark
            "EE", // Estonia
            "FI", // Finland
            "FR", // France
            "DE", // Germany
            "GR", // Greece
            "HU", // Hungary
            "IS", // Iceland
            "IE", // Ireland
            "IT", // Italy
            "LV", // Latvia
            "LI", // Liechtenstein
            "LT", // Lithuania
            "LU", // Luxembourg
            "MT", // Malta
            "NL", // Netherlands
            "NO", // Norway
            "PL", // Poland
            "PT", // Portugal
            "RO", // Romania
            "SK", // Slovakia
            "SI", // Slovenia
            "ES", // Spain
            "SE", // Sweden
        )

    @JvmStatic
    external fun jni_getprop(name: String): String?

    @JvmStatic
    external fun is_numeric_address(ip: String): Boolean

    @JvmStatic
    external fun dump_memory_profile()

    init {
        try {
            System.loadLibrary("netguard")
        } catch (_: UnsatisfiedLinkError) {
            System.exit(1)
        }
    }

    @JvmStatic
    fun getSelfVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (ex: PackageManager.NameNotFoundException) {
            ex.toString()
        }
    }

    @JvmStatic
    fun getSelfVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (_: PackageManager.NameNotFoundException) {
            -1
        }
    }

    @JvmStatic
    fun isNetworkActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return cm?.activeNetwork != null
    }

    @JvmStatic
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return false

        val activeNetwork = cm.activeNetwork
        if (activeNetwork != null) {
            val caps = cm.getNetworkCapabilities(activeNetwork)
            if (caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ) {
                return true
            }
        }

        return false
    }

    @JvmStatic
    fun isWifiActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val active = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @JvmStatic
    fun isMeteredNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return cm?.isActiveNetworkMetered == true
    }

    @JvmStatic
    fun getWifiSSID(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val active = cm?.activeNetwork
            val caps = if (active != null) cm.getNetworkCapabilities(active) else null
            val info = caps?.transportInfo as? android.net.wifi.WifiInfo
            val ssid = info?.ssid?.trim('"')
            if (!ssid.isNullOrBlank()) {
                return ssid
            }
        }

        @Suppress("DEPRECATION")
        val legacy = wm?.connectionInfo?.ssid
        return legacy?.trim('"') ?: "NULL"
    }

    @JvmStatic
    fun getNetworkType(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val active = cm?.activeNetwork
        val caps = if (active != null) cm.getNetworkCapabilities(active) else null
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        return if (isCellular) {
            try {
                tm?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
            } catch (_: SecurityException) {
                TelephonyManager.NETWORK_TYPE_UNKNOWN
            }
        } else {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
    }

    @JvmStatic
    fun getNetworkGeneration(context: Context): String? {
        val type = getNetworkType(context)
        return if (type == TelephonyManager.NETWORK_TYPE_UNKNOWN) null else getNetworkGeneration(
            type
        )
    }

    @JvmStatic
    fun isRoaming(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm != null) {
            return tm.isNetworkRoaming
        }
        return false
    }

    @JvmStatic
    fun isNational(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            tm != null && tm.simCountryIso != null && tm.simCountryIso == tm.networkCountryIso
        } catch (_: Throwable) {
            false
        }
    }

    @JvmStatic
    fun isEU(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            tm != null && isEU(tm.simCountryIso) && isEU(tm.networkCountryIso)
        } catch (_: Throwable) {
            false
        }
    }

    @JvmStatic
    fun isEU(country: String?): Boolean {
        return country != null && listEU.contains(country.uppercase(Locale.ROOT))
    }

    @JvmStatic
    fun isPrivateDns(context: Context): Boolean {
        var dnsMode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
        Log.i(TAG, "Private DNS mode=$dnsMode")
        if (dnsMode == null) {
            dnsMode = "off"
        }
        return dnsMode != "off"
    }

    @JvmStatic
    fun getPrivateDnsSpecifier(context: Context): String? {
        val dnsMode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
        return if ("hostname" == dnsMode) {
            Settings.Global.getString(context.contentResolver, "private_dns_specifier")
        } else {
            null
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getNetworkGeneration(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_IDEN,
            TelephonyManager.NETWORK_TYPE_GSM -> "2G"

            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE,
            TelephonyManager.NETWORK_TYPE_IWLAN -> "4G"

            else -> "?G"
        }
    }

    @JvmStatic
    fun hasPhoneStatePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @JvmStatic
    fun getDefaultDNS(context: Context): List<String> {
        val listDns = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            if (network != null) {
                val lp: LinkProperties? = cm.getLinkProperties(network)
                val dns = lp?.dnsServers
                if (dns != null) {
                    for (address in dns) {
                        val host = address.hostAddress
                        if (host != null) {
                            Log.i(TAG, "DNS from LP: $host")
                            listDns.add(host.split("%")[0])
                        }
                    }
                }
            }
        } else {
            val dns1 = jni_getprop("net.dns1")
            val dns2 = jni_getprop("net.dns2")
            if (dns1 != null) {
                listDns.add(dns1.split("%")[0])
            }
            if (dns2 != null) {
                listDns.add(dns2.split("%")[0])
            }
        }

        return listDns
    }

    @JvmStatic
    fun isNumericAddress(ip: String): Boolean {
        return is_numeric_address(ip)
    }

    @JvmStatic
    fun isInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return pm?.isInteractive == true
    }

    @JvmStatic
    fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun isSystem(uid: Int, context: Context): Boolean {
        val pm = context.packageManager
        val pkgs = pm.getPackagesForUid(uid)
        if (pkgs != null) {
            for (pkg in pkgs) {
                if (isSystem(pkg, context)) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun isSystem(packageName: String, context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, 0)
            val flags = info.applicationInfo?.flags ?: 0
            flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun hasInternet(packageName: String, context: Context): Boolean {
        val pm = context.packageManager
        return pm.checkPermission("android.permission.INTERNET", packageName) ==
                PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun hasInternet(uid: Int, context: Context): Boolean {
        val pm = context.packageManager
        val pkgs = pm.getPackagesForUid(uid)
        if (pkgs != null) {
            for (pkg in pkgs) {
                if (hasInternet(pkg, context)) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun isEnabled(info: PackageInfo, context: Context): Boolean {
        val setting = try {
            val pm = context.packageManager
            pm.getApplicationEnabledSetting(info.packageName)
        } catch (ex: IllegalArgumentException) {
            Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }
        return if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            info.applicationInfo?.enabled ?: false
        } else {
            setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }

    @JvmStatic
    fun getApplicationNames(uid: Int, context: Context): List<String> {
        val listResult = mutableListOf<String>()
        when (uid) {
            0 -> listResult.add(context.getString(R.string.title_root))
            1013 -> listResult.add(context.getString(R.string.title_mediaserver))
            9999 -> listResult.add(context.getString(R.string.title_nobody))
            else -> {
                val pm = context.packageManager
                val pkgs = pm.getPackagesForUid(uid)
                if (pkgs != null) {
                    for (pkg in pkgs) {
                        try {
                            val info = pm.getApplicationInfo(pkg, 0)
                            val name = pm.getApplicationLabel(info).toString()
                            if (!TextUtils.isEmpty(name)) {
                                listResult.add(name)
                            }
                        } catch (_: PackageManager.NameNotFoundException) {
                        }
                    }
                }
                listResult.sort()
            }
        }
        return listResult
    }

    @JvmStatic
    fun canFilter(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }

        // https://android-review.googlesource.com/#/c/206710/1/untrusted_app.te
        val tcp = File("/proc/net/tcp")
        val tcp6 = File("/proc/net/tcp6")
        try {
            if (tcp.exists() && tcp.canRead()) {
                return true
            }
        } catch (_: SecurityException) {
        }
        return try {
            tcp6.exists() && tcp6.canRead()
        } catch (_: SecurityException) {
            false
        }
    }

    @JvmStatic
    fun isDebuggable(context: Context): Boolean {
        return context.applicationContext.applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    @JvmStatic
    fun isPlayStoreInstall(context: Context): Boolean {
        if (BuildConfig.PLAY_STORE_RELEASE) {
            return true
        }
        return try {
            val pm = context.packageManager
            val installer =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(context.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(context.packageName)
                }
            "com.android.vending" == installer
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            false
        }
    }

    @JvmStatic
    fun ownFault(context: Context, ex: Throwable): Boolean {
        if (ex is OutOfMemoryError) {
            return false
        }
        val actual = ex.cause ?: ex
        for (ste in actual.stackTrace) {
            if (ste.className.startsWith(context.packageName)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun setTheme(context: Context) {
        val theme = Prefs.getString("theme", THEME_DEFAULT)

        if (context is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskColor(context, theme)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setTaskColor(context: Context, theme: String?) {
        val defaultColor = themePrimaryColor(theme)
        val activity = context as Activity
        val description =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityManager.TaskDescription.Builder()
                    .setPrimaryColor(defaultColor)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                ActivityManager.TaskDescription(null, null, defaultColor)
            }
        activity.setTaskDescription(description)
    }

    @JvmStatic
    fun dips2pixels(dips: Int, context: Context): Int {
        return Math.round(dips * context.resources.displayMetrics.density + 0.5f)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    @JvmStatic
    fun decodeSampledBitmapFromResource(
        resources: Resources,
        resourceId: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, resourceId, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeResource(resources, resourceId, options)
    }

    @JvmStatic
    fun getProtocolName(protocol: Int, version: Int, brief: Boolean): String {
        // https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
        var p: String? = null
        var b: String? = null
        when (protocol) {
            0 -> {
                p = "HOPO"
                b = "H"
            }

            2 -> {
                p = "IGMP"
                b = "G"
            }

            1, 58 -> {
                p = "ICMP"
                b = "I"
            }

            6 -> {
                p = "TCP"
                b = "T"
            }

            17 -> {
                p = "UDP"
                b = "U"
            }

            50 -> {
                p = "ESP"
                b = "E"
            }
        }
        if (p == null) {
            return "$protocol/$version"
        }
        return (if (brief) b else p) + if (version > 0) version.toString() else ""
    }

    fun interface DoubtListener {
        fun onSure()
    }

    @JvmStatic
    fun areYouSure(context: Context, explanation: Int, listener: DoubtListener) {
        AlertDialog.Builder(context)
            .setMessage(explanation)
            .setCancelable(true)
            .setPositiveButton(R.string.menu_ok) { _, _ ->
                listener.onSure()
            }
            .setNegativeButton(R.string.menu_cancel) { _, _ ->
                // Do nothing
            }
            .create()
            .show()
    }

    private val mapIPOrganization = mutableMapOf<String, String?>()

    @JvmStatic
    @Throws(Exception::class)
    fun getOrganization(ip: String): String? {
        synchronized(mapIPOrganization) {
            if (mapIPOrganization.containsKey(ip)) {
                return mapIPOrganization[ip]
            }
        }
        var reader: BufferedReader? = null
        try {
            val url = URL("https://ipinfo.io/$ip/org")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.readTimeout = 15 * 1000
            connection.connect()
            reader = BufferedReader(InputStreamReader(connection.inputStream))
            var organization: String? = reader.readLine()
            if ("undefined" == organization) {
                organization = null
            }
            synchronized(mapIPOrganization) {
                mapIPOrganization[ip] = organization
            }
            return organization
        } finally {
            reader?.close()
        }
    }

    @JvmStatic
    @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
    fun md5(text: String, salt: String): String {
        // MD5
        val bytes =
            MessageDigest.getInstance("MD5").digest((text + salt).toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    @JvmStatic
    fun logExtras(intent: Intent?) {
        intent?.extras?.let { logBundle(it) }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun logBundle(data: Bundle?) {
        if (data != null) {
            val keys = data.keySet()
            val stringBuilder = StringBuilder()
            for (key in keys) {
                val value = data[key]
                stringBuilder
                    .append(key)
                    .append("=")
                    .append(value)
                    .append(if (value == null) "" else " (" + value.javaClass.simpleName + ")")
                    .append("\r\n")
            }
            Log.d(TAG, stringBuilder.toString())
        }
    }

    @JvmStatic
    fun readString(reader: InputStreamReader): StringBuilder {
        val sb = StringBuilder(2048)
        val read = CharArray(128)
        try {
            var count: Int
            while (reader.read(read).also { count = it } >= 0) {
                sb.append(read, 0, count)
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
        return sb
    }

    @JvmStatic
    fun sendCrashReport(ex: Throwable, context: Context) {
        if (!isPlayStoreInstall(context) || isDebuggable(context)) {
            return
        }

        try {
            val report = ApplicationErrorReport()
            report.packageName = context.packageName
            report.processName = context.packageName
            report.time = System.currentTimeMillis()
            report.type = ApplicationErrorReport.TYPE_CRASH
            report.systemApp = false

            val crash = ApplicationErrorReport.CrashInfo()
            crash.exceptionClassName = ex.javaClass.simpleName
            crash.exceptionMessage = ex.message

            val writer = StringWriter()
            val printer = PrintWriter(writer)
            ex.printStackTrace(printer)

            crash.stackTrace = writer.toString()

            val stack = ex.stackTrace[0]
            crash.throwClassName = stack.className
            crash.throwFileName = stack.fileName
            crash.throwLineNumber = stack.lineNumber
            crash.throwMethodName = stack.methodName

            report.crashInfo = crash

            val bug = Intent(Intent.ACTION_APP_ERROR)
            bug.putExtra(Intent.EXTRA_BUG_REPORT, report)
            bug.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (bug.resolveActivity(context.packageManager) != null) {
                context.startActivity(bug)
            }
        } catch (exex: Throwable) {
            Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getGeneralInfo(context: Context): String {
        val sb = StringBuilder()
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        sb.append(String.format("Interactive %B\r\n", isInteractive(context)))
        sb.append(String.format("Connected %B\r\n", isConnected(context)))
        sb.append(String.format("WiFi %B\r\n", isWifiActive(context)))
        sb.append(String.format("Metered %B\r\n", isMeteredNetwork(context)))
        sb.append(String.format("Roaming %B\r\n", isRoaming(context)))

        if (tm.simState == TelephonyManager.SIM_STATE_READY) {
            sb.append(
                String.format(
                    "SIM %s/%s/%s\r\n",
                    tm.simCountryIso,
                    tm.simOperatorName,
                    tm.simOperator,
                ),
            )
        }
        try {
            sb.append(
                String.format(
                    "Network %s/%s/%s\r\n",
                    tm.networkCountryIso,
                    tm.networkOperatorName,
                    tm.networkOperator,
                ),
            )
        } catch (_: Throwable) {
            // Ignored
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append(String.format("Power saving %B\r\n", pm.isPowerSaveMode))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sb.append(String.format("Battery optimizing %B\r\n", batteryOptimizing(context)))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sb.append(String.format("Data saving %B\r\n", dataSaving(context)))
        }

        if (sb.length > 2) {
            sb.setLength(sb.length - 2)
        }

        return sb.toString()
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getNetworkInfo(context: Context): String {
        val sb = StringBuilder()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = cm.activeNetwork

        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            val lp = cm.getLinkProperties(network)

            val transports =
                buildList {
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WIFI")
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("CELL")
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ETH")
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("BT")
                }.joinToString("+")

            val internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val roaming = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING).not()

            sb.append(if (transports.isBlank()) "UNKNOWN" else transports)
                .append(' ')
                .append(if (internet) "I" else "-")
                .append(if (validated) "V" else "-")
                .append(if (metered) " M" else "")
                .append(if (roaming) " R" else "")
                .append(if (network == active) " *" else "")

            if (lp?.interfaceName != null) {
                sb.append(' ').append(lp.interfaceName)
            }
            if (!lp?.linkAddresses.isNullOrEmpty()) {
                sb.append(" [")
                sb.append(lp?.linkAddresses?.joinToString { it.address.hostAddress ?: "" })
                sb.append("]")
            }
            sb.append("\r\n")
        }

        try {
            val nis = NetworkInterface.getNetworkInterfaces()
            if (nis != null) {
                while (nis.hasMoreElements()) {
                    val ni = nis.nextElement()
                    if (ni != null && !ni.isLoopback) {
                        val ias = ni.interfaceAddresses
                        if (ias != null) {
                            for (ia in ias) {
                                sb.append(ni.name)
                                    .append(' ')
                                    .append(ia.address.hostAddress)
                                    .append('/')
                                    .append(ia.networkPrefixLength)
                                    .append(' ')
                                    .append(ni.mtu)
                                    .append(' ')
                                    .append(if (ni.isUp) '^' else 'v')
                                    .append("\r\n")
                            }
                        }
                    }
                }
            }
        } catch (ex: Throwable) {
            sb.append(ex.toString()).append("\r\n")
        }

        if (sb.length > 2) {
            sb.setLength(sb.length - 2)
        }

        return sb.toString()
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.M)
    fun batteryOptimizing(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.N)
    fun dataSaving(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
    }

    @JvmStatic
    fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getTrafficLog(context: Context): StringBuilder {
        val sb = StringBuilder()
        DatabaseHelper.getInstance(context).getLog(true, true, true, true, true).use { cursor ->
            val colTime = cursor.getColumnIndex("time")
            val colVersion = cursor.getColumnIndex("version")
            val colProtocol = cursor.getColumnIndex("protocol")
            val colFlags = cursor.getColumnIndex("flags")
            val colSAddr = cursor.getColumnIndex("saddr")
            val colSPort = cursor.getColumnIndex("sport")
            val colDAddr = cursor.getColumnIndex("daddr")
            val colDPort = cursor.getColumnIndex("dport")
            val colDName = cursor.getColumnIndex("dname")
            val colUid = cursor.getColumnIndex("uid")
            val colData = cursor.getColumnIndex("data")
            val colAllowed = cursor.getColumnIndex("allowed")
            val colConnection = cursor.getColumnIndex("connection")
            val colInteractive = cursor.getColumnIndex("interactive")

            val format: DateFormat = SimpleDateFormat.getDateTimeInstance()

            var count = 0
            while (cursor.moveToNext() && ++count < 250) {
                sb.append(format.format(cursor.getLong(colTime)))
                sb.append(" v").append(cursor.getInt(colVersion))
                sb.append(" p").append(cursor.getInt(colProtocol))
                sb.append(' ').append(cursor.getString(colFlags))
                sb.append(' ').append(cursor.getString(colSAddr))
                sb.append('/').append(cursor.getInt(colSPort))
                sb.append(" > ").append(cursor.getString(colDAddr))
                sb.append('/').append(cursor.getString(colDName))
                sb.append('/').append(cursor.getInt(colDPort))
                sb.append(" u").append(cursor.getInt(colUid))
                sb.append(" a").append(cursor.getInt(colAllowed))
                sb.append(" c").append(cursor.getInt(colConnection))
                sb.append(" i").append(cursor.getInt(colInteractive))
                sb.append(' ').append(cursor.getString(colData))
                sb.append("\r\n")
            }
        }

        return sb
    }
}
