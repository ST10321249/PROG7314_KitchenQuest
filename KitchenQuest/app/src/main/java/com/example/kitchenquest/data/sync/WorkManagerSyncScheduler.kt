package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerSyncScheduler(context: Context) : SyncScheduler {

    private val appContext = context.applicationContext

    // Only one sync runs at a time. A request made while one is running starts another
    // straight after it, so a change made at the wrong moment is never left behind.
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                SyncPolicy.INITIAL_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    companion object {
        const val WORK_NAME = "kitchenquest-sync"
    }
}
