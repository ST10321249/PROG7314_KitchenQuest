package com.example.kitchenquest.data

import android.content.Context
import com.example.kitchenquest.data.connectivity.NetworkConnectivity
import com.example.kitchenquest.data.favourites.DefaultFavouriteRepository
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.favourites.OfflineFavouriteRepository
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.pantry.OfflinePantryRepository
import com.example.kitchenquest.data.pantry.PantryApi
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.OfflineRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeOfflineStore
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.shopping.OfflineShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingApi
import com.example.kitchenquest.data.shopping.ShoppingRepository
import com.example.kitchenquest.data.sync.NoOpSyncScheduler
import com.example.kitchenquest.data.sync.RoomSyncOverviewSource
import com.example.kitchenquest.data.sync.SyncOverviewSource
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

    // Copies of opened recipes and of the saved-recipes list, kept per user.
    private val recipeOfflineStore: RecipeOfflineStore by lazy {
        RecipeOfflineStore(
            dao = database().recipeCacheDao(),
            currentUserId = signedInUserId
        )
    }

    // Recipe details can be read offline once they have been opened. Search and
    // recommendations still go to the API, because they need fresh results.
    val recipes: RecipeRepository by lazy {
        OfflineRecipeRepository(
            remote = DefaultRecipeRepository(),
            store = recipeOfflineStore
        )
    }

    // Saved recipes can be listed offline from the last copy the phone received.
    val favourites: FavouriteRepository by lazy {
        OfflineFavouriteRepository(
            remote = DefaultFavouriteRepository(),
            store = recipeOfflineStore
        )
    }

    // What the screens show about syncing: online or offline, and changes waiting.
    val syncOverview: SyncOverviewSource by lazy {
        val context = checkNotNull(appContext) {
            "AppRepositories.init must be called when the app starts."
        }
        RoomSyncOverviewSource(
            database = database(),
            connectivity = NetworkConnectivity(context).isOnline,
            currentUserId = signedInUserId
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