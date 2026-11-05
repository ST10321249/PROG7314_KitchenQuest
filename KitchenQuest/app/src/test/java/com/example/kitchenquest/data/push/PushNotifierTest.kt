package com.example.kitchenquest.data.push

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.notifications.NotificationChannels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PushNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = PushNotifier(context)

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
    }

    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    private fun Notification.text(key: String) = extras.getCharSequence(key)?.toString()

    @Test
    fun aPushMessageIsShownOnThePushChannel() {
        assertTrue(notifier.show("KitchenQuest", "Push notifications are working.", mapOf("type" to "test")))

        val notification = posted().single()
        assertEquals(NotificationChannels.PUSH_UPDATES, notification.channelId)
        assertEquals("KitchenQuest", notification.text(Notification.EXTRA_TITLE))
        assertEquals("Push notifications are working.", notification.text(Notification.EXTRA_TEXT))
    }

    @Test
    fun aMissingTitleUsesTheAppName() {
        notifier.show(null, "Hello", emptyMap())

        assertEquals("KitchenQuest", posted().single().text(Notification.EXTRA_TITLE))
    }

    @Test
    fun aMyKitchenRouteOpensMyKitchenWhenTapped() {
        notifier.show("Expiring", "Milk expires soon", mapOf("route" to "my_kitchen"))

        val intent = shadowOf(posted().single().contentIntent).savedIntent
        assertTrue(intent.getBooleanExtra(NotificationChannels.EXTRA_OPEN_MY_KITCHEN, false))
        assertEquals("my_kitchen", intent.getStringExtra(NotificationChannels.DATA_ROUTE))
    }

    @Test
    fun aMessageWithoutARouteJustOpensTheApp() {
        notifier.show("Hi", "Body", emptyMap())

        val intent = shadowOf(posted().single().contentIntent).savedIntent
        assertFalse(intent.getBooleanExtra(NotificationChannels.EXTRA_OPEN_MY_KITCHEN, false))
    }

    @Test
    fun nothingIsShownWithoutNotificationPermission() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertFalse(notifier.show("Hi", "Body", emptyMap()))
        assertTrue(posted().isEmpty())
    }
}