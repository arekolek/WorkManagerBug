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
import java.util.concurrent.TimeUnit

class UpdateWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val manager = applicationContext.getSystemService(ConnectivityManager::class.java)
        val result = fetchWithRetry("Worker", { networkStateString(manager) })
        TimestampWidgetProvider.updateAllWidgets(applicationContext, result)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            Log.d("UpdateWidgetWorker", "Scheduling worker in ${SCHEDULE_DELAY_SECONDS}s with network constraints")
            WorkManager.getInstance(context)
                .enqueue(
                    OneTimeWorkRequestBuilder<UpdateWidgetWorker>()
                        .setInitialDelay(SCHEDULE_DELAY_SECONDS, TimeUnit.SECONDS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkRequest(
                                    NetworkRequest.Builder()
                                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                                        .build(),
                                    NetworkType.CONNECTED,
                                )
//                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
        }
    }
}
