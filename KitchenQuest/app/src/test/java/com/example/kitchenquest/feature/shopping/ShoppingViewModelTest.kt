
package com.example.kitchenquest.feature.shopping

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.ShoppingRepository
import com.example.kitchenquest.data.sync.NoSyncOverview
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncFailure
import com.example.kitchenquest.data.sync.SyncOverview
import com.example.kitchenquest.data.sync.SyncOverviewSource
import com.example.kitchenquest.data.sync.SyncState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModelTest {

    // Behaves like the offline repository: a saved list that changes as items are saved.
    private class FakeShoppingRepository : ShoppingRepository {
        val saved = MutableStateFlow<List<ShoppingItemDto>>(emptyList())
        private var nextId = 1

        data class Transfer(
            val id: String,
            val name: String,
            val quantity: Double,
            val unit: String,
            val category: String,
            val expiryDate: String?
        )

        val transfers = mutableListOf<Transfer>()
        var deleteCalls = 0
        var transferFailure: Throwable? = null

        override fun observeShoppingItems(): Flow<List<ShoppingItemDto>> = saved

        override suspend fun getShoppingItems(): Result<List<ShoppingItemDto>> =
            Result.success(saved.value)

        override suspend fun addShoppingItem(
            name: String,
            quantity: Double?,
            unit: String?
        ): Result<ShoppingItemDto> {
            val item = ShoppingItemDto(
                id = "item-${nextId++}",
                ingredientName = name,
                quantity = quantity,
                unit = unit.orEmpty(),
                syncState = SyncState.PENDING
            )
            saved.value = saved.value + item
            return Result.success(item)
        }

        override suspend fun setPurchased(
            id: String,
            purchased: Boolean
        ): Result<ShoppingItemDto> {
            val updated = saved.value.first { it.id == id }.copy(isPurchased = purchased)
            saved.value = saved.value.map {
                if (it.id == id) updated else it
            }
            return Result.success(updated)
        }

        override suspend fun deleteShoppingItem(id: String): Result<Unit> {
            deleteCalls++
            saved.value = saved.value.filterNot { it.id == id }
            return Result.success(Unit)
        }

        override suspend fun transferToPantry(
            id: String,
            ingredientName: String,
            quantity: Double,
            unit: String,
            category: String,
            expiryDate: String?
        ): Result<PantryItemDto> {
            transferFailure?.let { return Result.failure(it) }

            transfers += Transfer(
                id,
                ingredientName,
                quantity,
                unit,
                category,
                expiryDate
            )

            saved.value = saved.value.filterNot { it.id == id }

            return Result.success(
                PantryItemDto(
                    id = "pantry-1",
                    ingredientName = ingredientName,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    syncState = SyncState.PENDING
                )
            )
        }
    }

    private val eggs = ShoppingItemDto(id = "eggs", ingredientName = "Eggs")

    private fun viewModel(repository: ShoppingRepository) =
        ShoppingViewModel(repository, NoSyncOverview)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savedItemsAreShownAtOnce() {
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(eggs)
        }
        val viewModel = viewModel(repository)

        viewModel.loadList()

        assertEquals(listOf(eggs), viewModel.uiState.value.items)
    }

    @Test
    fun anItemAddedOfflineAppearsAtOnceAndIsTrimmed() {
        val viewModel = viewModel(FakeShoppingRepository())
        viewModel.loadList()

        viewModel.addItem("  Eggs ")

        val item = viewModel.uiState.value.items.single()
        assertEquals("Eggs", item.ingredientName)
        assertEquals(SyncState.PENDING, item.syncState)
        assertEquals("Added Eggs.", viewModel.uiState.value.message)
    }

    @Test
    fun blankItemsAreIgnored() {
        val viewModel = viewModel(FakeShoppingRepository())
        viewModel.loadList()

        viewModel.addItem("   ")

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun tickingAnItemMarksItPurchased() {
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(eggs)
        }
        val viewModel = viewModel(repository)
        viewModel.loadList()

        viewModel.togglePurchased(eggs)

        assertTrue(viewModel.uiState.value.items.single().isPurchased)
    }

    @Test
    fun removingAnItemTakesItOffTheList() {
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(eggs)
        }
        val viewModel = viewModel(repository)
        viewModel.loadList()

        viewModel.removeItem("eggs")

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun clearingTheListEmptiesIt() {
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(
                eggs,
                ShoppingItemDto(id = "milk", ingredientName = "Milk")
            )
        }
        val viewModel = viewModel(repository)
        viewModel.loadList()

        viewModel.clearList()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals("Shopping list cleared.", viewModel.uiState.value.message)
    }

    @Test
    fun movingAnItemIsOneTransferThatUsesTheDefaults() {
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(eggs)
        }
        val viewModel = viewModel(repository)
        viewModel.loadList()

        viewModel.moveToPantry(eggs)

        assertEquals(
            listOf(
                FakeShoppingRepository.Transfer(
                    "eggs",
                    "Eggs",
                    1.0,
                    "unit",
                    "Uncategorised",
                    null
                )
            ),
            repository.transfers
        )

        // The old two-step code deleted the shopping item itself.
        // One transfer call must be enough.
        assertEquals(0, repository.deleteCalls)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertEquals(
            "Moved Eggs to My Kitchen.",
            viewModel.uiState.value.message
        )
    }

    @Test
    fun movingKeepsTheItemsQuantityAndUnit() {
        val dozen = eggs.copy(quantity = 12.0, unit = "pcs")
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(dozen)
        }
        val viewModel = viewModel(repository)
        viewModel.loadList()

        viewModel.moveToPantry(dozen)

        val transfer = repository.transfers.single()
        assertEquals(12.0, transfer.quantity, 0.0)
        assertEquals("pcs", transfer.unit)
    }

    @Test
    fun aFailedMoveLeavesTheItemOnTheList() {
        val repository = FakeShoppingRepository().apply {
            saved.value = listOf(eggs)
            transferFailure = IOException("Server unavailable")
        }
        val viewModel = viewModel(repository)
        viewModel.loadList()

        viewModel.moveToPantry(eggs)

        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(
            "Server unavailable",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun aFailedItemLoadsItsReasonForTheDialog() {
        val overview = object : SyncOverviewSource {
            override fun observe(): Flow<SyncOverview> =
                MutableStateFlow(
                    SyncOverview(
                        isOnline = false,
                        waitingChanges = 1
                    )
                )

            override suspend fun failureReason(
                entity: SyncEntity,
                itemId: String
            ): String? =
                if (entity == SyncEntity.SHOPPING_ITEM) {
                    "Item name is required"
                } else {
                    null
                }
        }

        val viewModel = ShoppingViewModel(FakeShoppingRepository(), overview)
        viewModel.loadList()

        viewModel.showFailure(eggs)

        assertEquals(
            SyncFailure("eggs", "Eggs", "Item name is required"),
            viewModel.uiState.value.failure
        )

        assertFalse(viewModel.uiState.value.sync.isOnline)
        assertEquals(1, viewModel.uiState.value.sync.waitingChanges)
    }
}
