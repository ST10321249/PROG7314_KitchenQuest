package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.pantry.OfflinePantryRepository
import com.example.kitchenquest.data.shopping.OfflineShoppingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
class SyncProcessorTest {

    private lateinit var database: KitchenQuestDatabase
    private lateinit var server: FakeServer
    private lateinit var pantry: OfflinePantryRepository
    private lateinit var shopping: OfflineShoppingRepository
    private lateinit var processor: SyncProcessor
    private var userId = "user-1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = FakeServer()
        pantry = OfflinePantryRepository(database, server, { userId })
        shopping = OfflineShoppingRepository(database, server, { userId })
        processor = SyncProcessor(database, server, server)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun sync(user: String = "user-1") = processor.run(user)

    private suspend fun waiting(user: String = "user-1") =
        database.pendingOperationDao().getOutstanding(user)

    private suspend fun addPantry(name: String = "Milk") =
        pantry.addPantryItem(name, 1.0, "L", "Dairy", null).getOrThrow()

    private fun timeout() = ApiException(ApiErrorType.TIMEOUT, "The server took too long.")

    @Test
    fun anItemAddedOfflineReachesTheServerOnceTheConnectionIsBack() = runBlocking {
        server.online = false
        val added = addPantry()

        assertEquals(SyncProcessor.Outcome.RETRY, sync())
        val stuck = waiting().single()
        assertEquals(SyncOperationStatus.PENDING, stuck.status)
        assertEquals(0, stuck.attemptCount)
        assertNotNull(stuck.lastError)
        assertTrue(server.pantry.isEmpty())

        server.online = true
        assertEquals(SyncProcessor.Outcome.DONE, sync())

        val onServer = server.pantry.values.single()
        assertEquals(added.id, onServer.clientId)
        assertTrue(waiting().isEmpty())
        val row = database.pantryDao().get("user-1", added.id)
        assertEquals(onServer.id, row?.serverId)
        assertEquals(onServer.updatedAt, row?.updatedAt)
        assertEquals(SyncState.SYNCED, row?.syncState)
    }

    @Test
    fun aLostReplyDoesNotCreateADuplicateWhenTheChangeIsSentAgain() = runBlocking {
        server.lostReplies = 1
        addPantry()

        assertEquals(SyncProcessor.Outcome.RETRY, sync())
        assertEquals(1, server.pantry.size)
        assertEquals(1, waiting().single().attemptCount)

        assertEquals(SyncProcessor.Outcome.DONE, sync())
        assertEquals(1, server.pantry.size)
        assertTrue(waiting().isEmpty())
    }

    @Test
    fun editsMadeBeforeTheFirstSyncAreSentAsOneCreate() = runBlocking {
        val added = addPantry()
        pantry.updatePantryItem(added.id, quantity = 5.0)
        pantry.updatePantryItem(added.id, category = "Chilled")

        sync()

        assertEquals(listOf("POST pantry"), server.calls)
        val onServer = server.pantry.values.single()
        assertEquals(5.0, onServer.quantity, 0.0)
        assertEquals("Chilled", onServer.category)
    }

    @Test
    fun editingAndThenDeletingASyncedItemOfflineSendsOnlyTheDelete() = runBlocking {
        val seeded = server.seedPantry("Rice")
        pantry.getPantryItems()
        server.calls.clear()
        server.online = false
        pantry.updatePantryItem(seeded.id, quantity = 9.0)
        pantry.deletePantryItem(seeded.id)
        server.online = true

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(listOf("DELETE pantry/${seeded.id}"), server.calls)
        assertTrue(server.pantry.isEmpty())
    }

    @Test
    fun anItemAddedAndDeletedBeforeAnySyncNeverTouchesTheServer() = runBlocking {
        val added = addPantry()
        pantry.deletePantryItem(added.id)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertTrue(server.calls.isEmpty())
    }

    @Test
    fun anEditToAnItemDeletedElsewhereIsDroppedAndTheItemRemovedFromThePhone() = runBlocking {
        val seeded = server.seedPantry("Rice")
        pantry.getPantryItems()
        server.pantry.clear()
        pantry.updatePantryItem(seeded.id, quantity = 4.0)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertTrue(waiting().isEmpty())
        assertNull(database.pantryDao().get("user-1", seeded.id))
    }

