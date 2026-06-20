package com.bernaferari.renetguard.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceKeysTest {
    @Test
    fun perAppRuleKeyMatchesPackageName() {
        assertTrue(PreferenceKeys.isPerAppRuleKey("wifi_com.example.app"))
        assertTrue(PreferenceKeys.isPerAppRuleKey("other_com.example.app"))
    }

    @Test
    fun globalKeysAreNotPerAppRuleKeys() {
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.ENABLED))
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.FILTER))
        assertFalse(PreferenceKeys.isPerAppRuleKey(PreferenceKeys.LOCKDOWN))
    }

    @Test
    fun prefixedKeysWithoutPackageAreNotPerAppRuleKeys() {
        assertFalse(PreferenceKeys.isPerAppRuleKey("wifi_home"))
        assertFalse(PreferenceKeys.isPerAppRuleKey("enabled"))
    }
}