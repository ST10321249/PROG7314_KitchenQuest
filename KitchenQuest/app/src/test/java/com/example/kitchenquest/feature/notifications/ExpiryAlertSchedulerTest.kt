package com.example.kitchenquest.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun checkNowSchedulesOnlyOnePendingJob() {
        ExpiryAlertScheduler.checkNow(context)
        ExpiryAlertScheduler.checkNow(context)

        val info = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ExpiryAlertScheduler.ON_DEMAND_WORK_NAME)
            .get()

        // Robolectric's WorkManager test executor may run the one-time worker
        // immediately. It can already be SUCCEEDED when this query finishes,
        // so requiring exactly one ENQUEUED job is not reliable.
        assertTrue("The immediate expiry check must have been scheduled", info.isNotEmpty())
        val active = info.count {
            it.state == WorkInfo.State.ENQUEUED ||
                it.state == WorkInfo.State.RUNNING ||
                it.state == WorkInfo.State.BLOCKED
        }
        assertTrue("Only one expiry check should be pending or running", active <= 1)
        assertTrue("The scheduled checks must not all be cancelled", info.any {
            it.state != WorkInfo.State.CANCELLED
        })
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