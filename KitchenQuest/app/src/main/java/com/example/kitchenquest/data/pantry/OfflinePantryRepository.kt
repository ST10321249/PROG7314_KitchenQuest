package com.example.kitchenquest.data.pantry

import androidx.room.withTransaction
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PantryItemEntity
import com.example.kitchenquest.data.local.toDto
import com.example.kitchenquest.data.local.toEntity
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.network.apiCall
import com.example.kitchenquest.data.sync.ClientIds
import com.example.kitchenquest.data.sync.NoOpSyncScheduler
import com.example.kitchenquest.data.sync.OperationQueue
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationType
import com.example.kitchenquest.data.sync.SyncScheduler
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.withTimeoutOrNull

// The pantry as the screens see it. It reads from the phone, saves changes on the phone
// straight away and queues them for the server, so it keeps working without a connection.
// See the sync contract for the rules it follows.
class OfflinePantryRepository(
    private val database: KitchenQuestDatabase,
    private val pantryApi: PantryApi,
    private val currentUserId: () -> String?,
    private val scheduler: SyncScheduler = NoOpSyncScheduler,
    private val refreshTimeoutMs: Long = REFRESH_TIMEOUT_MS,
    private val firstLoadTimeoutMs: Long = FIRST_LOAD_TIMEOUT_MS,
    private val queue: OperationQueue = OperationQueue(database.pendingOperationDao())
) : PantryRepository {

    private val dao get() = database.pantryDao()

    override suspend fun getPantryItems(): Result<List<PantryItemDto>> {
        val userId = currentUserId() ?: return signedOut()

        if (database.pendingOperationDao().getOutstanding(userId).isNotEmpty()) {
            scheduler.requestSync()
        }

        // The hosted API can take a long time to wake up. Wait longer only when there is
        // nothing on the phone to show in the meantime.
        val timeout = if (dao.getAll(userId).isEmpty()) firstLoadTimeoutMs else refreshTimeoutMs
        val fetched = withTimeoutOrNull(timeout) { apiCall { pantryApi.getPantryItems() } }
        fetched?.onSuccess { remote -> reconcile(userId, remote) }

        return Result.success(dao.getAll(userId).map { it.toDto() })
    }

    override suspend fun addPantryItem(
        ingredientName: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ): Result<PantryItemDto> {
        val userId = currentUserId() ?: return signedOut()

        val id = ClientIds.newId()
        val entity = PantryItemEntity(
            userId = userId,
            id = id,
            ingredientName = ingredientName,
            quantity = quantity,
            unit = unit,
            category = category,
            expiryDate = expiryDate,
            syncState = SyncState.PENDING
        )
        val request = CreatePantryItemRequest(
            clientId = id,
            ingredientName = ingredientName,
            quantity = quantity,
            unit = unit,
            category = category,
            expiryDate = expiryDate
        )

        database.withTransaction {
            dao.upsert(entity)
            queue.enqueue(
                userId,
                SyncEntity.PANTRY_ITEM,
                SyncOperationType.CREATE,
                id,
                ApiClient.json.encodeToString(CreatePantryItemRequest.serializer(), request)
            )
        }
        scheduler.requestSync()

        return Result.success(entity.toDto())
    }

    override suspend fun updatePantryItem(
        id: String,
        ingredientName: String?,
        quantity: Double?,
        unit: String?,
        category: String?,
        expiryDate: String?
    ): Result<PantryItemDto> {
        val userId = currentUserId() ?: return signedOut()

        val result = database.withTransaction {
            val current = dao.get(userId, id)
                ?: return@withTransaction notFound()

            val request = UpdatePantryItemRequest(
                ingredientName = ingredientName,
                quantity = quantity,
                unit = unit,
                category = category,
                expiryDate = expiryDate
            )
            val changed = current.copy(
                ingredientName = ingredientName ?: current.ingredientName,
                quantity = quantity ?: current.quantity,
                unit = unit ?: current.unit,
                category = category ?: current.category,
                expiryDate = expiryDate ?: current.expiryDate,
                syncState = SyncState.PENDING
            )

            if (changed.copy(syncState = current.syncState) == current) {
                return@withTransaction Result.success(current.toDto())
            }

            dao.upsert(changed)
            queue.enqueue(
                userId,
                SyncEntity.PANTRY_ITEM,
                SyncOperationType.UPDATE,
                id,
                ApiClient.json.encodeToString(UpdatePantryItemRequest.serializer(), request),
                serverId = current.serverId
            )
            Result.success(changed.toDto())
        }

        if (result.isSuccess) scheduler.requestSync()
        return result
    }

    override suspend fun deletePantryItem(id: String): Result<Unit> {
        val userId = currentUserId() ?: return signedOut()

        database.withTransaction {
            // Deleting something that is already gone is not an error.
            val current = dao.get(userId, id) ?: return@withTransaction

            dao.delete(userId, id)
            queue.enqueue(
                userId,
                SyncEntity.PANTRY_ITEM,
                SyncOperationType.DELETE,
                id,
                "{}",
                serverId = current.serverId
            )
        }
        scheduler.requestSync()

        return Result.success(Unit)
    }

    // Brings the phone up to date with the server without touching anything that still
    // has changes waiting to be sent (sync contract, section 8).
    private suspend fun reconcile(userId: String, remote: List<PantryItemDto>) {
        database.withTransaction {
            val busy = database.pendingOperationDao()
                .getBusyEntityIds(userId, SyncEntity.PANTRY_ITEM)
                .toSet()
            val local = dao.getAll(userId)
            val byId = local.associateBy { it.id }
            val byServerId = local.filter { it.serverId != null }.associateBy { it.serverId }

            val seen = mutableSetOf<String>()
            val updates = mutableListOf<PantryItemEntity>()

            remote.forEach { item ->
                val existing = byId[item.clientId ?: item.id] ?: byServerId[item.id]
                if (existing != null) seen.add(existing.id)

                val untouched = existing != null &&
                    (existing.id in busy || existing.syncState != SyncState.SYNCED)
                if (!untouched) {
                    updates.add(item.toEntity(userId, existing?.id ?: item.clientId ?: item.id))
                }
            }

            val gone = local
                .filter { it.syncState == SyncState.SYNCED && it.id !in busy && it.id !in seen }
                .map { it.id }

            dao.upsertAll(updates)
            if (gone.isNotEmpty()) dao.deleteAll(userId, gone)
        }
    }

    private fun <T> signedOut(): Result<T> = Result.failure(
        ApiException(ApiErrorType.UNAUTHORIZED, "Sign in to see your pantry.")
    )

    private fun <T> notFound(): Result<T> = Result.failure(
        ApiException(ApiErrorType.NOT_FOUND, "That pantry item couldn't be found.")
    )

    companion object {
        const val REFRESH_TIMEOUT_MS = 8_000L
        const val FIRST_LOAD_TIMEOUT_MS = 45_000L
    }
}
