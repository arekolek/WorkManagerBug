package io.github.arekolek.workmanagerbug

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.widget.RemoteViews
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimestampWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runBlocking {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val result = fetchWithRetry("onUpdate", { networkStateString(manager) }, maxAttempts = 1)
            updateWidgets(context, appWidgetManager, appWidgetIds, result)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context, result: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TimestampWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids, result)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            result: String,
        ) {
            val localTime = Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

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
    }
}
