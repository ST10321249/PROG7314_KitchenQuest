package com.example.kitchenquest.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kitchenquest.MainActivity
import com.example.kitchenquest.R
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.toDto
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.preferences.AppLanguagePreferences
import java.time.LocalDate

// Looks at the signed-in user's pantry on the phone and posts one notification if
// something is about to expire. It never needs a connection.
class ExpiryAlertNotifier(
    private val context: Context,
    private val database: KitchenQuestDatabase,
    private val currentUserId: () -> String?,
    private val settings: ExpiryAlertSettings = ExpiryAlertSettings(context),
    private val today: () -> LocalDate = { LocalDate.now() }
) {

    enum class Outcome { POSTED, NOTHING_EXPIRING, TURNED_OFF, SIGNED_OUT, NOT_ALLOWED }

    suspend fun run(): Outcome {
        val userId = currentUserId() ?: return Outcome.SIGNED_OUT
        if (!settings.isEnabled(userId)) return Outcome.TURNED_OFF

        val items = database.pantryDao().getAll(userId).map { it.toDto() }
        val expiring = ExpiryAlertRules.expiringSoon(items, today())
        if (expiring.isEmpty()) return Outcome.NOTHING_EXPIRING
        if (!canPost()) return Outcome.NOT_ALLOWED

        post(expiring)
        return Outcome.POSTED
    }

    private fun canPost(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // canPost() has already checked the permission.
    private fun post(expiring: List<PantryItemDto>) {
        NotificationChannels.createAll(context)

        val text = AppLanguagePreferences.localizedContext(context)
        val first = expiring.first().ingredientName
        val message = if (expiring.size == 1) {
            text.getString(R.string.expiry_alert_text_one, first, ExpiryAlertRules.WINDOW_DAYS.toInt())
        } else {
            text.getString(
                R.string.expiry_alert_text_many,
                first,
                expiring.size - 1,
                ExpiryAlertRules.WINDOW_DAYS.toInt()
            )
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(NotificationChannels.EXTRA_OPEN_MY_KITCHEN, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.EXPIRY_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_expiry)
            .setContentTitle(text.getString(R.string.expiry_alert_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 4101
    }
}