package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.auth.AuthUser
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.feature.onboarding.OnboardingSelection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSyncTest {

    private class FakeUserRepository(
        var profileResult: Result<UserProfileDto>,
        var syncResult: Result<UserProfileDto> =
            Result.success(UserProfileDto())
    ) : UserRepository {

        var getProfileCalls = 0
        var syncCalls = 0
        var syncedDisplayName: String? = "unset"
        var syncedDietary: List<String>? = listOf("unset")
        var syncedAvoided: List<String>? = listOf("unset")

        override suspend fun syncUser(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ): Result<UserProfileDto> {
            syncCalls++
            syncedDisplayName = displayName
            syncedDietary = dietaryPreferences
            syncedAvoided = avoidedIngredients
            return syncResult
        }

        override suspend fun getProfile(): Result<UserProfileDto> {
            getProfileCalls++
            return profileResult
        }

        override suspend fun updateProfile(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ): Result<UserProfileDto> =
            error("not used")
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

    private fun notFound(): Result<UserProfileDto> =
        Result.failure(
            ApiException(
                ApiErrorType.NOT_FOUND,
                "Profile not found"
            )
        )

    @Test
    fun anExistingServerProfileWinsOverDifferentLocalChoices() = runBlocking {
        val serverProfile =
            profile(
                dietary = listOf("Halal"),
                avoided = listOf("Shellfish")
            )

        val repository = FakeUserRepository(
            profileResult = Result.success(serverProfile)
        )

        val result = repository.syncSignedInUser(user, local)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().created)
        assertEquals(serverProfile, result.getOrThrow().profile)
        assertEquals(1, repository.getProfileCalls)
        assertEquals(0, repository.syncCalls)
    }

    @Test
    fun aMissingServerProfileIsCreatedFromLocalOnboardingChoices() = runBlocking {
        val createdProfile =
            profile(
                dietary = listOf("Vegan"),
                avoided = listOf("Nuts")
            )

        val repository = FakeUserRepository(
            profileResult = notFound(),
            syncResult = Result.success(createdProfile)
        )

        val result = repository.syncSignedInUser(user, local)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().created)
        assertEquals(createdProfile, result.getOrThrow().profile)
        assertEquals("Test User", repository.syncedDisplayName)
        assertEquals(listOf("Vegan"), repository.syncedDietary)
        assertEquals(listOf("Nuts"), repository.syncedAvoided)
        assertEquals(1, repository.syncCalls)
    }

    @Test
    fun aMissingProfileCanBeCreatedWithoutLocalChoices() = runBlocking {
        val createdProfile = profile(emptyList(), emptyList())

        val repository = FakeUserRepository(
            profileResult = notFound(),
            syncResult = Result.success(createdProfile)
        )

        val result = repository.syncSignedInUser(user, null)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().created)
        assertNull(repository.syncedDietary)
        assertNull(repository.syncedAvoided)
    }

    @Test
    fun aBlankDisplayNameIsNotSentWhenCreatingTheProfile() = runBlocking {
        val repository = FakeUserRepository(
            profileResult = notFound(),
            syncResult = Result.success(profile(emptyList(), emptyList()))
        )

        repository.syncSignedInUser(
            user.copy(displayName = "  "),
            local
        )

        assertNull(repository.syncedDisplayName)
    }

    @Test
    fun aProfileLookupFailureOtherThanNotFoundIsReturned() = runBlocking {
        val failure =
            ApiException(
                ApiErrorType.NO_CONNECTION,
                "offline"
            )

        val repository = FakeUserRepository(
            profileResult = Result.failure(failure)
        )

        val result = repository.syncSignedInUser(user, local)

        assertEquals(failure, result.exceptionOrNull())
        assertEquals(0, repository.syncCalls)
    }

    @Test
    fun aFailedProfileCreationIsReturned() = runBlocking {
        val failure =
            ApiException(
                ApiErrorType.SERVER,
                "server error"
            )

        val repository = FakeUserRepository(
            profileResult = notFound(),
            syncResult = Result.failure(failure)
        )

        val result = repository.syncSignedInUser(user, local)

        assertEquals(failure, result.exceptionOrNull())
        assertEquals(1, repository.syncCalls)
    }

    @Test
    fun anEmptyServerDietaryListBecomesNoRestrictionsLocally() {
        val selection =
            profile(
                dietary = emptyList(),
                avoided = listOf("Soy")
            ).toOnboardingSelection()

        assertEquals(
            setOf("No restrictions"),
            selection.dietaryPreferences
        )
        assertEquals(
            setOf("Soy"),
            selection.avoidedIngredients
        )
    }
}
