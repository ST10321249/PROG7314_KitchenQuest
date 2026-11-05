package com.example.kitchenquest.data.sync

import androidx.room.withTransaction
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PendingOperationEntity
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.network.apiCall
import com.example.kitchenquest.data.network.toApiException
import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.pantry.PantryApi
import com.example.kitchenquest.data.pantry.UpdatePantryItemRequest
import com.example.kitchenquest.data.shopping.CreateShoppingItemRequest
import com.example.kitchenquest.data.shopping.ShoppingApi
import com.example.kitchenquest.data.shopping.TransferToPantryRequest
import com.example.kitchenquest.data.shopping.UpdateShoppingItemRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// Sends the changes waiting in the queue to the server, oldest first, and records what
// happened. The rules it follows are in the sync contract (section 5).
class SyncProcessor(
    private val database: KitchenQuestDatabase,
    private val pantryApi: PantryApi,
    private val shoppingApi: ShoppingApi,
    private val clock: () -> Long = System::currentTimeMillis
) {

    enum class Outcome {
        // Nothing is left to send.
        DONE,

        // Something could not be sent yet. Try again later.
        RETRY
    }

    private sealed interface Step {
        // The server has the change. The ids are filled in for creates.
        data class Done(val serverId: String? = null, val updatedAt: String? = null) : Step

        // The item was deleted on the server, so an edit to it is no longer needed.
        data object Gone : Step

        // Try again later. Some problems, such as having no connection, are not the
        // change's fault, so they do not count towards giving up on it.
        data class Retry(val error: ApiException, val countsAsAttempt: Boolean) : Step

        // The server will never accept this change.
        data class Failed(val message: String) : Step
    }

    private val operations get() = database.pendingOperationDao()

    suspend fun run(userId: String): Outcome {
        // A change left "in flight" means the app was closed while sending it. Sending it
        // again is safe because creates and deletes can be repeated.
        operations.resetInFlight()

        while (true) {
            val operation = operations.oldestPending(userId) ?: return Outcome.DONE

            val sending = operation.copy(status = SyncOperationStatus.IN_FLIGHT)
            operations.update(sending)

            val step = try {
                send(sending)
            } catch (cancelled: CancellationException) {
                // The work was stopped part way. Put the change back so it is not lost.
                withContext(NonCancellable) { operations.update(operation) }
                throw cancelled
            }

            when (step) {
                is Step.Done -> applySuccess(sending, step)
                Step.Gone -> applyGone(sending)
                is Step.Failed -> applyFailure(sending, step.message)
                is Step.Retry -> if (applyRetry(sending, step)) return Outcome.RETRY
            }
        }
    }

    private suspend fun send(op: PendingOperationEntity): Step {
        return when (op.entityType) {
            SyncEntity.PANTRY_ITEM -> sendPantry(op)
            SyncEntity.SHOPPING_ITEM -> sendShopping(op)
        }
    }

    private suspend fun sendPantry(op: PendingOperationEntity): Step {
        return when (op.operationType) {
            SyncOperationType.CREATE -> {
                val request = ApiClient.json.decodeFromString(
                    CreatePantryItemRequest.serializer(),
                    op.payload
                )
                apiCall { pantryApi.addPantryItem(request) }
                    .toStep(op) { Step.Done(it.id, it.updatedAt) }
            }

            SyncOperationType.UPDATE -> {
                val serverId = op.serverId ?: localServerId(op)
                    ?: return Step.Failed("This item has not reached the server yet.")
                val request = ApiClient.json.decodeFromString(
                    UpdatePantryItemRequest.serializer(),
                    op.payload
                )
                apiCall { pantryApi.updatePantryItem(serverId, request) }
                    .toStep(op) { Step.Done(it.id, it.updatedAt) }
            }

            SyncOperationType.DELETE -> {
                // With no server id the server never had the item, so there is nothing to delete.
                val serverId = op.serverId ?: return Step.Done()
                apiCall { pantryApi.deletePantryItem(serverId) }
                    .toStep(op) { Step.Done() }
            }

            SyncOperationType.TRANSFER_TO_PANTRY ->
                Step.Failed("Pantry items cannot be moved to the pantry.")
        }
    }

    private suspend fun sendShopping(op: PendingOperationEntity): Step {
        return when (op.operationType) {
            SyncOperationType.CREATE -> {
                val request = ApiClient.json.decodeFromString(
                    CreateShoppingItemRequest.serializer(),
                    op.payload
                )
                apiCall { shoppingApi.addShoppingItem(request) }
                    .toStep(op) { Step.Done(it.id, it.updatedAt) }
            }

            SyncOperationType.UPDATE -> {
                val serverId = op.serverId ?: localServerId(op)
                    ?: return Step.Failed("This item has not reached the server yet.")
                val request = ApiClient.json.decodeFromString(
                    UpdateShoppingItemRequest.serializer(),
                    op.payload
                )
                apiCall { shoppingApi.updateShoppingItem(serverId, request) }
                    .toStep(op) { Step.Done(it.id, it.updatedAt) }
            }

            SyncOperationType.DELETE -> {
                val serverId = op.serverId ?: return Step.Done()
                apiCall { shoppingApi.deleteShoppingItem(serverId) }
                    .toStep(op) { Step.Done() }
            }

            SyncOperationType.TRANSFER_TO_PANTRY -> {
                val request = ApiClient.json.decodeFromString(
                    TransferToPantryRequest.serializer(),
                    op.payload
                )
                apiCall { shoppingApi.transferToPantry(request) }
                    .toStep(op) { Step.Done(it.id, it.updatedAt) }
            }
        }
    }

    private suspend fun localServerId(op: PendingOperationEntity): String? {
        return when (op.entityType) {
            SyncEntity.PANTRY_ITEM -> database.pantryDao().get(op.userId, op.entityId)?.serverId
            SyncEntity.SHOPPING_ITEM -> database.shoppingDao().get(op.userId, op.entityId)?.serverId
        }
    }

    private fun <T> Result<T>.toStep(op: PendingOperationEntity, onSuccess: (T) -> Step): Step {
        return fold(
            onSuccess = onSuccess,
            onFailure = { classify(op, it.toApiException()) }
        )
    }

    private fun classify(op: PendingOperationEntity, error: ApiException): Step {
        return when (error.type) {
            // The sign-in has expired. Nothing is wrong with the change itself.
            ApiErrorType.UNAUTHORIZED -> Step.Retry(error, countsAsAttempt = false)

            // The phone dropped off the network part way. Not the change's fault.
            ApiErrorType.NO_CONNECTION -> Step.Retry(error, countsAsAttempt = false)

            ApiErrorType.INVALID_REQUEST -> Step.Failed(error.message.orEmpty())

            ApiErrorType.NOT_FOUND -> when (op.operationType) {
                // Already deleted, which is what was wanted.
                SyncOperationType.DELETE -> Step.Done()
                SyncOperationType.UPDATE -> Step.Gone
                // A create or transfer should never be "not found". It most likely means
                // the server has not been updated yet, so try again later.
                else -> Step.Retry(error, countsAsAttempt = true)
            }

            ApiErrorType.TIMEOUT,
            ApiErrorType.SERVER,
            ApiErrorType.UNKNOWN -> Step.Retry(error, countsAsAttempt = true)
        }
    }

    private suspend fun applySuccess(op: PendingOperationEntity, step: Step.Done) {
        database.withTransaction {
            operations.delete(op.sequence)

            when (op.operationType) {
                SyncOperationType.CREATE ->
                    recordServerCopy(op.userId, op.entityType, op.entityId, step)

                SyncOperationType.UPDATE ->
                    recordServerCopy(op.userId, op.entityType, op.entityId, step)

                SyncOperationType.DELETE -> Unit

                SyncOperationType.TRANSFER_TO_PANTRY -> {
                    val request = ApiClient.json.decodeFromString(
                        TransferToPantryRequest.serializer(),
                        op.payload
                    )
                    request.pantryItem.clientId?.let { pantryId ->
                        recordServerCopy(op.userId, SyncEntity.PANTRY_ITEM, pantryId, step)
                    }
                }
            }
        }
    }

    // Remembers the server's id and timestamp for an item, and tells any changes queued
    // behind its create what the server id is.
    private suspend fun recordServerCopy(
        userId: String,
        entityType: SyncEntity,
        entityId: String,
        step: Step.Done
    ) {
        val serverId = step.serverId

        if (serverId != null) {
            operations.fillServerId(userId, entityType, entityId, serverId)
        }

        val state = stateFor(userId, entityType, entityId)

        when (entityType) {
            SyncEntity.PANTRY_ITEM -> {
                val dao = database.pantryDao()
                dao.get(userId, entityId)?.let {
                    dao.upsert(
                        it.copy(
                            serverId = serverId ?: it.serverId,
                            updatedAt = step.updatedAt ?: it.updatedAt,
                            syncState = state
                        )
                    )
                }
            }

            SyncEntity.SHOPPING_ITEM -> {
                val dao = database.shoppingDao()
                dao.get(userId, entityId)?.let {
                    dao.upsert(
                        it.copy(
                            serverId = serverId ?: it.serverId,
                            updatedAt = step.updatedAt ?: it.updatedAt,
                            syncState = state
                        )
                    )
                }
            }
        }
    }

    // The item shows as pending while anything is still waiting for it, and as failed
    // if a change for it was rejected.
    private suspend fun stateFor(
        userId: String,
        entityType: SyncEntity,
        entityId: String
    ): SyncState {
        val remaining = operations.getForEntity(userId, entityType, entityId)
        return when {
            remaining.any { it.status == SyncOperationStatus.FAILED } -> SyncState.FAILED
            remaining.isNotEmpty() -> SyncState.PENDING
            else -> SyncState.SYNCED
        }
    }

    // The item no longer exists on the server, so the phone drops it too.
    private suspend fun applyGone(op: PendingOperationEntity) {
        database.withTransaction {
            operations.getForEntity(op.userId, op.entityType, op.entityId)
                .forEach { operations.delete(it.sequence) }

            when (op.entityType) {
                SyncEntity.PANTRY_ITEM -> database.pantryDao().delete(op.userId, op.entityId)
                SyncEntity.SHOPPING_ITEM -> database.shoppingDao().delete(op.userId, op.entityId)
            }
        }
    }

    private suspend fun applyFailure(op: PendingOperationEntity, message: String) {
        database.withTransaction {
            operations.update(
                op.copy(
                    status = SyncOperationStatus.FAILED,
                    attemptCount = op.attemptCount + 1,
                    lastError = message
                )
            )
            markFailed(op)
        }
    }

    // Returns true when the work should stop and be tried again later.
    private suspend fun applyRetry(op: PendingOperationEntity, step: Step.Retry): Boolean {
        val attempts = if (step.countsAsAttempt) op.attemptCount + 1 else op.attemptCount

        if (attempts >= SyncPolicy.MAX_ATTEMPTS) {
            // Stop holding up everything behind it.
            applyFailure(
                op.copy(attemptCount = attempts - 1),
                step.error.message.orEmpty().ifBlank { "The server could not be reached." }
            )
            return false
        }

        operations.update(
            op.copy(
                status = SyncOperationStatus.PENDING,
                attemptCount = attempts,
                lastError = step.error.message,
                nextAttemptAt = clock() + SyncPolicy.backoffSeconds(attempts) * 1_000
            )
        )
        return true
    }

    private suspend fun markFailed(op: PendingOperationEntity) {
        // A failed transfer leaves the new pantry item unsent, so that is the item to flag.
        val (entityType, entityId) = if (op.operationType == SyncOperationType.TRANSFER_TO_PANTRY) {
            val request = ApiClient.json.decodeFromString(
                TransferToPantryRequest.serializer(),
                op.payload
            )
            SyncEntity.PANTRY_ITEM to (request.pantryItem.clientId ?: return)
        } else {
            op.entityType to op.entityId
        }

        when (entityType) {
            SyncEntity.PANTRY_ITEM ->
                database.pantryDao().setSyncState(op.userId, entityId, SyncState.FAILED)

            SyncEntity.SHOPPING_ITEM ->
                database.shoppingDao().setSyncState(op.userId, entityId, SyncState.FAILED)
        }
    }
}
