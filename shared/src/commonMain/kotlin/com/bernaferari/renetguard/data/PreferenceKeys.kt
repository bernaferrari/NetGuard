package com.bernaferari.renetguard.data

object PreferenceKeys {
    const val ENABLED = "enabled"
    const val FILTER = "filter"
    const val LOCKDOWN = "lockdown"
    const val SHOW_STATS = "show_stats"
    const val APPEARANCE = "appearance"
    const val THEME = "theme"
    const val AUTO_ENABLE = "auto_enable"
    const val LOG = "log"

    private val PER_APP_RULE_PREFIXES =
        listOf(
            "wifi",
            "other",
            "apply",
            "screen_wifi",
            "screen_other",
            "roaming",
            "lockdown",
            "notify",
            "unused",
        )

    fun isPerAppRuleKey(key: String): Boolean =
        PER_APP_RULE_PREFIXES.any { prefix ->
            key.startsWith("${prefix}_") &&
                key.substring(prefix.length + 1).contains('.')
        }
}