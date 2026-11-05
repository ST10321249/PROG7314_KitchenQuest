package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.R
import com.example.kitchenquest.data.UserMessages
import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.history.RecipeNoteDto
import com.example.kitchenquest.data.photos.DishPhotoReader
import com.example.kitchenquest.data.photos.PhotoRepository
import com.example.kitchenquest.data.photos.PreparedPhoto
import com.example.kitchenquest.data.recipes.RecipeDetailDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.data.recipes.RecipeStepDto
import com.example.kitchenquest.notifications.NoOpTimerAlarms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DishPhotoUploadTest {

    private class FakeRecipeRepository(val recipe: RecipeDetailDto) : RecipeRepository {
        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            Result.success(emptyList<RecipeSearchResultDto>())

        override suspend fun getRecipeById(id: String) = Result.success(recipe)

        override suspend fun getRecommendations() = Result.success(emptyList<RecipeRecommendationDto>())
    }

    private class FakeHistoryRepository : HistoryRepository {
        var fail = false
        var added = 0

        override suspend fun getHistory() = Result.success(emptyList<CookingHistoryDto>())

        override suspend fun addHistoryEntry(
            recipeSourceId: String,
            recipeTitle: String,
            servings: Int?,
            rating: Int?,
            difficultyFeedback: String?
        ): Result<CookingHistoryDto> {
            if (fail) return Result.failure(IllegalStateException("offline"))
            added++
            return Result.success(CookingHistoryDto(id = "entry-1", recipeSourceId = recipeSourceId, recipeTitle = recipeTitle))
        }

        override suspend fun getNote(recipeId: String) = Result.success<RecipeNoteDto?>(null)

        override suspend fun saveNote(recipeSourceId: String, note: String) =
            Result.success(RecipeNoteDto(recipeSourceId = recipeSourceId, note = note))
    }

    private class FakePhotoRepository : PhotoRepository {
        val uploads = mutableListOf<Pair<Int, String>>()
        val attached = mutableListOf<Pair<String, String>>()
        var failUpload = false

        override suspend fun upload(bytes: ByteArray, contentType: String): Result<String> {
            if (failUpload) return Result.failure(IllegalStateException("offline"))
            uploads += bytes.size to contentType
            return Result.success("photo-1")
        }

        override suspend fun attachToHistory(historyEntryId: String, photoFileId: String): Result<Unit> {
            attached += historyEntryId to photoFileId
            return Result.success(Unit)
        }

        override suspend fun download(photoFileId: String) = Result.success(ByteArray(0))
    }

    private class FakeReader : DishPhotoReader {
        var photo: PreparedPhoto? = PreparedPhoto(ByteArray(500), "image/jpeg")
        val read = mutableListOf<String>()

        override suspend fun read(uri: String): PreparedPhoto? {
            read += uri
            return photo
        }
    }

    private class RecordingMessages : UserMessages {
        val shown = mutableListOf<Int>()
        override fun show(message: Int) {
            shown += message
        }
    }

    private val recipe = RecipeDetailDto(
        recipeSourceId = "123",
        title = "Bobotie",
        steps = listOf(RecipeStepDto(1, "Bake"))
    )

    private lateinit var history: FakeHistoryRepository
    private lateinit var photos: FakePhotoRepository
    private lateinit var reader: FakeReader
    private lateinit var messages: RecordingMessages
    private lateinit var viewModel: CookingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        history = FakeHistoryRepository()
        photos = FakePhotoRepository()
        reader = FakeReader()
        messages = RecordingMessages()
        viewModel = CookingViewModel(
            recipeRepository = FakeRecipeRepository(recipe),
            historyRepository = history,
            sessionStore = NoOpCookingSessionStore,
            timerAlarms = NoOpTimerAlarms,
            photoRepository = photos,
            dishPhotoReader = reader,
            userMessages = messages
        )
        viewModel.loadRecipe("123")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun finish() = viewModel.completeCooking(rating = 5, difficulty = null, note = null) {}

    @Test
    fun aChosenPhotoIsUploadedAndLinkedToTheNewHistoryEntry() {
        viewModel.setDishPhoto("content://photos/1")

        finish()

        assertEquals(listOf("content://photos/1"), reader.read)
        assertEquals(listOf(500 to "image/jpeg"), photos.uploads)
        assertEquals(listOf("entry-1" to "photo-1"), photos.attached)
        assertEquals(listOf(R.string.dish_photo_saved), messages.shown)
    }

    @Test
    fun withoutAPhotoNothingIsUploaded() {
        finish()

        assertEquals(1, history.added)
        assertTrue(photos.uploads.isEmpty())
        assertTrue(messages.shown.isEmpty())
    }

    @Test
    fun aFailedUploadStillKeepsTheMealAndTellsTheUser() {
        photos.failUpload = true
        viewModel.setDishPhoto("content://photos/1")

        finish()

        assertEquals(1, history.added)
        assertTrue(photos.attached.isEmpty())
        assertEquals(listOf(R.string.dish_photo_upload_failed), messages.shown)
    }

    @Test
    fun anUnreadablePhotoIsNotUploaded() {
        reader.photo = null
        viewModel.setDishPhoto("content://photos/broken")

        finish()

        assertTrue(photos.uploads.isEmpty())
        assertEquals(listOf(R.string.dish_photo_unreadable), messages.shown)
    }

    @Test
    fun ifTheMealCannotBeSavedThePhotoIsNotUploaded() {
        history.fail = true
        viewModel.setDishPhoto("content://photos/1")

        finish()

        assertTrue(photos.uploads.isEmpty())
        assertEquals(listOf(R.string.dish_photo_upload_failed), messages.shown)
    }

    @Test
    fun finishingClearsTheChosenPhoto() {
        viewModel.setDishPhoto("content://photos/1")

        finish()

        assertNull(viewModel.uiState.value.dishPhotoUri)
    }

    @Test
    fun removingThePhotoBeforeFinishingMeansNothingIsUploaded() {
        viewModel.setDishPhoto("content://photos/1")
        viewModel.setDishPhoto(null)

        finish()

        assertTrue(photos.uploads.isEmpty())
    }
}