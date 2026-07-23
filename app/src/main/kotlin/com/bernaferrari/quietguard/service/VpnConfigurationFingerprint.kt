package com.bernaferrari.quietguard.service

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.InetAddress

/** Captures the effective VPN configuration for safe tunnel-reuse decisions. */
internal class VpnConfigurationFingerprint(connectivityManager: ConnectivityManager) {
    private val activeNetwork: Network? = connectivityManager.activeNetwork
    private val activeTransports: Set<Int> = activeNetwork
        ?.let(connectivityManager::getNetworkCapabilities)
        ?.let { capabilities ->
            buildSet {
                TRANSPORTS.filterTo(this) { capabilities.hasTransport(it) }
            }
        }
        .orEmpty()
    private var mtu = 0
    private val addresses = ArrayList<String>()
    private val routes = ArrayList<String>()
    private val dnsServers = ArrayList<InetAddress>()
    private val allowedApplications = ArrayList<String>()
    private val disallowedApplications = ArrayList<String>()

    fun setMtu(value: Int) {
        mtu = value
    }

    fun addAddress(address: String, prefixLength: Int) {
        addresses.add("$address/$prefixLength")
    }

    fun addRoute(address: String, prefixLength: Int) {
        routes.add("$address/$prefixLength")
    }

    fun addDnsServer(address: InetAddress) {
        dnsServers.add(address)
    }

    fun addAllowedApplication(packageName: String) {
        allowedApplications.add(packageName)
    }

    fun addDisallowedApplication(packageName: String) {
        disallowedApplications.add(packageName)
    }

    override fun equals(other: Any?): Boolean = other is VpnConfigurationFingerprint &&
        activeNetwork == other.activeNetwork &&
        activeTransports == other.activeTransports &&
        mtu == other.mtu &&
        addresses.counts() == other.addresses.counts() &&
        routes.counts() == other.routes.counts() &&
        dnsServers.counts() == other.dnsServers.counts() &&
        allowedApplications.counts() == other.allowedApplications.counts() &&
        disallowedApplications.counts() == other.disallowedApplications.counts()

    override fun hashCode(): Int = listOf(
        activeNetwork,
        activeTransports,
        mtu,
        addresses.counts(),
        routes.counts(),
        dnsServers.counts(),
        allowedApplications.counts(),
        disallowedApplications.counts(),
    ).hashCode()

    private fun <T> List<T>.counts(): Map<T, Int> = groupingBy { it }.eachCount()

    private companion object {
        val TRANSPORTS = setOf(
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_VPN,
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
        )
    }
}
