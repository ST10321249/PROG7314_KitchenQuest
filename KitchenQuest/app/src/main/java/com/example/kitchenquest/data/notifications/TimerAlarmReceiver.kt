package com.example.kitchenquest.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kitchenquest.MainActivity
import com.example.kitchenquest.R
import com.example.kitchenquest.data.preferences.AppLanguagePreferences

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidTimerAlarms.ACTION_TIMER_FINISHED) return
        val timerId = intent.getStringExtra(AndroidTimerAlarms.EXTRA_TIMER_ID) ?: return
        val label = intent.getStringExtra(AndroidTimerAlarms.EXTRA_TIMER_LABEL).orEmpty()
        Log.d(TAG, "Timer $timerId finished")
        TimerAlertNotifier(context).post(timerId, label)
    }

    private companion object {
        const val TAG = "TimerAlarmReceiver"
    }
}

class TimerAlertNotifier(private val context: Context) {

    fun canPost(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun post(timerId: String, label: String): Boolean {
        if (!canPost()) {
            Log.w(TAG, "Notifications are off, timer $timerId finished silently")
            return false
        }

        NotificationChannels.createAll(context)
        val text = AppLanguagePreferences.localizedContext(context)
        val name = label.ifBlank { text.getString(R.string.timer_alert_default_label) }
        val message = text.getString(R.string.timer_alert_text, name)

        val open = PendingIntent.getActivity(
            context,
            timerId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.TIMER_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(text.getString(R.string.timer_alert_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(timerId.hashCode(), notification)
        return true
    }

    private companion object {
        const val TAG = "TimerAlertNotifier"
    }
}