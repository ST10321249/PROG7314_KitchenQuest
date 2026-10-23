package com.example.kitchenquest.data.sync

import java.util.UUID

// The kinds of records that can be changed while offline and synced later.
enum class SyncEntity {
    PANTRY_ITEM,
    SHOPPING_ITEM
}

// A change the user made that still has to reach the server.
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,

    // Buying a shopping item and adding it to the pantry as one step.
    TRANSFER_TO_PANTRY
}

// Where a queued change is up to. Succeeded changes are removed from the queue
// once the server has confirmed them.
enum class SyncOperationStatus {
    PENDING,
    IN_FLIGHT,
    SUCCEEDED,
    FAILED
}

// What the screens can show next to a record.
enum class SyncState {
    SYNCED,
    PENDING,
    FAILED
}

object ClientIds {

    // Matches what the API accepts: 8-64 letters, numbers, - or _.
    private val validPattern = Regex("^[A-Za-z0-9_-]{8,64}$")

    // A record keeps this id for its whole life, even before the server knows about it.
    fun newId(): String = UUID.randomUUID().toString()

    fun isValid(id: String): Boolean = validPattern.matches(id)
}

object SyncPolicy {

    // After this many failed attempts a change stops being retried automatically.
    const val MAX_ATTEMPTS = 8

    const val INITIAL_BACKOFF_SECONDS = 30L
}
