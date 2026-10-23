package com.example.kitchenquest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncOperationStatus
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    @Query("SELECT * FROM pantry_items WHERE userId = :userId ORDER BY expiryDate IS NULL, expiryDate, ingredientName")
    suspend fun getAll(userId: String): List<PantryItemEntity>

    @Query("SELECT * FROM pantry_items WHERE userId = :userId ORDER BY expiryDate IS NULL, expiryDate, ingredientName")
    fun observeAll(userId: String): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE userId = :userId AND id = :id")
    suspend fun get(userId: String, id: String): PantryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PantryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PantryItemEntity>)

    @Query("UPDATE pantry_items SET syncState = :state WHERE userId = :userId AND id = :id")
    suspend fun setSyncState(userId: String, id: String, state: SyncState)

    @Query("DELETE FROM pantry_items WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("DELETE FROM pantry_items WHERE userId = :userId AND id IN (:ids)")
    suspend fun deleteAll(userId: String, ids: List<String>)
}

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items WHERE userId = :userId ORDER BY isPurchased, ingredientName")
    suspend fun getAll(userId: String): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_items WHERE userId = :userId ORDER BY isPurchased, ingredientName")
    fun observeAll(userId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE userId = :userId AND id = :id")
    suspend fun get(userId: String, id: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET syncState = :state WHERE userId = :userId AND id = :id")
    suspend fun setSyncState(userId: String, id: String, state: SyncState)

    @Query("DELETE FROM shopping_items WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("DELETE FROM shopping_items WHERE userId = :userId AND id IN (:ids)")
    suspend fun deleteAll(userId: String, ids: List<String>)
}

@Dao
interface PendingOperationDao {

    @Insert
    suspend fun insert(operation: PendingOperationEntity): Long

    @Update
    suspend fun update(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE sequence = :sequence")
    suspend fun delete(sequence: Long)

    // Everything for this user that has not finished, oldest change first.
    @Query("SELECT * FROM pending_operations WHERE userId = :userId AND status != 'SUCCEEDED' ORDER BY sequence")
    suspend fun getOutstanding(userId: String): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE userId = :userId AND status IN ('PENDING', 'IN_FLIGHT')")
    fun observeWaitingCount(userId: String): Flow<Int>

    // The oldest change that is ready to send. Changes are sent strictly in order, so
    // if the oldest waiting change has to wait for its next attempt, nothing newer
    // jumps ahead of it.
    @Query(
        """
        SELECT * FROM pending_operations
        WHERE userId = :userId AND status = 'PENDING'
        ORDER BY sequence
        LIMIT 1
        """
    )
    suspend fun oldestPending(userId: String): PendingOperationEntity?

    @Query("SELECT * FROM pending_operations WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId AND status != 'SUCCEEDED' ORDER BY sequence")
    suspend fun getForEntity(
        userId: String,
        entityType: SyncEntity,
        entityId: String
    ): List<PendingOperationEntity>

    // Ids of items that still have changes waiting, so a refresh does not overwrite them.
    @Query("SELECT DISTINCT entityId FROM pending_operations WHERE userId = :userId AND entityType = :entityType AND status IN ('PENDING', 'IN_FLIGHT')")
    suspend fun getBusyEntityIds(userId: String, entityType: SyncEntity): List<String>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE userId = :userId AND status = :status")
    suspend fun count(userId: String, status: SyncOperationStatus): Int

    // After the app was closed mid-send, a change left in flight is sent again.
    @Query("UPDATE pending_operations SET status = 'PENDING' WHERE status = 'IN_FLIGHT'")
    suspend fun resetInFlight()
}