    @Test
    fun deletingSomethingAlreadyDeletedElsewhereIsFine() = runBlocking {
        val seeded = server.seedPantry("Rice")
        pantry.getPantryItems()
        server.pantry.clear()
        pantry.deletePantryItem(seeded.id)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertTrue(waiting().isEmpty())
    }

    @Test
    fun aRejectedItemIsMarkedFailedAndDoesNotHoldUpTheRest() = runBlocking {
        server.rejectName = "Bad"
        val bad = addPantry("Bad")
        val good = addPantry("Good")

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(listOf("Good"), server.pantry.values.map { it.ingredientName })
        val failed = waiting().single()
        assertEquals(SyncOperationStatus.FAILED, failed.status)
        assertEquals("Bad is not valid", failed.lastError)
        assertEquals(SyncState.FAILED, database.pantryDao().get("user-1", bad.id)?.syncState)
        assertEquals(SyncState.SYNCED, database.pantryDao().get("user-1", good.id)?.syncState)
    }

    @Test
    fun fixingARejectedItemSendsItAgain() = runBlocking {
        server.rejectName = "Bad"
        val bad = addPantry("Bad")
        sync()

        pantry.updatePantryItem(bad.id, ingredientName = "Better")

        assertEquals(SyncProcessor.Outcome.DONE, sync())
        assertEquals(listOf("Better"), server.pantry.values.map { it.ingredientName })
        assertTrue(waiting().isEmpty())
        assertEquals(SyncState.SYNCED, database.pantryDao().get("user-1", bad.id)?.syncState)
    }

    @Test
    fun aChangeThatKeepsFailingIsGivenUpOnAndStopsHoldingUpOthers() = runBlocking {
        val added = addPantry()

        repeat(SyncPolicy.MAX_ATTEMPTS - 1) {
            server.failures.add(timeout())
            assertEquals(SyncProcessor.Outcome.RETRY, sync())
        }
        server.failures.add(timeout())
        assertEquals(SyncProcessor.Outcome.DONE, sync())

        val given = waiting().single()
        assertEquals(SyncOperationStatus.FAILED, given.status)
        assertEquals(SyncPolicy.MAX_ATTEMPTS, given.attemptCount)
        assertEquals(SyncState.FAILED, database.pantryDao().get("user-1", added.id)?.syncState)
        assertTrue(server.pantry.isEmpty())
    }

    @Test
    fun anExpiredSignInIsRetriedWithoutUsingUpAttempts() = runBlocking {
        addPantry()
        server.failures.add(ApiException(ApiErrorType.UNAUTHORIZED, "Session expired"))

        assertEquals(SyncProcessor.Outcome.RETRY, sync())

        val op = waiting().single()
        assertEquals(SyncOperationStatus.PENDING, op.status)
        assertEquals(0, op.attemptCount)
    }

    @Test
    fun aServerThatDoesNotKnowTheRouteYetIsRetriedRatherThanGivenUpOnAtOnce() = runBlocking {
        addPantry()
        server.failures.add(ApiException(ApiErrorType.NOT_FOUND, "Route not found"))

        assertEquals(SyncProcessor.Outcome.RETRY, sync())

        assertEquals(1, waiting().single().attemptCount)
        assertEquals(SyncOperationStatus.PENDING, waiting().single().status)
    }

    @Test
    fun aDeleteQueuedBehindAnUnfinishedCreateGetsTheServerIdOnceItExists() = runBlocking {
        val added = addPantry()
        val create = waiting().single()
        database.pendingOperationDao().update(create.copy(status = SyncOperationStatus.IN_FLIGHT))

        pantry.deletePantryItem(added.id)
        assertNull(waiting().last().serverId)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(1, server.calls.count { it == "POST pantry" })
        assertEquals(1, server.calls.count { it.startsWith("DELETE pantry/") })
        assertTrue(server.pantry.isEmpty())
        assertTrue(waiting().isEmpty())
    }

