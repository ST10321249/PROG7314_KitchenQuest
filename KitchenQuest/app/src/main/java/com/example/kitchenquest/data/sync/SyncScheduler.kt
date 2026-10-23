package com.example.kitchenquest.data.sync

// Asks for the queued changes to be sent to the server when a connection is available.
fun interface SyncScheduler {
    fun requestSync()
}

object NoOpSyncScheduler : SyncScheduler {
    override fun requestSync() = Unit
}
