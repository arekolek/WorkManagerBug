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
        logNetworkState(applicationContext.getSystemService(ConnectivityManager::class.java), "Worker")

        val result = fetchWithRetry("Worker")
        TimestampWidgetProvider.updateAllWidgets(applicationContext, result)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            Log.d("UpdateWidgetWorker", "Scheduling worker in 60s with network constraints")
            WorkManager.getInstance(context)
                .enqueue(
                    OneTimeWorkRequestBuilder<UpdateWidgetWorker>()
                        .setInitialDelay(60, TimeUnit.SECONDS)
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
