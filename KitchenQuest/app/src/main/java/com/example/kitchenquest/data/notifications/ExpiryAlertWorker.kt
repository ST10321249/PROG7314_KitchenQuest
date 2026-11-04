package com.example.kitchenquest.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.local.KitchenQuestDatabase

// Runs once a day. The notifier decides whether there is anything to say.
class ExpiryAlertWorker(
    context: Context,
    params: WorkerParameters,
    private val notifierFor: (Context) -> ExpiryAlertNotifier
) : CoroutineWorker(context, params) {

    // The constructor WorkManager uses.
    constructor(context: Context, params: WorkerParameters) :
            this(context, params, { defaultNotifier(it) })

    override suspend fun doWork(): Result {
        notifierFor(applicationContext).run()
        return Result.success()
    }
}

private fun defaultNotifier(context: Context) = ExpiryAlertNotifier(
    context = context,
    database = KitchenQuestDatabase.getInstance(context),
    currentUserId = AppRepositories::currentUserId
)