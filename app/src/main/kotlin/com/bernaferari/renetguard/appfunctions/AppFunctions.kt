package com.bernaferari.renetguard.appfunctions

import android.app.PendingIntent
import android.net.VpnService
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionStringValueConstraint
import androidx.appfunctions.service.AppFunction
import com.bernaferari.renetguard.ActivityMain
import com.bernaferari.renetguard.DatabaseHelper
import com.bernaferari.renetguard.Rule
import com.bernaferari.renetguard.ServiceSinkhole
import com.bernaferari.renetguard.Util
import com.bernaferari.renetguard.Widgets
import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.data.preferences
import kotlinx.coroutines.Dispatchers
import org.koin.core.annotation.Single
import kotlinx.coroutines.withContext
import android.content.Context as AndroidContext

/**
 * Exposes NetGuard firewall controls to the Android intelligence system.
 */
@Single
class AppFunctions(
    private val context: AndroidContext,
) {
        /**
         * Returns the current global firewall and lockdown state.
         *
         * @param appFunctionContext The execution context.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun getFirewallStatus(
            appFunctionContext: AppFunctionContext,
        ): FirewallStatus =
            withContext(Dispatchers.IO) {
                val rules = Rule.getRules(false, context)
                val blockedApps =
                    rules.count { rule ->
                        rule.internet && (rule.wifi_blocked || rule.other_blocked)
                    }
                FirewallStatus(
                    enabled = context.preferences().getBoolean("enabled", false),
                    lockdown = context.preferences().getBoolean("lockdown", false),
                    blockedAppCount = blockedApps,
                )
            }

        /**
         * Enables or disables the NetGuard VPN firewall.
         * When VPN permission has not been granted yet, returns a [PendingIntent] that opens
         * the app so the user can approve VPN access.
         *
         * @param appFunctionContext The execution context.
         * @param enabled Whether the firewall should be active.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun setFirewallEnabled(
            appFunctionContext: AppFunctionContext,
            enabled: Boolean,
        ): FirewallToggleResult =
            withContext(Dispatchers.IO) {
                if (enabled) {
                    val vpnIntent = VpnService.prepare(context)
                    if (vpnIntent != null) {
                        val confirmation =
                            PendingIntent.getActivity(
                                context,
                                0,
                                ActivityMain.createEnableFirewallIntent(context),
                                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                            )
                        return@withContext FirewallToggleResult(
                            enabled = context.preferences().getBoolean("enabled", false),
                            message = "VPN permission is required before the firewall can start.",
                            confirmationIntent = confirmation,
                        )
                    }
                    context.preferences().putBoolean("enabled", true)
                    ServiceSinkhole.start("appfunction", context)
                } else {
                    context.preferences().putBoolean("enabled", false)
                    ServiceSinkhole.stop("appfunction", context, false)
                }
                Widgets.updateFirewall(context)
                FirewallToggleResult(
                    enabled = context.preferences().getBoolean("enabled", false),
                    message =
                        if (enabled) {
                            "Firewall enabled."
                        } else {
                            "Firewall disabled."
                        },
                )
            }

        /**
         * Enables or disables global lockdown mode.
         * Lockdown blocks all apps except those explicitly allowed during lockdown.
         *
         * @param appFunctionContext The execution context.
         * @param enabled Whether lockdown mode should be active.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun setLockdownEnabled(
            appFunctionContext: AppFunctionContext,
            enabled: Boolean,
        ): LockdownResult =
            withContext(Dispatchers.IO) {
                context.preferences().putBoolean("lockdown", enabled)
                ServiceSinkhole.reload("appfunction lockdown", context, false)
                Widgets.updateFirewall(context)
                LockdownResult(
                    lockdown = context.preferences().getBoolean("lockdown", false),
                    message =
                        if (enabled) {
                            "Lockdown enabled."
                        } else {
                            "Lockdown disabled."
                        },
                )
            }

        /**
         * Searches installed apps that NetGuard can manage.
         * Use the returned package name with [getAppFirewallStatus] or [setAppInternetBlocked].
         *
         * @param appFunctionContext The execution context.
         * @param query App display name or package name fragment. Null or blank returns the first manageable apps.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun searchApps(
            appFunctionContext: AppFunctionContext,
            query: String?,
        ): List<AppSearchResult> =
            withContext(Dispatchers.IO) {
                val normalized = query?.trim()?.lowercase().orEmpty()
                val matches =
                    Rule.getRules(false, context)
                        .asSequence()
                        .filter { it.internet && it.pkg }
                        .filter { rule ->
                            if (normalized.isBlank()) {
                                true
                            } else {
                                val name = rule.name?.lowercase().orEmpty()
                                val packageName = rule.packageName?.lowercase().orEmpty()
                                name.contains(normalized) || packageName.contains(normalized)
                            }
                        }
                        .sortedBy { (it.name ?: it.packageName.orEmpty()).lowercase() }
                        .take(5)
                        .map { rule ->
                            AppSearchResult(
                                packageName = rule.packageName.orEmpty(),
                                displayName = rule.name ?: rule.packageName.orEmpty(),
                                wifiBlocked = rule.wifi_blocked,
                                mobileBlocked = rule.other_blocked,
                            )
                        }
                        .toList()

                if (matches.isEmpty()) {
                    throw AppFunctionElementNotFoundException(
                        "No app found for query '${query.orEmpty()}'. Ask the user to clarify the app name.",
                    )
                }
                matches
            }

        /**
         * Returns the current Wi-Fi and mobile blocking state for a single app.
         * Call [searchApps] first when the package name is unknown.
         *
         * @param appFunctionContext The execution context.
         * @param packageName The Android package name of the target app.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun getAppFirewallStatus(
            appFunctionContext: AppFunctionContext,
            packageName: String,
        ): AppFirewallStatus =
            withContext(Dispatchers.IO) {
                val rule = findRule(packageName)
                AppFirewallStatus(
                    packageName = rule.packageName.orEmpty(),
                    displayName = rule.name ?: rule.packageName.orEmpty(),
                    wifiBlocked = rule.wifi_blocked,
                    mobileBlocked = rule.other_blocked,
                    lockdownAllowed = rule.lockdown,
                )
            }

        /**
         * Blocks or allows internet access for an app on Wi-Fi and/or mobile data.
         * Call [searchApps] first when the package name is unknown.
         *
         * @param appFunctionContext The execution context.
         * @param packageName The Android package name of the target app.
         * @param network Which network type to change. Accepts "WIFI", "MOBILE", or "BOTH".
         * @param blocked Whether internet access should be blocked on the selected network(s).
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun setAppInternetBlocked(
            appFunctionContext: AppFunctionContext,
            packageName: String,
            @AppFunctionStringValueConstraint(enumValues = ["WIFI", "MOBILE", "BOTH"])
            network: String,
            blocked: Boolean,
        ): AppFirewallStatus =
            withContext(Dispatchers.IO) {
                val rule = findRule(packageName)
                when (network) {
                    "WIFI" -> rule.wifi_blocked = blocked
                    "MOBILE" -> rule.other_blocked = blocked
                    "BOTH" -> {
                        rule.wifi_blocked = blocked
                        rule.other_blocked = blocked
                    }
                    else ->
                        throw AppFunctionInvalidArgumentException(
                            "Network must be WIFI, MOBILE, or BOTH.",
                        )
                }
                persistRule(rule)
                AppFirewallStatus(
                    packageName = rule.packageName.orEmpty(),
                    displayName = rule.name ?: rule.packageName.orEmpty(),
                    wifiBlocked = rule.wifi_blocked,
                    mobileBlocked = rule.other_blocked,
                    lockdownAllowed = rule.lockdown,
                )
            }

        /**
         * Returns the most recent blocked connection attempts recorded by the firewall.
         *
         * @param appFunctionContext The execution context.
         * @param limit Maximum number of entries to return. Defaults to 5.
         */
        @AppFunction(isDescribedByKDoc = true)
        suspend fun getRecentBlockedConnections(
            appFunctionContext: AppFunctionContext,
            limit: Int = 5,
        ): List<BlockedConnection> =
            withContext(Dispatchers.IO) {
                if (limit <= 0 || limit > 20) {
                    throw AppFunctionInvalidArgumentException("Limit must be between 1 and 20.")
                }
                val entries = mutableListOf<BlockedConnection>()
                DatabaseHelper.getInstance(context)
                    .getLog(udp = true, tcp = true, other = true, allowed = false, blocked = true, limit = limit)
                    .use { cursor ->
                        val colUid = cursor.getColumnIndex("uid")
                        val colProtocol = cursor.getColumnIndex("protocol")
                        val colDAddr = cursor.getColumnIndex("daddr")
                        val colDPort = cursor.getColumnIndex("dport")
                        val colDName = cursor.getColumnIndex("dname")
                        while (cursor.moveToNext()) {
                            val uid = if (cursor.isNull(colUid)) -1 else cursor.getInt(colUid)
                            val protocol =
                                if (cursor.isNull(colProtocol)) {
                                    "unknown"
                                } else {
                                    protocolName(cursor.getInt(colProtocol))
                                }
                            entries +=
                                BlockedConnection(
                                    appName = Util.getApplicationNames(uid, context).joinToString(", "),
                                    destination =
                                        cursor.getString(colDAddr)?.let { address ->
                                            val port =
                                                if (cursor.isNull(colDPort)) {
                                                    null
                                                } else {
                                                    cursor.getInt(colDPort)
                                                }
                                            if (port != null && port > 0) "$address:$port" else address
                                        },
                                    hostName =
                                        if (cursor.isNull(colDName)) {
                                            null
                                        } else {
                                            cursor.getString(colDName)
                                        },
                                    protocol = protocol,
                                )
                        }
                    }
                entries
            }

        private fun findRule(packageName: String): Rule {
            val normalized = packageName.trim()
            if (normalized.isBlank()) {
                throw AppFunctionInvalidArgumentException("Package name cannot be blank.")
            }
            return Rule.getRules(true, context)
                .firstOrNull { it.packageName.equals(normalized, ignoreCase = true) }
                ?: throw AppFunctionElementNotFoundException(
                    "No manageable app found for package '$packageName'.",
                )
        }

        private fun persistRule(rule: Rule) {
            val packageName = rule.packageName ?: return
            val wifiKey = PreferencesRepository.namespaced("wifi", packageName)
            val otherKey = PreferencesRepository.namespaced("other", packageName)
            if (rule.wifi_blocked == rule.wifi_default) {
                context.preferences().remove(wifiKey)
            } else {
                context.preferences().putBoolean(wifiKey, rule.wifi_blocked)
            }
            if (rule.other_blocked == rule.other_default) {
                context.preferences().remove(otherKey)
            } else {
                context.preferences().putBoolean(otherKey, rule.other_blocked)
            }
            rule.updateChanged(context)
            ServiceSinkhole.reload("appfunction rule", context, false)
            Widgets.updateFirewall(context)
        }

        private fun protocolName(protocol: Int): String =
            when (protocol) {
                6 -> "TCP"
                17 -> "UDP"
                else -> protocol.toString()
            }

        /** Current global firewall state. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class FirewallStatus(
            /** Whether the NetGuard VPN firewall is active. */
            val enabled: Boolean,
            /** Whether global lockdown mode is active. */
            val lockdown: Boolean,
            /** Number of apps with at least one blocked network path. */
            val blockedAppCount: Int,
        )

        /** Result of enabling or disabling the firewall. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class FirewallToggleResult(
            /** Whether the firewall is active after this operation. */
            val enabled: Boolean,
            /** Human-readable status for the caller. */
            val message: String,
            /** Intent to launch when user confirmation is required, such as VPN approval. */
            val confirmationIntent: PendingIntent? = null,
        )

        /** Result of enabling or disabling lockdown mode. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class LockdownResult(
            /** Whether lockdown mode is active after this operation. */
            val lockdown: Boolean,
            /** Human-readable status for the caller. */
            val message: String,
        )

        /** App entry returned by [searchApps]. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class AppSearchResult(
            /** Android package name used by other app functions. */
            val packageName: String,
            /** Human-readable app label. */
            val displayName: String,
            /** Whether Wi-Fi access is currently blocked. */
            val wifiBlocked: Boolean,
            /** Whether mobile data access is currently blocked. */
            val mobileBlocked: Boolean,
        )

        /** Per-app firewall state. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class AppFirewallStatus(
            /** Android package name of the app. */
            val packageName: String,
            /** Human-readable app label. */
            val displayName: String,
            /** Whether Wi-Fi access is blocked. */
            val wifiBlocked: Boolean,
            /** Whether mobile data access is blocked. */
            val mobileBlocked: Boolean,
            /** Whether the app is explicitly allowed during lockdown. */
            val lockdownAllowed: Boolean,
        )

        /** A blocked connection from the traffic log. */
        @AppFunctionSerializable(isDescribedByKDoc = true)
        data class BlockedConnection(
            /** Name of the app that attempted the connection. */
            val appName: String,
            /** Destination IP address and optional port. */
            val destination: String?,
            /** Resolved hostname when available. */
            val hostName: String?,
            /** Transport protocol, such as TCP or UDP. */
            val protocol: String,
        )
    }