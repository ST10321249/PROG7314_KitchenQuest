package com.example.kitchenquest.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ExpiryAlertScheduler {

    const val WORK_NAME = "kitchenquest-expiry-alerts"

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

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    // Called when the app starts, so the daily check carries on for an account that has alerts on.
    fun scheduleIfEnabled(context: Context, userId: String?) {
        if (userId != null && ExpiryAlertSettings(context).isEnabled(userId)) schedule(context)
    }
}