package com.example.kitchenquest.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Checks that moving from database version 2 to 3 (the recipe copies) keeps everything
// the user already had on the phone, including changes still waiting to sync.
@RunWith(RobolectricTestRunner::class)
class RecipeCacheMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "recipe-cache-migration-test.db"
    private val databaseFile = context.getDatabasePath(databaseName)

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        databaseFile,
        AndroidSQLiteDriver(),
        KitchenQuestDatabase::class,
        { KitchenQuestDatabase_Impl() }
    )

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    private fun createVersion2WithData() {
        val connection = helper.createDatabase(2)
        connection.execSQL(
            "INSERT INTO pantry_items (userId, id, serverId, ingredientName, quantity, unit, " +
                    "category, expiryDate, updatedAt, syncState) " +
                    "VALUES ('user-1', 'item-1', 'srv-1', 'Milk', 1.0, 'L', 'Dairy', " +
                    "'2026-11-01', NULL, 'PENDING')"
        )
        connection.execSQL(
            "INSERT INTO pending_operations (operationId, userId, entityType, operationType, " +
                    "entityId, serverId, payload, status, attemptCount, lastError, createdAt, " +
                    "nextAttemptAt) VALUES ('op-1', 'user-1', 'PANTRY_ITEM', 'UPDATE', 'item-1', " +
                    "'srv-1', '{}', 'PENDING', 1, NULL, 1000, 0)"
        )
        connection.execSQL("INSERT INTO translation_cache VALUES ('en', 'af', 'abc123', 'Melk', 5000)")
        connection.close()
    }

    private fun SQLiteConnection.count(table: String): Int =
        prepare("SELECT COUNT(*) FROM $table").use {
            it.step()
            it.getLong(0).toInt()
        }

    @Test
    fun movingToVersion3KeepsPantryQueueAndTranslations() {
        createVersion2WithData()

        val db = helper.runMigrationsAndValidate(3, KitchenQuestDatabase.MIGRATIONS.toList())

        db.prepare("SELECT ingredientName, syncState FROM pantry_items").use {
            assertTrue(it.step())
            assertEquals("Milk", it.getText(0))
            assertEquals("PENDING", it.getText(1))
        }
        assertEquals(1, db.count("pending_operations"))
        assertEquals(1, db.count("translation_cache"))
        db.close()
    }

    @Test
    fun theNewRecipeTablesStartEmptyAndCanBeUsed() {
        createVersion2WithData()

        val db = helper.runMigrationsAndValidate(3, KitchenQuestDatabase.MIGRATIONS.toList())

        assertEquals(0, db.count("cached_recipes"))
        assertEquals(0, db.count("cached_favourites"))
        db.execSQL("INSERT INTO cached_recipes VALUES ('user-1', '101', 'Bobotie', '{}', 5000)")
        db.execSQL("INSERT INTO cached_favourites VALUES ('user-1', '101', 'fav-1', 'Bobotie', NULL, 0)")
        assertEquals(1, db.count("cached_recipes"))
        assertEquals(1, db.count("cached_favourites"))
        db.close()
    }

    @Test
    fun theRealDatabaseOpensAVersion2FileAndCanStoreRecipes() = runBlocking {
        createVersion2WithData()

        val database = Room.databaseBuilder(context, KitchenQuestDatabase::class.java, databaseName)
            .addMigrations(*KitchenQuestDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Milk", database.pantryDao().get("user-1", "item-1")?.ingredientName)
            database.recipeCacheDao().putRecipe(
                CachedRecipeEntity("user-1", "101", "Bobotie", "{}", 5000)
            )
            assertEquals(1, database.recipeCacheDao().countRecipes("user-1"))
        } finally {
            database.close()
        }
    }
}