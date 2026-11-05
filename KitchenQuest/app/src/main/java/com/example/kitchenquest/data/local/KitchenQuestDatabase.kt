package com.example.kitchenquest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PantryItemEntity::class,
        ShoppingItemEntity::class,
        PendingOperationEntity::class,
        TranslationCacheEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class KitchenQuestDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {

        private const val DATABASE_NAME = "kitchenquest.db"

        // Every schema change needs a migration here so users keep their offline
        // data and queued changes when they update the app. Never replace the
        // database with fallbackToDestructiveMigration: it would delete changes
        // that have not been sent yet.
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

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

// Version 2 adds the translation cache. Nothing that was already stored is touched.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `translation_cache` (" +
                "`sourceLanguage` TEXT NOT NULL, " +
                "`targetLanguage` TEXT NOT NULL, " +
                "`textHash` TEXT NOT NULL, " +
                "`translatedText` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`sourceLanguage`, `targetLanguage`, `textHash`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_translation_cache_createdAt` " +
                "ON `translation_cache` (`createdAt`)"
        )
    }
}
