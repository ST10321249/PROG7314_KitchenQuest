package com.example.kitchenquest.feature.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.notifications.AndroidTimerAlarms
import com.example.kitchenquest.notifications.NotificationChannels
import com.example.kitchenquest.notifications.TimerAlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TimerAlarmsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private lateinit var alarms: AndroidTimerAlarms

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notificationManager.cancelAll()
        alarms = AndroidTimerAlarms(context)
    }

    private fun scheduledCount() = shadowOf(alarmManager).scheduledAlarms.size

    private fun finishedIntent(id: String, label: String) =
        Intent(context, TimerAlarmReceiver::class.java).apply {
            action = AndroidTimerAlarms.ACTION_TIMER_FINISHED
            putExtra(AndroidTimerAlarms.EXTRA_TIMER_ID, id)
            putExtra(AndroidTimerAlarms.EXTRA_TIMER_LABEL, label)
        }

    private fun postedNotifications(): List<Notification> =
        shadowOf(notificationManager).allNotifications

    @Test
    fun schedulingATimerSetsOneAlarm() {
        val before = scheduledCount()

        alarms.schedule("t1", "Rice", System.currentTimeMillis() + 60_000L)

        assertEquals(before + 1, scheduledCount())
    }

    @Test
    fun schedulingTheSameTimerAgainReplacesTheAlarm() {
        val before = scheduledCount()

        alarms.schedule("t1", "Rice", System.currentTimeMillis() + 60_000L)
        alarms.schedule("t1", "Rice", System.currentTimeMillis() + 120_000L)

        assertEquals(before + 1, scheduledCount())
    }

    @Test
    fun differentTimersGetTheirOwnAlarms() {
        val before = scheduledCount()

        alarms.schedule("t1", "Rice", System.currentTimeMillis() + 60_000L)
        alarms.schedule("t2", "Eggs", System.currentTimeMillis() + 60_000L)

        assertEquals(before + 2, scheduledCount())
    }

    @Test
    fun cancellingRemovesTheAlarm() {
        val before = scheduledCount()
        alarms.schedule("t1", "Rice", System.currentTimeMillis() + 60_000L)

        alarms.cancel("t1")

        assertEquals(before, scheduledCount())
    }

    @Test
    fun aFinishedTimerPostsANotificationOnTheTimerChannel() {
        TimerAlarmReceiver().onReceive(context, finishedIntent("t1", "Rice"))

        val notification = postedNotifications().single()
        assertEquals(NotificationChannels.TIMER_ALERTS, notification.channelId)
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        assertTrue(text.contains("Rice"))
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun twoFinishedTimersShowTwoNotifications() {
        TimerAlarmReceiver().onReceive(context, finishedIntent("t1", "Rice"))
        TimerAlarmReceiver().onReceive(context, finishedIntent("t2", "Eggs"))

        assertEquals(2, postedNotifications().size)
    }

    @Test
    fun nothingIsPostedWithoutNotificationPermission() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        TimerAlarmReceiver().onReceive(context, finishedIntent("t1", "Rice"))

        assertTrue(postedNotifications().isEmpty())
    }

    @Test
    fun anUnrelatedBroadcastIsIgnored() {
        TimerAlarmReceiver().onReceive(context, Intent("something.else"))

        assertTrue(postedNotifications().isEmpty())
    }
}