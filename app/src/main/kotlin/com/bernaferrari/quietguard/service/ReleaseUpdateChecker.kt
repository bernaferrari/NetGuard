package com.bernaferrari.quietguard.service

import com.bernaferrari.quietguard.Version

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal enum class UpdateCheckStatus {
    available,
    upToDate,
    failed,
    unavailable,
}

internal data class UpdateCheckResult(
    val status: UpdateCheckStatus,
    val availableVersion: String? = null,
    val assetName: String? = null,
    val releaseUrl: String? = null,
)

/** Fetches and validates release metadata without coupling network I/O to service command handling. */
internal object ReleaseUpdateChecker {
    fun check(apiUrl: String, currentVersion: String): UpdateCheckResult {
        if (apiUrl.isBlank()) {
            Log.i(TAG, "Update check unavailable: empty API URL")
            return UpdateCheckResult(UpdateCheckStatus.unavailable)
        }

        val response = try {
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                if (connection.responseCode !in HTTP_OK..HTTP_MULTIPLE_CHOICES) {
                    Log.w(TAG, "Update check failed with HTTP ${connection.responseCode}")
                    return UpdateCheckResult(UpdateCheckStatus.failed)
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Update check request failed", error)
            return UpdateCheckResult(UpdateCheckStatus.failed)
        }

        return try {
            val release = JSONObject(response)
            val version = release.getString("tag_name")
            val releaseUrl = release.getString("html_url")
            val assets = release.getJSONArray("assets")
            if (assets.length() == 0) return UpdateCheckResult(UpdateCheckStatus.failed)

            val assetName = assets.getJSONObject(0).getString("name")
            if (Version(currentVersion) < Version(version)) {
                Log.i(TAG, "Update available from $currentVersion to $version")
                UpdateCheckResult(UpdateCheckStatus.available, version, assetName, releaseUrl)
            } else {
                Log.i(TAG, "Up-to-date current version=$currentVersion")
                UpdateCheckResult(UpdateCheckStatus.upToDate)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Update check response was invalid", error)
            UpdateCheckResult(UpdateCheckStatus.failed)
        }
    }

    private const val TAG = "NetGuard.Update"
    private const val TIMEOUT_MS = 15_000
    private const val HTTP_OK = 200
    private const val HTTP_MULTIPLE_CHOICES = 299
}
