package io.github.arekolek.glancebackgroundbug

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.arekolek.glancebackgroundbug.ui.theme.GlanceBackgroundBugTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlanceBackgroundBugTheme {
                MainScreen(
                    onPinGlanceWidget = {
                        val awm = AppWidgetManager.getInstance(applicationContext)
                        if (awm.isRequestPinAppWidgetSupported) {
                            awm.requestPinAppWidget(
                                ComponentName(applicationContext, TimestampWidgetReceiver::class.java),
                                null, null,
                            )
                        }
                    },
                    onPinRvWidget = {
                        val awm = AppWidgetManager.getInstance(applicationContext)
                        if (awm.isRequestPinAppWidgetSupported) {
                            awm.requestPinAppWidget(
                                ComponentName(applicationContext, TimestampRvWidgetProvider::class.java),
                                null, null,
                            )
                        }
                    },
                    onGlanceViaAlarm = {
                        UpdateWidgetReceiver.schedule(applicationContext, UpdateWidgetReceiver.TARGET_GLANCE)
                        finish()
                    },
                    onGlanceViaWorker = {
                        UpdateWidgetWorker.schedule(applicationContext, UpdateWidgetWorker.TARGET_GLANCE)
                        finish()
                    },
                    onRvViaAlarm = {
                        UpdateWidgetReceiver.schedule(applicationContext, UpdateWidgetReceiver.TARGET_RV)
                        finish()
                    },
                    onRvViaWorker = {
                        UpdateWidgetWorker.schedule(applicationContext, UpdateWidgetWorker.TARGET_RV)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onPinGlanceWidget: () -> Unit = {},
    onPinRvWidget: () -> Unit = {},
    onGlanceViaAlarm: () -> Unit = {},
    onGlanceViaWorker: () -> Unit = {},
    onRvViaAlarm: () -> Unit = {},
    onRvViaWorker: () -> Unit = {},
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("1. Pin widgets to home screen", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPinGlanceWidget) { Text("Pin Glance widget") }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onPinRvWidget) { Text("Pin RemoteViews widget") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("2. Glance widget — schedule update in 60s", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = onGlanceViaAlarm) { Text("Glance via AlarmManager") }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onGlanceViaWorker) { Text("Glance via WorkManager") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("3. RemoteViews widget — schedule update in 60s", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRvViaAlarm) { Text("RV via AlarmManager") }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRvViaWorker) { Text("RV via WorkManager") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    GlanceBackgroundBugTheme {
        MainScreen()
    }
}
