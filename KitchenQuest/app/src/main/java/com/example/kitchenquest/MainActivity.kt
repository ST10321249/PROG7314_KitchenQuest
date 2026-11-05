package com.example.kitchenquest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kitchenquest.data.preferences.AppLanguagePreferences
import com.example.kitchenquest.navigation.AppNavHost
import com.example.kitchenquest.notifications.NotificationChannels
import com.example.kitchenquest.notifications.NotificationPermissionRequests
import com.example.kitchenquest.ui.theme.KitchenQuestTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    // Set when an expiry alert was tapped. AppNavHost clears it once it has opened My Kitchen.
    private var openMyKitchen by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Notification permission granted: $granted")
    }

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificationPermissionRequests.requests.collect { askForNotificationPermission() }
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

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) return
        Log.d(TAG, "Asking for notification permission for timer alerts")
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}