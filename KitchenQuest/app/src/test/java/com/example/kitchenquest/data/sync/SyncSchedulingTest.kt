package com.example.kitchenquest.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncSchedulingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    private fun infos(): List<WorkInfo> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerSyncScheduler.WORK_NAME)
            .get()

    @Test
    fun aSyncRequestWaitsForANetworkConnection() {
        WorkManagerSyncScheduler(context).requestSync()

        val info = infos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun aSecondRequestWhileOneIsWaitingRunsAfterItInsteadOfBeingLost() {
        val scheduler = WorkManagerSyncScheduler(context)

        scheduler.requestSync()
        scheduler.requestSync()

        val states = infos().map { it.state }
        assertEquals(2, states.size)
        assertTrue(states.contains(WorkInfo.State.BLOCKED))
    }

    @Test
    fun theWorkerHasNothingToDoWhenNobodyIsSignedIn() = runBlocking {
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }
}
