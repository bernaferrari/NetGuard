package eu.faircode.netguard

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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.net.Uri
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
import android.provider.Settings
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import androidx.core.content.ContextCompat
import eu.faircode.netguard.data.Prefs
import eu.faircode.netguard.ui.theme.GraphGrayed
import eu.faircode.netguard.ui.theme.GraphReceive
import eu.faircode.netguard.ui.theme.GraphSend
import eu.faircode.netguard.ui.theme.THEME_DEFAULT
import eu.faircode.netguard.ui.theme.themePrimaryColor
import eu.faircode.netguard.ui.theme.themeOffColor
import eu.faircode.netguard.ui.theme.themeOnColor
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.net.UnknownHostException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import java.util.TreeMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.net.ssl.HttpsURLConnection
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ServiceSinkhole : VpnService() {
    private var registeredUser = false
    private var registeredIdleState = false
    private var registeredApState = false
    private var registeredPackageChanged = false

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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

    private var tunnelThread: Thread? = null
    private var lastBuilder: Builder? = null
    private var vpn: ParcelFileDescriptor? = null
    private var temporarilyStopped = false

    private var lastHostsModified: Long = 0
    private var lastMalwareModified: Long = 0
    private val mapHostsBlocked = HashMap<String, Boolean>()
    private val mapMalware = HashMap<String, Boolean>()
    private val mapUidAllowed = HashMap<Int, Boolean>()
    private val mapUidKnown = HashMap<Int, Int>()
    private val mapUidIPFilters = HashMap<IPKey, MutableMap<InetAddress, IPRule>>()
    private val mapForward = HashMap<Int, Forward>()
    private val mapNotify = HashMap<Int, Boolean>()
    private val lock = ReentrantReadWriteLock(true)

    @Volatile private lateinit var commandLooper: Looper
    @Volatile private lateinit var logLooper: Looper
    @Volatile private lateinit var statsLooper: Looper
    @Volatile private lateinit var commandHandler: CommandHandler
    @Volatile private lateinit var logHandler: LogHandler
    @Volatile private lateinit var statsHandler: StatsHandler

    private val executor: ExecutorService = Executors.newCachedThreadPool()

    private external fun jni_init(sdk: Int): Long

    private external fun jni_start(context: Long, loglevel: Int)

    private external fun jni_run(context: Long, tun: Int, fwd53: Boolean, rcode: Int)

    private external fun jni_stop(context: Long)

    private external fun jni_clear(context: Long)

    private external fun jni_get_mtu(): Int

    private external fun jni_get_stats(context: Long): IntArray

    private external fun jni_socks5(addr: String?, port: Int, username: String?, password: String?)

    private external fun jni_done(context: Long)

    private inner class CommandHandler(looper: Looper) : Handler(looper) {
        var queue = 0

        private fun reportQueueSize() {
            val ruleset = Intent(ActivityMain.ACTION_QUEUE_CHANGED).setPackage(packageName)
            ruleset.putExtra(ActivityMain.EXTRA_SIZE, queue)
            sendBroadcast(ruleset)
        }

        fun queue(intent: Intent) {
            synchronized(this) {
                queue++
                reportQueueSize()
            }
            val cmd = getCommandExtra(intent) ?: return
            val msg = obtainMessage()
            msg.obj = intent
            msg.what = cmd.ordinal
            sendMessage(msg)
        }

        override fun handleMessage(msg: Message) {
            try {
                synchronized(this@ServiceSinkhole) {
                    handleIntent(msg.obj as Intent)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            } finally {
                synchronized(this) {
                    queue--
                    reportQueueSize()
                }
                try {
                    val wl = getLock(this@ServiceSinkhole)
                    if (wl.isHeld) {
                        wl.release()
                    } else {
                        Log.w(TAG, "Wakelock under-locked")
                    }
                    Log.i(TAG, "Messages=" + hasMessages(0) + " wakelock=" + (wlInstance?.isHeld ?: false))
                } catch (ex: Throwable) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                }
            }
        }

        private fun handleIntent(intent: Intent) {
            val prefs = Prefs

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
                WorkScheduler.scheduleWatchdog(this@ServiceSinkhole, watchdog, enabled && cmd != Command.stop)
            }

            try {
                when (cmd) {
                    Command.run -> Unit
                    Command.start -> start()
                    Command.reload -> reload(intent.getBooleanExtra(EXTRA_INTERACTIVE, false))
                    Command.stop -> stop(temporarilyStopped)
                    Command.stats -> {
                        statsHandler.sendEmptyMessage(MSG_STATS_STOP)
                        statsHandler.sendEmptyMessage(MSG_STATS_START)
                    }
                    Command.householding -> householding(intent)
                    Command.watchdog -> watchdog(intent)
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
                    ruleset.putExtra(ActivityMain.EXTRA_CONNECTED, cmd != Command.stop && lastConnected)
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

                System.gc()
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
                            Prefs.putBoolean("enabled", false)
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

            val prefs = Prefs

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

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                lastBuilder = builder
                Log.i(TAG, "Legacy restart")

                if (vpn != null) {
                    stopNative(vpn!!)
                    stopVPN(vpn!!)
                    vpn = null
                    try {
                        Thread.sleep(500)
                    } catch (_: InterruptedException) {
                    }
                }
                vpn = startVPN(lastBuilder!!)
            } else {
                if (vpn != null && prefs.getBoolean("filter", false) && builder == lastBuilder) {
                    Log.i(TAG, "Native restart")
                    stopNative(vpn!!)
                } else {
                    lastBuilder = builder

                    var handover = prefs.getBoolean("handover", false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        handover = false
                    }
                    Log.i(TAG, "VPN restart handover=$handover")

                    if (handover) {
                        var prev = vpn
                        vpn = startVPN(builder)

                        if (prev != null && vpn == null) {
                            Log.w(TAG, "Handover failed")
                            stopNative(prev)
                            stopVPN(prev)
                            prev = null
                            try {
                                Thread.sleep(3000)
                            } catch (_: InterruptedException) {
                            }
                            vpn = startVPN(lastBuilder!!)
                            if (vpn == null) {
                                throw IllegalStateException("Handover failed")
                            }
                        }

                        if (prev != null) {
                            stopNative(prev)
                            stopVPN(prev)
                        }
                    } else {
                        if (vpn != null) {
                            stopNative(vpn!!)
                            stopVPN(vpn!!)
                        }

                        vpn = startVPN(builder)
                    }
                }
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
                stopNative(vpn!!)
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

                val prefs = Prefs
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
            val prefs = Prefs
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
                    Util.hasValidFingerprint(this@ServiceSinkhole) &&
                    prefs.getBoolean("update_check", true)
            ) {
                checkUpdate()
            }
        }

        private fun watchdog(intent: Intent) {
            if (vpn == null) {
                val prefs = Prefs
                if (prefs.getBoolean("enabled", false)) {
                    Log.e(TAG, "Service was killed")
                    start()
                }
            }
        }

        private fun checkUpdate() {
            val json = StringBuilder()
            var urlConnection: HttpsURLConnection? = null
            try {
                val url = URL(BuildConfig.GITHUB_LATEST_API)
                urlConnection = url.openConnection() as HttpsURLConnection
                val br = BufferedReader(InputStreamReader(urlConnection.inputStream))
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    json.append(line)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            } finally {
                urlConnection?.disconnect()
            }

            try {
                val jroot = JSONObject(json.toString())
                if (jroot.has("tag_name") && jroot.has("html_url") && jroot.has("assets")) {
                    val url = jroot.getString("html_url")
                    val jassets = jroot.getJSONArray("assets")
                    if (jassets.length() > 0) {
                        val jasset = jassets.getJSONObject(0)
                        if (jasset.has("name")) {
                            val version = jroot.getString("tag_name")
                            val name = jasset.getString("name")
                            Log.i(TAG, "Tag $version name $name url $url")

                            val current = Version(Util.getSelfVersionName(this@ServiceSinkhole))
                            val available = Version(version)
                            if (current.compareTo(available) < 0) {
                                Log.i(TAG, "Update available from $current to $available")
                                showUpdateNotification(name, url)
                            } else {
                                Log.i(TAG, "Up-to-date current version $current")
                            }
                        }
                    }
                }
            } catch (ex: JSONException) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        private inner class StartFailedException(msg: String) : IllegalStateException(msg)
    }

    private inner class LogHandler(looper: Looper) : Handler(looper) {
        var queue = 0

        fun queue(packet: Packet) {
            val msg = obtainMessage()
            msg.obj = packet
            msg.what = MSG_PACKET
            msg.arg1 = if (lastConnected) (if (lastMetered) 2 else 1) else 0
            msg.arg2 = if (lastInteractive) 1 else 0

            synchronized(this) {
                if (queue > maxQueue) {
                    Log.w(TAG, "Log queue full")
                    return
                }

                sendMessage(msg)

                queue++
            }
        }

        fun account(usage: Usage) {
            val msg = obtainMessage()
            msg.obj = usage
            msg.what = MSG_USAGE

            synchronized(this) {
                if (queue > maxQueue) {
                    Log.w(TAG, "Log queue full")
                    return
                }

                sendMessage(msg)

                queue++
            }
        }

        override fun handleMessage(msg: Message) {
            try {
                when (msg.what) {
                    MSG_PACKET -> log(msg.obj as Packet, msg.arg1, msg.arg2 > 0)
                    MSG_USAGE -> usage(msg.obj as Usage)
                    else -> Log.e(TAG, "Unknown log message=${msg.what}")
                }

                synchronized(this) {
                    queue--
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        private fun log(packet: Packet, connection: Int, interactive: Boolean) {
            val prefs = Prefs
            val log = prefs.getBoolean("log", false)

            val dh = DatabaseHelper.getInstance(this@ServiceSinkhole)

            val daddr = packet.daddr ?: return
            val dname = dh.getQName(packet.uid, daddr)

            if (log) {
                dh.insertLog(packet, dname, connection, interactive)
            }

            if (
                log &&
                    packet.uid >= 0 &&
                    !(packet.uid == 0 && (packet.protocol == 6 || packet.protocol == 17) && packet.dport == 53)
            ) {
                if (!(packet.protocol == 6 || packet.protocol == 17)) {
                    packet.dport = 0
                }
                if (dh.updateAccess(packet, dname, -1)) {
                    lock.readLock().lock()
                    if (!mapNotify.containsKey(packet.uid) || mapNotify[packet.uid] == true) {
                        showAccessNotification(packet.uid)
                    }
                    lock.readLock().unlock()
                }
            }
        }

        private fun usage(usage: Usage) {
            if (usage.Uid >= 0 && !(usage.Uid == 0 && usage.Protocol == 17 && usage.DPort == 53)) {
                val prefs = Prefs
                val filter = prefs.getBoolean("filter", false)
                val log = prefs.getBoolean("log", false)
                val trackUsage = prefs.getBoolean("track_usage", false)
                if (filter && log && trackUsage) {
                    val dh = DatabaseHelper.getInstance(this@ServiceSinkhole)
                    val daddr = usage.DAddr ?: return
                    val dname = dh.getQName(usage.Uid, daddr)
                    Log.i(TAG, "Usage account $usage dname=$dname")
                    dh.updateUsage(usage, dname)
                }
            }
        }

        private val maxQueue = 250
    }

    private inner class StatsHandler(looper: Looper) : Handler(looper) {
        private var stats = false
        private var whenMs: Long = 0

        private var t: Long = -1
        private var tx: Long = -1
        private var rx: Long = -1

        private val gt = ArrayList<Long>()
        private val gtx = ArrayList<Float>()
        private val grx = ArrayList<Float>()

        private val mapUidBytes = HashMap<Int, Long>()

        override fun handleMessage(msg: Message) {
            try {
                when (msg.what) {
                    MSG_STATS_START -> startStats()
                    MSG_STATS_STOP -> stopStats()
                    MSG_STATS_UPDATE -> updateStats()
                    else -> Log.e(TAG, "Unknown stats message=${msg.what}")
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }

        private fun startStats() {
            val prefs = Prefs
            val enabled = !stats && prefs.getBoolean("show_stats", false)
            Log.i(TAG, "Stats start enabled=$enabled")
            if (enabled) {
                whenMs = Date().time
                t = -1
                tx = -1
                rx = -1
                gt.clear()
                gtx.clear()
                grx.clear()
                mapUidBytes.clear()
                stats = true
                updateStats()
            }
        }

        private fun stopStats() {
            Log.i(TAG, "Stats stop")
            stats = false
            removeMessages(MSG_STATS_UPDATE)
            if (state == State.stats) {
                Log.d(TAG, "Stop foreground state=$state")
                stopForeground(STOP_FOREGROUND_REMOVE)
                state = State.none
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFY_TRAFFIC)
            }
        }

        private fun updateStats() {
            val prefs = Prefs
            val frequency = prefs.getString("stats_frequency", "1000")?.toLongOrNull() ?: 1000
            val samples = prefs.getString("stats_samples", "90")?.toLongOrNull() ?: 90
            val filter = prefs.getBoolean("filter", false)
            val showTop = prefs.getBoolean("show_top", false)
            val loglevel =
                prefs.getString("loglevel", Log.WARN.toString())?.toIntOrNull() ?: Log.WARN

            sendEmptyMessageDelayed(MSG_STATS_UPDATE, frequency)

            val ct = SystemClock.elapsedRealtime()

            while (gt.size > 0 && ct - gt[0] > samples * 1000) {
                gt.removeAt(0)
                gtx.removeAt(0)
                grx.removeAt(0)
            }

            var txsec = 0f
            var rxsec = 0f
            var ttx = TrafficStats.getTotalTxBytes()
            var trx = TrafficStats.getTotalRxBytes()
            if (filter) {
                ttx -= TrafficStats.getUidTxBytes(Process.myUid())
                trx -= TrafficStats.getUidRxBytes(Process.myUid())
                if (ttx < 0) ttx = 0
                if (trx < 0) trx = 0
            }
            if (t > 0 && tx > 0 && rx > 0) {
                val dt = (ct - t) / 1000f
                txsec = (ttx - tx) / dt
                rxsec = (trx - rx) / dt
                gt.add(ct)
                gtx.add(txsec)
                grx.add(rxsec)
            }

            var topText = ""
            if (showTop) {
                if (mapUidBytes.size == 0) {
                    for (ainfo in packageManager.getInstalledApplications(0)) {
                        if (ainfo.uid != Process.myUid()) {
                            mapUidBytes[ainfo.uid] =
                                TrafficStats.getUidTxBytes(ainfo.uid) +
                                    TrafficStats.getUidRxBytes(ainfo.uid)
                        }
                    }
                } else if (t > 0) {
                    val mapSpeedUid = TreeMap<Float, Int> { value, other -> -value.compareTo(other) }
                    val dt = (ct - t) / 1000f
                    for (uid in mapUidBytes.keys) {
                        val bytes =
                            TrafficStats.getUidTxBytes(uid) + TrafficStats.getUidRxBytes(uid)
                        val speed = (bytes - (mapUidBytes[uid] ?: 0L)) / dt
                        if (speed > 0) {
                            mapSpeedUid[speed] = uid
                            mapUidBytes[uid] = bytes
                        }
                    }

                    val sb = StringBuilder()
                    var i = 0
                    for (speed in mapSpeedUid.keys) {
                        if (i++ >= 3) break
                        if (speed < 1000 * 1000) {
                            sb.append(getString(R.string.msg_kbsec, speed / 1000))
                        } else {
                            sb.append(getString(R.string.msg_mbsec, speed / 1000 / 1000))
                        }
                        sb.append(' ')
                        val apps = Util.getApplicationNames(mapSpeedUid[speed] ?: 0, this@ServiceSinkhole)
                        sb.append(if (apps.isNotEmpty()) apps[0] else "?")
                        sb.append("\r\n")
                    }
                    if (sb.isNotEmpty()) {
                        sb.setLength(sb.length - 2)
                        topText = sb.toString()
                    }
                }
            }

            t = ct
            tx = ttx
            rx = trx

            val height = Util.dips2pixels(96, this@ServiceSinkhole)
            val width = Util.dips2pixels(96 * 5, this@ServiceSinkhole)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)

            var max = 0f
            var xmax: Long = 0
            var ymax = 0f
            for (i in gt.indices) {
                val t = gt[i]
                val tx = gtx[i]
                val rx = grx[i]
                if (t > xmax) xmax = t
                if (tx > max) max = tx
                if (rx > max) max = rx
                if (tx > ymax) ymax = tx
                if (rx > ymax) ymax = rx
            }

            val ptx = Path()
            val prx = Path()
            for (i in gtx.indices) {
                val x = width - width * (xmax - gt[i]) / 1000f / samples
                val ytx = height - height * gtx[i] / ymax
                val yrx = height - height * grx[i] / ymax
                if (i == 0) {
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
                    val count = jni_get_stats(jni_context)
                    getString(R.string.notify_traffic_debug, count[0], count[1], count[2], count[3], count[4])
                } else {
                    ""
                }

            val main = Intent(this@ServiceSinkhole, ActivityMain::class.java)
            val pi = PendingIntentCompat.getActivity(this@ServiceSinkhole, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

            val notificationColor = themePrimaryColor(Prefs.getString("theme", THEME_DEFAULT))
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = Notification.Builder(this@ServiceSinkhole, Notifications.CHANNEL_NOTIFY)
            val extraLines = buildList {
                if (topText.isNotBlank()) {
                    addAll(topText.split("\r\n"))
                }
                if (debugText.isNotBlank()) {
                    add(debugText)
                }
            }
            val headline = extraLines.firstOrNull() ?: statsSummary

            builder
                .setWhen(whenMs)
                .setSmallIcon(this@ServiceSinkhole.equalizerIcon())
                .setContentTitle(getString(R.string.notify_traffic_title))
                .setContentText(headline)
                .setSubText(statsSummary.takeIf { extraLines.isNotEmpty() })
                .setContentIntent(pi)
                .setColor(notificationColor)
                .setOngoing(true)
                .setAutoCancel(false)
                .setLargeIcon(bitmap)
                .setStyle(
                    Notification.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                        .setBigContentTitle(getString(R.string.notify_traffic_title))
                        .setSummaryText(statsSummary),
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
            }

            if (state == State.none || state == State.waiting) {
                if (state != State.none) {
                    Log.d(TAG, "Stop foreground state=$state")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                startForeground(NOTIFY_TRAFFIC, builder.build())
                state = State.stats
                Log.d(TAG, "Start foreground state=$state")
            } else {
                if (Util.canNotify(this@ServiceSinkhole)) {
                    notificationManager.notify(NOTIFY_TRAFFIC, builder.build())
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startVPN(builder: Builder): ParcelFileDescriptor? {
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

    private fun getBuilder(listAllowed: List<Rule>, listRule: List<Rule>): Builder {
        val prefs = Prefs
        val subnet = prefs.getBoolean("subnet", false)
        val tethering = prefs.getBoolean("tethering", false)
        val lan = prefs.getBoolean("lan", false)
        val ip6 = prefs.getBoolean("ip6", true)
        val filter = prefs.getBoolean("filter", false)
        val system = prefs.getBoolean("manage_system", false)

        val builder = Builder()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

        val mtu = jni_get_mtu()
        Log.i(TAG, "MTU=$mtu")
        builder.setMtu(mtu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        }

        val configure = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, configure, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setConfigureIntent(pi)

        return builder
    }

    private fun startNative(vpn: ParcelFileDescriptor, listAllowed: List<Rule>, listRule: List<Rule>) {
        val prefs = Prefs
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
            lock.writeLock().lock()
            mapUidAllowed.clear()
            mapUidKnown.clear()
            mapHostsBlocked.clear()
            mapMalware.clear()
            mapUidIPFilters.clear()
            mapForward.clear()
            lock.writeLock().unlock()
        }

        if (log) {
            prepareNotify(listRule)
        } else {
            lock.writeLock().lock()
            mapNotify.clear()
            lock.writeLock().unlock()
        }

        if (log || filter) {
            val prio = prefs.getString("loglevel", Log.WARN.toString())?.toIntOrNull() ?: Log.WARN
            val rcode = prefs.getString("rcode", "3")?.toIntOrNull() ?: 3
            if (prefs.getBoolean("socks5_enabled", false)) {
                jni_socks5(
                    prefs.getString("socks5_addr", ""),
                    prefs.getString("socks5_port", "0")?.toIntOrNull() ?: 0,
                    prefs.getString("socks5_username", ""),
                    prefs.getString("socks5_password", ""),
                )
            } else {
                jni_socks5("", 0, "", "")
            }

            if (tunnelThread == null) {
                Log.i(TAG, "Starting tunnel thread context=$jni_context")
                jni_start(jni_context, prio)

                tunnelThread =
                    Thread {
                        Log.i(TAG, "Running tunnel context=$jni_context")
                        jni_run(jni_context, vpn.fd, mapForward.containsKey(53), rcode)
                        Log.i(TAG, "Tunnel exited")
                        tunnelThread = null
                    }
                tunnelThread?.start()

                Log.i(TAG, "Started tunnel thread")
            }
        }
    }

    private fun stopNative(vpn: ParcelFileDescriptor) {
        Log.i(TAG, "Stop native")

        if (tunnelThread != null) {
            Log.i(TAG, "Stopping tunnel thread")

            jni_stop(jni_context)

            var thread = tunnelThread
            while (thread != null && thread.isAlive) {
                try {
                    Log.i(TAG, "Joining tunnel thread context=$jni_context")
                    thread.join()
                } catch (_: InterruptedException) {
                    Log.i(TAG, "Joined tunnel interrupted")
                }
                thread = tunnelThread
            }
            tunnelThread = null

            jni_clear(jni_context)

            Log.i(TAG, "Stopped tunnel thread")
        }
    }

    private fun unprepare() {
        lock.writeLock().lock()
        mapUidAllowed.clear()
        mapUidKnown.clear()
        mapHostsBlocked.clear()
        mapMalware.clear()
        mapUidIPFilters.clear()
        mapForward.clear()
        mapNotify.clear()
        lock.writeLock().unlock()
    }

    private fun prepareUidAllowed(listAllowed: List<Rule>, listRule: List<Rule>) {
        lock.writeLock().lock()

        mapUidAllowed.clear()
        for (rule in listAllowed) {
            mapUidAllowed[rule.uid] = true
        }

        mapUidKnown.clear()
        for (rule in listRule) {
            mapUidKnown[rule.uid] = rule.uid
        }

        lock.writeLock().unlock()
    }

    private fun prepareHostsBlocked() {
        val prefs = Prefs
        val useHosts = prefs.getBoolean("filter", false) && prefs.getBoolean("use_hosts", false)
        val hosts = File(filesDir, "hosts.txt")
        if (!useHosts || !hosts.exists() || !hosts.canRead()) {
            Log.i(TAG, "Hosts file use=$useHosts exists=${hosts.exists()}")
            lock.writeLock().lock()
            mapHostsBlocked.clear()
            lock.writeLock().unlock()
            return
        }

        val changed = hosts.lastModified() != lastHostsModified
        if (!changed && mapHostsBlocked.size > 0) {
            Log.i(TAG, "Hosts file unchanged")
            return
        }
        lastHostsModified = hosts.lastModified()

        lock.writeLock().lock()

        mapHostsBlocked.clear()

        var count = 0
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(hosts))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                var mutableLine = line ?: ""
                val hash = mutableLine.indexOf('#')
                if (hash >= 0) {
                    mutableLine = mutableLine.substring(0, hash)
                }
                mutableLine = mutableLine.trim()
                if (mutableLine.isNotEmpty()) {
                    val words = mutableLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                    if (words.size == 2) {
                        count++
                        mapHostsBlocked[words[1]] = true
                    } else {
                        Log.i(TAG, "Invalid hosts file line: $mutableLine")
                    }
                }
            }
            mapHostsBlocked["test.netguard.me"] = true
            Log.i(TAG, "$count hosts read")
        } catch (ex: IOException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (exex: IOException) {
                    Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                }
            }
        }

        lock.writeLock().unlock()
    }

    private fun prepareMalwareList() {
        val prefs = Prefs
        val malware = prefs.getBoolean("filter", false) && prefs.getBoolean("malware", false)
        val file = File(filesDir, "malware.txt")
        if (!malware || !file.exists() || !file.canRead()) {
            Log.i(TAG, "Malware use=$malware exists=${file.exists()}")
            lock.writeLock().lock()
            mapMalware.clear()
            lock.writeLock().unlock()
            return
        }

        val changed = file.lastModified() != lastMalwareModified
        if (!changed && mapMalware.size > 0) {
            Log.i(TAG, "Malware unchanged")
            return
        }
        lastMalwareModified = file.lastModified()

        lock.writeLock().lock()

        mapMalware.clear()

        var count = 0
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(file))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                var mutableLine = line ?: ""
                val hash = mutableLine.indexOf('#')
                if (hash >= 0) {
                    mutableLine = mutableLine.substring(0, hash)
                }
                mutableLine = mutableLine.trim()
                if (mutableLine.isNotEmpty()) {
                    val words = mutableLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                    if (words.size > 1) {
                        count++
                        mapMalware[words[1]] = true
                    } else {
                        Log.i(TAG, "Invalid malware file line: $mutableLine")
                    }
                }
            }
            Log.i(TAG, "$count malware read")
        } catch (ex: IOException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (exex: IOException) {
                    Log.e(TAG, exex.toString() + "\n" + Log.getStackTraceString(exex))
                }
            }
        }

        lock.writeLock().unlock()
    }

    private fun prepareUidIPFilters(dname: String?) {
        lock.writeLock().lock()

        if (dname == null) {
            mapUidIPFilters.clear()
            if (!IAB.isPurchased(ActivityPro.SKU_FILTER, this@ServiceSinkhole)) {
                lock.writeLock().unlock()
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
                val dresource = if (cursor.isNull(colResource)) null else cursor.getString(colResource)
                val dport = cursor.getInt(colDPort)
                val block = cursor.getInt(colBlock) > 0
                val time = if (cursor.isNull(colTime)) Date().time else cursor.getLong(colTime)
                val ttl = if (cursor.isNull(colTTL)) 7 * 24 * 3600 * 1000L else cursor.getLong(colTTL)

                if (isLockedDown(lastMetered)) {
                    val pkg = packageManager.getPackagesForUid(uid)
                    if (pkg != null && pkg.isNotEmpty()) {
                        if (!Prefs.getBoolean(Prefs.namespaced("lockdown", pkg[0]), false)) {
                            continue
                        }
                    }
                }

                val key = IPKey(version, protocol, dport, uid)
                synchronized(mapUidIPFilters) {
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
                                val rule = IPRule(key, "$name/$iname", block, time, ttl)
                                mapUidIPFilters[key]?.put(iname, rule)
                                if (exists) {
                                    Log.w(TAG, "Address conflict $key $daddr/$dresource")
                                }
                            } else if (exists) {
                                currentRule?.updateExpires(time, ttl)
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

        lock.writeLock().unlock()
    }

    private fun prepareForwarding() {
        lock.writeLock().lock()
        mapForward.clear()

        val prefs = Prefs
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
        lock.writeLock().unlock()
    }

    private fun prepareNotify(listRule: List<Rule>) {
        val prefs = Prefs
        val notify = prefs.getBoolean("notify_access", false)
        val system = prefs.getBoolean("manage_system", false)

        lock.writeLock().lock()
        mapNotify.clear()
        for (rule in listRule) {
            mapNotify[rule.uid] = notify && rule.notify && (system || !rule.system)
        }
        lock.writeLock().unlock()
    }

    private fun isLockedDown(metered: Boolean): Boolean {
        val prefs = Prefs
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
        val prefs = Prefs

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

            Prefs.putBoolean("enabled", false)
            Widgets.updateFirewall(this)
        }
    }

    private fun nativeError(error: Int, message: String?) {
        Log.w(TAG, "Native error $error: $message")
        showErrorNotification(message ?: "")
    }

    private fun logPacket(packet: Packet) {
        logHandler.queue(packet)
    }

    private fun dnsResolved(rr: ResourceRecord) {
        if (DatabaseHelper.getInstance(this@ServiceSinkhole).insertDns(rr)) {
            Log.i(TAG, "New IP $rr")
            prepareUidIPFilters(rr.QName)
        }
        if (rr.uid > 0 && !TextUtils.isEmpty(rr.AName)) {
            lock.readLock().lock()
            val malware = mapMalware.containsKey(rr.AName) && mapMalware[rr.AName] == true
            lock.readLock().unlock()

            if (malware) {
                val notified = Prefs.getBoolean("malware.${rr.uid}", false)
                if (!notified) {
                    Prefs.putBoolean("malware.${rr.uid}", true)
                    notifyNewApplication(rr.uid, true)
                }
            }
        }
    }

    private fun isDomainBlocked(name: String): Boolean {
        lock.readLock().lock()
        val blocked = mapHostsBlocked.containsKey(name) && mapHostsBlocked[name] == true
        lock.readLock().unlock()
        return blocked
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun getUidQ(version: Int, protocol: Int, saddr: String, sport: Int, daddr: String, dport: Int): Int {
        if (protocol != 6 && protocol != 17) {
            return Process.INVALID_UID
        }

        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return Process.INVALID_UID

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
        val prefs = Prefs

        lock.readLock().lock()

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
                    !mapUidKnown.containsKey(packet.uid) &&
                    isSupported(packet.protocol)
            ) {
                packet.allowed = true
                Log.w(TAG, "Allowing unknown system $packet")
            } else if (packet.uid == Process.myUid()) {
                packet.allowed = true
                Log.w(TAG, "Allowing self $packet")
            } else {
                var filtered = false
                val key = IPKey(packet.version, packet.protocol, packet.dport, packet.uid)
                if (mapUidIPFilters.containsKey(key)) {
                    try {
                        val daddr = packet.daddr
                        if (daddr == null) {
                            lock.readLock().unlock()
                            return null
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
                    if (mapUidAllowed.containsKey(packet.uid)) {
                        packet.allowed = mapUidAllowed[packet.uid] == true
                    } else {
                        Log.w(TAG, "No rules for $packet")
                    }
                }
            }
        }

        var allowed: Allowed? = null
        if (packet.allowed) {
            if (mapForward.containsKey(packet.dport)) {
                val fwd = mapForward[packet.dport]
                if (fwd != null) {
                    allowed =
                        if (fwd.ruid == packet.uid) {
                            Allowed()
                        } else {
                            packet.data = "> " + fwd.raddr + "/" + fwd.rport
                            Allowed(fwd.raddr, fwd.rport)
                        }
                }
            } else {
                allowed = Allowed()
            }
        }

        lock.readLock().unlock()

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

                executor.submit {
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
                        val prefs = Prefs
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
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    am.set(AlarmManager.RTC_WAKEUP, Date().time + delay * 60 * 1000L, pi)
                                } else {
                                    am.setAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        Date().time + delay * 60 * 1000L,
                                        pi,
                                    )
                                }
                            }
                        }

                        statsHandler.sendEmptyMessage(
                            if (Util.isInteractive(this@ServiceSinkhole)) MSG_STATS_START else MSG_STATS_STOP,
                        )
                    } catch (ex: Throwable) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            am.set(AlarmManager.RTC_WAKEUP, Date().time + 15 * 1000L, pi)
                        } else {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Date().time + 15 * 1000L, pi)
                        }
                    }
                }
            }
        }

    private val userReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received $intent")
                Util.logExtras(intent)

                userForeground = Intent.ACTION_USER_FOREGROUND == intent.action
                Log.i(TAG, "User foreground=$userForeground user=" + (Process.myUid() / 100000))

                if (userForeground) {
                    val prefs = Prefs
                    if (prefs.getBoolean("enabled", false)) {
                        try {
                            Thread.sleep(3000)
                        } catch (_: InterruptedException) {
                        }

                        start("foreground", this@ServiceSinkhole)
                    }
                } else {
                    stop("background", this@ServiceSinkhole, true)
                }
            }
        }

    private val idleStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.M)
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

    private val networkMonitorCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            private val tag = "NetGuard.Monitor"
            private val validated = HashMap<Network, Long>()

            override fun onAvailable(network: Network) {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val capabilities = cm.getNetworkCapabilities(network)
                Log.i(tag, "Available network $network")
                Log.i(tag, "Capabilities=$capabilities")
                checkConnectivity(network, capabilities)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                Log.i(tag, "New capabilities network $network")
                Log.i(tag, "Capabilities=$capabilities")
                checkConnectivity(network, capabilities)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                Log.i(tag, "Losing network $network within $maxMsToLive ms")
            }

            override fun onLost(network: Network) {
                Log.i(tag, "Lost network $network")

                synchronized(validated) { validated.remove(network) }
            }

            override fun onUnavailable() {
                Log.i(tag, "No networks available")
            }

            private fun checkConnectivity(
                network: Network,
                capabilities: NetworkCapabilities?,
            ) {
                if (
                    isActiveNetwork(network) &&
                        capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    synchronized(validated) {
                        if (validated.containsKey(network) &&
                            (validated[network] ?: 0L) + 20 * 1000 > Date().time
                        ) {
                            Log.i(tag, "Already validated $network")
                            return
                        }
                    }

                    val prefs = Prefs
                    val host = prefs.getString("validate", "www.google.com") ?: "www.google.com"
                    Log.i(tag, "Validating $network host=$host")

                    var socket: Socket? = null
                    try {
                        socket = network.socketFactory.createSocket()
                        socket.connect(InetSocketAddress(host, 443), 10000)
                        Log.i(tag, "Validated $network host=$host")
                        synchronized(validated) { validated[network] = Date().time }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                            cm.reportNetworkConnectivity(network, true)
                            Log.i(tag, "Reported $network")
                        }
                    } catch (ex: IOException) {
                        Log.e(tag, ex.toString())
                        Log.i(tag, "No connectivity $network")
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close()
                            } catch (ex: IOException) {
                                Log.e(tag, ex.toString() + "\n" + Log.getStackTraceString(ex))
                            }
                        }
                    }
                }
            }
        }

    private fun handleDataConnectionStateChanged(state: Int) {
        if (state == TelephonyManager.DATA_CONNECTED) {
            val currentGeneration = Util.getNetworkGeneration(this)
            Log.i(TAG, "Data connected generation=$currentGeneration")

            if (lastGeneration == null || lastGeneration != currentGeneration) {
                Log.i(TAG, "New network generation=$currentGeneration")
                lastGeneration = currentGeneration

                val prefs = Prefs
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
        if (Prefs.getBoolean("enabled", false)) {
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
                            val prefs = Prefs
                            if (IAB.isPurchased(ActivityPro.SKU_NOTIFY, context) && prefs.getBoolean("install", true)) {
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
                            Prefs.remove(Prefs.namespaced("wifi", packageName))
                            Prefs.remove(Prefs.namespaced("other", packageName))
                            Prefs.remove(Prefs.namespaced("screen_wifi", packageName))
                            Prefs.remove(Prefs.namespaced("screen_other", packageName))
                            Prefs.remove(Prefs.namespaced("roaming", packageName))
                            Prefs.remove(Prefs.namespaced("lockdown", packageName))
                            Prefs.remove(Prefs.namespaced("apply", packageName))
                            Prefs.remove(Prefs.namespaced("notify", packageName))

                            val uid = intent.getIntExtra(Intent.EXTRA_UID, 0)
                            if (uid > 0) {
                                val dh = DatabaseHelper.getInstance(context)
                                dh.clearLog(uid)
                                dh.clearAccess(uid, false)

                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        val prefs = Prefs
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
            val pi = PendingIntentCompat.getActivity(this, uid, main, PendingIntent.FLAG_UPDATE_CURRENT)

            val notificationColor = themePrimaryColor(Prefs.getString("theme", THEME_DEFAULT))
            val builder = Notification.Builder(this, if (malware) Notifications.CHANNEL_MALWARE else Notifications.CHANNEL_NOTIFY)
            builder
                .setSmallIcon(this.securityIcon())
                .setContentIntent(pi)
                .setColor(notificationColor)
                .setAutoCancel(true)

            if (malware) {
                builder.setContentTitle(name)
                    .setContentText(getString(R.string.msg_malware, name))
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setContentTitle(name)
                        .setContentText(getString(R.string.msg_installed_n))
                } else {
                    builder.setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.msg_installed, name))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_STATUS)
                    .setVisibility(Notification.VISIBILITY_SECRET)
            }

            val packageName = packages[0]
            val wifi = Prefs.getBoolean(
                Prefs.namespaced("wifi", packageName),
                prefs.getBoolean("whitelist_wifi", true),
            )
            val other = Prefs.getBoolean(
                Prefs.namespaced("other", packageName),
                prefs.getBoolean("whitelist_other", true),
            )

            val riWifi = Intent(this, ServiceSinkhole::class.java)
            riWifi.putExtra(EXTRA_COMMAND, Command.set)
            riWifi.putExtra(EXTRA_NETWORK, "wifi")
            riWifi.putExtra(EXTRA_UID, uid)
            riWifi.putExtra(EXTRA_PACKAGE, packageName)
            riWifi.putExtra(EXTRA_BLOCKED, !wifi)

            val piWifi = PendingIntentCompat.getService(this, uid, riWifi, PendingIntent.FLAG_UPDATE_CURRENT)
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
                PendingIntentCompat.getService(this, uid + 10000, riOther, PendingIntent.FLAG_UPDATE_CURRENT)
            val oAction =
                Notification.Action.Builder(
                    if (other) this.cellularIcon(true) else this.cellularIcon(false),
                    getString(if (other) R.string.title_allow_other else R.string.title_block_other),
                    piOther,
                ).build()
            builder.addAction(oAction)

            if (internet) {
                if (Util.canNotify(this)) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(uid, builder.build())
                }
            } else {
                val expanded = Notification.BigTextStyle(builder)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    expanded.bigText(getString(R.string.msg_installed_n))
                } else {
                    expanded.bigText(getString(R.string.msg_installed, name))
                }
                expanded.setSummaryText(getString(R.string.title_internet))
                if (Util.canNotify(this)) {
                    val notification = expanded.build() ?: builder.build()
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(uid, notification)
                }
            }
        } catch (ex: PackageManager.NameNotFoundException) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    override fun onCreate() {
        Log.i(TAG, "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this))
        startForeground(NOTIFY_WAITING, getWaitingNotification())

        val prefs = Prefs

        if (jni_context != 0L) {
            Log.w(TAG, "Create with context=$jni_context")
            jni_stop(jni_context)
            synchronized(jni_lock) {
                jni_done(jni_context)
                jni_context = 0
            }
        }

        jni_context = jni_init(Build.VERSION.SDK_INT)
        Log.i(TAG, "Created context=$jni_context")
        val pcap = prefs.getBoolean("pcap", false)
        setPcap(pcap, this)

        removePrefsListener = Prefs.addListener { key ->
            onPreferenceChanged(key)
        }

        Util.setTheme(this)
        super.onCreate()

        val commandThread =
            HandlerThread(getString(R.string.app_name) + " command", Process.THREAD_PRIORITY_FOREGROUND)
        val logThread =
            HandlerThread(getString(R.string.app_name) + " log", Process.THREAD_PRIORITY_BACKGROUND)
        val statsThread =
            HandlerThread(getString(R.string.app_name) + " stats", Process.THREAD_PRIORITY_BACKGROUND)
        commandThread.start()
        logThread.start()
        statsThread.start()

        commandLooper = commandThread.looper
        logLooper = logThread.looper
        statsLooper = statsThread.looper

        commandHandler = CommandHandler(commandLooper)
        logHandler = LogHandler(logLooper)
        statsHandler = StatsHandler(statsLooper)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val ifUser = IntentFilter()
            ifUser.addAction(Intent.ACTION_USER_BACKGROUND)
            ifUser.addAction(Intent.ACTION_USER_FOREGROUND)
            ContextCompat.registerReceiver(this, userReceiver, ifUser, ContextCompat.RECEIVER_NOT_EXPORTED)
            registeredUser = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val ifIdle = IntentFilter()
            ifIdle.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            ContextCompat.registerReceiver(this, idleStateReceiver, ifIdle, ContextCompat.RECEIVER_NOT_EXPORTED)
            registeredIdleState = true
        }

        val ifAp = IntentFilter()
        ifAp.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
        ContextCompat.registerReceiver(this, apStateReceiver, ifAp, ContextCompat.RECEIVER_NOT_EXPORTED)
        registeredApState = true

        val ifPackage = IntentFilter()
        ifPackage.addAction(Intent.ACTION_PACKAGE_ADDED)
        ifPackage.addAction(Intent.ACTION_PACKAGE_REMOVED)
        ifPackage.addDataScheme("package")
        ContextCompat.registerReceiver(this, packageChangedReceiver, ifPackage, ContextCompat.RECEIVER_NOT_EXPORTED)
        registeredPackageChanged = true

        try {
            listenNetworkChanges()
        } catch (ex: Throwable) {
            Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
        listenConnectivityChanges()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            networkMonitorCallback,
        )

        WorkScheduler.scheduleHousekeeping(this)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun listenNetworkChanges() {
        Log.i(TAG, "Starting listening to network changes")
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val nc =
            object : ConnectivityManager.NetworkCallback() {
                private var lastActive: Network? = null
                private var lastNetwork: Network? = null
                private var lastConnectedState: Boolean? = null
                private var lastMeteredState: Boolean? = null
                private var lastGeneration: String? = null
                private var lastDns: List<InetAddress>? = null

                override fun onAvailable(network: Network) {
                    Log.i(TAG, "Available network=$network")
                    if (!isActiveNetwork(network)) return

                    lastActive = network
                    lastConnectedState = Util.isConnected(this@ServiceSinkhole)
                    lastMeteredState = Util.isMeteredNetwork(this@ServiceSinkhole)
                    reload("network available", this@ServiceSinkhole, false)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    Log.i(TAG, "Changed properties=$network props=$linkProperties")
                    if (!isActiveNetwork(network)) return

                    val dns = linkProperties.dnsServers
                    val prefs = Prefs
                    if (
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            !same(lastDns, dns)
                        } else {
                            prefs.getBoolean("reload_onconnectivity", false)
                        }
                    ) {
                        Log.i(
                            TAG,
                            "Changed link properties=$linkProperties" +
                                "DNS cur=" + TextUtils.join(",", dns) +
                                "DNS prv=" + (lastDns?.let { TextUtils.join(",", it) }),
                        )
                        lastDns = dns
                        reload("link properties changed", this@ServiceSinkhole, false)
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    Log.i(TAG, "Changed capabilities=$network caps=$networkCapabilities")
                    if (!isActiveNetwork(network)) return

                    val connected = Util.isConnected(this@ServiceSinkhole)
                    val metered = Util.isMeteredNetwork(this@ServiceSinkhole)
                    val generation = Util.getNetworkGeneration(this@ServiceSinkhole)
                    Log.i(
                        TAG,
                        "Connected=$connected/$lastConnectedState" +
                            " unmetered=$metered/$lastMeteredState" +
                            " generation=$generation/$lastGeneration",
                    )

                    var reason: String? = null

                    if (reason == null && !Objects.equals(network, lastNetwork)) reason = "Network changed"

                    if (reason == null && lastConnectedState != null && lastConnectedState != connected) {
                        reason = "Connected state changed"
                    }

                    if (reason == null && lastMeteredState != null && lastMeteredState != metered) {
                        reason = "Unmetered state changed"
                    }

                    if (reason == null && lastGeneration != null && lastGeneration != generation) {
                        val prefs = Prefs
                        if (
                            prefs.getBoolean("unmetered_2g", false) ||
                                prefs.getBoolean("unmetered_3g", false) ||
                                prefs.getBoolean("unmetered_4g", false)
                        ) {
                            reason = "Generation changed"
                        }
                    }

                    if (reason != null) {
                        reload(reason, this@ServiceSinkhole, false)
                    }

                    lastNetwork = network
                    lastConnectedState = connected
                    lastMeteredState = metered
                    lastGeneration = generation
                }

                override fun onLost(network: Network) {
                    Log.i(TAG, "Lost network=$network active=${isActiveNetwork(network)}")
                    if (lastActive == null || lastActive != network) return

                    lastActive = null
                    lastConnectedState = Util.isConnected(this@ServiceSinkhole)
                    reload("network lost", this@ServiceSinkhole, false)
                }

                private fun same(last: List<InetAddress>?, current: List<InetAddress>?): Boolean {
                    if (last == null || current == null) return false
                    if (last.size != current.size) return false

                    for (i in current.indices) {
                        if (last[i] != current[i]) return false
                    }

                    return true
                }
            }
        cm.registerNetworkCallback(builder.build(), nc)
        networkCallback = nc
    }

    private fun listenConnectivityChanges() {
        Log.i(TAG, "Starting listening to service state changes")
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm != null) {
            registerDataConnectionListener(tm)
        }
    }

    private fun getActiveNetwork(): Network? {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager? ?: return null

        val active = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(active)
        return if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
            active
        } else {
            Log.w(TAG, "getActiveNetwork: active network is VPN")
            null
        }
    }

    private fun isActiveNetwork(network: Network?): Boolean {
        return network != null && network == getActiveNetwork()
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
            val watchdog = Prefs.getString("watchdog", "0")?.toIntOrNull() ?: 0
            val enabled = Prefs.getBoolean("enabled", false)
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

        val enabled = Prefs.getBoolean("enabled", false)

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

        commandHandler.queue(actualIntent)

        return START_STICKY
    }

    private fun set(intent: Intent) {
        val uid = intent.getIntExtra(EXTRA_UID, 0)
        val network = intent.getStringExtra(EXTRA_NETWORK)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        val blocked = intent.getBooleanExtra(EXTRA_BLOCKED, false)
        Log.i(TAG, "Set $pkg $network=$blocked")

        val settings = Prefs
        val defaultWifi = settings.getBoolean("whitelist_wifi", true)
        val defaultOther = settings.getBoolean("whitelist_other", true)

        val networkName = network ?: "other"
        val key = Prefs.namespaced(networkName, pkg ?: "")
        if (blocked == (if ("wifi" == networkName) defaultWifi else defaultOther)) {
            Prefs.remove(key)
        } else {
            Prefs.putBoolean(key, blocked)
        }

        reload("notification", this@ServiceSinkhole, false)

        notifyNewApplication(uid, false)

        val ruleset = Intent(ActivityMain.ACTION_RULES_CHANGED).setPackage(packageName)
        sendBroadcast(ruleset)
    }

    override fun onRevoke() {
        Log.i(TAG, "Revoke")

        Prefs.putBoolean("enabled", false)

        showDisabledNotification()
        Widgets.updateFirewall(this)

        super.onRevoke()
    }

    override fun onDestroy() {
        synchronized(this) {
            Log.i(TAG, "Destroy")
            commandLooper.quit()
            logLooper.quit()
            statsLooper.quit()

            for (command in Command.values()) {
                commandHandler.removeMessages(command.ordinal)
            }
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

            if (networkCallback != null) {
                unlistenNetworkChanges()
                networkCallback = null
            }
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(networkMonitorCallback)

            unregisterDataConnectionListener(tm)

            try {
                if (vpn != null) {
                    stopNative(vpn!!)
                    stopVPN(vpn!!)
                    vpn = null
                    unprepare()
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            Log.i(TAG, "Destroy context=$jni_context")
            synchronized(jni_lock) {
                jni_done(jni_context)
                jni_context = 0
            }

            removePrefsListener?.invoke()
            removePrefsListener = null
        }

        super.onDestroy()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun unlistenNetworkChanges() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let { cm.unregisterNetworkCallback(it) }
    }

    private fun getEnforcingNotification(allowed: Int, blocked: Int, hosts: Int): Notification {
        val main = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationColor = themePrimaryColor(Prefs.getString("theme", THEME_DEFAULT))
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setContentTitle(getString(R.string.msg_started))
        } else {
            builder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.msg_started))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setPriority(Notification.PRIORITY_MIN)
        }

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
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (Util.isPlayStoreInstall(this)) {
                    builder.setContentText(getString(R.string.msg_packages, allowedValue, blockedValue))
                } else {
                    builder.setContentText(getString(R.string.msg_hosts, allowedValue, blockedValue, hostsValue))
                }
                builder.build()
            } else {
                val notification = Notification.BigTextStyle(builder)
                notification.bigText(getString(R.string.msg_started))
                if (Util.isPlayStoreInstall(this)) {
                    notification.setSummaryText(getString(R.string.msg_packages, allowedValue, blockedValue))
                } else {
                    notification.setSummaryText(getString(R.string.msg_hosts, allowedValue, blockedValue, hostsValue))
                }
                notification.build() ?: builder.build()
            }
        }

        return builder.build()
    }

    private fun updateEnforcingNotification(allowed: Int, total: Int) {
        val notification = getEnforcingNotification(allowed, total - allowed, mapHostsBlocked.size)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Util.canNotify(this)) {
            nm.notify(NOTIFY_ENFORCING, notification)
        }
    }

    private fun getWaitingNotification(): Notification {
        val main = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationColor = themePrimaryColor(Prefs.getString("theme", THEME_DEFAULT))
        val builder = Notification.Builder(this, Notifications.CHANNEL_FOREGROUND)
        builder.setSmallIcon(this.securityIcon())
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(true)
            .setAutoCancel(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setContentTitle(getString(R.string.msg_waiting))
        } else {
            builder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.msg_waiting))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setPriority(Notification.PRIORITY_MIN)
        }

        return builder.build()
    }

    private fun showDisabledNotification() {
        val main = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(this, Notifications.CHANNEL_NOTIFY)
        val notificationColor = themeOffColor(Prefs.getString("theme", THEME_DEFAULT))
        builder.setSmallIcon(this.errorIcon())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.msg_revoked))
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(false)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        val notification = Notification.BigTextStyle(builder)
        notification.bigText(getString(R.string.msg_revoked))

        if (Util.canNotify(this)) {
            val built = notification.build() ?: builder.build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFY_DISABLED, built)
        }
    }

    private fun showLockdownNotification() {
        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
        val pi = PendingIntentCompat.getActivity(this, NOTIFY_LOCKDOWN, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(this, Notifications.CHANNEL_NOTIFY)
        val notificationColor = themeOffColor(Prefs.getString("theme", THEME_DEFAULT))
        builder.setSmallIcon(this.errorIcon())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.msg_always_on_lockdown))
            .setContentIntent(pi)
            .setPriority(Notification.PRIORITY_HIGH)
            .setColor(notificationColor)
            .setOngoing(false)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        val notification = Notification.BigTextStyle(builder)
        notification.bigText(getString(R.string.msg_always_on_lockdown))

        if (Util.canNotify(this)) {
            val built = notification.build() ?: builder.build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFY_LOCKDOWN, built)
        }
    }

    private fun removeLockdownNotification() {
        getSystemService(Context.NOTIFICATION_SERVICE).also {
            val notificationManager = it as NotificationManager
            notificationManager.cancel(NOTIFY_LOCKDOWN)
        }
    }

    private fun showAutoStartNotification() {
        val main = Intent(this, ActivityMain::class.java)
        main.putExtra(ActivityMain.EXTRA_APPROVE, true)
        val pi = PendingIntentCompat.getActivity(this, NOTIFY_AUTOSTART, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(this, Notifications.CHANNEL_NOTIFY)
        val notificationColor = themeOffColor(Prefs.getString("theme", THEME_DEFAULT))
        builder.setSmallIcon(this.errorIcon())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.msg_autostart))
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(false)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        val notification = Notification.BigTextStyle(builder)
        notification.bigText(getString(R.string.msg_autostart))

        if (Util.canNotify(this)) {
            val built = notification.build() ?: builder.build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFY_AUTOSTART, built)
        }
    }

    private fun showErrorNotification(message: String) {
        val main = Intent(this, ActivityMain::class.java)
        val pi = PendingIntentCompat.getActivity(this, 0, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(this, Notifications.CHANNEL_NOTIFY)
        val notificationColor = themeOffColor(Prefs.getString("theme", THEME_DEFAULT))
        builder.setSmallIcon(this.errorIcon())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.msg_error, message))
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(false)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        val notification = Notification.BigTextStyle(builder)
        notification.bigText(getString(R.string.msg_error, message))
        notification.setSummaryText(message)

        if (Util.canNotify(this)) {
            val built = notification.build() ?: builder.build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFY_ERROR, built)
        }
    }

    private fun showAccessNotification(uid: Int) {
        val apps = Util.getApplicationNames(uid, this@ServiceSinkhole)
        if (apps.isEmpty()) return
        val name = TextUtils.join(", ", apps)

        val main = Intent(this@ServiceSinkhole, ActivityMain::class.java)
        main.putExtra(ActivityMain.EXTRA_SEARCH, uid.toString())
        val pi =
            PendingIntentCompat.getActivity(this@ServiceSinkhole, uid + 10000, main, PendingIntent.FLAG_UPDATE_CURRENT)

        val theme = Prefs.getString("theme", THEME_DEFAULT)
        val colorOn = themeOnColor(theme)
        val colorOff = themeOffColor(theme)

        val builder = Notification.Builder(this, Notifications.CHANNEL_ACCESS)
        builder.setSmallIcon(this.cloudUploadIcon())
            .setGroup("AccessAttempt")
            .setContentIntent(pi)
            .setColor(colorOff)
            .setOngoing(false)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setContentTitle(name)
                .setContentText(getString(R.string.msg_access_n))
        } else {
            builder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.msg_access, name))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        val df: DateFormat = SimpleDateFormat("dd HH:mm")

        val notification = Notification.InboxStyle(builder)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.addLine(getString(R.string.msg_access_n))
        } else {
            val sname = getString(R.string.msg_access, name)
            val pos = sname.indexOf(name)
            val sp: Spannable = SpannableString(sname)
            sp.setSpan(StyleSpan(Typeface.BOLD), pos, pos + name.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            notification.addLine(sp)
        }

        var since: Long = 0
        val pm = packageManager
        val packages = pm.getPackagesForUid(uid)
        if (packages != null && packages.isNotEmpty()) {
            try {
                since = pm.getPackageInfo(packages[0], 0).firstInstallTime
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }

        DatabaseHelper.getInstance(this@ServiceSinkhole).getAccessUnset(uid, 7, since).use { cursor ->
            val colDAddr = cursor.getColumnIndex("daddr")
            val colTime = cursor.getColumnIndex("time")
            val colAllowed = cursor.getColumnIndex("allowed")
            while (cursor.moveToNext()) {
                val sb = StringBuilder()
                sb.append(df.format(cursor.getLong(colTime))).append(' ')

                var daddr = cursor.getString(colDAddr)
                if (Util.isNumericAddress(daddr)) {
                    try {
                        daddr = InetAddress.getByName(daddr).hostName
                    } catch (_: UnknownHostException) {
                    }
                }
                sb.append(daddr)

                val allowed = cursor.getInt(colAllowed)
                if (allowed >= 0) {
                    val pos = sb.indexOf(daddr)
                    val sp: Spannable = SpannableString(sb)
                    val fgsp = ForegroundColorSpan(if (allowed > 0) colorOn else colorOff)
                    sp.setSpan(fgsp, pos, pos + daddr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    notification.addLine(sp)
                } else {
                    notification.addLine(sb)
                }
            }
        }

        if (Util.canNotify(this)) {
            val built = notification.build() ?: builder.build()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(uid + 10000, built)
        }
    }

    private fun showUpdateNotification(name: String, url: String) {
        val download = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pi = PendingIntentCompat.getActivity(this, 0, download, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationColor = themePrimaryColor(Prefs.getString("theme", THEME_DEFAULT))
        val builder = Notification.Builder(this, Notifications.CHANNEL_NOTIFY)
        builder.setSmallIcon(this.securityIcon())
            .setContentTitle(name)
            .setContentText(getString(R.string.msg_update))
            .setContentIntent(pi)
            .setColor(notificationColor)
            .setOngoing(false)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
        }

        if (Util.canNotify(this)) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFY_UPDATE, builder.build())
        }
    }

    private fun removeWarningNotifications() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFY_DISABLED)
        notificationManager.cancel(NOTIFY_AUTOSTART)
        notificationManager.cancel(NOTIFY_ERROR)
    }

    private inner class Builder : VpnService.Builder() {
        private val activeNetwork: Network?
        private val activeTransports: Set<Int>
        private var mtu: Int = 0
        private val listAddress = ArrayList<String>()
        private val listRoute = ArrayList<String>()
        private val listDns = ArrayList<InetAddress>()
        private val listAllowed = ArrayList<String>()
        private val listDisallowed = ArrayList<String>()

        init {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            activeNetwork = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) null else cm.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
            activeTransports =
                buildSet {
                    if (caps == null) return@buildSet
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add(NetworkCapabilities.TRANSPORT_WIFI)
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add(NetworkCapabilities.TRANSPORT_CELLULAR)
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add(NetworkCapabilities.TRANSPORT_ETHERNET)
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add(NetworkCapabilities.TRANSPORT_VPN)
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                }
        }

        override fun setMtu(mtu: Int): VpnService.Builder {
            this.mtu = mtu
            super.setMtu(mtu)
            return this
        }

        override fun addAddress(address: String, prefixLength: Int): Builder {
            listAddress.add("$address/$prefixLength")
            super.addAddress(address, prefixLength)
            return this
        }

        override fun addRoute(address: String, prefixLength: Int): Builder {
            listRoute.add("$address/$prefixLength")
            super.addRoute(address, prefixLength)
            return this
        }

        override fun addRoute(address: InetAddress, prefixLength: Int): Builder {
            val host = address.hostAddress ?: return this
            listRoute.add("$host/$prefixLength")
            super.addRoute(address, prefixLength)
            return this
        }

        override fun addDnsServer(address: InetAddress): Builder {
            listDns.add(address)
            super.addDnsServer(address)
            return this
        }

        @Throws(PackageManager.NameNotFoundException::class)
        override fun addAllowedApplication(packageName: String): VpnService.Builder {
            listAllowed.add(packageName)
            return super.addAllowedApplication(packageName)
        }

        @Throws(PackageManager.NameNotFoundException::class)
        override fun addDisallowedApplication(packageName: String): Builder {
            listDisallowed.add(packageName)
            super.addDisallowedApplication(packageName)
            return this
        }

        override fun equals(other: Any?): Boolean {
            val otherBuilder = other as? Builder ?: return false

            if (!Objects.equals(activeNetwork, otherBuilder.activeNetwork)) return false

            if (activeTransports != otherBuilder.activeTransports) {
                return false
            }

            if (mtu != otherBuilder.mtu) return false

            if (listAddress.size != otherBuilder.listAddress.size) return false
            if (listRoute.size != otherBuilder.listRoute.size) return false
            if (listDns.size != otherBuilder.listDns.size) return false
            if (listAllowed.size != otherBuilder.listAllowed.size) return false
            if (listDisallowed.size != otherBuilder.listDisallowed.size) return false

            for (address in listAddress) if (!otherBuilder.listAddress.contains(address)) return false
            for (route in listRoute) if (!otherBuilder.listRoute.contains(route)) return false
            for (dns in listDns) if (!otherBuilder.listDns.contains(dns)) return false
            for (pkg in listAllowed) if (!otherBuilder.listAllowed.contains(pkg)) return false
            for (pkg in listDisallowed) if (!otherBuilder.listDisallowed.contains(pkg)) return false

            return true
        }
    }

    private class IPKey(
        val version: Int,
        val protocol: Int,
        dport: Int,
        val uid: Int,
    ) {
        val dport: Int = if (protocol == 6 || protocol == 17) dport else 0

        override fun equals(other: Any?): Boolean {
            val otherKey = other as? IPKey ?: return false
            return version == otherKey.version &&
                protocol == otherKey.protocol &&
                dport == otherKey.dport &&
                uid == otherKey.uid
        }

        override fun hashCode(): Int {
            return (version shl 40) or (protocol shl 32) or (dport shl 16) or uid
        }

        override fun toString(): String {
            return "v$version p$protocol port=$dport uid=$uid"
        }
    }

    private class IPRule(
        private val key: IPKey,
        private val name: String,
        private var block: Boolean,
        private var time: Long,
        private var ttl: Long,
    ) {
        fun isBlocked(): Boolean = block

        fun isExpired(): Boolean = System.currentTimeMillis() > time + ttl * 2

        fun updateExpires(time: Long, ttl: Long) {
            this.time = time
            this.ttl = ttl
        }

        override fun equals(other: Any?): Boolean {
            val otherRule = other as? IPRule ?: return false
            return block == otherRule.block && time == otherRule.time && ttl == otherRule.ttl
        }

        override fun toString(): String {
            return "$key $name"
        }
    }

    companion object {
        private const val TAG = "NetGuard.Service"

        private val jni_lock = Any()
        private var jni_context: Long = 0

        private const val NOTIFY_ENFORCING = 1
        private const val NOTIFY_WAITING = 2
        private const val NOTIFY_DISABLED = 3
        private const val NOTIFY_LOCKDOWN = 4
        private const val NOTIFY_AUTOSTART = 5
        private const val NOTIFY_ERROR = 6
        private const val NOTIFY_TRAFFIC = 7
        private const val NOTIFY_UPDATE = 8
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

        private const val MSG_STATS_START = 1
        private const val MSG_STATS_STOP = 2
        private const val MSG_STATS_UPDATE = 3
        private const val MSG_PACKET = 4
        private const val MSG_USAGE = 5

        @Volatile private var wlInstance: PowerManager.WakeLock? = null

        const val ACTION_HOUSE_HOLDING = "eu.faircode.netguard.HOUSE_HOLDING"
        private const val ACTION_SCREEN_OFF_DELAYED = "eu.faircode.netguard.SCREEN_OFF_DELAYED"
        const val ACTION_WATCHDOG = "eu.faircode.netguard.WATCHDOG"

        private external fun jni_pcap(name: String?, record_size: Int, file_size: Int)

        @JvmStatic
        fun setPcap(enabled: Boolean, context: Context) {
            val prefs = Prefs

            var recordSize = 64
            try {
                var r = prefs.getString("pcap_record_size", null)
                if (TextUtils.isEmpty(r)) r = "64"
                recordSize = r?.toIntOrNull() ?: 64
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            var fileSize = 2 * 1024 * 1024
            try {
                var f = prefs.getString("pcap_file_size", null)
                if (TextUtils.isEmpty(f)) f = "2"
                fileSize = (f?.toIntOrNull() ?: 2) * 1024 * 1024
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }

            val pcap = if (enabled) File(context.getDir("data", Context.MODE_PRIVATE), "netguard.pcap") else null
            try {
                jni_pcap(pcap?.absolutePath, recordSize, fileSize)
            } catch (ex: UnsatisfiedLinkError) {
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
            val listDns = ArrayList<InetAddress>()
            val sysDns = Util.getDefaultDNS(context)

            val prefs = Prefs
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
                                    Log.i(TAG, "Local DNS server host=$hostAddress/$prefix dns=$dns")
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
            val prefs = Prefs
            if (prefs.getBoolean("enabled", false)) {
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
    }
}