    @Test
    fun anEditQueuedBehindAnUnfinishedCreateIsSentAfterIt() = runBlocking {
        val added = addPantry()
        database.pendingOperationDao().update(
            waiting().single().copy(status = SyncOperationStatus.IN_FLIGHT)
        )
        pantry.updatePantryItem(added.id, quantity = 8.0)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(8.0, server.pantry.values.single().quantity, 0.0)
        assertEquals(SyncState.SYNCED, database.pantryDao().get("user-1", added.id)?.syncState)
    }

    @Test
    fun aChangeLeftInFlightWhenTheAppWasClosedIsSentAgain() = runBlocking {
        addPantry()
        database.pendingOperationDao().update(
            waiting().single().copy(status = SyncOperationStatus.IN_FLIGHT)
        )

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(1, server.pantry.size)
    }

    @Test
    fun movingAShoppingItemToThePantryLeavesExactlyOnePantryItemEvenIfAReplyIsLost() = runBlocking {
        val seeded = server.seedShopping("Eggs")
        shopping.getShoppingItems()
        server.lostReplies = 1
        shopping.transferToPantry(seeded.id, "Eggs", 12.0, "pcs", "Dairy", null)

        assertEquals(SyncProcessor.Outcome.RETRY, sync())
        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(1, server.pantry.size)
        assertTrue(server.shopping.isEmpty())
        val row = database.pantryDao().getAll("user-1").single()
        assertEquals(server.pantry.values.single().id, row.serverId)
        assertEquals(SyncState.SYNCED, row.syncState)
    }

    @Test
    fun aTransferForAnItemTheServerHasNotSeenYetStillFindsIt() = runBlocking {
        val added = shopping.addShoppingItem("Eggs", 12.0, "pcs").getOrThrow()
        database.pendingOperationDao().update(
            waiting().single().copy(status = SyncOperationStatus.IN_FLIGHT)
        )
        shopping.transferToPantry(added.id, "Eggs", 12.0, "pcs", "Dairy", null)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(1, server.pantry.size)
        assertTrue(server.shopping.isEmpty())
    }

    @Test
    fun aRejectedTransferMarksTheNewPantryItemAsFailed() = runBlocking {
        val seeded = server.seedShopping("Eggs")
        shopping.getShoppingItems()
        server.rejectName = "Eggs"
        val moved = shopping.transferToPantry(seeded.id, "Eggs", 12.0, "pcs", "Dairy", null)
            .getOrThrow()

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(SyncState.FAILED, database.pantryDao().get("user-1", moved.id)?.syncState)
        assertTrue(server.pantry.isEmpty())
    }

    @Test
    fun tickingOffAShoppingItemOfflineUpdatesTheServerLater() = runBlocking {
        val seeded = server.seedShopping("Bread")
        shopping.getShoppingItems()
        shopping.setPurchased(seeded.id, true)

        assertEquals(SyncProcessor.Outcome.DONE, sync())

        assertEquals(true, server.shopping.getValue(seeded.id).isPurchased)
    }

    @Test
    fun onlyTheSignedInAccountsChangesAreSent() = runBlocking {
        addPantry("Mine")
        userId = "user-2"
        addPantry("Theirs")

        assertEquals(SyncProcessor.Outcome.DONE, sync("user-1"))

        assertEquals(listOf("Mine"), server.pantry.values.map { it.ingredientName })
        assertEquals(1, waiting("user-2").size)
    }

    @Test
    fun stoppingTheWorkMidSendPutsTheChangeBackInTheQueue() = runBlocking {
        addPantry()
        server.delayMs = 10_000

        val job = launch(Dispatchers.Default) { sync() }
        delay(400)
        job.cancelAndJoin()

        val op = waiting().single()
        assertEquals(SyncOperationStatus.PENDING, op.status)
    }

    @Test
    fun theWaitBetweenAttemptsGrowsAndIsCapped() {
        assertEquals(30L, SyncPolicy.backoffSeconds(1))
        assertEquals(60L, SyncPolicy.backoffSeconds(2))
        assertEquals(120L, SyncPolicy.backoffSeconds(3))
        assertEquals(3_600L, SyncPolicy.backoffSeconds(50))
        assertEquals(30L, SyncPolicy.backoffSeconds(0))
    }
}
