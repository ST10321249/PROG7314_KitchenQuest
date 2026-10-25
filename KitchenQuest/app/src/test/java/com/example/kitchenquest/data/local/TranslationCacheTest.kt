package com.example.kitchenquest.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TranslationCacheTest {

    private lateinit var database: KitchenQuestDatabase
    private var now = 1_000L
    private lateinit var cache: TranslationCache

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cache = TranslationCache(database.translationCacheDao(), maxEntries = 3, clock = { now++ })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aTranslationThatWasNotSavedIsNotFound() = runBlocking {
        assertNull(cache.get("en", "af", "Chop the onions"))
    }

    @Test
    fun aSavedTranslationIsFoundAgain() = runBlocking {
        cache.put("en", "af", "Chop the onions", "Sny die uie")

        assertEquals("Sny die uie", cache.get("en", "af", "Chop the onions"))
    }

    @Test
    fun spacesAroundTheOriginalTextDoNotMatter() = runBlocking {
        cache.put("en", "af", "  Chop the onions ", "Sny die uie")

        assertEquals("Sny die uie", cache.get("en", "af", "Chop the onions"))
    }

    @Test
    fun differentLanguagePairsAreKeptApart() = runBlocking {
        cache.put("en", "af", "Salt", "Sout")
        cache.put("af", "en", "Salt", "Something else")

        assertEquals("Sout", cache.get("en", "af", "Salt"))
        assertEquals("Something else", cache.get("af", "en", "Salt"))
        assertNull(cache.get("en", "xx", "Salt"))
    }

    @Test
    fun savingTheSameTextAgainReplacesTheTranslation() = runBlocking {
        cache.put("en", "af", "Salt", "Sout")
        cache.put("en", "af", "Salt", "Sout (verbeter)")

        assertEquals("Sout (verbeter)", cache.get("en", "af", "Salt"))
        assertEquals(1, database.translationCacheDao().count())
    }

    @Test
    fun blankTextAndBlankTranslationsAreIgnored() = runBlocking {
        cache.put("en", "af", "   ", "x")
        cache.put("en", "af", "Salt", "  ")

        assertEquals(0, database.translationCacheDao().count())
        assertNull(cache.get("en", "af", "   "))
    }

    @Test
    fun theOldestTranslationsAreRemovedWhenTheCacheIsFull() = runBlocking {
        cache.put("en", "af", "one", "een")
        cache.put("en", "af", "two", "twee")
        cache.put("en", "af", "three", "drie")
        cache.put("en", "af", "four", "vier")
        cache.put("en", "af", "five", "vyf")

        assertEquals(3, database.translationCacheDao().count())
        assertNull(cache.get("en", "af", "one"))
        assertNull(cache.get("en", "af", "two"))
        assertEquals("drie", cache.get("en", "af", "three"))
        assertEquals("vyf", cache.get("en", "af", "five"))
    }

    @Test
    fun clearingRemovesEverything() = runBlocking {
        cache.put("en", "af", "Salt", "Sout")

        cache.clear()

        assertNull(cache.get("en", "af", "Salt"))
    }
}
