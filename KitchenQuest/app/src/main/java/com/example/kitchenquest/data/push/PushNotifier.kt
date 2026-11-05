package com.example.kitchenquest.data.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
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
import com.example.kitchenquest.notifications.NotificationChannels

class PushNotifier(private val context: Context) {

    fun canPost(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(title: String?, body: String?, data: Map<String, String>): Boolean {
        if (!canPost()) {
            Log.w(TAG, "Push message received but notifications are off")
            return false
        }

        NotificationChannels.createAll(context)
        val text = AppLanguagePreferences.localizedContext(context)
        val shownTitle = title?.takeIf { it.isNotBlank() } ?: text.getString(R.string.push_default_title)
        val route = data[NotificationChannels.DATA_ROUTE]
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val open = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                route?.let { putExtra(NotificationChannels.DATA_ROUTE, it) }
                if (route == NotificationChannels.ROUTE_MY_KITCHEN) {
                    putExtra(NotificationChannels.EXTRA_OPEN_MY_KITCHEN, true)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.PUSH_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_push)
            .setContentTitle(shownTitle)
            .setContentText(body.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.orEmpty()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Showed push notification of type ${data["type"] ?: "unknown"}")
        return true
    }

    private companion object {
        const val TAG = "PushNotifier"
    }
}