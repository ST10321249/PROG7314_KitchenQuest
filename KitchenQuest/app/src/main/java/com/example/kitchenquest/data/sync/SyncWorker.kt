package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kitchenquest.data.AppRepositories

// Runs in the background, only while the phone is connected, and sends whatever changes
// are waiting for the signed-in account. WorkManager runs it again later, with a longer
// wait each time, if something could not be sent.
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Nobody is signed in. The next change or sign-in asks for another sync.
        val userId = AppRepositories.currentUserId() ?: return Result.success()

        return when (AppRepositories.syncProcessor().run(userId)) {
            SyncProcessor.Outcome.DONE -> Result.success()
            SyncProcessor.Outcome.RETRY -> Result.retry()
        }
    }
}
