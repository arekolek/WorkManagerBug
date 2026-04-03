package io.github.arekolek.workmanagerbug

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainScreen(
                    onPinWidget = {
                        val awm = AppWidgetManager.getInstance(applicationContext)
                        if (awm.isRequestPinAppWidgetSupported) {
                            awm.requestPinAppWidget(
                                ComponentName(applicationContext, TimestampWidgetProvider::class.java),
                                null, null,
                            )
                        }
                    },
                    onAlarmManager = {
                        UpdateWidgetReceiver.schedule(applicationContext)
                        finish()
                    },
                    onWorkManager = {
                        UpdateWidgetWorker.schedule(applicationContext)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onPinWidget: () -> Unit = {},
    onAlarmManager: () -> Unit = {},
    onWorkManager: () -> Unit = {},
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
            Text("1. Pin widget to home screen", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPinWidget) { Text("Pin widget") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("2. Schedule update in ${SCHEDULE_DELAY_SECONDS}s and wait on the launcher", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAlarmManager) { Text("Update via AlarmManager") }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onWorkManager) { Text("Update via WorkManager") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen()
    }
}
