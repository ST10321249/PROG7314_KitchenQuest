package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.pantry.OfflinePantryRepository
import com.example.kitchenquest.data.shopping.OfflineShoppingRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Two phones signed in to the same account, sharing one server. These check that the
// conflict rules in docs/SYNC_CONTRACT.md (section 6) hold when both are used at once.
@RunWith(RobolectricTestRunner::class)
class TwoDeviceSyncTest {

    private class Phone(context: Context, server: FakeServer, userId: String) {
        val database: KitchenQuestDatabase =
            Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val pantry = OfflinePantryRepository(database, server, { userId })
        val shopping = OfflineShoppingRepository(database, server, { userId })
        private val processor = SyncProcessor(database, server, server)
        private val user = userId

        suspend fun sync() = processor.run(user)

        suspend fun items() = pantry.getPantryItems().getOrThrow()
    }

    private lateinit var server: FakeServer
    private lateinit var a: Phone
    private lateinit var b: Phone

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        server = FakeServer()
        a = Phone(context, server, "user-1")
        b = Phone(context, server, "user-1")
    }

    @After
    fun tearDown() {
        a.database.close()
        b.database.close()
    }

    @Test
    fun anItemAddedOnOnePhoneAppearsOnTheOther() = runBlocking {
        val added = a.pantry.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()
        a.sync()

        val seen = b.items().single()

        assertEquals(added.id, seen.id)
        assertEquals("Milk", seen.ingredientName)
        assertEquals(SyncState.SYNCED, seen.syncState)
    }

    @Test
    fun anEditOnOnePhoneReachesTheOtherAfterItRefreshes() = runBlocking {
        val added = a.pantry.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()
        a.sync()
        b.items()

        b.pantry.updatePantryItem(added.id, quantity = 6.0)
        b.sync()

        assertEquals(6.0, a.items().single().quantity, 0.0)
    }

    @Test
    fun editsToDifferentFieldsOfTheSameItemAreBothKept() = runBlocking {
        val added = a.pantry.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()
        a.sync()
        b.items()

        // Both phones are offline when they make their changes.
        a.pantry.updatePantryItem(added.id, quantity = 4.0)
        b.pantry.updatePantryItem(added.id, category = "Chilled")
        a.sync()
        b.sync()

        val onServer = server.pantry.values.single()
        assertEquals(4.0, onServer.quantity, 0.0)
        assertEquals("Chilled", onServer.category)
    }

    @Test
    fun whenBothEditTheSameFieldTheLastChangeToReachTheServerWins() = runBlocking {
        val added = a.pantry.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()
        a.sync()
        b.items()

        a.pantry.updatePantryItem(added.id, quantity = 2.0)
        b.pantry.updatePantryItem(added.id, quantity = 9.0)
        b.sync()
        a.sync()

        assertEquals(2.0, server.pantry.values.single().quantity, 0.0)
    }

    @Test
    fun aDeleteOnOnePhoneBeatsAnEditMadeOnTheOther() = runBlocking {
        val added = a.pantry.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()
        a.sync()
        b.items()

        b.pantry.deletePantryItem(added.id)
        b.sync()
        a.pantry.updatePantryItem(added.id, quantity = 5.0)

        assertEquals(SyncProcessor.Outcome.DONE, a.sync())

        assertTrue(server.pantry.isEmpty())
        assertTrue(a.items().isEmpty())
    }

    @Test
    fun bothPhonesAddingTheSameThingKeepsBothBecauseTheyAreDifferentItems() = runBlocking {
        a.pantry.addPantryItem("Rice", 1.0, "kg", "Grains", null)
        b.pantry.addPantryItem("Rice", 2.0, "kg", "Grains", null)
        a.sync()
        b.sync()

        assertEquals(2, server.pantry.size)
        assertEquals(2, a.items().size)
    }

    @Test
    fun aListOpenedOnOnePhoneWhileTheOtherHasUnsentChangesDoesNotLoseThem() = runBlocking {
        val added = a.pantry.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()
        a.sync()
        b.items()

        b.pantry.updatePantryItem(added.id, quantity = 7.0)
        b.pantry.addPantryItem("Eggs", 12.0, "pcs", "Dairy", null)

        // Opening the list refreshes from the server but must not undo the unsent changes.
        val listed = b.items().associateBy { it.ingredientName }

        assertEquals(7.0, listed.getValue("Milk").quantity, 0.0)
        assertEquals(SyncState.PENDING, listed.getValue("Eggs").syncState)
        assertNull(server.pantry.values.firstOrNull { it.ingredientName == "Eggs" })
    }

    @Test
    fun aShoppingItemBoughtOnOnePhoneDisappearsFromTheOthersList() = runBlocking {
        val seeded = server.seedShopping("Eggs")
        a.shopping.getShoppingItems()
        b.shopping.getShoppingItems()

        a.shopping.transferToPantry(seeded.id, "Eggs", 12.0, "pcs", "Dairy", null)
        a.sync()

        assertTrue(b.shopping.getShoppingItems().getOrThrow().isEmpty())
        assertEquals(listOf("Eggs"), b.items().map { it.ingredientName })
    }
}
