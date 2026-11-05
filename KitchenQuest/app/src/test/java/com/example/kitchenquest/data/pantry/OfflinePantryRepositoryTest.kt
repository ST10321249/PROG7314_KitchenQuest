package com.example.kitchenquest.data.pantry

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PantryItemEntity
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.sync.ClientIds
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationType
import com.example.kitchenquest.data.sync.SyncScheduler
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

private class FakePantryApi : PantryApi {
    var items: List<PantryItemDto> = emptyList()
    var offline = false
    var delayMs = 0L
    var calls = 0

    override suspend fun getPantryItems(): List<PantryItemDto> {
        calls++
        if (delayMs > 0) delay(delayMs)
        if (offline) throw IOException("no network")
        return items
    }

    override suspend fun addPantryItem(request: CreatePantryItemRequest): PantryItemDto =
        error("The offline repository must not call this directly")

    override suspend fun updatePantryItem(id: String, request: UpdatePantryItemRequest): PantryItemDto =
        error("The offline repository must not call this directly")

    override suspend fun deletePantryItem(id: String) =
        error("The offline repository must not call this directly")
}

@RunWith(RobolectricTestRunner::class)
class OfflinePantryRepositoryTest {

    private lateinit var database: KitchenQuestDatabase
    private lateinit var api: FakePantryApi
    private lateinit var repository: OfflinePantryRepository
    private var userId: String? = "user-1"
    private var syncRequests = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakePantryApi()
        repository = OfflinePantryRepository(
            database = database,
            pantryApi = api,
            currentUserId = { userId },
            scheduler = SyncScheduler { syncRequests++ }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun server(
        id: String,
        name: String = "Milk",
        clientId: String? = null,
        quantity: Double = 1.0
    ) = PantryItemDto(
        id = id,
        clientId = clientId,
        ingredientName = name,
        quantity = quantity,
        unit = "L",
        category = "Dairy"
    )

    private suspend fun add(name: String = "Milk") =
        repository.addPantryItem(name, 1.0, "L", "Dairy", null).getOrThrow()

    private suspend fun queued() = database.pendingOperationDao().getOutstanding("user-1")

    @Test
    fun anItemCanBeAddedWithoutAConnectionAndStaysAfterwards() = runBlocking {
        api.offline = true

        val added = add()

        assertTrue(ClientIds.isValid(added.id))
        assertEquals(added.id, added.clientId)
        assertEquals(SyncState.PENDING, added.syncState)

        val listed = repository.getPantryItems().getOrThrow()
        assertEquals(listOf(added.id), listed.map { it.id })
        assertEquals(SyncState.PENDING, listed.single().syncState)
    }

    @Test
    fun addingAnItemQueuesOneCreateAndAsksForASync() = runBlocking {
        val added = add()

        val ops = queued()
        assertEquals(1, ops.size)
        assertEquals(SyncOperationType.CREATE, ops.single().operationType)
        assertEquals(SyncEntity.PANTRY_ITEM, ops.single().entityType)
        assertEquals(added.id, ops.single().entityId)
        assertTrue(ops.single().payload.contains("\"clientId\":\"${added.id}\""))
        assertEquals(1, syncRequests)
    }

    @Test
    fun theListComesFromThePhoneWhenTheServerCannotBeReached() = runBlocking {
        api.items = listOf(server("srv-1", "Rice"))
        repository.getPantryItems()

        api.offline = true
        val listed = repository.getPantryItems().getOrThrow()

        assertEquals(listOf("Rice"), listed.map { it.ingredientName })
    }

    @Test
    fun aFreshInstallThatIsOfflineSeesAnEmptyPantryRatherThanAnError() = runBlocking {
        api.offline = true

        val result = repository.getPantryItems()

        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun serverItemsAreAddedAndOlderItemsKeepTheirServerIdAsTheLocalId() = runBlocking {
        api.items = listOf(
            server("srv-1", "Rice"),
            server("srv-2", "Milk", clientId = "client-aaaa-1111")
        )

        val listed = repository.getPantryItems().getOrThrow().associateBy { it.ingredientName }

        assertEquals("srv-1", listed.getValue("Rice").id)
        assertNull(listed.getValue("Rice").clientId)
        assertEquals("client-aaaa-1111", listed.getValue("Milk").id)
        assertEquals("client-aaaa-1111", listed.getValue("Milk").clientId)
        assertEquals(SyncState.SYNCED, listed.getValue("Milk").syncState)
    }

    @Test
    fun anItemDeletedElsewhereDisappearsOnTheNextRefresh() = runBlocking {
        api.items = listOf(server("srv-1", "Rice"), server("srv-2", "Milk"))
        repository.getPantryItems()

        api.items = listOf(server("srv-2", "Milk"))
        val listed = repository.getPantryItems().getOrThrow()

        assertEquals(listOf("Milk"), listed.map { it.ingredientName })
    }

    @Test
    fun aRefreshNeverOverwritesOrRemovesAnItemWithChangesWaiting() = runBlocking {
        api.items = listOf(server("srv-1", "Rice"))
        repository.getPantryItems()
        repository.updatePantryItem("srv-1", quantity = 7.0)
        val added = add("Beans")

        // The server still has the old quantity and knows nothing about the new item.
        val listed = repository.getPantryItems().getOrThrow().associateBy { it.ingredientName }

        assertEquals(7.0, listed.getValue("Rice").quantity, 0.0)
        assertEquals(SyncState.PENDING, listed.getValue("Rice").syncState)
        assertEquals(added.id, listed.getValue("Beans").id)
    }

    @Test
    fun anItemWhoseCreateReachedTheServerIsNotDuplicatedByARefresh() = runBlocking {
        val added = add("Milk")
        // The create arrived but the app never heard back, so the server already has it.
        api.items = listOf(server("srv-9", "Milk", clientId = added.id))

        val listed = repository.getPantryItems().getOrThrow()

        assertEquals(1, listed.size)
        assertEquals(added.id, listed.single().id)
    }

    @Test
    fun editingAnItemChangesItOnThePhoneAndQueuesOneUpdate() = runBlocking {
        api.items = listOf(server("srv-1", "Rice", quantity = 1.0))
        repository.getPantryItems()

        val updated = repository.updatePantryItem("srv-1", quantity = 4.0, unit = "kg").getOrThrow()

        assertEquals(4.0, updated.quantity, 0.0)
        assertEquals("kg", updated.unit)
        assertEquals("Rice", updated.ingredientName)
        val op = queued().single()
        assertEquals(SyncOperationType.UPDATE, op.operationType)
        assertEquals("srv-1", op.serverId)
        assertFalse(op.payload.contains("ingredientName"))
    }

    @Test
    fun editingTwiceBeforeASyncSendsASingleUpdate() = runBlocking {
        api.items = listOf(server("srv-1", "Rice"))
        repository.getPantryItems()

        repository.updatePantryItem("srv-1", quantity = 4.0)
        repository.updatePantryItem("srv-1", category = "Grains")

        val op = queued().single()
        assertTrue(op.payload.contains("\"quantity\":4.0"))
        assertTrue(op.payload.contains("\"category\":\"Grains\""))
    }

    @Test
    fun savingWithNothingChangedQueuesNothing() = runBlocking {
        api.items = listOf(server("srv-1", "Rice"))
        repository.getPantryItems()

        val result = repository.updatePantryItem("srv-1", quantity = 1.0, ingredientName = "Rice")

        assertEquals(SyncState.SYNCED, result.getOrThrow().syncState)
        assertTrue(queued().isEmpty())
    }

    @Test
    fun editingAnItemThatDoesNotExistFailsWithNotFound() = runBlocking {
        val result = repository.updatePantryItem("missing", quantity = 2.0)

        val error = result.exceptionOrNull() as ApiException
        assertEquals(ApiErrorType.NOT_FOUND, error.type)
    }

    @Test
    fun deletingASyncedItemRemovesItNowAndQueuesAServerDelete() = runBlocking {
        api.items = listOf(server("srv-1", "Rice"))
        repository.getPantryItems()

        assertTrue(repository.deletePantryItem("srv-1").isSuccess)

        assertTrue(database.pantryDao().getAll("user-1").isEmpty())
        val op = queued().single()
        assertEquals(SyncOperationType.DELETE, op.operationType)
        assertEquals("srv-1", op.serverId)
    }

    @Test
    fun deletingAnItemThatWasNeverSyncedSendsNothing() = runBlocking {
        api.offline = true
        val added = add()

        repository.deletePantryItem(added.id)

        assertTrue(database.pantryDao().getAll("user-1").isEmpty())
        assertTrue(queued().isEmpty())
    }

    @Test
    fun deletingSomethingAlreadyGoneIsNotAnError() = runBlocking {
        assertTrue(repository.deletePantryItem("nothing-here").isSuccess)
        assertTrue(queued().isEmpty())
    }

    @Test
    fun eachAccountOnlySeesItsOwnPantry() = runBlocking {
        api.offline = true
        add("Milk")

        userId = "user-2"
        assertTrue(repository.getPantryItems().getOrThrow().isEmpty())
        repository.addPantryItem("Bread", 1.0, "loaf", "Bakery", null)

        userId = "user-1"
        assertEquals(listOf("Milk"), repository.getPantryItems().getOrThrow().map { it.ingredientName })
    }

    @Test
    fun everythingFailsClearlyWhenNobodyIsSignedIn() = runBlocking {
        userId = null

        val results = listOf(
            repository.getPantryItems().exceptionOrNull(),
            repository.addPantryItem("Milk", 1.0, "L", "Dairy", null).exceptionOrNull(),
            repository.updatePantryItem("x", quantity = 2.0).exceptionOrNull(),
            repository.deletePantryItem("x").exceptionOrNull()
        )

        results.forEach { error ->
            assertEquals(ApiErrorType.UNAUTHORIZED, (error as ApiException).type)
        }
    }

    @Test
    fun aSlowServerDoesNotHoldUpAPantryThatIsAlreadyOnThePhone() = runBlocking {
        val slow = OfflinePantryRepository(
            database = database,
            pantryApi = api,
            currentUserId = { userId },
            refreshTimeoutMs = 50,
            firstLoadTimeoutMs = 50
        )
        api.offline = true
        add("Milk")
        api.offline = false
        api.delayMs = 5_000
        api.items = emptyList()

        val started = System.currentTimeMillis()
        val listed = slow.getPantryItems().getOrThrow()

        assertTrue(System.currentTimeMillis() - started < 3_000)
        assertEquals(listOf("Milk"), listed.map { it.ingredientName })
    }

    @Test
    fun anItemWithAFailedSyncIsKeptUntilTheUserDealsWithIt() = runBlocking {
        database.pantryDao().upsert(
            PantryItemEntity(
                userId = "user-1",
                id = "client-failed-1",
                ingredientName = "Odd item",
                quantity = 1.0,
                unit = "x",
                category = "Other",
                syncState = SyncState.FAILED
            )
        )
        api.items = emptyList()

        val listed = repository.getPantryItems().getOrThrow()

        assertEquals(SyncState.FAILED, listed.single().syncState)
    }
}
