package com.example.kitchenquest.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationStatus
import com.example.kitchenquest.data.sync.SyncOperationType
import com.example.kitchenquest.data.sync.SyncState

// "id" is the local key described in the sync contract: the clientId, or the
// server id for items that were created before client ids existed. Rows are always
// looked up together with the userId so one account never sees data from another.
@Entity(
    tableName = "pantry_items",
    primaryKeys = ["userId", "id"],
    indices = [Index("userId", "serverId")]
)
data class PantryItemEntity(
    val userId: String,
    val id: String,
    val serverId: String? = null,
    val ingredientName: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val expiryDate: String? = null,
    val updatedAt: String? = null,
    val syncState: SyncState = SyncState.SYNCED
)

@Entity(
    tableName = "shopping_items",
    primaryKeys = ["userId", "id"],
    indices = [Index("userId", "serverId")]
)
data class ShoppingItemEntity(
    val userId: String,
    val id: String,
    val serverId: String? = null,
    val ingredientName: String,
    val quantity: Double? = null,
    val unit: String = "",
    val isPurchased: Boolean = false,
    val updatedAt: String? = null,
    val syncState: SyncState = SyncState.SYNCED
)

// One change waiting to be sent to the server. "sequence" gives the order the
// changes were made in, and survives the app being closed.
@Entity(
    tableName = "pending_operations",
    indices = [
        Index(value = ["operationId"], unique = true),
        Index("userId", "status", "nextAttemptAt")
    ]
)
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val sequence: Long = 0,
    val operationId: String,
    val userId: String,
    val entityType: SyncEntity,
    val operationType: SyncOperationType,
    // The local key of the item this change is about.
    val entityId: String,
    // The server id of that item, once known. Needed to delete or edit it through the API.
    val serverId: String? = null,
    // The fields to send, as JSON.
    val payload: String,
    val status: SyncOperationStatus = SyncOperationStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val nextAttemptAt: Long = 0
)

// A translation that was already worked out, so it can be shown again without the
// translation models and without a connection. Only the translated text is kept here.
// The original text always stays where it came from, and translations are for display only.
@Entity(
    tableName = "translation_cache",
    primaryKeys = ["sourceLanguage", "targetLanguage", "textHash"],
    indices = [Index("createdAt")]
)
data class TranslationCacheEntity(
    val sourceLanguage: String,
    val targetLanguage: String,
    // A fingerprint of the original text, so long recipe instructions are not used as a key.
    val textHash: String,
    val translatedText: String,
    val createdAt: Long
)
