package io.github.arekolek.workmanagerbug

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimestampWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "TimestampWidget"
        private const val API_URL = "https://www.google.com/generate_204"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TimestampWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                TimestampWidgetProvider().onUpdate(context, appWidgetManager, ids)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate called")

        val (localTime, result) = runBlocking { fetchTimestamp() }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_timestamp)
            views.setTextViewText(R.id.tv_local_time, "Local: $localTime")
            views.setTextViewText(R.id.tv_result, result)

            val refreshIntent = Intent(context, TimestampWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val refreshPending = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private suspend fun fetchTimestamp(): Pair<String, String> {
        val result = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching from $API_URL...")
                val connection = URL(API_URL).openConnection() as HttpURLConnection
                val code = connection.responseCode
                val date = connection.getHeaderField("Date")
                connection.disconnect()
                Log.d(TAG, "Response code: $code, Date: $date")
                "HTTP $code — $date"
            } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                "Error: ${e::class.simpleName}: ${e.message}"
            }
        }

        val localTime = Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        return localTime to result
    }
}
