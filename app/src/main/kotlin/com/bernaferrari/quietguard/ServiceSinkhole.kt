package com.bernaferrari.quietguard

import com.bernaferrari.quietguard.shared.R
import com.bernaferrari.quietguard.netguard.NativePcapConfig
import com.bernaferrari.quietguard.netguard.NativeSocks5Config
import com.bernaferrari.quietguard.netguard.capabilities
import com.bernaferrari.quietguard.service.*

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import androidx.core.content.ContextCompat
import com.bernaferrari.quietguard.data.PreferencesRepository
import com.bernaferrari.quietguard.data.preferences
import com.bernaferrari.quietguard.ui.theme.GraphGrayed
import com.bernaferrari.quietguard.ui.theme.GraphReceive
import com.bernaferrari.quietguard.ui.theme.GraphSend
import com.bernaferrari.quietguard.ui.theme.THEME_DEFAULT
import com.bernaferrari.quietguard.ui.theme.themeOffColor
import com.bernaferrari.quietguard.ui.theme.themeOnColor
import com.bernaferrari.quietguard.ui.theme.themePrimaryColor
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantReadWriteLock

class ServiceSinkhole : VpnService() {
    @Volatile
    private var destroying = false

    private var registeredUser = false
    private var registeredIdleState = false
    private var registeredApState = false
    private var registeredPackageChanged = false

    private var registeredInteractiveState = false
    private var legacyCallStateToken: Any? = null
    private var legacyDataConnectionToken: Any? = null
    private var callStateCallback: TelephonyCallback? = null
    private var dataConnectionCallback: TelephonyCallback? = null
    private var lastGeneration: String? = null

    private var state = State.none
    private var userForeground = true
    private var lastConnected = false
    private var lastMetered = true
    private var lastInteractive = false

    private var lastAllowed = -1
    private var lastBlocked = -1
    private var lastHosts = -1

    private var removePrefsListener: (() -> Unit)? = null

    private val prefs: PreferencesRepository
        get() = applicationContext.preferences()
    private val notifications by lazy { SinkholeNotifications(this, prefs) }
    private val trafficNotifications by lazy { TrafficNotificationPresenter(this, prefs) }
    private val networkMonitor by lazy {
        SinkholeNetworkMonitor(this, prefs) { reason -> reload(reason, this, false) }
    }

    private var lastBuilder: TrackingVpnBuilder? = null
    private var vpn: ParcelFileDescriptor? = null
    private var temporarilyStopped = false

    private var lastHostsModified: Long = 0
    private var lastMalwareModified: Long = 0
    private val blockedHosts = HashSet<String>()
    private val malwareHosts = HashSet<String>()
    private val allowedUids = HashSet<Int>()
    private val knownUids = HashSet<Int>()
    private val mapUidIPFilters = HashMap<IpRuleKey, MutableMap<InetAddress, IpRule>>()
    private val mapForward = HashMap<Int, Forward>()
    private val mapNotify = HashMap<Int, Boolean>()
    private val lock = ReentrantReadWriteLock(true)

    private inline fun <T> withPolicyRead(block: () -> T): T {
        lock.readLock().lock()
        return try {
            block()
        } finally {
            lock.readLock().unlock()
        }
    }

    private inline fun <T> withPolicyWrite(block: () -> T): T {
        lock.writeLock().lock()
        return try {
            block()
        } finally {
            lock.writeLock().unlock()
        }
    }

    @Volatile
    private lateinit var commandLooper: Looper

    @Volatile
    private lateinit var logLooper: Looper

    @Volatile
    private lateinit var statsLooper: Looper

    @Volatile
    private lateinit var commandHandler: SinkholeCommandHandler

    @Volatile
    private lateinit var logHandler: SinkholeLogHandler

    @Volatile
    private lateinit var statsHandler: SinkholeStatsHandler
    private var statsWhenMs: Long = 0
    private val trafficStatistics = TrafficStatistics()

    private val executor: ExecutorService = Executors.newCachedThreadPool()

    private lateinit var nativeEngine: NativeVpnEngineController

    private val nativeCallbacks by lazy {
        NativeVpnCallbacks(
            uidFor = ::getUidQ,
            addressAllowed = ::isAddressAllowed,
            domainBlocked = ::isDomainBlocked,
            packetLogger = ::logPacket,
            dnsRecorder = ::dnsResolved,
            socketProtector = ::protect,
            usageRecorder = ::accountUsage,
            exitReporter = { nativeExit(it) },
        )
    }

