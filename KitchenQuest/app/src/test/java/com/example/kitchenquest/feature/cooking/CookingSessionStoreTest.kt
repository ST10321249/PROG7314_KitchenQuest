package com.example.kitchenquest.feature.cooking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.recipes.RecipeDetailDto
import com.example.kitchenquest.data.recipes.RecipeStepDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CookingSessionStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var signedInUser: String? = "user-1"
    private lateinit var store: SharedPreferencesCookingSessionStore

    private val session = CookingSession(
        recipe = RecipeDetailDto(
            recipeSourceId = "123",
            title = "Bobotie",
            steps = listOf(RecipeStepDto(1, "Fry the onions"), RecipeStepDto(2, "Bake"))
        ),
        currentStepIndex = 1,
        timers = listOf(
            CookingTimer(
                id = "t1",
                label = "Step 2",
                totalSeconds = 600,
                remainingSeconds = 600,
                isRunning = true,
                isFinished = false,
                sourceType = TimerSource.RECIPE,
                endsAtMillis = 5_000_000L
            )
        ),
        savedAtMillis = 4_400_000L
    )

    @Before
    fun setUp() {
        context.getSharedPreferences("kitchenquest_cooking_session", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = SharedPreferencesCookingSessionStore(context) { signedInUser }
    }

    @Test
    fun aSavedSessionIsReadBackExactly() {
        store.save(session)

        assertEquals(session, store.load())
    }

    @Test
    fun aSessionSurvivesANewStoreInstance() {
        store.save(session)

        val reopened = SharedPreferencesCookingSessionStore(context) { signedInUser }

        assertEquals(session, reopened.load())
    }

    @Test
    fun eachAccountHasItsOwnSession() {
        store.save(session)

        signedInUser = "user-2"

        assertNull(store.load())
    }

    @Test
    fun nothingIsSavedWhenNobodyIsSignedIn() {
        signedInUser = null
        store.save(session)
        signedInUser = "user-1"

        assertNull(store.load())
    }

    @Test
    fun clearingRemovesTheSession() {
        store.save(session)

        store.clear()

        assertNull(store.load())
    }

    @Test
    fun aDamagedSessionIsIgnoredInsteadOfCrashing() {
        context.getSharedPreferences("kitchenquest_cooking_session", Context.MODE_PRIVATE)
            .edit().putString("session_user-1", "{not json").commit()

        assertNull(store.load())
        assertNull(store.load())
    }
}