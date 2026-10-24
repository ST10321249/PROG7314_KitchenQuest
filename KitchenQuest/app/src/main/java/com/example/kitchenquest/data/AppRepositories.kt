package com.example.kitchenquest.data

import android.content.Context
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.pantry.OfflinePantryRepository
import com.example.kitchenquest.data.pantry.PantryApi
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.shopping.OfflineShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingApi
import com.example.kitchenquest.data.shopping.ShoppingRepository
import com.example.kitchenquest.data.sync.NoOpSyncScheduler
import com.example.kitchenquest.data.sync.SyncProcessor
import com.example.kitchenquest.data.sync.SyncScheduler
import com.google.firebase.auth.FirebaseAuth

// The repositories the screens use by default. Pantry and shopping keep a copy of the
// data on the phone, so they need the app context to open the local database.
object AppRepositories {

    private var appContext: Context? = null

    @Volatile
    var syncScheduler: SyncScheduler = NoOpSyncScheduler

    private val signedInUserId: () -> String? = {
        try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (error: IllegalStateException) {
            // Firebase has not been set up, so nobody can be signed in.
            null
        }
    }

    // The Firebase uid of whoever is signed in, or null.
    fun currentUserId(): String? = signedInUserId()

    // Called once when the app starts.
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val pantry: PantryRepository by lazy {
        OfflinePantryRepository(
            database = database(),
            pantryApi = ApiClient.create(PantryApi::class.java),
            currentUserId = signedInUserId,
            scheduler = syncScheduler
        )
    }

    val shopping: ShoppingRepository by lazy {
        OfflineShoppingRepository(
            database = database(),
            shoppingApi = ApiClient.create(ShoppingApi::class.java),
            currentUserId = signedInUserId,
            scheduler = syncScheduler
        )
    }

    // Sends the changes waiting in the queue. Used by the background sync.
    fun syncProcessor(): SyncProcessor = SyncProcessor(
        database = database(),
        pantryApi = ApiClient.create(PantryApi::class.java),
        shoppingApi = ApiClient.create(ShoppingApi::class.java)
    )

    private fun database(): KitchenQuestDatabase {
        val context = checkNotNull(appContext) {
            "AppRepositories.init must be called when the app starts."
        }
        return KitchenQuestDatabase.getInstance(context)
    }
}
