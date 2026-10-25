package com.example.kitchenquest.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kitchenquest.data.sync.SyncOperationStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "migration-test.db"
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

    private fun createVersion1WithData() {
        val connection = helper.createDatabase(1)
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
                "'srv-1', '{}', 'PENDING', 2, 'timeout', 1000, 0)"
        )
        connection.close()
    }

    private fun SQLiteConnection.count(table: String): Int =
        prepare("SELECT COUNT(*) FROM $table").use {
            it.step()
            it.getLong(0).toInt()
        }

    @Test
    fun movingFromVersion1KeepsPantryItemsAndQueuedChanges() {
        createVersion1WithData()

        val db = helper.runMigrationsAndValidate(2, KitchenQuestDatabase.MIGRATIONS.toList())

        db.prepare("SELECT ingredientName, syncState FROM pantry_items").use {
            assertTrue(it.step())
            assertEquals("Milk", it.getText(0))
            assertEquals("PENDING", it.getText(1))
        }
        db.prepare("SELECT operationId, attemptCount, lastError FROM pending_operations").use {
            assertTrue(it.step())
            assertEquals("op-1", it.getText(0))
            assertEquals(2L, it.getLong(1))
            assertEquals("timeout", it.getText(2))
        }
        db.close()
    }

    @Test
    fun theTranslationCacheExistsAndWorksAfterMigrating() {
        createVersion1WithData()

        val db = helper.runMigrationsAndValidate(2, KitchenQuestDatabase.MIGRATIONS.toList())

        assertEquals(0, db.count("translation_cache"))
        db.execSQL("INSERT INTO translation_cache VALUES ('en', 'af', 'abc123', 'Melk', 5000)")
        assertEquals(1, db.count("translation_cache"))
        db.close()
    }

    @Test
    fun theRealDatabaseOpensAnOlderFileAndKeepsItsData() = runBlocking {
        createVersion1WithData()

        val database = Room.databaseBuilder(context, KitchenQuestDatabase::class.java, databaseName)
            .addMigrations(*KitchenQuestDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Milk", database.pantryDao().get("user-1", "item-1")?.ingredientName)
            val op = database.pendingOperationDao().getOutstanding("user-1").single()
            assertEquals(SyncOperationStatus.PENDING, op.status)
            assertEquals(2, op.attemptCount)
            assertEquals(0, database.translationCacheDao().count())
        } finally {
            database.close()
        }
    }

    // Raising the database version without writing a migration would delete users' offline
    // data and unsent changes. This fails until a migration exists for every version step.
    @Test
    fun everyVersionStepHasAMigration() {
        val database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java).build()
        val currentVersion = database.openHelper.readableDatabase.version
        database.close()

        var version = 1
        KitchenQuestDatabase.MIGRATIONS.sortedBy { it.startVersion }.forEach { migration ->
            assertEquals("Missing a migration from version $version", version, migration.startVersion)
            version = migration.endVersion
        }

        assertEquals("No migration reaches the current version", currentVersion, version)
    }
}
