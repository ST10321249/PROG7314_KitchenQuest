package com.example.kitchenquest

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.example.kitchenquest.navigation.AppNavHost
import com.example.kitchenquest.ui.theme.KitchenQuestTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KitchenQuestTheme {
                AppNavHost()
            }
        }
    }
}