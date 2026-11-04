package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PendingOperationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncOverviewTest {

    private lateinit var database: KitchenQuestDatabase
    private var userId: String? = "user-1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun source(online: Boolean = true) =
        RoomSyncOverviewSource(database, flowOf(online), { userId })

    private suspend fun queue(
        user: String,
        operationId: String,
        status: SyncOperationStatus,
        entityId: String = "item",
        error: String? = null
    ) {
        database.pendingOperationDao().insert(
            PendingOperationEntity(
                operationId = operationId,
                userId = user,
                entityType = SyncEntity.PANTRY_ITEM,
                operationType = SyncOperationType.CREATE,
                entityId = entityId,
                payload = "{}",
                status = status,
                lastError = error,
                createdAt = 1
            )
        )
    }

    @Test
    fun waitingChangesOnlyCountThisAccountsUnsentChanges() = runBlocking {
        queue("user-1", "op-1", SyncOperationStatus.PENDING)
        queue("user-1", "op-2", SyncOperationStatus.FAILED)
        queue("user-2", "op-3", SyncOperationStatus.PENDING)

        // The failed change is not waiting, and the other account's change is not ours.
        assertEquals(1, source().observe().first().waitingChanges)
    }

    @Test
    fun aFailedChangeReportsTheServersReason() = runBlocking {
        queue("user-1", "op-1", SyncOperationStatus.FAILED, "milk", "Quantity must be greater than zero")

        assertEquals(
            "Quantity must be greater than zero",
            source().failureReason(SyncEntity.PANTRY_ITEM, "milk")
        )
        assertNull(source().failureReason(SyncEntity.PANTRY_ITEM, "eggs"))
    }

    @Test
    fun theOfflineStateComesFromTheConnection() = runBlocking {
        assertFalse(source(online = false).observe().first().isOnline)
    }

    @Test
    fun nobodySignedInHasNothingWaiting() = runBlocking {
        queue("user-1", "op-1", SyncOperationStatus.PENDING)
        userId = null

        assertEquals(0, source().observe().first().waitingChanges)
    }
}