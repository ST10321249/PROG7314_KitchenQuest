package com.example.kitchenquest.data.sync

import androidx.room.withTransaction
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.shopping.TransferToPantryRequest

import com.example.kitchenquest.data.local.KitchenQuestDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

// What the screens show about syncing as a whole: is the phone online, and how many
// changes are still waiting to be sent.
data class SyncOverview(
    val isOnline: Boolean = true,
    val waitingChanges: Int = 0
)

// A change the server refused, with the reason it gave (if any).
data class SyncFailure(
    val itemId: String,
    val itemName: String,
    val reason: String?,
    val canRetryTransfer: Boolean = false
)

interface SyncOverviewSource {
    fun observe(): Flow<SyncOverview>
    suspend fun failureReason(entity: SyncEntity, itemId: String): String?
    suspend fun isFailedTransfer(pantryItemId: String): Boolean = false
    suspend fun retryFailedTransfer(pantryItemId: String): Boolean = false
}

// Used when there is nothing to report, such as in tests.
object NoSyncOverview : SyncOverviewSource {
    override fun observe(): Flow<SyncOverview> = flowOf(SyncOverview())
    override suspend fun failureReason(entity: SyncEntity, itemId: String): String? = null
}

class RoomSyncOverviewSource(
    private val database: KitchenQuestDatabase,
    private val connectivity: Flow<Boolean>,
    private val currentUserId: () -> String?,
    private val scheduler: SyncScheduler = NoOpSyncScheduler
) : SyncOverviewSource {

    override fun observe(): Flow<SyncOverview> {
        val userId = currentUserId() ?: return flowOf(SyncOverview())
        return combine(
            connectivity,
            database.pendingOperationDao().observeWaitingCount(userId)
        ) { online, waiting -> SyncOverview(isOnline = online, waitingChanges = waiting) }
    }

    private suspend fun failedTransfer(userId: String, pantryItemId: String) =
        database.pendingOperationDao().getOutstanding(userId)
            .firstOrNull { op ->
                if (op.operationType != SyncOperationType.TRANSFER_TO_PANTRY ||
                    op.status != SyncOperationStatus.FAILED) return@firstOrNull false
                try {
                    ApiClient.json.decodeFromString(TransferToPantryRequest.serializer(), op.payload)
                        .pantryItem.clientId == pantryItemId
                } catch (_: Exception) {
                    false
                }
            }

    override suspend fun isFailedTransfer(pantryItemId: String): Boolean {
        val userId = currentUserId() ?: return false
        return failedTransfer(userId, pantryItemId) != null
    }

    override suspend fun retryFailedTransfer(pantryItemId: String): Boolean {
        val userId = currentUserId() ?: return false
        val retried = database.withTransaction {
            val operation = failedTransfer(userId, pantryItemId) ?: return@withTransaction false
            database.pendingOperationDao().update(
                operation.copy(
                    status = SyncOperationStatus.PENDING,
                    attemptCount = 0,
                    nextAttemptAt = 0,
                    lastError = null
                )
            )
            database.pantryDao().setSyncState(userId, pantryItemId, SyncState.PENDING)
            true
        }
        if (retried) scheduler.requestSync()
        return retried
    }

    override suspend fun failureReason(entity: SyncEntity, itemId: String): String? {
        val userId = currentUserId() ?: return null
        if (entity == SyncEntity.PANTRY_ITEM) {
            failedTransfer(userId, itemId)?.let { return it.lastError }
        }
        return database.pendingOperationDao()
            .getForEntity(userId, entity, itemId)
            .lastOrNull { it.status == SyncOperationStatus.FAILED }
            ?.lastError
    }
}
