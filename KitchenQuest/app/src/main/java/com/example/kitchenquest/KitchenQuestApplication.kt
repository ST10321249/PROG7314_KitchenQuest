package com.example.kitchenquest

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.sync.WorkManagerSyncScheduler
import com.example.kitchenquest.notifications.ExpiryAlertScheduler
import com.example.kitchenquest.notifications.NotificationChannels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class KitchenQuestApplication : Application(), Configuration.Provider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // Channels must exist before anything can be posted to them.
        NotificationChannels.createAll(this)

        // Keep the daily expiry check going for the signed-in account, if it has alerts on.
        ExpiryAlertScheduler.scheduleIfEnabled(this, AppRepositories.currentUserId())

        watchSignInForPush()
    }

    private fun watchSignInForPush() {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (error: Exception) {
            Log.w(TAG, "Firebase is not set up, push notifications are off", error)
            return
        }

        var lastUid: String? = auth.currentUser?.uid
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                appScope.launch { AppRepositories.pushTokens().registerCurrentDevice() }
            } else if (lastUid != null) {
                appScope.launch { forgetPushToken() }
            }
            lastUid = uid
        }
    }

    private suspend fun forgetPushToken() {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
            Log.d(TAG, "Signed out, so this phone's push token was removed")
        } catch (error: Exception) {
            Log.w(TAG, "Could not remove the push token after signing out", error)
        }
    }

    private companion object {
        const val TAG = "KitchenQuestApp"
    }
}