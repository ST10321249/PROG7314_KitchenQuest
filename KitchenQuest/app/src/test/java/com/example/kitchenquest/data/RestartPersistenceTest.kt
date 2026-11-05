package com.example.kitchenquest.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.pantry.OfflinePantryRepository
import com.example.kitchenquest.data.pantry.PantryApi
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.UpdatePantryItemRequest
import com.example.kitchenquest.data.shopping.CreateShoppingItemRequest
import com.example.kitchenquest.data.shopping.OfflineShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingApi
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.TransferToPantryRequest
import com.example.kitchenquest.data.shopping.UpdateShoppingItemRequest
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class OfflinePantryApi : PantryApi {
    override suspend fun getPantryItems(): List<PantryItemDto> = error("offline")
    override suspend fun addPantryItem(request: CreatePantryItemRequest): PantryItemDto = error("offline")
    override suspend fun updatePantryItem(id: String, request: UpdatePantryItemRequest): PantryItemDto = error("offline")
    override suspend fun deletePantryItem(id: String) = error("offline")
}

private class OfflineShoppingApi : ShoppingApi {
    override suspend fun getShoppingItems(): List<ShoppingItemDto> = error("offline")
    override suspend fun addShoppingItem(request: CreateShoppingItemRequest): ShoppingItemDto = error("offline")
    override suspend fun updateShoppingItem(id: String, request: UpdateShoppingItemRequest): ShoppingItemDto = error("offline")
    override suspend fun deleteShoppingItem(id: String) = error("offline")
    override suspend fun transferToPantry(request: TransferToPantryRequest): PantryItemDto = error("offline")
}

@RunWith(RobolectricTestRunner::class)
class RestartPersistenceTest {

    private val databaseName = "restart-persistence-test.db"
    private lateinit var context: Context
    private var database: KitchenQuestDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    // Opening the database again is what happens when the app is closed and reopened.
    private fun openDatabase(): KitchenQuestDatabase {
        database?.close()
        return Room.databaseBuilder(context, KitchenQuestDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
            .also { database = it }
    }

    @Test
    fun pantryChangesMadeOfflineSurviveARestartAndStayWaiting() = runBlocking {
        var db = openDatabase()
        OfflinePantryRepository(db, OfflinePantryApi(), { "user-1" })
            .addPantryItem("Milk", 1.0, "l", "Dairy", null)

        db = openDatabase()
        val repository = OfflinePantryRepository(db, OfflinePantryApi(), { "user-1" })

        val item = repository.observePantryItems().first().single()
        assertEquals("Milk", item.ingredientName)
        assertEquals(SyncState.PENDING, item.syncState)
        assertEquals(1, db.pendingOperationDao().getOutstanding("user-1").size)
    }

    @Test
    fun shoppingChangesMadeOfflineSurviveARestartAndStayWaiting() = runBlocking {
        var db = openDatabase()
        OfflineShoppingRepository(db, OfflineShoppingApi(), { "user-1" })
            .addShoppingItem("Eggs", null, null)

        db = openDatabase()
        val repository = OfflineShoppingRepository(db, OfflineShoppingApi(), { "user-1" })

        val item = repository.observeShoppingItems().first().single()
        assertEquals("Eggs", item.ingredientName)
        assertEquals(SyncState.PENDING, item.syncState)
        assertEquals(1, db.pendingOperationDao().getOutstanding("user-1").size)
    }
}