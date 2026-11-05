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
        TranslationCacheEntity::class,
        CachedRecipeEntity::class,
        CachedFavouriteEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class KitchenQuestDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun translationCacheDao(): TranslationCacheDao

    abstract fun recipeCacheDao(): RecipeCacheDao

    companion object {

        private const val DATABASE_NAME = "kitchenquest.db"


        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

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


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cached_recipes` (" +
                    "`userId` TEXT NOT NULL, " +
                    "`recipeSourceId` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`recipeJson` TEXT NOT NULL, " +
                    "`savedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`userId`, `recipeSourceId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cached_recipes_userId_savedAt` " +
                    "ON `cached_recipes` (`userId`, `savedAt`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cached_favourites` (" +
                    "`userId` TEXT NOT NULL, " +
                    "`recipeSourceId` TEXT NOT NULL, " +
                    "`favouriteId` TEXT NOT NULL, " +
                    "`recipeTitle` TEXT NOT NULL, " +
                    "`imageUrl` TEXT, " +
                    "`listOrder` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`userId`, `recipeSourceId`))"
        )
    }
}