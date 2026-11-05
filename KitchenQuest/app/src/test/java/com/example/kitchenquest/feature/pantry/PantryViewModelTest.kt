package com.example.kitchenquest.feature.pantry

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.sync.NoSyncOverview
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncFailure
import com.example.kitchenquest.data.sync.SyncOverview
import com.example.kitchenquest.data.sync.SyncOverviewSource
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    // Behaves like the offline repository: a saved list that changes as items are saved.
    // With live = false it has no stream, like the plain network repository.
    private class FakePantryRepository(private val live: Boolean = true) : PantryRepository {
        val saved = MutableStateFlow<List<PantryItemDto>>(emptyList())
        var refreshGate: CompletableDeferred<Unit>? = null
        var refreshFailure: Throwable? = null
        private var nextId = 1

        override fun observePantryItems(): Flow<List<PantryItemDto>> = if (live) saved else emptyFlow()

        override suspend fun getPantryItems(): Result<List<PantryItemDto>> {
            refreshGate?.await()
            return refreshFailure?.let { Result.failure(it) } ?: Result.success(saved.value)
        }

        override suspend fun addPantryItem(
            ingredientName: String,
            quantity: Double,
            unit: String,
            category: String,
            expiryDate: String?
        ): Result<PantryItemDto> {
            val item = PantryItemDto(
                id = "item-${nextId++}",
                ingredientName = ingredientName,
                quantity = quantity,
                unit = unit,
                category = category,
                expiryDate = expiryDate,
                syncState = SyncState.PENDING
            )
            saved.value = saved.value + item
            return Result.success(item)
        }

        override suspend fun updatePantryItem(
            id: String,
            ingredientName: String?,
            quantity: Double?,
            unit: String?,
            category: String?,
            expiryDate: String?
        ): Result<PantryItemDto> {
            val current = saved.value.first { it.id == id }
            val updated = current.copy(
                ingredientName = ingredientName ?: current.ingredientName,
                quantity = quantity ?: current.quantity,
                unit = unit ?: current.unit,
                category = category ?: current.category,
                expiryDate = expiryDate ?: current.expiryDate,
                syncState = SyncState.PENDING
            )
            saved.value = saved.value.map { if (it.id == id) updated else it }
            return Result.success(updated)
        }

        override suspend fun deletePantryItem(id: String): Result<Unit> {
            saved.value = saved.value.filterNot { it.id == id }
            return Result.success(Unit)
        }
    }

    private class FakeOverview : SyncOverviewSource {
        val overview = MutableStateFlow(SyncOverview())
        val reasons = mutableMapOf<String, String?>()

        override fun observe(): Flow<SyncOverview> = overview

        override suspend fun failureReason(
            entity: SyncEntity,
            itemId: String
        ): String? = reasons[itemId]
    }

    private val milk = PantryItemDto(
        id = "milk", ingredientName = "Milk", quantity = 1.0, unit = "l", category = "Dairy"
    )

    private fun viewModel(repository: PantryRepository) =
        PantryViewModel(repository, NoSyncOverview)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savedItemsAreShownWhileTheRefreshIsStillRunning() {
        val repository = FakePantryRepository().apply {
            saved.value = listOf(milk)
            refreshGate = CompletableDeferred()
        }
        val viewModel = viewModel(repository)

        viewModel.loadPantry()

        val waiting = viewModel.uiState.value
        assertEquals(listOf(milk), waiting.items)
        assertTrue(waiting.isLoading)
        assertFalse(waiting.showLoading)

        repository.refreshGate!!.complete(Unit)

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.hasRefreshed)
    }

    @Test
    fun anEmptyPantryShowsTheSpinnerOnlyUntilTheFirstRefreshFinishes() {
        val repository = FakePantryRepository().apply { refreshGate = CompletableDeferred() }
        val viewModel = viewModel(repository)

        viewModel.loadPantry()
        assertTrue(viewModel.uiState.value.showLoading)

        repository.refreshGate!!.complete(Unit)

        val state = viewModel.uiState.value
        assertFalse(state.showLoading)
        assertTrue(state.items.isEmpty())
        // An empty pantry with no connection is the normal empty state, not an error.
        assertNull(state.errorMessage)
    }

    @Test
    fun aFailedRefreshWithNothingSavedReportsTheError() {
        val repository = FakePantryRepository().apply {
            refreshFailure = IllegalStateException("Sign in to see your pantry.")
        }
        val viewModel = viewModel(repository)

        viewModel.loadPantry()

        assertEquals("Sign in to see your pantry.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.showLoading)
    }

    @Test
    fun anAddedItemAppearsAtOnceAsPending() {
        val viewModel = viewModel(FakePantryRepository())
        viewModel.loadPantry()

        viewModel.addItem("Eggs", 12.0, "pcs", "Dairy", null)

        val item = viewModel.uiState.value.items.single()
        assertEquals("Eggs", item.ingredientName)
        assertEquals(SyncState.PENDING, item.syncState)
    }

    @Test
    fun anEditedItemChangesInTheList() {
        val repository = FakePantryRepository().apply { saved.value = listOf(milk) }
        val viewModel = viewModel(repository)
        viewModel.loadPantry()

        viewModel.updateItem("milk", "Oat milk", 2.0, "l", "Dairy", null)

        val item = viewModel.uiState.value.items.single()
        assertEquals("Oat milk", item.ingredientName)
        assertEquals(2.0, item.quantity, 0.0)
        assertEquals(SyncState.PENDING, item.syncState)
    }

    @Test
    fun aDeletedItemLeavesTheList() {
        val repository = FakePantryRepository().apply { saved.value = listOf(milk) }
        val viewModel = viewModel(repository)
        viewModel.loadPantry()

        viewModel.deleteItem("milk")

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun markingAnItemFinishedRemovesIt() {
        val repository = FakePantryRepository().apply { saved.value = listOf(milk) }
        val viewModel = viewModel(repository)
        viewModel.loadPantry()

        viewModel.markFinished("milk")

        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun aRepositoryWithoutALiveListStillUpdatesTheScreen() {
        val repository = FakePantryRepository(live = false).apply { saved.value = listOf(milk) }
        val viewModel = viewModel(repository)
        viewModel.loadPantry()

        viewModel.addItem("Eggs", 12.0, "pcs", "Dairy", null)
        assertEquals(listOf("Milk", "Eggs"), viewModel.uiState.value.items.map { it.ingredientName })

        viewModel.deleteItem("milk")
        assertEquals(listOf("Eggs"), viewModel.uiState.value.items.map { it.ingredientName })
    }

    @Test
    fun theScreenKnowsWhenThePhoneIsOfflineAndHowManyChangesWait() {
        val overview = FakeOverview()
        val viewModel = PantryViewModel(FakePantryRepository(), overview)

        viewModel.loadPantry()

        overview.overview.value = SyncOverview(
            isOnline = false,
            waitingChanges = 3
        )

        assertFalse(viewModel.uiState.value.sync.isOnline)
        assertEquals(3, viewModel.uiState.value.sync.waitingChanges)
    }

    @Test
    fun tappingAFailedItemLoadsTheReasonForTheDialog() {
        val overview = FakeOverview().apply {
            reasons["milk"] = "Quantity must be greater than zero"
        }

        val viewModel = PantryViewModel(FakePantryRepository(), overview)

        viewModel.showFailure(milk.copy(syncState = SyncState.FAILED))

        assertEquals(
            SyncFailure(
                "milk",
                "Milk",
                "Quantity must be greater than zero"
            ),
            viewModel.uiState.value.failure
        )

        viewModel.dismissFailure()

        assertNull(viewModel.uiState.value.failure)
    }
}