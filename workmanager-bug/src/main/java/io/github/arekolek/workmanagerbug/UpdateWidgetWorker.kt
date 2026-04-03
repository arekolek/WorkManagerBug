package io.github.arekolek.workmanagerbug

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class UpdateWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Log network state
        val cm = applicationContext.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        Log.d(TAG, "Worker started — " +
                "activeNetwork=$network, " +
                "internet=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}, " +
                "validated=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")

        // Retry network call within the same doWork, as recommended by Google
        for (attempt in 1..30) {
            val success = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(API_URL).openConnection() as HttpURLConnection
                    val code = connection.responseCode
                    connection.disconnect()
                    Log.d(TAG, "Worker attempt #$attempt — OK (HTTP $code)")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Worker attempt #$attempt — FAILED: ${e::class.simpleName}: ${e.message}")
                    false
                }
            }

            if (success) {
                Log.d(TAG, "Worker — network available after $attempt attempt(s), updating widget")
                TimestampWidgetProvider.updateAllWidgets(applicationContext)
                return Result.success()
            }

            delay(2_000)
        }

        Log.e(TAG, "Worker — gave up after 30 attempts (60s), network never became available")
        return Result.failure()
    }

    companion object {
        private const val TAG = "UpdateWidgetWorker"
        private const val API_URL = "https://www.google.com/generate_204"

        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling worker in 60s with network constraints")
            WorkManager.getInstance(context)
                .enqueue(
                    OneTimeWorkRequestBuilder<UpdateWidgetWorker>()
                        .setInitialDelay(60, TimeUnit.SECONDS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
        }
    }
}
