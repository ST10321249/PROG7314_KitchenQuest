package com.example.kitchenquest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.kitchenquest.data.preferences.AppLanguagePreferences
import com.example.kitchenquest.navigation.AppNavHost
import com.example.kitchenquest.notifications.NotificationChannels
import com.example.kitchenquest.ui.theme.KitchenQuestTheme

class MainActivity : FragmentActivity() {

    // Set when an expiry alert was tapped. AppNavHost clears it once it has opened My Kitchen.
    private var openMyKitchen by mutableStateOf(false)

    // Create the Activity with the saved language before Compose reads strings.
    // This supports Android 8+ without changing our FragmentActivity-based biometric flow.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguagePreferences.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openMyKitchen = savedInstanceState == null &&
                intent.getBooleanExtra(NotificationChannels.EXTRA_OPEN_MY_KITCHEN, false)

        setContent {
            KitchenQuestTheme {
                AppNavHost(
                    openMyKitchen = openMyKitchen,
                    onOpenMyKitchenHandled = { openMyKitchen = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NotificationChannels.EXTRA_OPEN_MY_KITCHEN, false)) {
            openMyKitchen = true
        }
    }
}