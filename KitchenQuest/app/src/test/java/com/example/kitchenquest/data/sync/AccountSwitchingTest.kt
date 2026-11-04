package com.example.kitchenquest.data.pantry

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The offline repository must never let one account see another's pantry, and must keep
// the first account's items and waiting changes while someone else is signed in.
private class NoConnectionPantryApi : PantryApi {
    override suspend fun getPantryItems(): List<PantryItemDto> = error("offline")
    override suspend fun addPantryItem(request: CreatePantryItemRequest): PantryItemDto = error("offline")
    override suspend fun updatePantryItem(id: String, request: UpdatePantryItemRequest): PantryItemDto = error("offline")
    override suspend fun deletePantryItem(id: String) = error("offline")

}

@RunWith(RobolectricTestRunner::class)
class AccountSwitchingTest {

    private lateinit var database: KitchenQuestDatabase
    private lateinit var repository: OfflinePantryRepository
    private var userId: String? = "user-1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflinePantryRepository(database, NoConnectionPantryApi(), { userId })
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun names() = repository.observePantryItems().first().map { it.ingredientName }

    @Test
    fun eachAccountOnlySeesItsOwnItemsAndKeepsItsWaitingChanges() = runBlocking {
        repository.addPantryItem("Milk", 1.0, "l", "Dairy", null)
        assertEquals(listOf("Milk"), names())

        userId = "user-2"
        assertEquals(emptyList<String>(), names())
        repository.addPantryItem("Eggs", 12.0, "pcs", "Dairy", null)
        assertEquals(listOf("Eggs"), names())

        userId = "user-1"
        assertEquals(listOf("Milk"), names())
        assertEquals(1, database.pendingOperationDao().getOutstanding("user-1").size)
        assertEquals(1, database.pendingOperationDao().getOutstanding("user-2").size)
    }

    @Test
    fun signedOutThereIsNothingToObserve() = runBlocking {
        repository.addPantryItem("Milk", 1.0, "l", "Dairy", null)
        userId = null

        assertEquals(emptyList<String>(), names())
    }
}