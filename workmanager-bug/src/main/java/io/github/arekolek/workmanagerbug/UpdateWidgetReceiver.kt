package io.github.arekolek.workmanagerbug

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class UpdateWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        try {
            // Log network state
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val network = cm.activeNetwork
            val caps = network?.let { cm.getNetworkCapabilities(it) }
            Log.d(TAG, "Alarm fired — " +
                    "activeNetwork=$network, " +
                    "internet=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}, " +
                    "validated=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")

            // Retry network call within the same onReceive (using goAsync)
            runBlocking {
                for (attempt in 1..30) {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            val connection = URL(API_URL).openConnection() as HttpURLConnection
                            val code = connection.responseCode
                            connection.disconnect()
                            Log.d(TAG, "Alarm attempt #$attempt — OK (HTTP $code)")
                            true
                        } catch (e: Exception) {
                            Log.e(TAG, "Alarm attempt #$attempt — FAILED: ${e::class.simpleName}: ${e.message}")
                            false
                        }
                    }

                    if (success) {
                        Log.d(TAG, "Alarm — network available after $attempt attempt(s), updating widget")
                        TimestampWidgetProvider.updateAllWidgets(context)
                        return@runBlocking
                    }

                    delay(2_000)
                }
                Log.e(TAG, "Alarm — gave up after 30 attempts (60s), network never became available")
            }
        } finally {
            pendingResult.finish()
        }
    }

    companion object {
        private const val TAG = "UpdateWidgetReceiver"
        private const val API_URL = "https://www.google.com/generate_204"

        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling alarm in 60s")
            val intent = Intent(context, UpdateWidgetReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60_000,
                pending,
            )
        }
    }
}
