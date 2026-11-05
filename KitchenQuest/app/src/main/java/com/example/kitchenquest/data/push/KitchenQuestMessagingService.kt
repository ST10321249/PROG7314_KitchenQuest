package com.example.kitchenquest.data.push

import android.util.Log
import com.example.kitchenquest.data.AppRepositories
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KitchenQuestMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d(TAG, "Firebase Messaging gave this phone a new token")
        scope.launch { AppRepositories.pushTokens().register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Push message received while the app is open")
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        PushNotifier(this).show(title, body, message.data)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MessagingService"
    }
}