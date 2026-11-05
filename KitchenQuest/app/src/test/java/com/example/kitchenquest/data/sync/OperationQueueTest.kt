package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PendingOperationDao
import com.example.kitchenquest.data.local.PendingOperationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OperationQueueTest {

    private lateinit var database: KitchenQuestDatabase
    private lateinit var dao: PendingOperationDao
    private lateinit var queue: OperationQueue

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.pendingOperationDao()
        queue = OperationQueue(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun enqueue(
        type: SyncOperationType,
        payload: String,
        entityId: String = "item-1",
        entityType: SyncEntity = SyncEntity.PANTRY_ITEM,
        userId: String = "user-1",
        serverId: String? = null
    ) = queue.enqueue(userId, entityType, type, entityId, payload, serverId)

    private suspend fun outstanding(userId: String = "user-1"): List<PendingOperationEntity> =
        dao.getOutstanding(userId)

    @Test
    fun aCreateIsQueuedAsANewEntry() = runBlocking {
        val result = enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk"}""")

        assertEquals(OperationQueue.Result.QUEUED, result)
        val ops = outstanding()
        assertEquals(1, ops.size)
        assertEquals(SyncOperationType.CREATE, ops.single().operationType)
        assertTrue(ClientIds.isValid(ops.single().operationId))
    }

    @Test
    fun anEditBeforeTheCreateIsSentIsFoldedIntoTheCreate() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk","quantity":1.0}""")

        val result = enqueue(SyncOperationType.UPDATE, """{"quantity":3.0}""")

        assertEquals(OperationQueue.Result.MERGED, result)
        val op = outstanding().single()
        assertEquals(SyncOperationType.CREATE, op.operationType)
        assertTrue(op.payload.contains("\"ingredientName\":\"Milk\""))
        assertTrue(op.payload.contains("\"quantity\":3.0"))
    }

    @Test
    fun severalEditsToAnAlreadySyncedItemBecomeOneWithTheLatestValues() = runBlocking {
        enqueue(SyncOperationType.UPDATE, """{"quantity":2.0,"unit":"kg"}""", serverId = "srv-1")
        enqueue(SyncOperationType.UPDATE, """{"quantity":5.0}""", serverId = "srv-1")

        val op = outstanding().single()
        assertEquals(SyncOperationType.UPDATE, op.operationType)
        assertTrue(op.payload.contains("\"quantity\":5.0"))
        assertTrue(op.payload.contains("\"unit\":\"kg\""))
    }

    @Test
    fun anEditWhileTheCreateIsBeingSentIsQueuedBehindIt() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk"}""")
        val create = outstanding().single()
        dao.update(create.copy(status = SyncOperationStatus.IN_FLIGHT))

        val result = enqueue(SyncOperationType.UPDATE, """{"quantity":3.0}""")

        assertEquals(OperationQueue.Result.QUEUED, result)
        assertEquals(
            listOf(SyncOperationType.CREATE, SyncOperationType.UPDATE),
            outstanding().map { it.operationType }
        )
        assertEquals(SyncOperationStatus.IN_FLIGHT, outstanding().first().status)
    }

    @Test
    fun deletingAnItemThatNeverReachedTheServerSendsNothing() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk"}""")
        enqueue(SyncOperationType.UPDATE, """{"quantity":3.0}""")

        val result = enqueue(SyncOperationType.DELETE, "{}")

        assertEquals(OperationQueue.Result.CANCELLED, result)
        assertTrue(outstanding().isEmpty())
    }

    @Test
    fun deletingASyncedItemDropsItsWaitingEditsAndQueuesOneDelete() = runBlocking {
        enqueue(SyncOperationType.UPDATE, """{"quantity":3.0}""", serverId = "srv-1")

        val result = enqueue(SyncOperationType.DELETE, "{}", serverId = "srv-1")

        assertEquals(OperationQueue.Result.QUEUED, result)
        val op = outstanding().single()
        assertEquals(SyncOperationType.DELETE, op.operationType)
        assertEquals("srv-1", op.serverId)
    }

    @Test
    fun deletingWhileTheCreateIsBeingSentStillSendsTheDeleteAfterwards() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk"}""")
        dao.update(outstanding().single().copy(status = SyncOperationStatus.IN_FLIGHT))

        val result = enqueue(SyncOperationType.DELETE, "{}")

        assertEquals(OperationQueue.Result.QUEUED, result)
        assertEquals(
            listOf(SyncOperationType.CREATE, SyncOperationType.DELETE),
            outstanding().map { it.operationType }
        )
    }

    @Test
    fun editingAFailedCreateGivesItAFreshStart() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk","quantity":0.0}""")
        dao.update(
            outstanding().single().copy(
                status = SyncOperationStatus.FAILED,
                attemptCount = 4,
                lastError = "Quantity must be greater than zero"
            )
        )

        enqueue(SyncOperationType.UPDATE, """{"quantity":2.0}""")

        val op = outstanding().single()
        assertEquals(SyncOperationStatus.PENDING, op.status)
        assertEquals(0, op.attemptCount)
        assertEquals(null, op.lastError)
        assertTrue(op.payload.contains("\"quantity\":2.0"))
    }

    @Test
    fun deletingAnItemWhoseCreateFailedLeavesNothingBehind() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"ingredientName":"Milk"}""")
        dao.update(outstanding().single().copy(status = SyncOperationStatus.FAILED))

        val result = enqueue(SyncOperationType.DELETE, "{}")

        assertEquals(OperationQueue.Result.CANCELLED, result)
        assertTrue(outstanding().isEmpty())
    }

    @Test
    fun movingAnUnsentShoppingItemToThePantryCancelsTheShoppingChanges() = runBlocking {
        enqueue(
            SyncOperationType.CREATE,
            """{"ingredientName":"Eggs"}""",
            entityId = "shop-1",
            entityType = SyncEntity.SHOPPING_ITEM
        )

        val result = enqueue(
            SyncOperationType.TRANSFER_TO_PANTRY,
            "{}",
            entityId = "shop-1",
            entityType = SyncEntity.SHOPPING_ITEM
        )

        assertEquals(OperationQueue.Result.CANCELLED, result)
        assertTrue(outstanding().isEmpty())
    }

    @Test
    fun movingASyncedShoppingItemToThePantryQueuesOneTransfer() = runBlocking {
        val result = enqueue(
            SyncOperationType.TRANSFER_TO_PANTRY,
            """{"shoppingItemId":"srv-1"}""",
            entityId = "shop-1",
            entityType = SyncEntity.SHOPPING_ITEM,
            serverId = "srv-1"
        )

        assertEquals(OperationQueue.Result.QUEUED, result)
        assertEquals(SyncOperationType.TRANSFER_TO_PANTRY, outstanding().single().operationType)
    }

    @Test
    fun changesToDifferentItemsAndUsersAreKeptSeparate() = runBlocking {
        enqueue(SyncOperationType.CREATE, """{"a":1}""", entityId = "item-1")
        enqueue(SyncOperationType.CREATE, """{"a":2}""", entityId = "item-2")
        enqueue(SyncOperationType.CREATE, """{"a":3}""", entityId = "item-1", userId = "user-2")

        enqueue(SyncOperationType.UPDATE, """{"a":9}""", entityId = "item-1")

        assertEquals(2, outstanding("user-1").size)
        assertTrue(outstanding("user-1").first { it.entityId == "item-1" }.payload.contains("9"))
        assertTrue(outstanding("user-1").first { it.entityId == "item-2" }.payload.contains("2"))
        assertTrue(outstanding("user-2").single().payload.contains("3"))
    }
}
