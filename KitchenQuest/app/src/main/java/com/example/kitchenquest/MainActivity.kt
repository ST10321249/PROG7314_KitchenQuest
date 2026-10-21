package com.example.kitchenquest

import android.os.Bundle
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.example.kitchenquest.navigation.AppNavHost
import com.example.kitchenquest.data.preferences.AppLanguagePreferences
import com.example.kitchenquest.ui.theme.KitchenQuestTheme

class MainActivity : FragmentActivity() {

    // Create the Activity with the saved language before Compose reads strings.
    // This supports Android 8+ without changing our FragmentActivity-based biometric flow.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguagePreferences.localizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KitchenQuestTheme {
                AppNavHost()
            }
        }
    }
}