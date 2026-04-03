package io.github.arekolek.glancebackgroundbug

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Context.widgetDataStore by preferencesDataStore("TimestampWidget")

private val ResultKey = stringPreferencesKey("result")
private val TimeKey = stringPreferencesKey("localTime")

class TimestampWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "TimestampWidget"
        private const val API_URL = "https://www.google.com/generate_204"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "provideGlance called")
        val store = context.widgetDataStore

        fetchAndStore(store)
        val initial = store.data.first()

        provideContent {
            val data by store.data.collectAsState(initial)
            val scope = rememberCoroutineScope()

            TimestampWidgetContent(
                localTime = data[TimeKey] ?: "—",
                result = data[ResultKey] ?: "—",
                onRefresh = {
                    scope.launch { fetchAndStore(store) }
                },
            )
        }
    }

    private suspend fun fetchAndStore(store: DataStore<Preferences>) {
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

        store.edit {
            it[ResultKey] = result
            it[TimeKey] = localTime
        }
    }
}

@Composable
fun TimestampWidgetContent(
    localTime: String,
    result: String,
    onRefresh: (() -> Unit)? = null,
) {
    GlanceTheme {
        Scaffold(
            backgroundColor = GlanceTheme.colors.widgetBackground,
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(R.drawable.ic_launcher_foreground),
                    title = "Timestamp",
                    iconColor = GlanceTheme.colors.primary,
                    textColor = GlanceTheme.colors.onSurface,
                    actions = {
                        if (onRefresh != null) {
                            CircleIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_refresh),
                                contentDescription = "Refresh",
                                contentColor = GlanceTheme.colors.secondary,
                                backgroundColor = null,
                                onClick = onRefresh,
                            )
                        }
                    },
                )
            },
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "Local: $localTime",
                    style = TextStyle(color = GlanceTheme.colors.secondary),
                )
                Text(
                    text = result,
                    style = TextStyle(color = GlanceTheme.colors.onSurface),
                )
            }
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 200)
@Composable
fun TimestampWidgetPreview() {
    TimestampWidgetContent(
        localTime = "12:34:56",
        result = "HTTP 204 — Tue, 31 Mar 2026 14:09:31 GMT",
    )
}
