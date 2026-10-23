package com.example.kitchenquest

import android.app.Application
import com.example.kitchenquest.data.AppRepositories

class KitchenQuestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppRepositories.init(this)
    }
}
