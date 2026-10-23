package com.example.kitchenquest.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.KitchenQuestApplication
import com.example.kitchenquest.data.pantry.OfflinePantryRepository
import com.example.kitchenquest.data.shopping.OfflineShoppingRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppRepositoriesTest {

    @Test
    fun theAppStartsWithItsOwnApplicationClass() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertTrue(context is KitchenQuestApplication)
    }

    @Test
    fun pantryAndShoppingScreensGetTheOfflineRepositoriesOnceTheAppHasStarted() {
        assertTrue(AppRepositories.pantry is OfflinePantryRepository)
        assertTrue(AppRepositories.shopping is OfflineShoppingRepository)
    }
}
