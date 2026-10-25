package com.example.kitchenquest.data.shopping

import androidx.room.withTransaction
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PantryItemEntity
import com.example.kitchenquest.data.local.ShoppingItemEntity
import com.example.kitchenquest.data.local.toDto
import com.example.kitchenquest.data.local.toEntity
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.network.apiCall
import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.sync.ClientIds
import com.example.kitchenquest.data.sync.NoOpSyncScheduler
import com.example.kitchenquest.data.sync.OperationQueue
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationType
import com.example.kitchenquest.data.sync.SyncScheduler
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.withTimeoutOrNull

// The shopping list as the screens see it. It reads from the phone, saves changes on the
// phone straight away and queues them for the server, so it keeps working without a
// connection. See the sync contract for the rules it follows.
class OfflineShoppingRepository(
    private val database: KitchenQuestDatabase,
    private val shoppingApi: ShoppingApi,
    private val currentUserId: () -> String?,
    private val scheduler: SyncScheduler = NoOpSyncScheduler,
    private val refreshTimeoutMs: Long = REFRESH_TIMEOUT_MS,
    private val firstLoadTimeoutMs: Long = FIRST_LOAD_TIMEOUT_MS,
    private val queue: OperationQueue = OperationQueue(database.pendingOperationDao())
) : ShoppingRepository {

    private val dao get() = database.shoppingDao()

    override suspend fun getShoppingItems(): Result<List<ShoppingItemDto>> {
        val userId = currentUserId() ?: return signedOut()

        if (database.pendingOperationDao().getOutstanding(userId).isNotEmpty()) {
            scheduler.requestSync()
        }

        val timeout = if (dao.getAll(userId).isEmpty()) firstLoadTimeoutMs else refreshTimeoutMs
        val fetched = withTimeoutOrNull(timeout) { apiCall { shoppingApi.getShoppingItems() } }
        fetched?.onSuccess { remote -> reconcile(userId, remote) }

        return Result.success(dao.getAll(userId).map { it.toDto() })
    }

    override suspend fun addShoppingItem(
        name: String,
        quantity: Double?,
        unit: String?
    ): Result<ShoppingItemDto> {
        val userId = currentUserId() ?: return signedOut()

        val id = ClientIds.newId()
        val entity = ShoppingItemEntity(
            userId = userId,
            id = id,
            ingredientName = name,
            quantity = quantity,
            unit = unit.orEmpty(),
            syncState = SyncState.PENDING
        )
        val request = CreateShoppingItemRequest(
            clientId = id,
            ingredientName = name,
            quantity = quantity,
            unit = unit
        )

        database.withTransaction {
            dao.upsert(entity)
            queue.enqueue(
                userId,
                SyncEntity.SHOPPING_ITEM,
                SyncOperationType.CREATE,
                id,
                ApiClient.json.encodeToString(CreateShoppingItemRequest.serializer(), request)
            )
        }
        scheduler.requestSync()

        return Result.success(entity.toDto())
    }

    override suspend fun setPurchased(id: String, purchased: Boolean): Result<ShoppingItemDto> {
        val userId = currentUserId() ?: return signedOut()

        val result = database.withTransaction {
            val current = dao.get(userId, id)
                ?: return@withTransaction notFound()

            if (current.isPurchased == purchased) {
                return@withTransaction Result.success(current.toDto())
            }

            val changed = current.copy(isPurchased = purchased, syncState = SyncState.PENDING)
            dao.upsert(changed)
            queue.enqueue(
                userId,
                SyncEntity.SHOPPING_ITEM,
                SyncOperationType.UPDATE,
                id,
                ApiClient.json.encodeToString(
                    UpdateShoppingItemRequest.serializer(),
                    UpdateShoppingItemRequest(isPurchased = purchased)
                ),
                serverId = current.serverId
            )
            Result.success(changed.toDto())
        }

        if (result.isSuccess) scheduler.requestSync()
        return result
    }

    override suspend fun deleteShoppingItem(id: String): Result<Unit> {
        val userId = currentUserId() ?: return signedOut()

        database.withTransaction {
            // Deleting something that is already gone is not an error.
            val current = dao.get(userId, id) ?: return@withTransaction

            dao.delete(userId, id)
            queue.enqueue(
                userId,
                SyncEntity.SHOPPING_ITEM,
                SyncOperationType.DELETE,
                id,
                "{}",
                serverId = current.serverId
            )
        }
        scheduler.requestSync()

        return Result.success(Unit)
    }

    // Moves a bought item into the pantry as one step. On the phone the pantry item is
    // added and the shopping item removed in a single transaction. One request is queued
    // for the server, which can safely be sent again (sync contract, section 7).
    override suspend fun transferToPantry(
        id: String,
        ingredientName: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ): Result<PantryItemDto> {
        val userId = currentUserId() ?: return signedOut()

        val result = database.withTransaction {
            val shopping = dao.get(userId, id)
                ?: return@withTransaction notFound()

            val pantryId = ClientIds.newId()
            val pantryEntity = PantryItemEntity(
                userId = userId,
                id = pantryId,
                ingredientName = ingredientName,
                quantity = quantity,
                unit = unit,
                category = category,
                expiryDate = expiryDate,
                syncState = SyncState.PENDING
            )
            val pantryRequest = CreatePantryItemRequest(
                clientId = pantryId,
                ingredientName = ingredientName,
                quantity = quantity,
                unit = unit,
                category = category,
                expiryDate = expiryDate
            )

            database.pantryDao().upsert(pantryEntity)
            dao.delete(userId, id)

            // Use the server id when there is one. Otherwise the server finds the item by
            // the client id it was created with.
            val transfer = TransferToPantryRequest(
                shoppingItemId = shopping.serverId,
                shoppingClientId = if (shopping.serverId == null) id else null,
                pantryItem = pantryRequest
            )
            val queued = queue.enqueue(
                userId,
                SyncEntity.SHOPPING_ITEM,
                SyncOperationType.TRANSFER_TO_PANTRY,
                id,
                ApiClient.json.encodeToString(TransferToPantryRequest.serializer(), transfer),
                serverId = shopping.serverId
            )

            if (queued == OperationQueue.Result.CANCELLED) {
                // The shopping item never reached the server, so just create the pantry item.
                queue.enqueue(
                    userId,
                    SyncEntity.PANTRY_ITEM,
                    SyncOperationType.CREATE,
                    pantryId,
                    ApiClient.json.encodeToString(
                        CreatePantryItemRequest.serializer(),
                        pantryRequest
                    )
                )
            }

            Result.success(pantryEntity.toDto())
        }

        if (result.isSuccess) scheduler.requestSync()
        return result
    }

    // Brings the phone up to date with the server without touching anything that still
    // has changes waiting to be sent (sync contract, section 8).
    private suspend fun reconcile(userId: String, remote: List<ShoppingItemDto>) {
        database.withTransaction {
            val busy = database.pendingOperationDao()
                .getBusyEntityIds(userId, SyncEntity.SHOPPING_ITEM)
                .toSet()
            val local = dao.getAll(userId)
            val byId = local.associateBy { it.id }
            val byServerId = local.filter { it.serverId != null }.associateBy { it.serverId }

            val seen = mutableSetOf<String>()
            val updates = mutableListOf<ShoppingItemEntity>()

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
        ApiException(ApiErrorType.UNAUTHORIZED, "Sign in to see your shopping list.")
    )

    private fun <T> notFound(): Result<T> = Result.failure(
        ApiException(ApiErrorType.NOT_FOUND, "That shopping item couldn't be found.")
    )

    companion object {
        const val REFRESH_TIMEOUT_MS = 8_000L
        const val FIRST_LOAD_TIMEOUT_MS = 45_000L
    }
}
