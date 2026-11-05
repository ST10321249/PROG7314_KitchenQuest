package com.example.kitchenquest.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface TimerAlarms {
    fun schedule(timerId: String, label: String, triggerAtMillis: Long)
    fun cancel(timerId: String)
}

object NoOpTimerAlarms : TimerAlarms {
    override fun schedule(timerId: String, label: String, triggerAtMillis: Long) = Unit
    override fun cancel(timerId: String) = Unit
}

object NotificationPermissionRequests {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    fun request() {
        _requests.tryEmit(Unit)
    }
}

class AndroidTimerAlarms(context: Context) : TimerAlarms {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(timerId: String, label: String, triggerAtMillis: Long) {
        if (needsNotificationPermission()) NotificationPermissionRequests.request()

        val operation = pendingIntent(timerId, label)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
            Log.d(TAG, "Scheduled alarm for timer $timerId")
        } catch (error: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            Log.w(TAG, "Exact alarms not allowed, using an inexact alarm for timer $timerId", error)
        }
    }

    override fun cancel(timerId: String) {
        alarmManager.cancel(pendingIntent(timerId, ""))
        Log.d(TAG, "Cancelled alarm for timer $timerId")
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(timerId: String, label: String): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            timerId.hashCode(),
            Intent(appContext, TimerAlarmReceiver::class.java).apply {
                action = ACTION_TIMER_FINISHED
                data = Uri.parse("kitchenquest://timer/$timerId")
                putExtra(EXTRA_TIMER_ID, timerId)
                putExtra(EXTRA_TIMER_LABEL, label)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_TIMER_FINISHED = "com.example.kitchenquest.TIMER_FINISHED"
        const val EXTRA_TIMER_ID = "kitchenquest_timer_id"
        const val EXTRA_TIMER_LABEL = "kitchenquest_timer_label"
        private const val TAG = "TimerAlarms"
    }
}