    private fun handleCommandIntent(intent: Intent) {
            
            val cmd = getCommandExtra(intent) ?: return
            val reason = intent.getStringExtra(EXTRA_REASON)
            Log.i(
                TAG,
                "Executing intent=$intent command=$cmd reason=$reason" +
                        " vpn=" + (vpn != null) + " user=" + (Process.myUid() / 100000),
            )

            if (cmd != Command.stop && !userForeground) {
                Log.i(TAG, "Command $cmd ignored for background user")
                return
            }

            if (cmd == Command.stop) {
                temporarilyStopped = intent.getBooleanExtra(EXTRA_TEMPORARY, false)
            } else if (cmd == Command.start) {
                temporarilyStopped = false
            } else if (cmd == Command.reload && temporarilyStopped) {
                Log.i(TAG, "Command $cmd ignored because of temporary stop")
                return
            }

            if (prefs.getBoolean("screen_on", true)) {
                if (!registeredInteractiveState) {
                    Log.i(TAG, "Starting listening for interactive state changes")
                    lastInteractive = Util.isInteractive(this@ServiceSinkhole)
                    val ifInteractive = IntentFilter()
                    ifInteractive.addAction(Intent.ACTION_SCREEN_ON)
                    ifInteractive.addAction(Intent.ACTION_SCREEN_OFF)
                    ifInteractive.addAction(ACTION_SCREEN_OFF_DELAYED)
                    ContextCompat.registerReceiver(
                        this@ServiceSinkhole,
                        interactiveStateReceiver,
                        ifInteractive,
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                    registeredInteractiveState = true
                }
            } else {
                if (registeredInteractiveState) {
                    Log.i(TAG, "Stopping listening for interactive state changes")
                    unregisterReceiver(interactiveStateReceiver)
                    registeredInteractiveState = false
                    lastInteractive = false
                }
            }

            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            if (prefs.getBoolean("disable_on_call", false)) {
                if (tm != null) {
                    Log.i(TAG, "Starting listening for call states")
                    registerCallStateListener(tm)
                }
            } else {
                if (tm != null) {
                    Log.i(TAG, "Stopping listening for call states")
                    unregisterCallStateListener(tm)
                }
            }

            if (cmd == Command.start || cmd == Command.reload || cmd == Command.stop) {
                val watchdog = prefs.getString("watchdog", "0")?.toIntOrNull() ?: 0
                val enabled = prefs.getBoolean("enabled", false)
                WorkScheduler.scheduleWatchdog(
                    this@ServiceSinkhole,
                    watchdog,
                    enabled && cmd != Command.stop
                )
            }

            try {
                when (cmd) {
                    Command.run -> Unit
                    Command.start -> start()
                    Command.reload -> reload(intent.getBooleanExtra(EXTRA_INTERACTIVE, false))
                    Command.stop -> stop(temporarilyStopped)
                    Command.stats -> {
                        statsHandler.restart()
                    }

                    Command.householding -> householding(intent)
                    Command.watchdog -> watchdog(intent)
                    Command.updatecheck -> checkUpdateResult(checkUpdate())
                    else -> Log.e(TAG, "Unknown command=$cmd")
                }

                if (cmd == Command.start || cmd == Command.reload) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val filter = prefs.getBoolean("filter", false)
                        if (filter && isLockdownEnabled()) {
                            showLockdownNotification()
                        } else {
                            removeLockdownNotification()
                        }
                    }
                }

                if (cmd == Command.start || cmd == Command.reload || cmd == Command.stop) {
                    val ruleset = Intent(ActivityMain.ACTION_RULES_CHANGED).setPackage(packageName)
                    ruleset.putExtra(
                        ActivityMain.EXTRA_CONNECTED,
                        cmd != Command.stop && lastConnected
                    )
                    ruleset.putExtra(ActivityMain.EXTRA_METERED, cmd != Command.stop && lastMetered)
                    sendBroadcast(ruleset)

                    Widgets.updateFirewall(this@ServiceSinkhole)
                }

                if (
                    !commandHandler.hasMessages(Command.start.ordinal) &&
                    !commandHandler.hasMessages(Command.reload.ordinal) &&
                    !prefs.getBoolean("enabled", false) &&
                    !prefs.getBoolean("show_stats", false)
                ) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }

            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))

                if (cmd == Command.start || cmd == Command.reload) {
                    if (VpnService.prepare(this@ServiceSinkhole) == null) {
                        Log.w(TAG, "VPN prepared connected=$lastConnected")
                        if (lastConnected && ex !is StartFailedException) {
                            if (!Util.isPlayStoreInstall(this@ServiceSinkhole)) {
                                showErrorNotification(ex.toString())
                            }
                        }
                    } else {
                        showErrorNotification(ex.toString())

                        if (ex !is StartFailedException) {
                            prefs.putBoolean("enabled", false)
                            Widgets.updateFirewall(this@ServiceSinkhole)
                        }
                    }
                } else {
                    showErrorNotification(ex.toString())
                }
            }
        }

        private fun start() {
            if (vpn == null) {
                if (state != State.none) {
                    Log.d(TAG, "Stop foreground state=$state")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                startForeground(NOTIFY_ENFORCING, getEnforcingNotification(-1, -1, -1))
                state = State.enforcing
                Log.d(TAG, "Start foreground state=$state")

                val listRule = Rule.getRules(true, this@ServiceSinkhole)
                val listAllowed = getAllowedRules(listRule)

                lastBuilder = getBuilder(listAllowed, listRule)
                vpn = startVPN(lastBuilder!!)
                if (vpn == null) {
                    throw StartFailedException(getString((R.string.msg_start_failed)))
                }

                startNative(vpn!!, listAllowed, listRule)

                removeWarningNotifications()
                updateEnforcingNotification(listAllowed.size, listRule.size)
            }
        }

        private fun reload(interactive: Boolean) {
            val listRule = Rule.getRules(true, this@ServiceSinkhole)

            if (interactive) {
                var process = false
                for (rule in listRule) {
                    val blocked = if (lastMetered) rule.other_blocked else rule.wifi_blocked
                    val screen = if (lastMetered) rule.screen_other else rule.screen_wifi
                    if (blocked && screen) {
                        process = true
                        break
                    }
                }
                if (!process) {
                    Log.i(TAG, "No changed rules on interactive state change")
                    return
                }
            }

            
            if (state != State.enforcing) {
                if (state != State.none) {
                    Log.d(TAG, "Stop foreground state=$state")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                startForeground(NOTIFY_ENFORCING, getEnforcingNotification(-1, -1, -1))
                state = State.enforcing
                Log.d(TAG, "Start foreground state=$state")
            }

            val listAllowed = getAllowedRules(listRule)
            val builder = getBuilder(listAllowed, listRule)

            if (vpn != null && prefs.getBoolean("filter", false) && builder == lastBuilder) {
                Log.i(TAG, "Native restart")
                stopNative()
            } else {
                lastBuilder = builder
                vpn?.let {
                    stopNative()
                    stopVPN(it)
                }
                vpn = startVPN(builder)
            }

            if (vpn == null) {
                throw StartFailedException(getString((R.string.msg_start_failed)))
            }

            startNative(vpn!!, listAllowed, listRule)

            removeWarningNotifications()
            updateEnforcingNotification(listAllowed.size, listRule.size)
        }

        private fun stop(temporary: Boolean) {
            if (vpn != null) {
                stopNative()
                stopVPN(vpn!!)
                vpn = null
                unprepare()
            }
            if (state == State.enforcing && !temporary) {
                Log.d(TAG, "Stop foreground state=$state")
                lastAllowed = -1
                lastBlocked = -1
                lastHosts = -1

                stopForeground(STOP_FOREGROUND_REMOVE)

                                if (prefs.getBoolean("show_stats", false)) {
                    startForeground(NOTIFY_WAITING, getWaitingNotification())
                    state = State.waiting
                    Log.d(TAG, "Start foreground state=$state")
                } else {
                    state = State.none
                    stopSelf()
                }
            }
        }

        private fun householding(intent: Intent) {
                        val retentionDays =
                (prefs.getString("log_retention_days", "3")?.toIntOrNull() ?: 3)
                    .coerceIn(0, 365)
            if (retentionDays > 0) {
                val cutoffTime = Date().time - retentionDays * 24L * 3600L * 1000L
                DatabaseHelper.getInstance(this@ServiceSinkhole).cleanupLog(cutoffTime)
            } else {
                Log.i(TAG, "Log cleanup disabled by preference")
            }

            DatabaseHelper.getInstance(this@ServiceSinkhole).cleanupDns()

            if (
                !Util.isPlayStoreInstall(this@ServiceSinkhole) &&
                prefs.getBoolean("update_check", true)
            ) {
                checkUpdate()
            }
        }

        private fun watchdog(intent: Intent) {
            if (vpn == null) {
                                if (prefs.getBoolean("enabled", false)) {
                    Log.e(TAG, "Service was killed")
                    start()
                }
            }
        }

        private fun checkUpdateResult(result: UpdateCheckResult) {
            val resultIntent = Intent(ACTION_UPDATE_CHECK_RESULT).setPackage(packageName)
            resultIntent.putExtra(EXTRA_UPDATE_CHECK_STATUS, result.status.name)
            result.availableVersion?.let { resultIntent.putExtra(EXTRA_UPDATE_CHECK_VERSION, it) }
            sendBroadcast(resultIntent)
        }

        private fun checkUpdate(): UpdateCheckResult {
            val result = ReleaseUpdateChecker.check(
                apiUrl = BuildConfig.GITHUB_LATEST_API,
                currentVersion = Util.getSelfVersionName(this@ServiceSinkhole),
            )
            if (result.status == UpdateCheckStatus.available) {
                showUpdateNotification(requireNotNull(result.assetName), requireNotNull(result.releaseUrl))
            }
            return result
        }

    private class StartFailedException(msg: String) : IllegalStateException(msg)

    private fun onStatsStarted() {
        statsWhenMs = Date().time
        trafficStatistics.reset()
    }

    private fun onStatsStopped() {
        if (state == State.stats) {
            Log.d(TAG, "Stop foreground state=$state")
            stopForeground(STOP_FOREGROUND_REMOVE)
            state = State.none
        } else {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFY_TRAFFIC)
        }
    }

    private fun renderTrafficStats(): Long? {
            if (destroying) return null

            val frequency =
                (prefs.getString("stats_frequency", "1000")?.toLongOrNull() ?: 1000)
                    .coerceIn(MIN_STATS_FREQUENCY_MS, MAX_STATS_FREQUENCY_MS)
            val samples =
                (prefs.getString("stats_samples", "90")?.toLongOrNull() ?: 90)
                    .coerceIn(MIN_STATS_SAMPLES, MAX_STATS_SAMPLES)
            val filter = prefs.getBoolean("filter", false)
            val showTop = prefs.getBoolean("show_top", false)

            val ct = SystemClock.elapsedRealtime()

            var ttx = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
            var trx = TrafficStats.getTotalRxBytes().coerceAtLeast(0)
            if (filter) {
                ttx = (ttx - TrafficStats.getUidTxBytes(Process.myUid()).coerceAtLeast(0)).coerceAtLeast(0)
                trx = (trx - TrafficStats.getUidRxBytes(Process.myUid()).coerceAtLeast(0)).coerceAtLeast(0)
            }
            val rates = trafficStatistics.sample(ct, ttx, trx, samples)
            val txsec = rates.transmitPerSecond
            val rxsec = rates.receivePerSecond

            var topText = ""
            if (showTop) {
                val uidBytes = packageManager.getInstalledApplications(0)
                    .asSequence()
                    .map { it.uid }
                    .filter { it != Process.myUid() }
                    .distinct()
                    .associateWith { uid ->
                        TrafficStats.getUidTxBytes(uid).coerceAtLeast(0) +
                            TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
                    }
                if (!trafficStatistics.hasUidBaseline()) {
                    trafficStatistics.initializeUidBytes(uidBytes)
                } else {

                    val sb = StringBuilder()
                    var i = 0
                    for ((uid, speed) in trafficStatistics.uidRates(uidBytes)) {
                        if (i++ >= 3) break
                        if (speed < 1000 * 1000) {
                            sb.append(getString(R.string.msg_kbsec, speed / 1000))
                        } else {
                            sb.append(getString(R.string.msg_mbsec, speed / 1000 / 1000))
                        }
                        sb.append(' ')
                        val apps = Util.getApplicationNames(uid, this@ServiceSinkhole)
                        sb.append(if (apps.isNotEmpty()) apps[0] else "?")
                        sb.append("\r\n")
                    }
                    if (sb.isNotEmpty()) {
                        sb.setLength(sb.length - 2)
                        topText = sb.toString()
                    }
                }
            } else {
                trafficStatistics.clearUidBaseline()
            }

            val points = trafficStatistics.points()

            val height = Util.dips2pixels(96, this@ServiceSinkhole)
            val width = Util.dips2pixels(96 * 5, this@ServiceSinkhole)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            var max = 0f
            var xmax: Long = 0
            var ymax = 0f
            for (point in points) {
                if (point.timestampMs > xmax) xmax = point.timestampMs
                val tx = point.transmitPerSecond
                val rx = point.receivePerSecond
                if (tx > max) max = tx
                if (rx > max) max = rx
                if (tx > ymax) ymax = tx
                if (rx > ymax) ymax = rx
            }

            val ptx = Path()
            val prx = Path()
            for ((index, point) in points.withIndex()) {
                val x = width - width * (xmax - point.timestampMs) / 1000f / samples
                val ytx = height - height * point.transmitPerSecond / ymax.coerceAtLeast(1f)
                val yrx = height - height * point.receivePerSecond / ymax.coerceAtLeast(1f)
                if (index == 0) {
                    ptx.moveTo(x, ytx)
                    prx.moveTo(x, yrx)
                } else {
                    ptx.lineTo(x, ytx)
                    prx.lineTo(x, yrx)
                }
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE

            paint.strokeWidth = Util.dips2pixels(1, this@ServiceSinkhole).toFloat()
            paint.color = GraphGrayed
            val y = height / 2f
            canvas.drawLine(0f, y, width.toFloat(), y, paint)

            paint.strokeWidth = Util.dips2pixels(2, this@ServiceSinkhole).toFloat()
            paint.color = GraphSend
            canvas.drawPath(ptx, paint)
            paint.color = GraphReceive
            canvas.drawPath(prx, paint)

            val txText =
                if (txsec < 1000 * 1000) {
                    getString(R.string.msg_kbsec, txsec / 1000)
                } else {
                    getString(R.string.msg_mbsec, txsec / 1000 / 1000)
                }
            val rxText =
                if (rxsec < 1000 * 1000) {
                    getString(R.string.msg_kbsec, rxsec / 1000)
                } else {
                    getString(R.string.msg_mbsec, rxsec / 1000 / 1000)
                }
            val maxText =
                if (max < 1000 * 1000) {
                    getString(R.string.msg_kbsec, max / 2 / 1000)
                } else {
                    getString(R.string.msg_mbsec, max / 2 / 1000 / 1000)
                }
            val statsSummary =
                getString(
                    R.string.notify_traffic_summary,
                    txText,
                    rxText,
                    maxText,
                )
            val debugText =
                if (BuildConfig.DEBUG) {
                    val count = nativeEngine.stats()
                    getString(
                        R.string.notify_traffic_debug,
                        count.icmpSessions,
                        count.udpSessions,
                        count.tcpSessions,
                        count.openFileDescriptors,
                        count.fileDescriptorLimit,
                    )
                } else {
                    ""
                }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val extraLines = buildList {
                if (topText.isNotBlank()) {
                    addAll(topText.split("\r\n"))
                }
                if (debugText.isNotBlank()) {
                    add(debugText)
                }
            }
            val notification = trafficNotifications.build(
                TrafficNotificationContent(statsWhenMs, bitmap, statsSummary, extraLines),
            )

            if (state == State.none || state == State.waiting) {
                if (state != State.none) {
                    Log.d(TAG, "Stop foreground state=$state")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                startForeground(NOTIFY_TRAFFIC, notification)
                state = State.stats
                Log.d(TAG, "Start foreground state=$state")
            } else {
                if (Util.canNotify(this@ServiceSinkhole)) {
                    notificationManager.notify(NOTIFY_TRAFFIC, notification)
                }
            }
            return frequency
    }

    private fun startVPN(builder: TrackingVpnBuilder): ParcelFileDescriptor? {
        return try {
            val pfd = builder.establish()

            pfd
        } catch (ex: SecurityException) {
            throw ex
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            null
        }
    }

    private fun getBuilder(listAllowed: List<Rule>, listRule: List<Rule>): TrackingVpnBuilder {
                val subnet = prefs.getBoolean("subnet", false)
        val tethering = prefs.getBoolean("tethering", false)
        val lan = prefs.getBoolean("lan", false)
        val ip6 = prefs.getBoolean("ip6", true)
        val filter = prefs.getBoolean("filter", false)
        val system = prefs.getBoolean("manage_system", false)

        val builder = TrackingVpnBuilder()
        builder.setSession(getString(R.string.app_name))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(Util.isMeteredNetwork(this))
        }

        val vpn4 = prefs.getString("vpn4", "10.1.10.1") ?: "10.1.10.1"
        Log.i(TAG, "Using VPN4=$vpn4")
        builder.addAddress(vpn4, 32)
        if (ip6) {
            val vpn6 = prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1")
                ?: "fd00:1:fd00:1:fd00:1:fd00:1"
            Log.i(TAG, "Using VPN6=$vpn6")
            builder.addAddress(vpn6, 128)
        }

        if (filter) {
            for (dns in getDns(this@ServiceSinkhole)) {
                if (ip6 || dns is Inet4Address) {
                    Log.i(TAG, "Using DNS=$dns")
                    builder.addDnsServer(dns)
                }
            }
        }

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active = cm.activeNetwork
            val props = if (active == null) null else cm.getLinkProperties(active)
            val domain = props?.domains
            if (domain != null) {
                Log.i(TAG, "Using search domain=$domain")
                builder.addSearchDomain(domain)
            }
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }

        if (subnet) {
            val listExclude = ArrayList<IPUtil.CIDR>()
            listExclude.add(IPUtil.CIDR("127.0.0.0", 8))

            if (tethering && !lan) {
                listExclude.add(IPUtil.CIDR("192.168.42.0", 23))
                listExclude.add(IPUtil.CIDR("192.168.44.0", 24))
                listExclude.add(IPUtil.CIDR("192.168.49.0", 24))

                try {
                    val nis = NetworkInterface.getNetworkInterfaces()
                    if (nis != null) {
                        while (nis.hasMoreElements()) {
                            val ni = nis.nextElement()
                            if (
                                ni != null &&
                                !ni.isLoopback &&
                                ni.isUp &&
                                ni.name != null &&
                                ni.name.startsWith("ap_br_wlan")
                            ) {
                                val ias = ni.interfaceAddresses
                                if (ias != null) {
                                    for (ia in ias) {
                                        if (ia.address is Inet4Address) {
                                            val host = ia.address.hostAddress
                                            if (host != null) {
                                                listExclude.add(IPUtil.CIDR(host, 24))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString())
                }
            }

            if (lan) {
                listExclude.add(IPUtil.CIDR("10.0.0.0", 8))
                listExclude.add(IPUtil.CIDR("172.16.0.0", 12))
                listExclude.add(IPUtil.CIDR("192.168.0.0", 16))
            }

            if (!filter) {
                for (dns in getDns(this@ServiceSinkhole)) {
                    if (dns is Inet4Address) {
                        val host = dns.hostAddress
                        if (host != null) {
                            listExclude.add(IPUtil.CIDR(host, 32))
                        }
                    }
                }

                val dnsSpecifier = Util.getPrivateDnsSpecifier(this@ServiceSinkhole)
                if (!dnsSpecifier.isNullOrEmpty()) {
                    try {
                        Log.i(TAG, "Resolving private dns=$dnsSpecifier")
                        for (pdns in InetAddress.getAllByName(dnsSpecifier)) {
                            if (pdns is Inet4Address) {
                                val host = pdns.hostAddress
                                if (host != null) {
                                    listExclude.add(IPUtil.CIDR(host, 32))
                                }
                            }
                        }
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString())
                    }
                }
            }

            val config: Configuration = resources.configuration

            if (
                config.mcc == 310 &&
                (config.mnc == 160 ||
                        config.mnc == 200 ||
                        config.mnc == 210 ||
                        config.mnc == 220 ||
                        config.mnc == 230 ||
                        config.mnc == 240 ||
                        config.mnc == 250 ||
                        config.mnc == 260 ||
                        config.mnc == 270 ||
                        config.mnc == 310 ||
                        config.mnc == 490 ||
                        config.mnc == 660 ||
                        config.mnc == 800)
            ) {
                listExclude.add(IPUtil.CIDR("66.94.2.0", 24))
                listExclude.add(IPUtil.CIDR("66.94.6.0", 23))
                listExclude.add(IPUtil.CIDR("66.94.8.0", 22))
                listExclude.add(IPUtil.CIDR("208.54.0.0", 16))
            }

            if (
                (config.mcc == 310 &&
                        (config.mnc == 4 ||
                                config.mnc == 5 ||
                                config.mnc == 6 ||
                                config.mnc == 10 ||
                                config.mnc == 12 ||
                                config.mnc == 13 ||
                                config.mnc == 350 ||
                                config.mnc == 590 ||
                                config.mnc == 820 ||
                                config.mnc == 890 ||
                                config.mnc == 910)) ||
                (config.mcc == 311 &&
                        (config.mnc == 12 ||
                                config.mnc == 110 ||
                                (config.mnc >= 270 && config.mnc <= 289) ||
                                config.mnc == 390 ||
                                (config.mnc >= 480 && config.mnc <= 489) ||
                                config.mnc == 590)) ||
                (config.mcc == 312 && config.mnc == 770)
            ) {
                listExclude.add(IPUtil.CIDR("66.174.0.0", 16))
                listExclude.add(IPUtil.CIDR("66.82.0.0", 15))
                listExclude.add(IPUtil.CIDR("69.96.0.0", 13))
                listExclude.add(IPUtil.CIDR("70.192.0.0", 11))
                listExclude.add(IPUtil.CIDR("97.128.0.0", 9))
                listExclude.add(IPUtil.CIDR("174.192.0.0", 9))
                listExclude.add(IPUtil.CIDR("72.96.0.0", 9))
                listExclude.add(IPUtil.CIDR("75.192.0.0", 9))
                listExclude.add(IPUtil.CIDR("97.0.0.0", 10))
            }

            if (config.mnc == 10 && config.mcc == 208) {
                listExclude.add(IPUtil.CIDR("10.151.0.0", 24))
            }

            listExclude.add(IPUtil.CIDR("224.0.0.0", 3))

            listExclude.sort()

            try {
                var start = InetAddress.getByName("0.0.0.0")
                for (exclude in listExclude) {
                    val excludeStart = exclude.getStart() ?: continue
                    val excludeEnd = exclude.getEnd() ?: continue
                    Log.i(
                        TAG,
                        "Exclude " +
                                excludeStart.hostAddress +
                                "..." +
                                excludeEnd.hostAddress,
                    )
                    val before = IPUtil.minus1(excludeStart) ?: continue
                    for (include in IPUtil.toCIDR(start, before)) {
                        val address = include.address ?: continue
                        try {
                            builder.addRoute(address, include.prefix)
                        } catch (ex: Throwable) {
                            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                        }
                    }
                    start = IPUtil.plus1(excludeEnd) ?: start
                }
                val end = if (lan) "255.255.255.254" else "255.255.255.255"
                for (include in IPUtil.toCIDR("224.0.0.0", end)) {
                    val address = include.address ?: continue
                    try {
                        builder.addRoute(address, include.prefix)
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                    }
                }
            } catch (ex: UnknownHostException) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        } else {
            builder.addRoute("0.0.0.0", 0)
        }

        Log.i(TAG, "IPv6=$ip6")
        if (ip6) {
            builder.addRoute("2000::", 3)
        }

        val mtu = capabilities().mtu.toInt()
        Log.i(TAG, "MTU=$mtu")
        builder.setMtu(mtu)

        if (lastConnected && !filter) {
            val mapDisallowed = HashMap<String, Rule>()
            for (rule in listRule) {
                rule.packageName?.let { mapDisallowed[it] = rule }
            }
            for (rule in listAllowed) {
                rule.packageName?.let { mapDisallowed.remove(it) }
            }
            for (packageName in mapDisallowed.keys) {
                try {
                    builder.addAllowedApplication(packageName)
                    Log.i(TAG, "Sinkhole $packageName")
                } catch (ex: PackageManager.NameNotFoundException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
            if (mapDisallowed.isEmpty()) {
                try {
                    builder.addAllowedApplication(packageName)
                } catch (ex: PackageManager.NameNotFoundException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
        } else if (filter) {
            try {
                builder.addDisallowedApplication(packageName)
            } catch (ex: PackageManager.NameNotFoundException) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
            for (rule in listRule) {
                if (!rule.apply || (!system && rule.system)) {
                    try {
                        Log.i(TAG, "Not routing " + rule.packageName)
                        rule.packageName?.let { builder.addDisallowedApplication(it) }
                    } catch (ex: PackageManager.NameNotFoundException) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                    }
                }
            }
        }

        val configure = Intent(this, ActivityMain::class.java)
        val pi =
            PendingIntentCompat.getActivity(this, 0, configure, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setConfigureIntent(pi)

        return builder
    }

    private fun startNative(
        vpn: ParcelFileDescriptor,
        listAllowed: List<Rule>,
        listRule: List<Rule>
    ) {
                val log = prefs.getBoolean("log", false)
        val filter = prefs.getBoolean("filter", false)

        Log.i(TAG, "Start native log=$log filter=$filter")

        if (filter) {
            prepareUidAllowed(listAllowed, listRule)
            prepareHostsBlocked()
            prepareMalwareList()
            prepareUidIPFilters(null)
            prepareForwarding()
        } else {
            withPolicyWrite {
                allowedUids.clear()
                knownUids.clear()
                blockedHosts.clear()
                malwareHosts.clear()
                mapUidIPFilters.clear()
                mapForward.clear()
            }
        }

        if (log) {
            prepareNotify(listRule)
        } else {
            withPolicyWrite { mapNotify.clear() }
        }

        if (log || filter) {
            val rcode = prefs.getString("rcode", "3")?.toIntOrNull() ?: 3
            if (prefs.getBoolean("socks5_enabled", false)) {
                nativeEngine.configureSocks5(
                    NativeSocks5Config(
                        prefs.getString("socks5_addr", "") ?: "",
                        prefs.getString("socks5_port", "0")?.toIntOrNull() ?: 0,
                        prefs.getString("socks5_username", "") ?: "",
                        prefs.getString("socks5_password", "") ?: "",
                    ),
                )
            } else {
                nativeEngine.configureSocks5(null)
            }

            Log.i(TAG, "Starting UniFFI tunnel engine")
            nativeEngine.start(vpn.fd, withPolicyRead { 53 in mapForward }, rcode)
            Log.i(TAG, "Started tunnel thread")
        }
    }

    private fun stopNative() {
        Log.i(TAG, "Stop native")

        nativeEngine.stop()
        Log.i(TAG, "Stopped tunnel thread")
    }

    private fun unprepare() {
        withPolicyWrite {
            allowedUids.clear()
            knownUids.clear()
            blockedHosts.clear()
            malwareHosts.clear()
            mapUidIPFilters.clear()
            mapForward.clear()
            mapNotify.clear()
        }
    }

    private fun prepareUidAllowed(listAllowed: List<Rule>, listRule: List<Rule>) {
        withPolicyWrite {
            allowedUids.clear()
            allowedUids.addAll(listAllowed.mapTo(HashSet()) { it.uid })
            knownUids.clear()
            knownUids.addAll(listRule.mapTo(HashSet()) { it.uid })
        }
    }

    private fun prepareHostsBlocked() {
        val useHosts = prefs.getBoolean("filter", false) && prefs.getBoolean("use_hosts", false)
        val hosts = File(filesDir, "hosts.txt")
        if (!useHosts || !hosts.exists() || !hosts.canRead()) {
            Log.i(TAG, "Hosts file use=$useHosts exists=${hosts.exists()}")
            withPolicyWrite { blockedHosts.clear() }
            return
        }

        val changed = hosts.lastModified() != lastHostsModified
        if (!changed && blockedHosts.isNotEmpty()) {
            Log.i(TAG, "Hosts file unchanged")
            return
        }
        val domains = DomainListLoader.load(hosts, true) { line ->
            Log.i(TAG, "Invalid hosts file line: $line")
        } ?: run {
            Log.e(TAG, "Unable to read hosts file")
            return
        }
        withPolicyWrite {
            blockedHosts.clear()
            blockedHosts.addAll(domains)
            blockedHosts.add("test.netguard.me")
            lastHostsModified = hosts.lastModified()
        }
        Log.i(TAG, "${domains.size} hosts read")
    }

    private fun prepareMalwareList() {
        val malware = prefs.getBoolean("filter", false) && prefs.getBoolean("malware", false)
        val file = File(filesDir, "malware.txt")
        if (!malware || !file.exists() || !file.canRead()) {
            Log.i(TAG, "Malware use=$malware exists=${file.exists()}")
            withPolicyWrite { malwareHosts.clear() }
            return
        }

        val changed = file.lastModified() != lastMalwareModified
        if (!changed && malwareHosts.isNotEmpty()) {
            Log.i(TAG, "Malware unchanged")
            return
        }
        val domains = DomainListLoader.load(file, false) { line ->
            Log.i(TAG, "Invalid malware file line: $line")
        } ?: run {
            Log.e(TAG, "Unable to read malware file")
            return
        }
        withPolicyWrite {
            malwareHosts.clear()
            malwareHosts.addAll(domains)
            lastMalwareModified = file.lastModified()
        }
        Log.i(TAG, "${domains.size} malware read")
    }

    private fun prepareUidIPFilters(dname: String?) {
        withPolicyWrite {
            if (dname == null) {
                mapUidIPFilters.clear()
                if (!IAB.isPurchased(ActivityPro.SKU_FILTER, this@ServiceSinkhole)) {
                    return
                }
            }

            DatabaseHelper.getInstance(this@ServiceSinkhole).getAccessDns(dname).use { cursor ->
            val colUid = cursor.getColumnIndex("uid")
            val colVersion = cursor.getColumnIndex("version")
            val colProtocol = cursor.getColumnIndex("protocol")
            val colDAddr = cursor.getColumnIndex("daddr")
            val colResource = cursor.getColumnIndex("resource")
            val colDPort = cursor.getColumnIndex("dport")
            val colBlock = cursor.getColumnIndex("block")
            val colTime = cursor.getColumnIndex("time")
            val colTTL = cursor.getColumnIndex("ttl")
            while (cursor.moveToNext()) {
                val uid = cursor.getInt(colUid)
                val version = cursor.getInt(colVersion)
                val protocol = cursor.getInt(colProtocol)
                val daddr = cursor.getString(colDAddr)
                val dresource =
                    if (cursor.isNull(colResource)) null else cursor.getString(colResource)
                val dport = cursor.getInt(colDPort)
                val block = cursor.getInt(colBlock) > 0
                val time = if (cursor.isNull(colTime)) Date().time else cursor.getLong(colTime)
                val ttl =
                    if (cursor.isNull(colTTL)) 7 * 24 * 3600 * 1000L else cursor.getLong(colTTL)

                if (isLockedDown(lastMetered)) {
                    val pkg = packageManager.getPackagesForUid(uid)
                    if (pkg != null && pkg.isNotEmpty()) {
                        if (!prefs.getBoolean(PreferencesRepository.namespaced("lockdown", pkg[0]), false)) {
                            continue
                        }
                    }
                }

                val key = IpRuleKey(version, protocol, dport, uid)
                if (!mapUidIPFilters.containsKey(key)) {
                    mapUidIPFilters[key] = HashMap()
                }

                try {
                        val name = if (dresource == null) daddr else dresource
                        if (Util.isNumericAddress(name)) {
                            val iname = InetAddress.getByName(name)
                            if (version == 4 && iname !is Inet4Address) {
                                continue
                            }
                            if (version == 6 && iname !is Inet6Address) {
                                continue
                            }

                            val exists = mapUidIPFilters[key]?.containsKey(iname) == true
                            val currentRule = mapUidIPFilters[key]?.get(iname)
                            if (!exists || currentRule?.isBlocked() == false) {
                                val rule = IpRule(key, "$name/$iname", block, time, ttl)
                                mapUidIPFilters[key]?.put(iname, rule)
                                if (exists) {
                                    Log.w(TAG, "Address conflict $key $daddr/$dresource")
                                }
                            } else if (exists) {
                                currentRule?.refresh(time, ttl)
                                if (dname != null && ttl > 60 * 1000L) {
                                    Log.w(TAG, "Address updated $key $daddr/$dresource")
                                }
                            } else {
                                if (dname != null) {
                                    Log.i(TAG, "Ignored $key $daddr/$dresource=$block")
                                }
                            }
                        } else {
                            Log.w(TAG, "Address not numeric $name")
                        }
                } catch (ex: UnknownHostException) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
            }
        }
    }

    private fun prepareForwarding() {
        withPolicyWrite {
            mapForward.clear()
            if (prefs.getBoolean("filter", false)) {
                DatabaseHelper.getInstance(this@ServiceSinkhole).getForwarding().use { cursor ->
                    val colProtocol = cursor.getColumnIndex("protocol")
                    val colDPort = cursor.getColumnIndex("dport")
                    val colRAddr = cursor.getColumnIndex("raddr")
                    val colRPort = cursor.getColumnIndex("rport")
                    val colRUid = cursor.getColumnIndex("ruid")
                    while (cursor.moveToNext()) {
                        val fwd = Forward()
                        fwd.protocol = cursor.getInt(colProtocol)
                        fwd.dport = cursor.getInt(colDPort)
                        fwd.raddr = cursor.getString(colRAddr)
                        fwd.rport = cursor.getInt(colRPort)
                        fwd.ruid = cursor.getInt(colRUid)
                        mapForward[fwd.dport] = fwd
                        Log.i(TAG, "Forward $fwd")
                    }
                }
            }
        }
    }

    private fun prepareNotify(listRule: List<Rule>) {
                val notify = prefs.getBoolean("notify_access", false)
        val system = prefs.getBoolean("manage_system", false)

        withPolicyWrite {
            mapNotify.clear()
            for (rule in listRule) {
                mapNotify[rule.uid] = notify && rule.notify && (system || !rule.system)
            }
        }
    }

    private fun isLockedDown(metered: Boolean): Boolean {
                var lockdown = prefs.getBoolean("lockdown", false)
        val lockdownWifi = prefs.getBoolean("lockdown_wifi", true)
        val lockdownOther = prefs.getBoolean("lockdown_other", true)
        if (metered) {
            if (!lockdownOther) lockdown = false
        } else {
            if (!lockdownWifi) lockdown = false
        }
        return lockdown
    }

    private fun getAllowedRules(listRule: List<Rule>): List<Rule> {
        val listAllowed = ArrayList<Rule>()
        
        val wifi = Util.isWifiActive(this)
        var metered = Util.isMeteredNetwork(this)
        val useMetered = prefs.getBoolean("use_metered", false)
        val ssidHomes = prefs.getStringSet("wifi_homes", emptySet()).toMutableSet()
        val ssidNetwork = Util.getWifiSSID(this)
        val generation = Util.getNetworkGeneration(this)
        val unmetered2g = prefs.getBoolean("unmetered_2g", false)
        val unmetered3g = prefs.getBoolean("unmetered_3g", false)
        val unmetered4g = prefs.getBoolean("unmetered_4g", false)
        var roaming = Util.isRoaming(this@ServiceSinkhole)
        val national = prefs.getBoolean("national_roaming", false)
        val eu = prefs.getBoolean("eu_roaming", false)
        val tethering = prefs.getBoolean("tethering", false)
        val filter = prefs.getBoolean("filter", false)

        lastConnected = Util.isConnected(this@ServiceSinkhole)

        val orgMetered = metered
        val orgRoaming = roaming

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            ssidHomes.clear()
        }

        if (wifi && !useMetered) {
            metered = false
        }
        if (
            wifi &&
            ssidHomes.isNotEmpty() &&
            !(ssidHomes.contains(ssidNetwork) || ssidHomes.contains('"' + ssidNetwork + '"'))
        ) {
            metered = true
            Log.i(TAG, "!@home=$ssidNetwork homes=" + TextUtils.join(",", ssidHomes))
        }
        if (unmetered2g && "2G" == generation) metered = false
        if (unmetered3g && "3G" == generation) metered = false
        if (unmetered4g && "4G" == generation) metered = false
        lastMetered = metered

        val lockdown = isLockedDown(lastMetered)

        if (roaming && eu) roaming = !Util.isEU(this)
        if (roaming && national) roaming = !Util.isNational(this)

        Log.i(
            TAG,
            "Get allowed" +
                    " connected=" + lastConnected +
                    " wifi=" + wifi +
                    " home=" + TextUtils.join(",", ssidHomes) +
                    " network=" + ssidNetwork +
                    " metered=" + metered + "/" + orgMetered +
                    " generation=" + generation +
                    " roaming=" + roaming + "/" + orgRoaming +
                    " interactive=" + lastInteractive +
                    " tethering=" + tethering +
                    " filter=" + filter +
                    " lockdown=" + lockdown,
        )

        if (lastConnected) {
            for (rule in listRule) {
                val blocked = if (metered) rule.other_blocked else rule.wifi_blocked
                val screen = if (metered) rule.screen_other else rule.screen_wifi
                if (
                    (!blocked || (screen && lastInteractive)) &&
                    (!metered || !(rule.roaming && roaming)) &&
                    (!lockdown || rule.lockdown)
                ) {
                    listAllowed.add(rule)
                }
            }
        }

        Log.i(TAG, "Allowed ${listAllowed.size} of ${listRule.size}")
        return listAllowed
    }

    private fun stopVPN(pfd: ParcelFileDescriptor) {
        Log.i(TAG, "Stopping")
        try {
            pfd.close()
        } catch (ex: IOException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    private fun nativeExit(reason: String?) {
        Log.w(TAG, "Native exit reason=$reason")
        if (reason != null) {
            showErrorNotification(reason)

            prefs.putBoolean("enabled", false)
            Widgets.updateFirewall(this)
        }
    }

    private fun nativeError(error: Int, message: String?) {
        Log.w(TAG, "Native error $error: $message")
        showErrorNotification(message ?: "")
    }

    private fun logPacket(packet: Packet) {
        val connection = if (lastConnected) if (lastMetered) 2 else 1 else 0
        logHandler.queue(packet, connection, lastInteractive)
    }

    private fun dnsResolved(rr: ResourceRecord) {
        if (DatabaseHelper.getInstance(this@ServiceSinkhole).insertDns(rr)) {
            Log.i(TAG, "New IP $rr")
            prepareUidIPFilters(rr.QName)
        }
        if (rr.uid > 0 && !TextUtils.isEmpty(rr.AName)) {
            val malware = withPolicyRead { rr.AName in malwareHosts }

            if (malware) {
                val notified = prefs.getBoolean("malware.${rr.uid}", false)
                if (!notified) {
                    prefs.putBoolean("malware.${rr.uid}", true)
                    notifyNewApplication(rr.uid, true)
                }
            }
        }
    }

    private fun isDomainBlocked(name: String): Boolean {
        return withPolicyRead { name in blockedHosts }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun getUidQ(
        version: Int,
        protocol: Int,
        saddr: String,
        sport: Int,
        daddr: String,
        dport: Int
    ): Int {
        if (protocol != 6 && protocol != 17) {
            return Process.INVALID_UID
        }

        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return Process.INVALID_UID

        val local = InetSocketAddress(saddr, sport)
        val remote = InetSocketAddress(daddr, dport)

        Log.i(TAG, "Get uid local=$local remote=$remote")
        val uid = cm.getConnectionOwnerUid(protocol, local, remote)
        Log.i(TAG, "Get uid=$uid")
        return uid
    }

    private fun isSupported(protocol: Int): Boolean {
        return protocol == 1 || protocol == 58 || protocol == 6 || protocol == 17
    }

    private fun isAddressAllowed(packet: Packet): Allowed? {
        val allowed = withPolicyRead {

        packet.allowed = false
        if (prefs.getBoolean("filter", false)) {
            if (packet.protocol == 17 && !prefs.getBoolean("filter_udp", false)) {
                packet.allowed = true
                Log.i(TAG, "Allowing UDP $packet")
            } else if (packet.uid < 2000 && !lastConnected && isSupported(packet.protocol) && false) {
                packet.allowed = true
                Log.w(TAG, "Allowing disconnected system $packet")
            } else if (
                (packet.uid < 2000 || BuildConfig.PLAY_STORE_RELEASE) &&
                packet.uid !in knownUids &&
                isSupported(packet.protocol)
            ) {
                packet.allowed = true
                Log.w(TAG, "Allowing unknown system $packet")
            } else if (packet.uid == Process.myUid()) {
                packet.allowed = true
                Log.w(TAG, "Allowing self $packet")
            } else {
                var filtered = false
                val key = IpRuleKey(packet.version, packet.protocol, packet.dport, packet.uid)
                if (mapUidIPFilters.containsKey(key)) {
                    try {
                        val daddr = packet.daddr
                        if (daddr == null) {
                            return@withPolicyRead null
                        }
                        val iaddr = InetAddress.getByName(daddr)
                        val map = mapUidIPFilters[key]
                        if (map != null && map.containsKey(iaddr)) {
                            val rule = map[iaddr]
                            if (rule != null && rule.isExpired()) {
                                Log.i(TAG, "DNS expired $packet rule $rule")
                            } else if (rule != null) {
                                filtered = true
                                packet.allowed = !rule.isBlocked()
                                Log.i(TAG, "Filtering $packet allowed=${packet.allowed} rule $rule")
                            }
                        }
                    } catch (ex: UnknownHostException) {
                        Log.w(TAG, "Allowed $ex\n" + Log.getStackTraceString(ex))
                    }
                }

                if (!filtered) {
                    if (packet.uid in allowedUids) {
                        packet.allowed = true
                    } else {
                        Log.w(TAG, "No rules for $packet")
                    }
                }
            }
        }

        var redirect: Allowed? = null
        if (packet.allowed) {
            if (mapForward.containsKey(packet.dport)) {
                val fwd = mapForward[packet.dport]
                if (fwd != null) {
                    redirect =
                        if (fwd.ruid == packet.uid) {
                            Allowed()
                        } else {
                            packet.data = "> " + fwd.raddr + "/" + fwd.rport
                            Allowed(fwd.raddr, fwd.rport)
                        }
                }
            } else {
                redirect = Allowed()
            }
        }

        redirect
        }

        if (prefs.getBoolean("log", false)) {
            if (packet.protocol != 6 || packet.flags != "") {
                if (packet.uid != Process.myUid()) {
                    logPacket(packet)
                }
            }
        }

        return allowed
    }

    private fun accountUsage(usage: Usage) {
        logHandler.account(usage)
    }

    private val interactiveStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                if (destroying || executor.isShutdown) return
                try {
                    executor.execute {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val i = Intent(ACTION_SCREEN_OFF_DELAYED)
                    i.setPackage(context.packageName)
                    val pi =
                        PendingIntentCompat.getBroadcast(
                            context,
                            0,
                            i,
                            PendingIntent.FLAG_UPDATE_CURRENT,
                        )
                    am.cancel(pi)

                    try {
                                                val delay = prefs.getString("screen_delay", "0")?.toIntOrNull() ?: 0
                        val interactive = Intent.ACTION_SCREEN_ON == intent.action

                        if (interactive || delay == 0) {
                            lastInteractive = interactive
                            reload("interactive state changed", this@ServiceSinkhole, true)
                        } else {
                            if (ACTION_SCREEN_OFF_DELAYED == intent.action) {
                                lastInteractive = interactive
                                reload("interactive state changed", this@ServiceSinkhole, true)
                            } else {
                                am.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    Date().time + delay * 60 * 1000L,
                                    pi,
                                )
                            }
                        }

                        if (Util.isInteractive(this@ServiceSinkhole)) statsHandler.start()
                        else statsHandler.stop()
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))

                        am.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            Date().time + 15 * 1000L,
                            pi,
                        )
                    }
                    }
                } catch (_: RejectedExecutionException) {
                    Log.i(TAG, "Ignoring interactive change after executor shutdown")
                }
            }
        }

    private val userReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                userForeground = Intent.ACTION_USER_FOREGROUND == intent.action
                Log.i(TAG, "User foreground=$userForeground user=" + (Process.myUid() / 100000))

                if (userForeground) {
                    if (prefs.getBoolean("enabled", false) && !destroying) {
                        try {
                            executor.execute {
                                try {
                                    Thread.sleep(USER_FOREGROUND_DELAY_MS)
                                } catch (_: InterruptedException) {
                                    Thread.currentThread().interrupt()
                                    return@execute
                                }
                                if (!destroying && prefs.getBoolean("enabled", false)) {
                                    start("foreground", this@ServiceSinkhole)
                                }
                            }
                        } catch (_: RejectedExecutionException) {
                            Log.i(TAG, "Ignoring user foreground after executor shutdown")
                        }
                    }
                } else {
                    stop("background", this@ServiceSinkhole, true)
                }
            }
        }

    private val idleStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                Log.i(TAG, "device idle=" + pm.isDeviceIdleMode)

                if (!pm.isDeviceIdleMode) {
                    reload("idle state changed", this@ServiceSinkhole, false)
                }
            }
        }

    private val apStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)
                reload("AP state changed", this@ServiceSinkhole, false)
            }
        }

    private fun handleDataConnectionStateChanged(state: Int) {
        if (state == TelephonyManager.DATA_CONNECTED) {
            val currentGeneration = Util.getNetworkGeneration(this)
            Log.i(TAG, "Data connected generation=$currentGeneration")

            if (lastGeneration == null || lastGeneration != currentGeneration) {
                Log.i(TAG, "New network generation=$currentGeneration")
                lastGeneration = currentGeneration

                if (
                    prefs.getBoolean("unmetered_2g", false) ||
                    prefs.getBoolean("unmetered_3g", false) ||
                    prefs.getBoolean("unmetered_4g", false)
                ) {
                    reload("data connection state changed", this, false)
                }
            }
        }
    }

    private fun handleCallStateChanged(state: Int) {
        Log.i(TAG, "New call state=$state")
        if (prefs.getBoolean("enabled", false)) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                start("call state", this)
            } else {
                stop("call state", this, true)
            }
        }
    }

    private fun registerCallStateListener(tm: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (callStateCallback == null) {
                val callback =
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallStateChanged(state)
                        }
                    }
                tm.registerTelephonyCallback(ContextCompat.getMainExecutor(this), callback)
                callStateCallback = callback
            }
        } else if (legacyCallStateToken == null && Util.hasPhoneStatePermission(this)) {
            legacyCallStateToken = LegacyTelephony.registerCallState(tm) { state ->
                handleCallStateChanged(state)
            }
        }
    }

    private fun unregisterCallStateListener(tm: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callStateCallback?.let { tm.unregisterTelephonyCallback(it) }
            callStateCallback = null
        } else if (legacyCallStateToken != null) {
            LegacyTelephony.unregisterCallState(tm, legacyCallStateToken)
            legacyCallStateToken = null
        }
    }

    private fun registerDataConnectionListener(tm: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dataConnectionCallback == null) {
                val callback =
                    object : TelephonyCallback(), TelephonyCallback.DataConnectionStateListener {
                        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
                            handleDataConnectionStateChanged(state)
                        }
                    }
                tm.registerTelephonyCallback(ContextCompat.getMainExecutor(this), callback)
                dataConnectionCallback = callback
            }
        } else if (legacyDataConnectionToken == null) {
            legacyDataConnectionToken = LegacyTelephony.registerDataConnection(tm) { state ->
                handleDataConnectionStateChanged(state)
            }
        }
    }

    private fun unregisterDataConnectionListener(tm: TelephonyManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dataConnectionCallback?.let { tm.unregisterTelephonyCallback(it) }
            dataConnectionCallback = null
        } else if (legacyDataConnectionToken != null) {
            LegacyTelephony.unregisterDataConnection(tm, legacyDataConnectionToken)
            legacyDataConnectionToken = null
        }
    }

    private val packageChangedReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                try {
                    if (Intent.ACTION_PACKAGE_ADDED == intent.action) {
                        Rule.clearCache(context)

                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                                                        if (IAB.isPurchased(
                                    ActivityPro.SKU_NOTIFY,
                                    context
                                ) && prefs.getBoolean("install", true)
                            ) {
                                val uid = intent.getIntExtra(Intent.EXTRA_UID, -1)
                                notifyNewApplication(uid, false)
                            }
                        }

                        reload("package added", context, false)
                    } else if (Intent.ACTION_PACKAGE_REMOVED == intent.action) {
                        Rule.clearCache(context)

                        if (intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)) {
                            val packageName = intent.data?.schemeSpecificPart ?: ""
                            Log.i(TAG, "Deleting settings package=$packageName")
                            prefs.remove(PreferencesRepository.namespaced("wifi", packageName))
                            prefs.remove(PreferencesRepository.namespaced("other", packageName))
                            prefs.remove(PreferencesRepository.namespaced("screen_wifi", packageName))
                            prefs.remove(PreferencesRepository.namespaced("screen_other", packageName))
                            prefs.remove(PreferencesRepository.namespaced("roaming", packageName))
                            prefs.remove(PreferencesRepository.namespaced("lockdown", packageName))
                            prefs.remove(PreferencesRepository.namespaced("apply", packageName))
                            prefs.remove(PreferencesRepository.namespaced("notify", packageName))

                            val uid = intent.getIntExtra(Intent.EXTRA_UID, 0)
                            if (uid > 0) {
                                val dh = DatabaseHelper.getInstance(context)
                                dh.clearLog(uid)
                                dh.clearAccess(uid, false)

                                val notificationManager =
                                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.cancel(uid)
                                notificationManager.cancel(uid + 10000)
                            }
                        }

                        reload("package deleted", context, false)
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
        }

    fun notifyNewApplication(uid: Int, malware: Boolean) {
        if (uid < 0 || uid == Process.myUid()) {
            return
        }

                try {
            val names = Util.getApplicationNames(uid, this)
            if (names.isEmpty()) {
                return
            }
            val name = TextUtils.join(", ", names)

            val pm = packageManager
            val packages = pm.getPackagesForUid(uid)
            if (packages == null || packages.isEmpty()) {
                throw PackageManager.NameNotFoundException(uid.toString())
            }
            val internet = Util.hasInternet(uid, this)

            val main = Intent(this, ActivityMain::class.java)
            main.putExtra(ActivityMain.EXTRA_REFRESH, true)
            main.putExtra(ActivityMain.EXTRA_SEARCH, uid.toString())
            val pi =
                PendingIntentCompat.getActivity(this, uid, main, PendingIntent.FLAG_UPDATE_CURRENT)

            val notificationColor = themePrimaryColor(prefs.getString("theme", THEME_DEFAULT))
            val builder = Notification.Builder(
                this,
                if (malware) Notifications.CHANNEL_MALWARE else Notifications.CHANNEL_NOTIFY
            )
            builder
                .setSmallIcon(this.securityIcon())
                .setContentIntent(pi)
                .setColor(notificationColor)
                .setAutoCancel(true)

            if (malware) {
                builder.setContentTitle(name)
                    .setContentText(getString(R.string.msg_malware, name))
            } else {
                builder.setContentTitle(name)
                    .setContentText(getString(R.string.msg_installed_n))
            }

            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)

            val packageName = packages[0]
            val wifi = prefs.getBoolean(
                PreferencesRepository.namespaced("wifi", packageName),
                prefs.getBoolean("whitelist_wifi", true),
            )
            val other = prefs.getBoolean(
                PreferencesRepository.namespaced("other", packageName),
                prefs.getBoolean("whitelist_other", true),
            )

            val riWifi = Intent(this, ServiceSinkhole::class.java)
            riWifi.putExtra(EXTRA_COMMAND, Command.set)
            riWifi.putExtra(EXTRA_NETWORK, "wifi")
            riWifi.putExtra(EXTRA_UID, uid)
            riWifi.putExtra(EXTRA_PACKAGE, packageName)
            riWifi.putExtra(EXTRA_BLOCKED, !wifi)

            val piWifi =
                PendingIntentCompat.getService(this, uid, riWifi, PendingIntent.FLAG_UPDATE_CURRENT)
            val wAction =
                Notification.Action.Builder(
                    if (wifi) this.wifiIcon(true) else this.wifiIcon(false),
                    getString(if (wifi) R.string.title_allow_wifi else R.string.title_block_wifi),
                    piWifi,
                ).build()
            builder.addAction(wAction)

            val riOther = Intent(this, ServiceSinkhole::class.java)
            riOther.putExtra(EXTRA_COMMAND, Command.set)
            riOther.putExtra(EXTRA_NETWORK, "other")
            riOther.putExtra(EXTRA_UID, uid)
            riOther.putExtra(EXTRA_PACKAGE, packageName)
            riOther.putExtra(EXTRA_BLOCKED, !other)
            val piOther =
                PendingIntentCompat.getService(
                    this,
                    uid + 10000,
                    riOther,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            val oAction =
                Notification.Action.Builder(
                    if (other) this.cellularIcon(true) else this.cellularIcon(false),
                    getString(if (other) R.string.title_allow_other else R.string.title_block_other),
                    piOther,
                ).build()
            builder.addAction(oAction)

            if (internet) {
                if (Util.canNotify(this)) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(uid, builder.build())
                }
            } else {
                builder.setStyle(
                    Notification.BigTextStyle()
                        .bigText(getString(R.string.msg_installed_n))
                        .setSummaryText(getString(R.string.title_internet)),
                )
                if (Util.canNotify(this)) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(uid, builder.build())
                }
            }
        } catch (ex: PackageManager.NameNotFoundException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    override fun onCreate() {
        destroying = false
        Log.i(
            TAG,
            "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this)
        )
        startForeground(NOTIFY_WAITING, getWaitingNotification())

        
        nativeEngine = NativeVpnEngineController(Build.VERSION.SDK_INT, nativeCallbacks)
        Log.i(TAG, "Created UniFFI native engine")
        val pcap = prefs.getBoolean("pcap", false)
        setPcap(pcap, this, nativeEngine)

        removePrefsListener = prefs.addListener { key ->
            onPreferenceChanged(key)
        }

        Util.setTheme(this)
        super.onCreate()

        val commandThread =
            HandlerThread(
                getString(R.string.app_name) + " command",
                Process.THREAD_PRIORITY_FOREGROUND
            )
        val logThread =
            HandlerThread(getString(R.string.app_name) + " log", Process.THREAD_PRIORITY_BACKGROUND)
        val statsThread =
            HandlerThread(
                getString(R.string.app_name) + " stats",
                Process.THREAD_PRIORITY_BACKGROUND
            )
        commandThread.start()
        logThread.start()
        statsThread.start()

        commandLooper = commandThread.looper
        logLooper = logThread.looper
        statsLooper = statsThread.looper

        commandHandler = SinkholeCommandHandler(
            looper = commandLooper,
            commandCode = { intent -> getCommandExtra(intent)?.ordinal },
            handleCommand = { intent ->
                if (!destroying) synchronized(this) { if (!destroying) handleCommandIntent(intent) }
            },
            reportQueueSize = { size ->
                sendBroadcast(
                    Intent(ActivityMain.ACTION_QUEUE_CHANGED)
                        .setPackage(packageName)
                        .putExtra(ActivityMain.EXTRA_SIZE, size),
                )
            },
            releaseWakeLock = {
                val wakeLock = getLock(this)
                if (wakeLock.isHeld) wakeLock.release() else Log.w(TAG, "Wakelock under-locked")
            },
        )
        logHandler = SinkholeLogHandler(
            looper = logLooper,
            context = applicationContext,
            prefs = prefs,
            shouldShowAccessNotification = { uid -> withPolicyRead { mapNotify[uid] != false } },
            showAccessNotification = ::showAccessNotification,
        )
        statsHandler = SinkholeStatsHandler(
            looper = statsLooper,
            isEnabled = { !destroying && prefs.getBoolean("show_stats", false) },
            onStarted = ::onStatsStarted,
            onStopped = ::onStatsStopped,
            onRefresh = ::renderTrafficStats,
        )

        val ifUser = IntentFilter()
        ifUser.addAction(Intent.ACTION_USER_BACKGROUND)
        ifUser.addAction(Intent.ACTION_USER_FOREGROUND)
        ContextCompat.registerReceiver(this, userReceiver, ifUser, ContextCompat.RECEIVER_NOT_EXPORTED)
        registeredUser = true

        val ifIdle = IntentFilter()
        ifIdle.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        ContextCompat.registerReceiver(this, idleStateReceiver, ifIdle, ContextCompat.RECEIVER_NOT_EXPORTED)
        registeredIdleState = true

        val ifAp = IntentFilter()
        ifAp.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
        ContextCompat.registerReceiver(
            this,
            apStateReceiver,
            ifAp,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registeredApState = true

        val ifPackage = IntentFilter()
        ifPackage.addAction(Intent.ACTION_PACKAGE_ADDED)
        ifPackage.addAction(Intent.ACTION_PACKAGE_REMOVED)
        ifPackage.addDataScheme("package")
        ContextCompat.registerReceiver(
            this,
            packageChangedReceiver,
            ifPackage,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registeredPackageChanged = true

        try {
            networkMonitor.start()
        } catch (ex: RuntimeException) {
            Log.w(TAG, "Could not register network callbacks", ex)
        }
        listenConnectivityChanges()

        WorkScheduler.scheduleHousekeeping(this)
    }

    private fun listenConnectivityChanges() {
        Log.i(TAG, "Starting listening to service state changes")
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm != null) {
            registerDataConnectionListener(tm)
        }
    }

    private fun getCommandExtra(intent: Intent): Command? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_COMMAND, Command::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_COMMAND) as? Command
        }
    }

    private fun onPreferenceChanged(name: String?) {
        if ("theme" == name) {
            Log.i(TAG, "Theme changed")
            Util.setTheme(this)
            if (state != State.none) {
                Log.d(TAG, "Stop foreground state=$state")
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
            if (state == State.enforcing) {
                startForeground(NOTIFY_ENFORCING, getEnforcingNotification(-1, -1, -1))
            } else if (state != State.none) {
                startForeground(NOTIFY_WAITING, getWaitingNotification())
            }
            Log.d(TAG, "Start foreground state=$state")
        }
        if (name == "watchdog" || name == "enabled") {
            val watchdog = prefs.getString("watchdog", "0")?.toIntOrNull() ?: 0
            val enabled = prefs.getBoolean("enabled", false)
            WorkScheduler.scheduleWatchdog(this, watchdog, enabled)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (state == State.enforcing) {
            startForeground(NOTIFY_ENFORCING, getEnforcingNotification(-1, -1, -1))
        } else {
            startForeground(NOTIFY_WAITING, getWaitingNotification())
        }

        Log.i(TAG, "Received $intent")
        Util.logExtras(intent)

        var actualIntent = intent

        if (actualIntent != null && actualIntent.hasExtra(EXTRA_COMMAND) &&
            getCommandExtra(actualIntent) == Command.set
        ) {
            set(actualIntent)
            return START_STICKY
        }

        getLock(this).acquire()

        val enabled = prefs.getBoolean("enabled", false)

        if (actualIntent == null) {
            Log.i(TAG, "Restart")
            actualIntent = Intent(this, ServiceSinkhole::class.java)
            actualIntent.putExtra(EXTRA_COMMAND, if (enabled) Command.start else Command.stop)
        }

        if (ACTION_HOUSE_HOLDING == actualIntent.action) {
            actualIntent.putExtra(EXTRA_COMMAND, Command.householding)
        }
        if (ACTION_WATCHDOG == actualIntent.action) {
            actualIntent.putExtra(EXTRA_COMMAND, Command.watchdog)
        }

        var cmd = getCommandExtra(actualIntent)
        if (cmd == null) {
            actualIntent.putExtra(EXTRA_COMMAND, if (enabled) Command.start else Command.stop)
            cmd = getCommandExtra(actualIntent) ?: Command.stop
        }
        val reason = actualIntent.getStringExtra(EXTRA_REASON)
        Log.i(
            TAG,
            "Start intent=$actualIntent command=$cmd reason=$reason" +
                    " vpn=" + (vpn != null) + " user=" + (Process.myUid() / 100000),
        )

        if (!commandHandler.queue(actualIntent)) {
            getLock(this).release()
        }

        return START_STICKY
    }

    private fun set(intent: Intent) {
        val uid = intent.getIntExtra(EXTRA_UID, 0)
        val network = intent.getStringExtra(EXTRA_NETWORK)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        val blocked = intent.getBooleanExtra(EXTRA_BLOCKED, false)
        Log.i(TAG, "Set $pkg $network=$blocked")

        val defaultWifi = prefs.getBoolean("whitelist_wifi", true)
        val defaultOther = prefs.getBoolean("whitelist_other", true)

        val networkName = network ?: "other"
        val key = PreferencesRepository.namespaced(networkName, pkg ?: "")
        if (blocked == (if ("wifi" == networkName) defaultWifi else defaultOther)) {
            prefs.remove(key)
        } else {
            prefs.putBoolean(key, blocked)
        }

        reload("notification", this@ServiceSinkhole, false)

        notifyNewApplication(uid, false)

        val ruleset = Intent(ActivityMain.ACTION_RULES_CHANGED).setPackage(packageName)
        sendBroadcast(ruleset)
    }

    override fun onRevoke() {
        Log.i(TAG, "Revoke")

        prefs.putBoolean("enabled", false)

        showDisabledNotification()
        Widgets.updateFirewall(this)

        super.onRevoke()
    }

    override fun onDestroy() {
        destroying = true
        synchronized(this) {
            Log.i(TAG, "Destroy")
            commandLooper.quit()
            logLooper.quit()
            statsLooper.quit()

            for (command in Command.values()) {
                commandHandler.removeMessages(command.ordinal)
            }
            executor.shutdownNow()
            releaseLock(this)

            if (registeredInteractiveState) {
                unregisterReceiver(interactiveStateReceiver)
                registeredInteractiveState = false
            }
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            unregisterCallStateListener(tm)

            if (registeredUser) {
                unregisterReceiver(userReceiver)
                registeredUser = false
            }
            if (registeredIdleState) {
                unregisterReceiver(idleStateReceiver)
                registeredIdleState = false
            }
            if (registeredApState) {
                unregisterReceiver(apStateReceiver)
                registeredApState = false
            }
            if (registeredPackageChanged) {
                unregisterReceiver(packageChangedReceiver)
                registeredPackageChanged = false
            }

            networkMonitor.stop()

            unregisterDataConnectionListener(tm)

            try {
                if (vpn != null) {
                    stopNative()
                    stopVPN(vpn!!)
                    vpn = null
                    unprepare()
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            Log.i(TAG, "Destroy UniFFI native engine")
            nativeEngine.close()

            removePrefsListener?.invoke()
            removePrefsListener = null
        }

        super.onDestroy()
    }

    private fun getEnforcingNotification(allowed: Int, blocked: Int, hosts: Int): Notification {
        val main = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationColor = themePrimaryColor(prefs.getString("theme", THEME_DEFAULT))
        val builder = Notification.Builder(this, Notifications.CHANNEL_FOREGROUND)
        builder
            .setSmallIcon(
                if (isLockedDown(lastMetered)) {
                    this.lockIcon()
                } else {
                    this.securityIcon()
                },
            )
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(true)
            .setAutoCancel(false)

        builder.setContentTitle(getString(R.string.msg_started))
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_SECRET)

        var allowedValue = allowed
        var blockedValue = blocked
        var hostsValue = hosts
        if (allowedValue >= 0) {
            lastAllowed = allowedValue
        } else {
            allowedValue = lastAllowed
        }
        if (blockedValue >= 0) {
            lastBlocked = blockedValue
        } else {
            blockedValue = lastBlocked
        }
        if (hostsValue >= 0) {
            lastHosts = hostsValue
        } else {
            hostsValue = lastHosts
        }

        if (allowedValue >= 0 || blockedValue >= 0 || hostsValue >= 0) {
            builder.setContentText(
                if (Util.isPlayStoreInstall(this)) {
                    getString(R.string.msg_packages, allowedValue, blockedValue)
                } else {
                    getString(R.string.msg_hosts, allowedValue, blockedValue, hostsValue)
                },
            )
            return builder.build()
        }

        return builder.build()
    }

    private fun updateEnforcingNotification(allowed: Int, total: Int) {
        val notification = getEnforcingNotification(allowed, total - allowed, blockedHosts.size)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Util.canNotify(this)) {
            nm.notify(NOTIFY_ENFORCING, notification)
        }
    }

    private fun getWaitingNotification(): Notification {
        val main = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationColor = themePrimaryColor(prefs.getString("theme", THEME_DEFAULT))
        val builder = Notification.Builder(this, Notifications.CHANNEL_FOREGROUND)
        builder.setSmallIcon(this.securityIcon())
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(true)
            .setAutoCancel(false)

        builder.setContentTitle(getString(R.string.msg_waiting))
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_SECRET)

        return builder.build()
    }

    private fun showDisabledNotification() {
        notifications.showDisabled()
    }

    private fun showLockdownNotification() {
        notifications.showLockdown()
    }

    private fun removeLockdownNotification() {
        notifications.removeLockdown()
    }

    private fun showAutoStartNotification() {
        notifications.showAutoStart()
    }

    private fun showErrorNotification(message: String) {
        notifications.showError(message)
    }

    private fun showAccessNotification(uid: Int) {
        notifications.showAccess(uid)
    }

    private fun showUpdateNotification(name: String, url: String) {
        notifications.showUpdate(name, url)
    }

    private fun removeWarningNotifications() {
        notifications.removeWarnings()
    }

    /** Records the effective VPN configuration so equivalent reloads can reuse the tunnel. */
    private inner class TrackingVpnBuilder : VpnService.Builder() {
        private val fingerprint = VpnConfigurationFingerprint(
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )

        override fun setMtu(mtu: Int): VpnService.Builder {
            fingerprint.setMtu(mtu)
            super.setMtu(mtu)
            return this
        }

        override fun addAddress(address: String, prefixLength: Int): TrackingVpnBuilder {
            fingerprint.addAddress(address, prefixLength)
            super.addAddress(address, prefixLength)
            return this
        }

        override fun addRoute(address: String, prefixLength: Int): TrackingVpnBuilder {
            fingerprint.addRoute(address, prefixLength)
            super.addRoute(address, prefixLength)
            return this
        }

        override fun addRoute(address: InetAddress, prefixLength: Int): TrackingVpnBuilder {
            val host = address.hostAddress ?: return this
            fingerprint.addRoute(host, prefixLength)
            super.addRoute(address, prefixLength)
            return this
        }

        override fun addDnsServer(address: InetAddress): TrackingVpnBuilder {
            fingerprint.addDnsServer(address)
            super.addDnsServer(address)
            return this
        }

        @Throws(PackageManager.NameNotFoundException::class)
        override fun addAllowedApplication(packageName: String): VpnService.Builder {
            fingerprint.addAllowedApplication(packageName)
            return super.addAllowedApplication(packageName)
        }

        @Throws(PackageManager.NameNotFoundException::class)
        override fun addDisallowedApplication(packageName: String): TrackingVpnBuilder {
            fingerprint.addDisallowedApplication(packageName)
            super.addDisallowedApplication(packageName)
            return this
        }

        override fun equals(other: Any?): Boolean =
            other is TrackingVpnBuilder && fingerprint == other.fingerprint

        override fun hashCode(): Int = fingerprint.hashCode()
    }

    companion object {
        private const val TAG = "NetGuard.Service"

        private const val NOTIFY_ENFORCING = 1
        private const val NOTIFY_WAITING = 2
        private const val NOTIFY_TRAFFIC = 7
        const val NOTIFY_EXTERNAL = 9
        const val NOTIFY_DOWNLOAD = 10

        const val EXTRA_COMMAND = "Command"
        private const val EXTRA_REASON = "Reason"
        const val EXTRA_NETWORK = "Network"
        const val EXTRA_UID = "UID"
        const val EXTRA_PACKAGE = "Package"
        const val EXTRA_BLOCKED = "Blocked"
        const val EXTRA_INTERACTIVE = "Interactive"
        const val EXTRA_TEMPORARY = "Temporary"
        const val EXTRA_UPDATE_CHECK_STATUS = "UpdateCheckStatus"
        const val EXTRA_UPDATE_CHECK_VERSION = "UpdateCheckVersion"

        private const val MIN_STATS_FREQUENCY_MS = 250L
        private const val MAX_STATS_FREQUENCY_MS = 60_000L
        private const val MIN_STATS_SAMPLES = 1L
        private const val MAX_STATS_SAMPLES = 3_600L
        private const val USER_FOREGROUND_DELAY_MS = 3_000L

        @Volatile
        private var wlInstance: PowerManager.WakeLock? = null

        const val ACTION_HOUSE_HOLDING = "com.bernaferrari.quietguard.HOUSE_HOLDING"
        private const val ACTION_SCREEN_OFF_DELAYED =
            "com.bernaferrari.quietguard.SCREEN_OFF_DELAYED"
        const val ACTION_WATCHDOG = "com.bernaferrari.quietguard.WATCHDOG"
        const val ACTION_UPDATE_CHECK_RESULT = "com.bernaferrari.quietguard.UPDATE_CHECK_RESULT"

        private const val MIN_PCAP_RECORD_SIZE = 1
        private const val MAX_PCAP_RECORD_SIZE = 10_000
        private const val MIN_PCAP_FILE_SIZE_MIB = 1
        private const val MAX_PCAP_FILE_SIZE_MIB = 1_024

        private fun setPcap(enabled: Boolean, context: Context, engine: NativeVpnEngineController) {
            val prefs = context.preferences()
            var recordSize = 64
            try {
                var r = prefs.getString("pcap_record_size", null)
                if (TextUtils.isEmpty(r)) r = "64"
                recordSize = (r?.toIntOrNull() ?: 64)
                    .coerceIn(MIN_PCAP_RECORD_SIZE, MAX_PCAP_RECORD_SIZE)
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            var fileSize = 2 * 1024 * 1024
            try {
                var f = prefs.getString("pcap_file_size", null)
                if (TextUtils.isEmpty(f)) f = "2"
                val fileSizeMib = (f?.toIntOrNull() ?: 2)
                    .coerceIn(MIN_PCAP_FILE_SIZE_MIB, MAX_PCAP_FILE_SIZE_MIB)
                fileSize = fileSizeMib * 1024 * 1024
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            val pcap = if (enabled) File(
                context.getDir("data", Context.MODE_PRIVATE),
                "netguard.pcap"
            ) else null
            try {
                engine.configurePcap(NativePcapConfig(pcap?.absolutePath, recordSize, fileSize.toLong()))
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        @Synchronized
        private fun getLock(context: Context): PowerManager.WakeLock {
            if (wlInstance == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wlInstance =
                    pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        context.getString(R.string.app_name) + " wakelock",
                    )
                wlInstance?.setReferenceCounted(true)
            }
            return wlInstance!!
        }

        @Synchronized
        private fun releaseLock(context: Context) {
            if (wlInstance != null) {
                while (wlInstance?.isHeld == true) {
                    wlInstance?.release()
                }
                wlInstance = null
            }
        }

        @JvmStatic
        fun getDns(context: Context): List<InetAddress> {
            val prefs = context.preferences()
            val listDns = ArrayList<InetAddress>()
            val sysDns = Util.getDefaultDNS(context)

            val ip6 = prefs.getBoolean("ip6", true)
            val filter = prefs.getBoolean("filter", false)
            val vpnDns1 = prefs.getString("dns", null)
            val vpnDns2 = prefs.getString("dns2", null)
            Log.i(TAG, "DNS system=" + TextUtils.join(",", sysDns) + " config=$vpnDns1,$vpnDns2")

            if (vpnDns1 != null) {
                try {
                    val dns = InetAddress.getByName(vpnDns1)
                    if (!(dns.isLoopbackAddress || dns.isAnyLocalAddress) && (ip6 || dns is Inet4Address)) {
                        listDns.add(dns)
                    }
                } catch (_: Throwable) {
                }
            }

            if (vpnDns2 != null) {
                try {
                    val dns = InetAddress.getByName(vpnDns2)
                    if (!(dns.isLoopbackAddress || dns.isAnyLocalAddress) && (ip6 || dns is Inet4Address)) {
                        listDns.add(dns)
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }

            if (listDns.size == 2) {
                return listDns
            }

            for (defDns in sysDns) {
                try {
                    val ddns = InetAddress.getByName(defDns)
                    if (!listDns.contains(ddns) &&
                        !(ddns.isLoopbackAddress || ddns.isAnyLocalAddress) &&
                        (ip6 || ddns is Inet4Address)
                    ) {
                        listDns.add(ddns)
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }

            val count = listDns.size
            val lan = prefs.getBoolean("lan", false)
            val useHosts = prefs.getBoolean("use_hosts", false)
            if (lan && useHosts && filter) {
                try {
                    val subnets = ArrayList<Pair<InetAddress, Int>>()
                    subnets.add(Pair(InetAddress.getByName("10.0.0.0"), 8))
                    subnets.add(Pair(InetAddress.getByName("172.16.0.0"), 12))
                    subnets.add(Pair(InetAddress.getByName("192.168.0.0"), 16))

                    for (subnet in subnets) {
                        val hostAddress = subnet.first
                        val host = BigInteger(1, hostAddress.address)

                        val prefix = subnet.second
                        val mask =
                            BigInteger.valueOf(-1)
                                .shiftLeft(hostAddress.address.size * 8 - prefix)

                        for (dns in ArrayList(listDns)) {
                            if (hostAddress.address.size == dns.address.size) {
                                val ip = BigInteger(1, dns.address)

                                if (host.and(mask) == ip.and(mask)) {
                                    Log.i(
                                        TAG,
                                        "Local DNS server host=$hostAddress/$prefix dns=$dns"
                                    )
                                    listDns.remove(dns)
                                }
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }

            if (listDns.isEmpty() || listDns.size < count) {
                try {
                    listDns.add(InetAddress.getByName("8.8.8.8"))
                    listDns.add(InetAddress.getByName("8.8.4.4"))
                    if (ip6) {
                        listDns.add(InetAddress.getByName("2001:4860:4860::8888"))
                        listDns.add(InetAddress.getByName("2001:4860:4860::8844"))
                    }
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }

            Log.i(TAG, "Get DNS=" + TextUtils.join(",", listDns))

            return listDns
        }

        @JvmStatic
        fun run(reason: String, context: Context) {
            val intent = Intent(context, ServiceSinkhole::class.java)
            intent.putExtra(EXTRA_COMMAND, Command.run)
            intent.putExtra(EXTRA_REASON, reason)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (ex: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ex is ForegroundServiceStartNotAllowedException) {
                    try {
                        context.startService(intent)
                    } catch (exex: Throwable) {
                        Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                    }
                }
            }
        }

        @JvmStatic
        fun start(reason: String, context: Context) {
            val intent = Intent(context, ServiceSinkhole::class.java)
            intent.putExtra(EXTRA_COMMAND, Command.start)
            intent.putExtra(EXTRA_REASON, reason)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (ex: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ex is ForegroundServiceStartNotAllowedException) {
                    try {
                        context.startService(intent)
                    } catch (exex: Throwable) {
                        Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                    }
                }
            }
        }

        @JvmStatic
        fun reload(reason: String, context: Context, interactive: Boolean) {
            if (context.preferences().getBoolean("enabled", false)) {
                val intent = Intent(context, ServiceSinkhole::class.java)
                intent.putExtra(EXTRA_COMMAND, Command.reload)
                intent.putExtra(EXTRA_REASON, reason)
                intent.putExtra(EXTRA_INTERACTIVE, interactive)
                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (ex: Throwable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ex is ForegroundServiceStartNotAllowedException) {
                        try {
                            context.startService(intent)
                        } catch (exex: Throwable) {
                            Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun stop(reason: String, context: Context, vpnonly: Boolean) {
            val intent = Intent(context, ServiceSinkhole::class.java)
            intent.putExtra(EXTRA_COMMAND, Command.stop)
            intent.putExtra(EXTRA_REASON, reason)
            intent.putExtra(EXTRA_TEMPORARY, vpnonly)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (ex: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ex is ForegroundServiceStartNotAllowedException) {
                    try {
                        context.startService(intent)
                    } catch (exex: Throwable) {
                        Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                    }
                }
            }
        }

        @JvmStatic
        fun reloadStats(reason: String, context: Context) {
            val intent = Intent(context, ServiceSinkhole::class.java)
            intent.putExtra(EXTRA_COMMAND, Command.stats)
            intent.putExtra(EXTRA_REASON, reason)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (ex: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ex is ForegroundServiceStartNotAllowedException) {
                    try {
                        context.startService(intent)
                    } catch (exex: Throwable) {
                        Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                    }
                }
            }
        }

        @JvmStatic
        fun checkForUpdateNow(reason: String, context: Context) {
            val intent = Intent(context, ServiceSinkhole::class.java)
            intent.putExtra(EXTRA_COMMAND, Command.updatecheck)
            intent.putExtra(EXTRA_REASON, reason)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (ex: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ex is ForegroundServiceStartNotAllowedException) {
                    try {
                        context.startService(intent)
                    } catch (exex: Throwable) {
                        Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                    }
                }
            }
        }
    }

    private enum class State {
        none,
        waiting,
        enforcing,
        stats,
    }

    enum class Command {
        run,
        start,
        reload,
        stop,
        stats,
        set,
        householding,
        watchdog,
        updatecheck,
    }
}
