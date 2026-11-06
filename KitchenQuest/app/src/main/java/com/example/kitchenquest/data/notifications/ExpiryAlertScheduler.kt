package com.example.kitchenquest.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ExpiryAlertScheduler {

    const val WORK_NAME = "kitchenquest-expiry-alerts"
    const val ON_DEMAND_WORK_NAME = "kitchenquest-expiry-check-now"

    // One daily check, whatever the number of times this is called.
    fun schedule(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val request = PeriodicWorkRequestBuilder<ExpiryAlertWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(ExpiryAlertRules.minutesUntilNextAlert(now), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // A new or updated ingredient should not wait until the next daily check.
    // The worker reads the signed-in user's local Room pantry, including offline edits.
    fun checkNow(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            ON_DEMAND_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ExpiryAlertWorker>().build()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(ON_DEMAND_WORK_NAME)
    }

    // Called when the app starts, so the daily check carries on for an account that has alerts on.
    fun scheduleIfEnabled(context: Context, userId: String?) {
        if (userId != null && ExpiryAlertSettings(context).isEnabled(userId)) schedule(context)
    }
}