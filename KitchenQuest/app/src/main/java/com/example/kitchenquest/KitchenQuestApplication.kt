package com.example.kitchenquest

import android.app.Application
import androidx.work.Configuration
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.sync.WorkManagerSyncScheduler

class KitchenQuestApplication : Application(), Configuration.Provider {

    // Lets background work start itself the first time it is needed, so the app never
    // depends on the order things start up in.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        // The scheduler is set before any repository is created so they all use it.
        AppRepositories.syncScheduler = WorkManagerSyncScheduler(this)
        AppRepositories.init(this)

        // Send anything left over from the last time the app was open.
        AppRepositories.syncScheduler.requestSync()
    }
}
