package com.example.kitchenquest.notifications

import android.content.Context

/** Device-only choice, kept per Firebase uid so one account cannot inherit another's setting. */
class ExpiryAlertSettings(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "kitchenquest_expiry_alert_settings",
        Context.MODE_PRIVATE
    )

    fun isEnabled(uid: String): Boolean =
        uid.isNotBlank() && preferences.getBoolean("enabled_$uid", false)

    fun setEnabled(uid: String, enabled: Boolean) {
        if (uid.isBlank()) return
        preferences.edit().putBoolean("enabled_$uid", enabled).apply()
    }
}