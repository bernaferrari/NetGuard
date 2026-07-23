package com.bernaferrari.quietguard.service

import com.bernaferrari.quietguard.Util

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.bernaferrari.quietguard.data.PreferencesRepository
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/** Owns network callbacks and converts connectivity changes into service reload requests. */
internal class SinkholeNetworkMonitor(
    context: Context,
    private val prefs: PreferencesRepository,
    private val reload: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var started = false

    private val validatedNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        private var lastActive: Network? = null
        private var lastNetwork: Network? = null
        private var lastConnected: Boolean? = null
        private var lastMetered: Boolean? = null
        private var lastGeneration: String? = null
        private var lastDns: List<InetAddress>? = null

        override fun onAvailable(network: Network) {
            Log.i(TAG, "Available network=$network")
            if (!isActiveNetwork(network)) return

            lastActive = network
            lastConnected = Util.isConnected(appContext)
            lastMetered = Util.isMeteredNetwork(appContext)
            reload("network available")
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            Log.i(TAG, "Changed properties=$network props=$linkProperties")
            if (!isActiveNetwork(network)) return

            val dns = linkProperties.dnsServers
            if (lastDns != dns) {
                Log.i(TAG, "Changed link properties=$linkProperties DNS cur=$dns DNS prv=$lastDns")
                lastDns = dns.toList()
                reload("link properties changed")
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            Log.i(TAG, "Changed capabilities=$network caps=$networkCapabilities")
            if (!isActiveNetwork(network)) return

            val connected = Util.isConnected(appContext)
            val metered = Util.isMeteredNetwork(appContext)
            val generation = Util.getNetworkGeneration(appContext)
            Log.i(
                TAG,
                "Connected=$connected/$lastConnected unmetered=$metered/$lastMetered " +
                    "generation=$generation/$lastGeneration",
            )

            val reason = when {
                network != lastNetwork -> "Network changed"
                lastConnected != null && lastConnected != connected -> "Connected state changed"
                lastMetered != null && lastMetered != metered -> "Unmetered state changed"
                lastGeneration != null && lastGeneration != generation && shouldReloadForGeneration() ->
                    "Generation changed"
                else -> null
            }
            reason?.let(reload)

            lastNetwork = network
            lastConnected = connected
            lastMetered = metered
            lastGeneration = generation
        }

        override fun onLost(network: Network) {
            Log.i(TAG, "Lost network=$network active=${isActiveNetwork(network)}")
            if (lastActive != network) return

            lastActive = null
            lastConnected = Util.isConnected(appContext)
            reload("network lost")
        }
    }

    private val connectivityProbeCallback = object : ConnectivityManager.NetworkCallback() {
        private val recentlyValidated = HashMap<Network, Long>()

        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            Log.i(MONITOR_TAG, "Available network $network")
            Log.i(MONITOR_TAG, "Capabilities=$capabilities")
            checkConnectivity(network, capabilities)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            Log.i(MONITOR_TAG, "New capabilities network $network")
            Log.i(MONITOR_TAG, "Capabilities=$capabilities")
            checkConnectivity(network, capabilities)
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            Log.i(MONITOR_TAG, "Losing network $network within $maxMsToLive ms")
        }

        override fun onLost(network: Network) {
            Log.i(MONITOR_TAG, "Lost network $network")
            synchronized(recentlyValidated) { recentlyValidated.remove(network) }
        }

        override fun onUnavailable() {
            Log.i(MONITOR_TAG, "No networks available")
        }

        private fun checkConnectivity(network: Network, capabilities: NetworkCapabilities?) {
            if (
                !isActiveNetwork(network) ||
                capabilities == null ||
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) ||
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                return
            }

            val now = System.currentTimeMillis()
            synchronized(recentlyValidated) {
                if ((recentlyValidated[network] ?: 0L) + VALIDATION_COOLDOWN_MS > now) {
                    Log.i(MONITOR_TAG, "Already validated $network")
                    return
                }
            }

            val host = prefs.getString("validate", DEFAULT_VALIDATION_HOST) ?: DEFAULT_VALIDATION_HOST
            Log.i(MONITOR_TAG, "Validating $network host=$host")
            try {
                network.socketFactory.createSocket().use { socket ->
                    socket.connect(InetSocketAddress(host, HTTPS_PORT), SOCKET_TIMEOUT_MS)
                }
                Log.i(MONITOR_TAG, "Validated $network host=$host")
                synchronized(recentlyValidated) { recentlyValidated[network] = now }
                connectivityManager.reportNetworkConnectivity(network, true)
                Log.i(MONITOR_TAG, "Reported $network")
            } catch (ex: IOException) {
                Log.i(MONITOR_TAG, "No connectivity $network: $ex")
            }
        }
    }

    fun start() {
        if (started) return
        Log.i(TAG, "Starting listening to network changes")
        val validatedRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        val internetRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(validatedRequest, validatedNetworkCallback)
        try {
            connectivityManager.registerNetworkCallback(internetRequest, connectivityProbeCallback)
            started = true
        } catch (error: RuntimeException) {
            unregister(validatedNetworkCallback)
            throw error
        }
    }

    fun stop() {
        if (!started) return
        started = false
        unregister(validatedNetworkCallback)
        unregister(connectivityProbeCallback)
    }

    private fun unregister(callback: ConnectivityManager.NetworkCallback) {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Network callback was already unregistered", error)
        }
    }

    private fun isActiveNetwork(network: Network): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(active)
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) != true) {
            Log.w(TAG, "Active network is VPN")
            return false
        }
        return network == active
    }

    private fun shouldReloadForGeneration(): Boolean =
        prefs.getBoolean("unmetered_2g", false) ||
            prefs.getBoolean("unmetered_3g", false) ||
            prefs.getBoolean("unmetered_4g", false)

    private companion object {
        const val TAG = "NetGuard.Network"
        const val MONITOR_TAG = "NetGuard.Monitor"
        const val DEFAULT_VALIDATION_HOST = "www.google.com"
        const val HTTPS_PORT = 443
        const val SOCKET_TIMEOUT_MS = 10_000
        val VALIDATION_COOLDOWN_MS = TimeUnit.SECONDS.toMillis(20)
    }
}
