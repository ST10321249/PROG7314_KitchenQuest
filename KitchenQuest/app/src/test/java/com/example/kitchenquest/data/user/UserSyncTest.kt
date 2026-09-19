package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.auth.AuthUser
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.feature.onboarding.OnboardingSelection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSyncTest {

    private class FakeUserRepository(
        var syncResult: Result<UserProfileDto>,
        var updateResult: Result<UserProfileDto> = Result.success(UserProfileDto())
    ) : UserRepository {

        var syncedDisplayName: String? = "unset"
        var syncedDietary: List<String>? = listOf("unset")
        var syncedAvoided: List<String>? = listOf("unset")
        var updateCalls = 0
        var updatedDietary: List<String>? = null
        var updatedAvoided: List<String>? = null

        override suspend fun syncUser(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ): Result<UserProfileDto> {
            syncedDisplayName = displayName
            syncedDietary = dietaryPreferences
            syncedAvoided = avoidedIngredients
            return syncResult
        }

        override suspend fun getProfile(): Result<UserProfileDto> =
            error("not used")

        override suspend fun updateProfile(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ): Result<UserProfileDto> {
            updateCalls++
            updatedDietary = dietaryPreferences
            updatedAvoided = avoidedIngredients
            return updateResult
        }
    }

    private val user = AuthUser(
        uid = "uid-1",
        email = "test@example.com",
        displayName = "Test User"
    )

    private val local = OnboardingSelection(
        dietaryPreferences = setOf("Vegan"),
        avoidedIngredients = setOf("Nuts")
    )

    private fun profile(
        dietary: List<String>,
        avoided: List<String>
    ) = UserProfileDto(
        displayName = "Test User",
        email = "test@example.com",
        dietaryPreferences = dietary,
        avoidedIngredients = avoided
    )

    @Test
    fun aNewProfileIsCreatedFromTheLocalChoicesWithNoExtraUpdate() = runBlocking {
        val repository = FakeUserRepository(
            Result.success(profile(listOf("Vegan"), listOf("Nuts")))
        )

        val result = repository.syncSignedInUser(user, local)

        assertTrue(result.isSuccess)
        assertEquals("Test User", repository.syncedDisplayName)
        assertEquals(listOf("Vegan"), repository.syncedDietary)
        assertEquals(listOf("Nuts"), repository.syncedAvoided)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun aServerProfileThatDiffersIsUpdatedToMatchTheDevice() = runBlocking {
        val repository = FakeUserRepository(
            Result.success(profile(emptyList(), emptyList()))
        )

        repository.syncSignedInUser(user, local)

        assertEquals(1, repository.updateCalls)
        assertEquals(listOf("Vegan"), repository.updatedDietary)
        assertEquals(listOf("Nuts"), repository.updatedAvoided)
    }

    @Test
    fun theOrderOfChoicesDoesNotCauseAnUpdate() = runBlocking {
        val repository = FakeUserRepository(
            Result.success(profile(listOf("Halal", "Vegan"), listOf("Soy", "Nuts")))
        )

        repository.syncSignedInUser(
            user,
            OnboardingSelection(
                dietaryPreferences = setOf("Vegan", "Halal"),
                avoidedIngredients = setOf("Nuts", "Soy")
            )
        )

        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun withNoLocalChoicesOnlyTheSyncHappens() = runBlocking {
        val repository = FakeUserRepository(
            Result.success(profile(listOf("Vegetarian"), emptyList()))
        )

        repository.syncSignedInUser(user, null)

        assertNull(repository.syncedDietary)
        assertNull(repository.syncedAvoided)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun aBlankDisplayNameIsNotSent() = runBlocking {
        val repository = FakeUserRepository(
            Result.success(profile(emptyList(), emptyList()))
        )

        repository.syncSignedInUser(user.copy(displayName = "  "), null)

        assertNull(repository.syncedDisplayName)
    }

    @Test
    fun aFailedSyncIsReturnedAndNothingIsUpdated() = runBlocking {
        val failure = ApiException(ApiErrorType.NO_CONNECTION, "offline")
        val repository = FakeUserRepository(Result.failure(failure))

        val result = repository.syncSignedInUser(user, local)

        assertEquals(failure, result.exceptionOrNull())
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun aFailedUpdateIsReturned() = runBlocking {
        val failure = ApiException(ApiErrorType.SERVER, "down")
        val repository = FakeUserRepository(
            syncResult = Result.success(profile(emptyList(), emptyList())),
            updateResult = Result.failure(failure)
        )

        val result = repository.syncSignedInUser(user, local)

        assertEquals(failure, result.exceptionOrNull())
    }
}
