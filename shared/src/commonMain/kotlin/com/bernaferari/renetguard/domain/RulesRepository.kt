package com.bernaferari.renetguard.domain

interface RulesRepository {
    suspend fun loadRules(refresh: Boolean): List<FirewallRule>

    fun persistRule(rule: FirewallRule, allRules: List<FirewallRule>)
}