package com.bernaferrari.quietguard.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PreferencesRepositoryTest {
    @Test
    fun namespacedKeyUsesPrefix() {
        assertEquals("wifi_42", PreferencesRepository.namespaced("wifi", "42"))
        assertEquals("enabled", PreferencesRepository.namespaced("", "enabled"))
    }

    @Test
    fun uidKeyUsesIntString() {
        assertEquals("notify_1000", PreferencesRepository.uidKey("notify", 1000))
    }

    @Test
    fun gettersReflectWrittenValues() {
        runBlocking {
            val repository = createRepository()
            repository.putBoolean("filter", true)
            repository.putInt("delay", 5)
            repository.putString("theme", "blue")
            repository.putStringSet("wifi_home", setOf("home", "office"))
            repository.setEnabled(true)

            withTimeout(2_000) {
                repository.data.first { prefs ->
                    prefs[booleanPreferencesKey("filter")] == true &&
                        prefs[intPreferencesKey("delay")] == 5 &&
                        prefs[stringPreferencesKey("theme")] == "blue" &&
                        prefs[stringSetPreferencesKey("wifi_home")] == setOf("home", "office") &&
                        prefs[booleanPreferencesKey("enabled")] == true
                }
            }

            assertTrue(repository.getBoolean("filter"))
            assertEquals(5, repository.getInt("delay"))
            assertEquals("blue", repository.getString("theme"))
            assertEquals(setOf("home", "office"), repository.getStringSet("wifi_home"))
            assertTrue(repository.getBoolean("enabled"))
        }
    }

    @Test
    fun loadedStateChangesAfterFirstDataStoreEmission() {
        runBlocking {
            val repository = createRepository()

            withTimeout(2_000) {
                repository.isLoaded.first { it }
            }

            assertTrue(repository.isLoaded.value)
        }
    }

    @Test
    fun changesEmitsUpdatedKeyNames() {
        runBlocking {
            val repository = createRepository()
            repository.putString("theme", "teal")

            withTimeout(5_000) {
                repository.changes.first { it.contains("theme") }
            }

            repository.putString("theme", "blue")

            withTimeout(5_000) {
                repository.changes.first { it.contains("theme") }
            }
        }
    }

    @Test
    fun changedKeysDetectsAddUpdateAndRemove() {
        val repository = createRepository()
        val empty = emptyPreferences()
        val withTheme =
            mutablePreferencesOf(
                stringPreferencesKey("theme") to "teal",
            )
        val updated =
            mutablePreferencesOf(
                stringPreferencesKey("theme") to "blue",
            )

        assertEquals(setOf("theme"), repository.changedKeys(empty, withTheme))
        assertEquals(setOf("theme"), repository.changedKeys(withTheme, updated))
        assertEquals(setOf("theme"), repository.changedKeys(withTheme, empty))
    }

    @Test
    fun keysWithPrefixReturnsMatchingNames() {
        runBlocking {
            val repository = createRepository()
            repository.putBoolean("wifi_com.example", true)
            repository.putBoolean("wifi_com.other", false)
            repository.putBoolean("other_com.example", true)

            withTimeout(2_000) {
                repository.data.first { prefs ->
                    prefs.asMap().keys.count { it.name.startsWith("wifi_") } == 2
                }
            }

            assertEquals(
                setOf("wifi_com.example", "wifi_com.other"),
                repository.keysWithPrefix("wifi"),
            )
        }
    }

    @Test
    fun typedRemovalHandlesStoredAndMissingPreferences() {
        runBlocking {
            val repository = createRepository()
            repository.putBoolean("lockdown_app", true)
            repository.putString("dark_theme", "dark")

            withTimeout(2_000) {
                repository.data.first { prefs ->
                    prefs[booleanPreferencesKey("lockdown_app")] == true &&
                        prefs[stringPreferencesKey("dark_theme")] == "dark"
                }
            }

            repository.removeBoolean("lockdown_app")
            repository.removeString("dark_theme")
            repository.removeBoolean("missing_boolean")

            withTimeout(2_000) {
                repository.data.first { prefs ->
                    booleanPreferencesKey("lockdown_app") !in prefs &&
                        stringPreferencesKey("dark_theme") !in prefs
                }
            }

            assertFalse(repository.getBoolean("lockdown_app"))
            assertEquals(null, repository.getString("dark_theme"))
        }
    }

    private fun createRepository(): PreferencesRepository {
        val file = File.createTempFile("prefs", ".preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = { file },
            )
        return PreferencesRepository(dataStore)
    }
}
