package com.example.kitchenquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.kitchenquest.navigation.AppNavHost
import com.example.kitchenquest.ui.theme.KitchenQuestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KitchenQuestTheme {
                AppNavHost()
            }
        }
    }
}