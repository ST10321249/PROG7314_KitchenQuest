package com.example.kitchenquest.data.shopping

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationType
import com.example.kitchenquest.data.sync.SyncScheduler
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

private class FakeShoppingApi : ShoppingApi {
    var items: List<ShoppingItemDto> = emptyList()
    var offline = false

    override suspend fun getShoppingItems(): List<ShoppingItemDto> {
        if (offline) throw IOException("no network")
        return items
    }

    override suspend fun addShoppingItem(request: CreateShoppingItemRequest): ShoppingItemDto =
        error("The offline repository must not call this directly")

    override suspend fun updateShoppingItem(id: String, request: UpdateShoppingItemRequest): ShoppingItemDto =
        error("The offline repository must not call this directly")

    override suspend fun transferToPantry(request: TransferToPantryRequest): PantryItemDto =
        error("The offline repository must not call this directly")

    override suspend fun deleteShoppingItem(id: String) =
        error("The offline repository must not call this directly")
}

@RunWith(RobolectricTestRunner::class)
class OfflineShoppingRepositoryTest {

    private lateinit var database: KitchenQuestDatabase
    private lateinit var api: FakeShoppingApi
    private lateinit var repository: OfflineShoppingRepository
    private var userId: String? = "user-1"
    private var syncRequests = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeShoppingApi()
        repository = OfflineShoppingRepository(
            database = database,
            shoppingApi = api,
            currentUserId = { userId },
            scheduler = SyncScheduler { syncRequests++ }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun server(id: String, name: String = "Eggs", purchased: Boolean = false) =
        ShoppingItemDto(id = id, ingredientName = name, isPurchased = purchased)

    private suspend fun queued() = database.pendingOperationDao().getOutstanding("user-1")

    @Test
    fun anItemCanBeAddedWithoutAConnection() = runBlocking {
        api.offline = true

        val added = repository.addShoppingItem("Eggs", 12.0, "pcs").getOrThrow()

        assertEquals(added.id, added.clientId)
        assertEquals(SyncState.PENDING, added.syncState)
        assertEquals(1, syncRequests)
        val op = queued().single()
        assertEquals(SyncOperationType.CREATE, op.operationType)
        assertEquals(SyncEntity.SHOPPING_ITEM, op.entityType)
        assertTrue(op.payload.contains("\"clientId\":\"${added.id}\""))
        assertEquals(listOf("Eggs"), repository.getShoppingItems().getOrThrow().map { it.ingredientName })
    }

    @Test
    fun theListIsReadFromThePhoneWhenOffline() = runBlocking {
        api.items = listOf(server("srv-1", "Bread"))
        repository.getShoppingItems()

        api.offline = true

        assertEquals(listOf("Bread"), repository.getShoppingItems().getOrThrow().map { it.ingredientName })
    }

    @Test
    fun tickingAnItemOffQueuesOneUpdateAndTickingItBackReplacesIt() = runBlocking {
        api.items = listOf(server("srv-1", "Bread"))
        repository.getShoppingItems()

        val ticked = repository.setPurchased("srv-1", true).getOrThrow()
        repository.setPurchased("srv-1", false)

        assertEquals(true, ticked.isPurchased)
        val op = queued().single()
        assertEquals(SyncOperationType.UPDATE, op.operationType)
        assertTrue(op.payload.contains("\"isPurchased\":false"))
        assertEquals("srv-1", op.serverId)
    }

    @Test
    fun aSyncedItemRemovedOfflineQueuesAServerDelete() = runBlocking {
        api.items = listOf(server("srv-1", "Bread"))
        repository.getShoppingItems()

        repository.deleteShoppingItem("srv-1")

        assertTrue(database.shoppingDao().getAll("user-1").isEmpty())
        assertEquals(SyncOperationType.DELETE, queued().single().operationType)
    }

    @Test
    fun anItemAddedAndRemovedWhileOfflineSendsNothing() = runBlocking {
        api.offline = true
        val added = repository.addShoppingItem("Eggs", null, null).getOrThrow()

        repository.deleteShoppingItem(added.id)

        assertTrue(queued().isEmpty())
    }

    @Test
    fun movingASyncedItemToThePantryHappensInOneStepOnThePhone() = runBlocking {
        api.items = listOf(server("srv-1", "Eggs"))
        repository.getShoppingItems()

        val pantryItem = repository.transferToPantry(
            id = "srv-1",
            ingredientName = "Eggs",
            quantity = 12.0,
            unit = "pcs",
            category = "Dairy",
            expiryDate = "2026-11-01"
        ).getOrThrow()

        assertEquals(SyncState.PENDING, pantryItem.syncState)
        assertTrue(database.shoppingDao().getAll("user-1").isEmpty())
        assertEquals(listOf("Eggs"), database.pantryDao().getAll("user-1").map { it.ingredientName })

        val op = queued().single()
        assertEquals(SyncOperationType.TRANSFER_TO_PANTRY, op.operationType)
        val request = ApiClient.json.decodeFromString<TransferToPantryRequest>(op.payload)
        assertEquals("srv-1", request.shoppingItemId)
        assertNull(request.shoppingClientId)
        assertEquals(pantryItem.id, request.pantryItem.clientId)
        assertEquals("2026-11-01", request.pantryItem.expiryDate)
    }

    @Test
    fun movingAnItemTheServerHasNotSeenYetIdentifiesItByItsClientId() = runBlocking {
        api.offline = true
        val added = repository.addShoppingItem("Eggs", 12.0, "pcs").getOrThrow()
        // Pretend the create is already on its way, so it cannot simply be cancelled.
        val create = queued().single()
        database.pendingOperationDao().update(
            create.copy(status = com.example.kitchenquest.data.sync.SyncOperationStatus.IN_FLIGHT)
        )

        repository.transferToPantry(added.id, "Eggs", 12.0, "pcs", "Dairy", null)

        val transfer = queued().last()
        assertEquals(SyncOperationType.TRANSFER_TO_PANTRY, transfer.operationType)
        val request = ApiClient.json.decodeFromString<TransferToPantryRequest>(transfer.payload)
        assertNull(request.shoppingItemId)
        assertEquals(added.id, request.shoppingClientId)
    }

    @Test
    fun movingAnItemThatWasNeverSentJustCreatesThePantryItem() = runBlocking {
        api.offline = true
        val added = repository.addShoppingItem("Eggs", 12.0, "pcs").getOrThrow()

        val pantryItem = repository.transferToPantry(
            added.id, "Eggs", 12.0, "pcs", "Dairy", null
        ).getOrThrow()

        val op = queued().single()
        assertEquals(SyncOperationType.CREATE, op.operationType)
        assertEquals(SyncEntity.PANTRY_ITEM, op.entityType)
        assertEquals(pantryItem.id, op.entityId)
        assertTrue(database.shoppingDao().getAll("user-1").isEmpty())
    }

    @Test
    fun movingAnItemThatDoesNotExistChangesNothing() = runBlocking {
        val result = repository.transferToPantry("missing", "Eggs", 1.0, "pcs", "Dairy", null)

        assertEquals(ApiErrorType.NOT_FOUND, (result.exceptionOrNull() as ApiException).type)
        assertTrue(database.pantryDao().getAll("user-1").isEmpty())
        assertTrue(queued().isEmpty())
    }

    @Test
    fun aRefreshKeepsItemsWithChangesWaitingAndRemovesOnesDeletedElsewhere() = runBlocking {
        api.items = listOf(server("srv-1", "Bread"), server("srv-2", "Milk"))
        repository.getShoppingItems()
        repository.setPurchased("srv-1", true)

        api.items = listOf(server("srv-1", "Bread", purchased = false))
        val listed = repository.getShoppingItems().getOrThrow().associateBy { it.ingredientName }

        assertEquals(true, listed.getValue("Bread").isPurchased)
        assertEquals(SyncState.PENDING, listed.getValue("Bread").syncState)
        assertNull(listed["Milk"])
    }

    @Test
    fun eachAccountOnlySeesItsOwnList() = runBlocking {
        api.offline = true
        repository.addShoppingItem("Eggs", null, null)

        userId = "user-2"

        assertTrue(repository.getShoppingItems().getOrThrow().isEmpty())
    }

    @Test
    fun everythingFailsClearlyWhenNobodyIsSignedIn() = runBlocking {
        userId = null

        val errors = listOf(
            repository.getShoppingItems().exceptionOrNull(),
            repository.addShoppingItem("Eggs", null, null).exceptionOrNull(),
            repository.setPurchased("x", true).exceptionOrNull(),
            repository.deleteShoppingItem("x").exceptionOrNull(),
            repository.transferToPantry("x", "Eggs", 1.0, "pcs", "Dairy", null).exceptionOrNull()
        )

        errors.forEach { assertEquals(ApiErrorType.UNAUTHORIZED, (it as ApiException).type) }
    }
}
