package io.github.arekolek.workmanagerbug

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

const val SCHEDULE_DELAY_SECONDS = 2 * 60L

private const val TAG = "NetworkFetch"
private const val API_URL = "https://www.google.com/generate_204"

fun networkStateString(manager: ConnectivityManager): String {
    val network = manager.activeNetwork
    val capabilities = manager.getNetworkCapabilities(network)
    return "activeNetwork=$network, " +
            "internet=${capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}, " +
            "validated=${capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}"
}

/**
 * Attempts to fetch [API_URL] up to [maxAttempts] times with [delayMs] between retries.
 * Returns the result string on success, or an error string if all attempts fail.
 */
suspend fun fetchWithRetry(label: String, networkStateInfo: () -> String, maxAttempts: Int = 30, delayMs: Long = 2_000): String {
    for (attempt in 1..maxAttempts) {
        val extra = " (${networkStateInfo()})"
        val result = withContext(Dispatchers.IO) {
            try {
                val connection = URL(API_URL).openConnection() as HttpURLConnection
                val code = connection.responseCode
                val date = connection.getHeaderField("Date")
                connection.disconnect()
                Log.d(TAG, "$label attempt #$attempt$extra — OK (HTTP $code)")
                "HTTP $code — $date"
            } catch (e: Exception) {
                Log.e(TAG, "$label attempt #$attempt$extra — FAILED: ${e::class.simpleName}: ${e.message}")
                null
            }
        }

        if (result != null) {
            Log.d(TAG, "$label — network available after $attempt attempt(s)")
            return result
        }

        if (attempt < maxAttempts) delay(delayMs)
    }

    Log.e(TAG, "$label — gave up after $maxAttempts attempts, network never became available")
    return "Error: network unavailable after $maxAttempts attempts"
}
