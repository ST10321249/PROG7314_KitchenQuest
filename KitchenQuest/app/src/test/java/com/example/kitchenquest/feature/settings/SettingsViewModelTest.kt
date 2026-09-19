package com.example.kitchenquest.feature.settings

import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.user.UserProfileDto
import com.example.kitchenquest.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SettingsViewModelTest {

    private class FakeUserRepository : UserRepository {

        var getProfileResult: Result<UserProfileDto> = Result.success(profile())
        var syncResult: Result<UserProfileDto> = Result.success(profile())
        var updateResult: Result<UserProfileDto>? = null

        var getProfileCalls = 0
        var syncCalls = 0
        var updateCalls = 0
        var lastUpdate: Triple<String?, List<String>?, List<String>?>? = null

        override suspend fun syncUser(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ): Result<UserProfileDto> {
            syncCalls++
            return syncResult
        }

        override suspend fun getProfile(): Result<UserProfileDto> {
            getProfileCalls++
            return getProfileResult
        }

        override suspend fun updateProfile(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ): Result<UserProfileDto> {
            updateCalls++
            lastUpdate = Triple(displayName, dietaryPreferences, avoidedIngredients)

            return updateResult ?: Result.success(
                UserProfileDto(
                    displayName = displayName ?: "",
                    email = "test@example.com",
                    dietaryPreferences = dietaryPreferences ?: emptyList(),
                    avoidedIngredients = avoidedIngredients ?: emptyList()
                )
            )
        }
    }

    private lateinit var repository: FakeUserRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeUserRepository()
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun loadProfileFillsInTheState() {
        viewModel.loadProfile()

        assertFalse(state.isLoading)
        assertTrue(state.hasLoaded)
        assertEquals("Test User", state.displayName)
        assertEquals("test@example.com", state.email)
        assertEquals(setOf("Vegetarian"), state.dietaryPreferences)
        assertEquals(setOf("Peanuts"), state.avoidedIngredients)
        assertFalse(state.hasUnsavedChanges)
        assertNull(state.errorMessage)
    }

    @Test
    fun aMissingProfileIsCreatedThenLoaded() {
        repository.getProfileResult = Result.failure(
            ApiException(ApiErrorType.NOT_FOUND, "missing")
        )

        viewModel.loadProfile()

        assertEquals(1, repository.syncCalls)
        assertTrue(state.hasLoaded)
        assertEquals("Test User", state.displayName)
    }

    @Test
    fun aFailedLoadShowsTheErrorAndCanBeRetried() {
        repository.getProfileResult = Result.failure(
            ApiException(ApiErrorType.NO_CONNECTION, "Can't reach the server.")
        )

        viewModel.loadProfile()

        assertFalse(state.hasLoaded)
        assertFalse(state.isLoading)
        assertEquals("Can't reach the server.", state.errorMessage)
        assertEquals(0, repository.syncCalls)

        repository.getProfileResult = Result.success(profile())
        viewModel.loadProfile()

        assertTrue(state.hasLoaded)
        assertNull(state.errorMessage)
    }

    @Test
    fun editingMarksUnsavedChangesAndRevertingClearsThem() {
        viewModel.loadProfile()

        viewModel.onDisplayNameChange("New Name")
        assertTrue(state.hasUnsavedChanges)

        viewModel.onDisplayNameChange("Test User")
        assertFalse(state.hasUnsavedChanges)

        viewModel.onAvoidedIngredientsChange(setOf("Peanuts", "Shellfish"))
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun savingSendsTheEditsAndClearsUnsavedChanges() {
        viewModel.loadProfile()
        viewModel.onDisplayNameChange("  New Name  ")
        viewModel.onDietaryPreferencesChange(setOf("Vegan", "Gluten free"))
        viewModel.onAvoidedIngredientsChange(emptySet())

        viewModel.save()

        assertEquals(
            Triple("New Name", listOf("Vegan", "Gluten free"), emptyList<String>()),
            repository.lastUpdate
        )
        assertTrue(state.saveSucceeded)
        assertFalse(state.isSaving)
        assertFalse(state.hasUnsavedChanges)
        assertEquals("New Name", state.displayName)
        assertNull(state.errorMessage)
    }

    @Test
    fun aFailedSaveKeepsTheEditsAndShowsTheError() {
        viewModel.loadProfile()
        viewModel.onDisplayNameChange("New Name")
        repository.updateResult = Result.failure(
            ApiException(ApiErrorType.TIMEOUT, "Took too long.")
        )

        viewModel.save()

        assertFalse(state.saveSucceeded)
        assertFalse(state.isSaving)
        assertTrue(state.hasUnsavedChanges)
        assertEquals("New Name", state.displayName)
        assertEquals("Took too long.", state.errorMessage)
    }

    @Test
    fun aBlankDisplayNameIsRejectedWithoutCallingTheApi() {
        viewModel.loadProfile()
        viewModel.onDisplayNameChange("   ")

        viewModel.save()

        assertEquals(0, repository.updateCalls)
        assertEquals("Display name can't be empty.", state.errorMessage)
    }

    @Test
    fun savingWithNoChangesDoesNothing() {
        viewModel.loadProfile()

        viewModel.save()

        assertEquals(0, repository.updateCalls)
        assertFalse(state.saveSucceeded)
    }

    @Test
    fun editingClearsOldFeedbackAndClearFeedbackWorks() {
        viewModel.loadProfile()
        viewModel.onDisplayNameChange("New Name")
        viewModel.save()
        assertTrue(state.saveSucceeded)

        viewModel.clearFeedback()
        assertFalse(state.saveSucceeded)

        viewModel.onDisplayNameChange("Another")
        viewModel.save()
        viewModel.onDisplayNameChange("Third")
        assertFalse(state.saveSucceeded)
        assertNull(state.errorMessage)
    }

    private companion object {
        fun profile() = UserProfileDto(
            displayName = "Test User",
            email = "test@example.com",
            dietaryPreferences = listOf("Vegetarian"),
            avoidedIngredients = listOf("Peanuts")
        )
    }
}
