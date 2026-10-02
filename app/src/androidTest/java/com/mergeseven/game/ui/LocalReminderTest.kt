package com.mergeseven.game.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mergeseven.game.MainActivity
import com.mergeseven.game.core.liveops.LocalReminderManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LocalReminderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun reminderIsUniqueOfflineAndCancelsWhenDisabled() = runBlocking {
        val activity = compose.activity
        val work = WorkManager.getInstance(activity)
        activity.settingsRepository.setNotificationsEnabled(true)
        activity.reminderManager.sync()
        activity.reminderManager.sync()
        compose.waitUntil(10_000) {
            work.getWorkInfosForUniqueWork(LocalReminderManager.WORK).get().any { !it.state.isFinished }
        }
        val active = work.getWorkInfosForUniqueWork(LocalReminderManager.WORK).get().filter { !it.state.isFinished }
        assertEquals(1, active.size)
        assertEquals(NetworkType.NOT_REQUIRED, active.single().constraints.requiredNetworkType)
        activity.settingsRepository.setNotificationsEnabled(false)
        activity.reminderManager.sync()
        compose.waitUntil(10_000) {
            work.getWorkInfosForUniqueWork(LocalReminderManager.WORK).get().all { it.state.isFinished }
        }
        assertTrue(work.getWorkInfosForUniqueWork(LocalReminderManager.WORK).get().any { it.state == WorkInfo.State.CANCELLED })
    }
}
