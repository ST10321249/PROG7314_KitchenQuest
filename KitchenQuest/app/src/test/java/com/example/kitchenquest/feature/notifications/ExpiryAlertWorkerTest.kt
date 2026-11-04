package com.example.kitchenquest.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.local.PantryItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ExpiryAlertWorkerTest {

    private lateinit var context: Context
    private lateinit var database: KitchenQuestDatabase
    private val today = LocalDate.of(2026, 11, 3)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ExpiryAlertSettings(context).setEnabled("user-1", true)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun pantryItem(userId: String, name: String, expiry: String?) = PantryItemEntity(
        userId = userId,
        id = "$userId-$name",
        ingredientName = name,
        quantity = 1.0,
        unit = "pcs",
        category = "Dairy",
        expiryDate = expiry
    )

    private fun worker(signedIn: String? = "user-1"): ExpiryAlertWorker =
        TestListenableWorkerBuilder<ExpiryAlertWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker = ExpiryAlertWorker(appContext, workerParameters) {
                    ExpiryAlertNotifier(it, database, { signedIn }, today = { today })
                }
            })
            .build()

    private fun posted(): List<Notification> {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return shadowOf(manager).allNotifications
    }

    @Test
    fun anItemExpiringTomorrowPostsOneNotification() = runBlocking {
        database.pantryDao().upsert(pantryItem("user-1", "Milk", "2026-11-04"))

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val notification = posted().single()
        val text = notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()
        assertTrue(text.contains("Milk"))
        // Tapping it opens the app.
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun manyExpiringItemsStillPostASingleNotification() = runBlocking {
        database.pantryDao().upsert(pantryItem("user-1", "Milk", "2026-11-04"))
        database.pantryDao().upsert(pantryItem("user-1", "Chicken", "2026-11-03"))

        worker().doWork()

        assertEquals(1, posted().size)
    }

    @Test
    fun nothingIsPostedWhenAlertsAreTurnedOff() = runBlocking {
        ExpiryAlertSettings(context).setEnabled("user-1", false)
        database.pantryDao().upsert(pantryItem("user-1", "Milk", "2026-11-04"))

        worker().doWork()

        assertTrue(posted().isEmpty())
    }

    @Test
    fun nothingIsPostedWhenNothingExpiresSoon() = runBlocking {
        database.pantryDao().upsert(pantryItem("user-1", "Rice", "2026-12-25"))

        worker().doWork()

        assertTrue(posted().isEmpty())
    }

    @Test
    fun anotherAccountsItemsAreNeverUsed() = runBlocking {
        database.pantryDao().upsert(pantryItem("user-2", "Milk", "2026-11-04"))

        worker().doWork()

        assertTrue(posted().isEmpty())
    }

    @Test
    fun nothingIsPostedWhenNobodyIsSignedIn() = runBlocking {
        database.pantryDao().upsert(pantryItem("user-1", "Milk", "2026-11-04"))

        worker(signedIn = null).doWork()

        assertTrue(posted().isEmpty())
    }

    @Test
    fun nothingIsPostedWithoutThePermission() = runBlocking {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        database.pantryDao().upsert(pantryItem("user-1", "Milk", "2026-11-04"))

        worker().doWork()

        assertTrue(posted().isEmpty())
    }
}