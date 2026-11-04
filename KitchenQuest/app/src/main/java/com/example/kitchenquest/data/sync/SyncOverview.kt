package com.example.kitchenquest.data.sync

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
    val reason: String?
)

interface SyncOverviewSource {
    fun observe(): Flow<SyncOverview>
    suspend fun failureReason(entity: SyncEntity, itemId: String): String?
}

// Used when there is nothing to report, such as in tests.
object NoSyncOverview : SyncOverviewSource {
    override fun observe(): Flow<SyncOverview> = flowOf(SyncOverview())
    override suspend fun failureReason(entity: SyncEntity, itemId: String): String? = null
}

class RoomSyncOverviewSource(
    private val database: KitchenQuestDatabase,
    private val connectivity: Flow<Boolean>,
    private val currentUserId: () -> String?
) : SyncOverviewSource {

    override fun observe(): Flow<SyncOverview> {
        val userId = currentUserId() ?: return flowOf(SyncOverview())
        return combine(
            connectivity,
            database.pendingOperationDao().observeWaitingCount(userId)
        ) { online, waiting -> SyncOverview(isOnline = online, waitingChanges = waiting) }
    }

    override suspend fun failureReason(entity: SyncEntity, itemId: String): String? {
        val userId = currentUserId() ?: return null
        return database.pendingOperationDao()
            .getForEntity(userId, entity, itemId)
            .lastOrNull { it.status == SyncOperationStatus.FAILED }
            ?.lastError
    }
}