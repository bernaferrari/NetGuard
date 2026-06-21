package com.bernaferari.renetguard.domain

import android.app.NotificationManager
import android.content.Context
import com.bernaferari.renetguard.Rule
import com.bernaferari.renetguard.data.PreferencesRepository
import com.bernaferari.renetguard.platform.NetGuardPlatform
import org.koin.core.annotation.Single

@Single
class AndroidRulesRepository(
    private val preferencesRepository: PreferencesRepository,
    private val context: Context,
) : RulesRepository {
    override suspend fun loadRules(refresh: Boolean): List<FirewallRule> =
        Rule.getRules(refresh, context).map { it.toFirewallRule() }

    override fun persistRule(rule: FirewallRule, allRules: List<FirewallRule>) {
        persistRuleInternal(rule, allRules, mutableSetOf())
    }

    private fun persistRuleInternal(
        rule: FirewallRule,
        allRules: List<FirewallRule>,
        visited: MutableSet<String>,
    ) {
        val packageName = rule.packageName ?: return
        if (!visited.add(packageName)) return

        if (rule.wifi_blocked == rule.wifi_default) {
            preferencesRepository.remove(PreferencesRepository.namespaced("wifi", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("wifi", packageName),
                rule.wifi_blocked,
            )
        }
        if (rule.other_blocked == rule.other_default) {
            preferencesRepository.remove(PreferencesRepository.namespaced("other", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("other", packageName),
                rule.other_blocked,
            )
        }
        if (rule.apply) {
            preferencesRepository.remove(PreferencesRepository.namespaced("apply", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("apply", packageName),
                rule.apply,
            )
        }
        if (rule.screen_wifi == rule.screen_wifi_default) {
            preferencesRepository.remove(PreferencesRepository.namespaced("screen_wifi", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("screen_wifi", packageName),
                rule.screen_wifi,
            )
        }
        if (rule.screen_other == rule.screen_other_default) {
            preferencesRepository.remove(PreferencesRepository.namespaced("screen_other", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("screen_other", packageName),
                rule.screen_other,
            )
        }
        if (rule.roaming == rule.roaming_default) {
            preferencesRepository.remove(PreferencesRepository.namespaced("roaming", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("roaming", packageName),
                rule.roaming,
            )
        }
        if (rule.lockdown) {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("lockdown", packageName),
                rule.lockdown,
            )
        } else {
            preferencesRepository.remove(PreferencesRepository.namespaced("lockdown", packageName))
        }
        if (rule.notify) {
            preferencesRepository.remove(PreferencesRepository.namespaced("notify", packageName))
        } else {
            preferencesRepository.putBoolean(
                PreferencesRepository.namespaced("notify", packageName),
                rule.notify,
            )
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(rule.uid)
        NetGuardPlatform.firewall.reload("rule changed", false)
        NetGuardPlatform.widgets.updateFirewall()

        rule.related.forEach { relatedPkg ->
            val related = allRules.firstOrNull { it.packageName == relatedPkg } ?: return@forEach
            related.wifi_blocked = rule.wifi_blocked
            related.other_blocked = rule.other_blocked
            related.apply = rule.apply
            related.screen_wifi = rule.screen_wifi
            related.screen_other = rule.screen_other
            related.roaming = rule.roaming
            related.lockdown = rule.lockdown
            related.notify = rule.notify
            persistRuleInternal(related, allRules, visited)
        }
    }
}

private fun Rule.toFirewallRule(): FirewallRule =
    FirewallRule(
        uid = uid,
        packageName = packageName,
        name = name,
        icon = icon,
        wifi_blocked = wifi_blocked,
        other_blocked = other_blocked,
        wifi_default = wifi_default,
        other_default = other_default,
        screen_wifi = screen_wifi,
        screen_other = screen_other,
        screen_wifi_default = screen_wifi_default,
        screen_other_default = screen_other_default,
        roaming = roaming,
        roaming_default = roaming_default,
        lockdown = lockdown,
        notify = notify,
        apply = apply,
        system = system,
        internet = internet,
        enabled = enabled,
        related = related?.toList() ?: emptyList(),
    )