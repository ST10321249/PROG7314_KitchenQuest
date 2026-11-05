package com.example.kitchenquest.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kitchenquest.R
import com.example.kitchenquest.data.preferences.AppLanguagePreferences

// Every notification channel lives here. Push notifications should reuse these ids
// and add their own constants here rather than creating channels elsewhere.
object NotificationChannels {

    const val EXPIRY_ALERTS = "kitchenquest_expiry_alerts"

    const val TIMER_ALERTS = "kitchenquest_timer_alerts"

    const val PUSH_UPDATES = "kitchenquest_push"

    const val DATA_ROUTE = "route"

    const val ROUTE_MY_KITCHEN = "my_kitchen"

    // Set on the intent of a notification that should open My Kitchen when it is tapped.
    const val EXTRA_OPEN_MY_KITCHEN = "kitchenquest_open_my_kitchen"

    // Safe to call again. It also refreshes the names if the app language changed.
    fun createAll(context: Context) {
        val text = AppLanguagePreferences.localizedContext(context)
        val expiry = NotificationChannelCompat.Builder(
            EXPIRY_ALERTS,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(text.getString(R.string.expiry_alert_channel_name))
            .setDescription(text.getString(R.string.expiry_alert_channel_description))
            .build()

        val timers = NotificationChannelCompat.Builder(
            TIMER_ALERTS,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(text.getString(R.string.timer_alert_channel_name))
            .setDescription(text.getString(R.string.timer_alert_channel_description))
            .build()

        val push = NotificationChannelCompat.Builder(
            PUSH_UPDATES,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(text.getString(R.string.push_channel_name))
            .setDescription(text.getString(R.string.push_channel_description))
            .build()

        NotificationManagerCompat.from(context).createNotificationChannel(expiry)
        NotificationManagerCompat.from(context).createNotificationChannel(timers)
        NotificationManagerCompat.from(context).createNotificationChannel(push)
    }
}