package com.example.kitchenquest.data.sync

import com.example.kitchenquest.data.local.PendingOperationDao
import com.example.kitchenquest.data.local.PendingOperationEntity
import com.example.kitchenquest.data.network.ApiClient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// Adds changes to the list of work still to be sent to the server and keeps that list
// as small and as safe as possible, following the sync contract (section 5).
//
// Callers should run enqueue inside the same Room transaction that changes the item,
// so a change to the data and its queue entry are saved together or not at all.
class OperationQueue(
    private val dao: PendingOperationDao,
    private val clock: () -> Long = System::currentTimeMillis
) {

    enum class Result {
        // A new entry was added to the queue.
        QUEUED,

        // The change was folded into an entry that had not been sent yet.
        MERGED,

        // The item never reached the server, so there is nothing left to send for it.
        CANCELLED
    }

    suspend fun enqueue(
        userId: String,
        entityType: SyncEntity,
        type: SyncOperationType,
        entityId: String,
        payload: String,
        serverId: String? = null
    ): Result {
        val outstanding = dao.getForEntity(userId, entityType, entityId)

        // A change that is being sent right now can no longer be altered.
        val editable = outstanding.filter { it.status != SyncOperationStatus.IN_FLIGHT }
        val unsentCreate = editable.firstOrNull { it.operationType == SyncOperationType.CREATE }

        return when (type) {
            SyncOperationType.CREATE -> {
                insert(userId, entityType, type, entityId, payload, serverId)
                Result.QUEUED
            }

            SyncOperationType.UPDATE -> {
                if (unsentCreate != null) {
                    // The server has not seen the item yet, so just change what will be created.
                    dao.update(restart(unsentCreate, mergePayload(unsentCreate.payload, payload)))
                    return Result.MERGED
                }

                val last = outstanding.lastOrNull()
                if (last != null &&
                    last.operationType == SyncOperationType.UPDATE &&
                    last.status != SyncOperationStatus.IN_FLIGHT
                ) {
                    dao.update(restart(last, mergePayload(last.payload, payload)))
                    return Result.MERGED
                }

                insert(userId, entityType, type, entityId, payload, serverId)
                Result.QUEUED
            }

            SyncOperationType.DELETE -> {
                // Edits to an item that is being deleted no longer matter.
                editable.forEach { dao.delete(it.sequence) }

                if (unsentCreate != null) {
                    Result.CANCELLED
                } else {
                    insert(userId, entityType, type, entityId, payload, serverId)
                    Result.QUEUED
                }
            }

            SyncOperationType.TRANSFER_TO_PANTRY -> {
                // The shopping item is going away, so waiting edits to it no longer matter.
                editable.forEach { dao.delete(it.sequence) }

                if (unsentCreate != null) {
                    // The server never saw the shopping item. The caller only needs to
                    // create the pantry item.
                    Result.CANCELLED
                } else {
                    insert(userId, entityType, type, entityId, payload, serverId)
                    Result.QUEUED
                }
            }
        }
    }

    private suspend fun insert(
        userId: String,
        entityType: SyncEntity,
        type: SyncOperationType,
        entityId: String,
        payload: String,
        serverId: String?
    ) {
        dao.insert(
            PendingOperationEntity(
                operationId = ClientIds.newId(),
                userId = userId,
                entityType = entityType,
                operationType = type,
                entityId = entityId,
                serverId = serverId,
                payload = payload,
                createdAt = clock()
            )
        )
    }

    // Editing an entry that had failed is the user fixing it, so it is tried again.
    private fun restart(operation: PendingOperationEntity, payload: String) = operation.copy(
        payload = payload,
        status = SyncOperationStatus.PENDING,
        attemptCount = 0,
        lastError = null,
        nextAttemptAt = 0
    )

    // The newest value of each field wins.
    private fun mergePayload(existing: String, changes: String): String {
        val merged = ApiClient.json.parseToJsonElement(existing).jsonObject.toMutableMap()
        merged.putAll(ApiClient.json.parseToJsonElement(changes).jsonObject)
        return JsonObject(merged).toString()
    }
}
