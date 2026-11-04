package com.example.kitchenquest.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpiryAlertSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    private fun infos(): List<WorkInfo> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWork(ExpiryAlertScheduler.WORK_NAME)
        .get()

    @Test
    fun turningAlertsOnSchedulesOneDailyJob() {
        ExpiryAlertScheduler.schedule(context)

        val info = infos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
    }

    @Test
    fun schedulingTwiceStillLeavesOneJob() {
        ExpiryAlertScheduler.schedule(context)
        ExpiryAlertScheduler.schedule(context)

        assertEquals(1, infos().size)
    }

    @Test
    fun turningAlertsOffStopsTheJob() {
        ExpiryAlertScheduler.schedule(context)

        ExpiryAlertScheduler.cancel(context)

        assertEquals(WorkInfo.State.CANCELLED, infos().single().state)
    }

    @Test
    fun theJobOnlyStartsForAnAccountThatHasAlertsOn() {
        ExpiryAlertSettings(context).setEnabled("user-1", true)

        ExpiryAlertScheduler.scheduleIfEnabled(context, "user-2")
        assertEquals(0, infos().size)

        ExpiryAlertScheduler.scheduleIfEnabled(context, "user-1")
        assertEquals(1, infos().size)
    }
}