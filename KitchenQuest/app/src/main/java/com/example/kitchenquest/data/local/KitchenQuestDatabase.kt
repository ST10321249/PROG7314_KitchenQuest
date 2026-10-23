package com.example.kitchenquest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        PantryItemEntity::class,
        ShoppingItemEntity::class,
        PendingOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class KitchenQuestDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {

        private const val DATABASE_NAME = "kitchenquest.db"

        // Every schema change needs a migration here so users keep their offline
        // data and queued changes when they update the app. Never replace the
        // database with fallbackToDestructiveMigration: it would delete changes
        // that have not been sent yet.
        val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var instance: KitchenQuestDatabase? = null

        fun getInstance(context: Context): KitchenQuestDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KitchenQuestDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
