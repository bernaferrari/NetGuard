package com.bernaferari.renetguard.domain

import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.platform.NetGuardPlatform
import org.koin.core.annotation.Single

@Single
class WasmRulesRepository(
    private val preferencesRepository: PreferencesRepository,
) : RulesRepository {
    private var cachedRules: List<FirewallRule>? = null

    override suspend fun loadRules(refresh: Boolean): List<FirewallRule> {
        if (!refresh && cachedRules != null) {
            return cachedRules!!
        }
        val demo =
            listOf(
                demoRule("com.android.chrome", "Chrome", 10101, wifiBlocked = false, otherBlocked = false),
                demoRule("com.google.android.gm", "Gmail", 10102, wifiBlocked = true, otherBlocked = false),
                demoRule("com.spotify.music", "Spotify", 10103, wifiBlocked = false, otherBlocked = true),
                demoRule("com.instagram.android", "Instagram", 10104, wifiBlocked = true, otherBlocked = true),
                demoRule("com.whatsapp", "WhatsApp", 10105, wifiBlocked = false, otherBlocked = false),
                demoRule("com.google.android.youtube", "YouTube", 10106, wifiBlocked = false, otherBlocked = false),
                demoRule("com.twitter.android", "X", 10107, wifiBlocked = true, otherBlocked = false),
                demoRule("com.facebook.katana", "Facebook", 10108, wifiBlocked = true, otherBlocked = true),
                demoRule("com.google.android.apps.maps", "Maps", 10109, wifiBlocked = false, otherBlocked = false),
                demoRule("com.amazon.mShop.android.shopping", "Amazon", 10110, wifiBlocked = false, otherBlocked = true),
                demoRule("com.netflix.mediaclient", "Netflix", 10111, wifiBlocked = false, otherBlocked = false),
                demoRule("com.discord", "Discord", 10112, wifiBlocked = true, otherBlocked = false, system = false),
                demoRule("com.android.vending", "Play Store", 10001, wifiBlocked = false, otherBlocked = false, system = true),
                demoRule("com.google.android.gms", "Google Play services", 10002, wifiBlocked = false, otherBlocked = false, system = true),
            )
        cachedRules = demo.map { applyPrefs(it) }
        return cachedRules!!
    }

    override fun persistRule(rule: FirewallRule, allRules: List<FirewallRule>) {
        val packageName = rule.packageName ?: return
        persistPref("wifi", packageName, rule.wifi_blocked, rule.wifi_default)
        persistPref("other", packageName, rule.other_blocked, rule.other_default)
        persistPref("apply", packageName, rule.apply, default = true)
        persistPref("screen_wifi", packageName, rule.screen_wifi, rule.screen_wifi_default)
        persistPref("screen_other", packageName, rule.screen_other, rule.screen_other_default)
        persistPref("roaming", packageName, rule.roaming, rule.roaming_default)
        if (rule.lockdown) {
            preferencesRepository.putBoolean(PreferencesRepository.namespaced("lockdown", packageName), true)
        } else {
            preferencesRepository.remove(PreferencesRepository.namespaced("lockdown", packageName))
        }
        if (rule.notify) {
            preferencesRepository.remove(PreferencesRepository.namespaced("notify", packageName))
        } else {
            preferencesRepository.putBoolean(PreferencesRepository.namespaced("notify", packageName), false)
        }
        NetGuardPlatform.firewall.reload("rule changed", false)
        NetGuardPlatform.widgets.updateFirewall()
        cachedRules = allRules
    }

    private fun persistPref(
        prefix: String,
        packageName: String,
        value: Boolean,
        default: Boolean,
    ) {
        val key = PreferencesRepository.namespaced(prefix, packageName)
        if (value == default) {
            preferencesRepository.remove(key)
        } else {
            preferencesRepository.putBoolean(key, value)
        }
    }

    private fun applyPrefs(rule: FirewallRule): FirewallRule {
        val packageName = rule.packageName ?: return rule
        rule.wifi_blocked =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("wifi", packageName),
                rule.wifi_default,
            )
        rule.other_blocked =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("other", packageName),
                rule.other_default,
            )
        rule.apply =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("apply", packageName),
                true,
            )
        rule.screen_wifi =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("screen_wifi", packageName),
                rule.screen_wifi_default,
            )
        rule.screen_other =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("screen_other", packageName),
                rule.screen_other_default,
            )
        rule.roaming =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("roaming", packageName),
                rule.roaming_default,
            )
        rule.lockdown =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("lockdown", packageName),
                false,
            )
        rule.notify =
            preferencesRepository.getBoolean(
                PreferencesRepository.namespaced("notify", packageName),
                true,
            )
        return rule
    }

    private fun demoRule(
        packageName: String,
        name: String,
        uid: Int,
        wifiBlocked: Boolean,
        otherBlocked: Boolean,
        system: Boolean = false,
    ): FirewallRule =
        FirewallRule(
            uid = uid,
            packageName = packageName,
            name = name,
            wifi_blocked = wifiBlocked,
            other_blocked = otherBlocked,
            wifi_default = false,
            other_default = false,
            internet = true,
            enabled = true,
            system = system,
        )
}