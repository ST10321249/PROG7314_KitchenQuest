package com.example.kitchenquest.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationStatus
import com.example.kitchenquest.data.sync.SyncOperationType
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KitchenQuestDatabaseTest {

    private lateinit var context: Context
    private lateinit var database: KitchenQuestDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun pantry(
        userId: String = "user-1",
        id: String = "item-1",
        name: String = "Milk",
        expiry: String? = null
    ) = PantryItemEntity(
        userId = userId,
        id = id,
        ingredientName = name,
        quantity = 1.0,
        unit = "L",
        category = "Dairy",
        expiryDate = expiry
    )

    private fun shopping(
        userId: String = "user-1",
        id: String = "shop-1",
        name: String = "Eggs",
        purchased: Boolean = false
    ) = ShoppingItemEntity(
        userId = userId,
        id = id,
        ingredientName = name,
        isPurchased = purchased
    )

    private fun operation(
        operationId: String,
        userId: String = "user-1",
        entityId: String = "item-1",
        type: SyncOperationType = SyncOperationType.CREATE,
        status: SyncOperationStatus = SyncOperationStatus.PENDING,
        entityType: SyncEntity = SyncEntity.PANTRY_ITEM
    ) = PendingOperationEntity(
        operationId = operationId,
        userId = userId,
        entityType = entityType,
        operationType = type,
        entityId = entityId,
        payload = "{}",
        status = status,
        createdAt = 1_000L
    )

    @Test
    fun pantryItemsAreOnlyVisibleToTheirOwnUser() = runBlocking {
        val dao = database.pantryDao()
        dao.upsert(pantry(userId = "user-1", id = "a", name = "Milk"))
        dao.upsert(pantry(userId = "user-2", id = "b", name = "Bread"))

        assertEquals(listOf("Milk"), dao.getAll("user-1").map { it.ingredientName })
        assertEquals(listOf("Bread"), dao.getAll("user-2").map { it.ingredientName })
        assertNull(dao.get("user-1", "b"))
    }

    @Test
    fun twoUsersCanHoldAnItemWithTheSameId() = runBlocking {
        val dao = database.pantryDao()
        dao.upsert(pantry(userId = "user-1", id = "same", name = "Milk"))
        dao.upsert(pantry(userId = "user-2", id = "same", name = "Bread"))

        assertEquals("Milk", dao.get("user-1", "same")?.ingredientName)
        assertEquals("Bread", dao.get("user-2", "same")?.ingredientName)
    }

    @Test
    fun upsertReplacesAnExistingItemAndDeleteRemovesIt() = runBlocking {
        val dao = database.pantryDao()
        dao.upsert(pantry(name = "Milk"))
        dao.upsert(pantry(name = "Oat milk"))

        assertEquals(1, dao.getAll("user-1").size)
        assertEquals("Oat milk", dao.get("user-1", "item-1")?.ingredientName)

        dao.delete("user-1", "item-1")
        assertTrue(dao.getAll("user-1").isEmpty())
    }

    @Test
    fun deleteAllOnlyRemovesTheGivenItemsForThatUser() = runBlocking {
        val dao = database.pantryDao()
        dao.upsertAll(listOf(pantry(id = "a"), pantry(id = "b"), pantry(id = "c")))
        dao.upsert(pantry(userId = "user-2", id = "a"))

        dao.deleteAll("user-1", listOf("a", "b"))

        assertEquals(listOf("c"), dao.getAll("user-1").map { it.id })
        assertNotNull(dao.get("user-2", "a"))
    }

    @Test
    fun pantryIsSortedByExpiryWithUndatedItemsLast() = runBlocking {
        val dao = database.pantryDao()
        dao.upsertAll(
            listOf(
                pantry(id = "a", name = "Rice", expiry = null),
                pantry(id = "b", name = "Yoghurt", expiry = "2026-11-05"),
                pantry(id = "c", name = "Milk", expiry = "2026-10-30")
            )
        )

        assertEquals(listOf("Milk", "Yoghurt", "Rice"), dao.getAll("user-1").map { it.ingredientName })
    }

    @Test
    fun syncStateCanBeChangedWithoutTouchingOtherFields() = runBlocking {
        val dao = database.pantryDao()
        dao.upsert(pantry())

        dao.setSyncState("user-1", "item-1", SyncState.FAILED)

        val item = dao.get("user-1", "item-1")
        assertEquals(SyncState.FAILED, item?.syncState)
        assertEquals("Milk", item?.ingredientName)
    }

    @Test
    fun observingPantryEmitsTheLatestItems() = runBlocking {
        val dao = database.pantryDao()
        dao.upsert(pantry(name = "Milk"))

        assertEquals(listOf("Milk"), dao.observeAll("user-1").first().map { it.ingredientName })
    }

    @Test
    fun shoppingItemsAreScopedToTheUserAndUnpurchasedComeFirst() = runBlocking {
        val dao = database.shoppingDao()
        dao.upsertAll(
            listOf(
                shopping(id = "a", name = "Bread", purchased = true),
                shopping(id = "b", name = "Eggs", purchased = false)
            )
        )
        dao.upsert(shopping(userId = "user-2", id = "c", name = "Tea"))

        assertEquals(listOf("Eggs", "Bread"), dao.getAll("user-1").map { it.ingredientName })
        assertEquals(listOf("Tea"), dao.getAll("user-2").map { it.ingredientName })
    }

    @Test
    fun queuedChangesComeBackInTheOrderTheyWereMade() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("op-1"))
        dao.insert(operation("op-2", type = SyncOperationType.UPDATE))
        dao.insert(operation("op-3", type = SyncOperationType.DELETE))

        assertEquals(
            listOf("op-1", "op-2", "op-3"),
            dao.getOutstanding("user-1").map { it.operationId }
        )
        assertEquals("op-1", dao.oldestPending("user-1")?.operationId)
    }

    @Test
    fun theSameOperationIdCannotBeQueuedTwice() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("op-1"))

        var rejected = false
        try {
            dao.insert(operation("op-1"))
        } catch (error: SQLiteConstraintException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(1, dao.getOutstanding("user-1").size)
    }

    @Test
    fun oneUsersQueueIsNeverReturnedForAnother() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("op-1", userId = "user-1"))
        dao.insert(operation("op-2", userId = "user-2"))

        assertEquals(listOf("op-1"), dao.getOutstanding("user-1").map { it.operationId })
        assertEquals("op-2", dao.oldestPending("user-2")?.operationId)
    }

    @Test
    fun succeededChangesAreNotOutstandingAndFailedOnesDoNotBlockTheItem() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("done", status = SyncOperationStatus.SUCCEEDED))
        dao.insert(operation("failed", entityId = "item-2", status = SyncOperationStatus.FAILED))
        dao.insert(operation("waiting", entityId = "item-3"))

        assertEquals(
            listOf("failed", "waiting"),
            dao.getOutstanding("user-1").map { it.operationId }
        )
        assertEquals(
            listOf("item-3"),
            dao.getBusyEntityIds("user-1", SyncEntity.PANTRY_ITEM)
        )
        assertEquals("waiting", dao.oldestPending("user-1")?.operationId)
    }

    @Test
    fun changesForOneItemCanBeFoundTogether() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("op-1", entityId = "item-1"))
        dao.insert(operation("op-2", entityId = "item-2"))
        dao.insert(operation("op-3", entityId = "item-1", type = SyncOperationType.UPDATE))
        dao.insert(operation("op-4", entityId = "item-1", entityType = SyncEntity.SHOPPING_ITEM))

        val found = dao.getForEntity("user-1", SyncEntity.PANTRY_ITEM, "item-1")

        assertEquals(listOf("op-1", "op-3"), found.map { it.operationId })
    }

    @Test
    fun updatingAChangeRecordsTheAttemptAndError() = runBlocking {
        val dao = database.pendingOperationDao()
        val sequence = dao.insert(operation("op-1"))
        val stored = dao.getOutstanding("user-1").single()
        assertEquals(sequence, stored.sequence)

        dao.update(
            stored.copy(
                status = SyncOperationStatus.FAILED,
                attemptCount = 3,
                lastError = "Quantity must be greater than zero"
            )
        )

        val updated = dao.getOutstanding("user-1").single()
        assertEquals(SyncOperationStatus.FAILED, updated.status)
        assertEquals(3, updated.attemptCount)
        assertEquals("Quantity must be greater than zero", updated.lastError)
        assertEquals(1, dao.count("user-1", SyncOperationStatus.FAILED))
    }

    @Test
    fun resetInFlightReturnsInterruptedChangesToTheQueue() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("op-1", status = SyncOperationStatus.IN_FLIGHT))
        dao.insert(operation("op-2", userId = "user-2", status = SyncOperationStatus.IN_FLIGHT))

        dao.resetInFlight()

        assertEquals(SyncOperationStatus.PENDING, dao.getOutstanding("user-1").single().status)
        assertEquals(SyncOperationStatus.PENDING, dao.getOutstanding("user-2").single().status)
    }

    @Test
    fun waitingCountIncludesOnlyPendingAndInFlightChanges() = runBlocking {
        val dao = database.pendingOperationDao()
        dao.insert(operation("a", status = SyncOperationStatus.PENDING))
        dao.insert(operation("b", status = SyncOperationStatus.IN_FLIGHT))
        dao.insert(operation("c", status = SyncOperationStatus.FAILED))
        dao.insert(operation("d", userId = "user-2"))

        assertEquals(2, dao.observeWaitingCount("user-1").first())
    }

    @Test
    fun deletingAChangeRemovesItFromTheQueue() = runBlocking {
        val dao = database.pendingOperationDao()
        val sequence = dao.insert(operation("op-1"))

        dao.delete(sequence)

        assertTrue(dao.getOutstanding("user-1").isEmpty())
    }

    @Test
    fun dataAndQueuedChangesSurviveTheDatabaseBeingClosedAndReopened() = runBlocking {
        val name = "restart-test.db"
        val first = Room.databaseBuilder(context, KitchenQuestDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
        first.pantryDao().upsert(pantry(name = "Milk"))
        first.pendingOperationDao().insert(operation("op-1"))
        first.close()

        val reopened = Room.databaseBuilder(context, KitchenQuestDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Milk", reopened.pantryDao().get("user-1", "item-1")?.ingredientName)
            assertEquals("op-1", reopened.pendingOperationDao().oldestPending("user-1")?.operationId)
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
