package io.github.arekolek.workmanagerbug

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.runBlocking

class UpdateWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        try {
            logNetworkState(context.getSystemService(ConnectivityManager::class.java), "Alarm")

            val result = runBlocking { fetchWithRetry("Alarm") }
            TimestampWidgetProvider.updateAllWidgets(context, result)
        } finally {
            pendingResult.finish()
        }
    }

    companion object {
        fun schedule(context: Context) {
            Log.d("UpdateWidgetReceiver", "Scheduling alarm in 60s")
            val intent = Intent(context, UpdateWidgetReceiver::class.java)
            val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60_000, pending)
        }
    }
}
