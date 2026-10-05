package com.mergeseven.game.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mergeseven.game.MainActivity
import com.mergeseven.game.core.liveops.LocalReminderManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LocalReminderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun offlineWorkerPostsGiftAndSuppressesDuplicateOnSameDate() = runBlocking {
        org.junit.Assume.assumeTrue("Delivery observes local quiet hours", java.time.ZonedDateTime.now().hour in 8..20)
        val activity = compose.activity
        org.junit.Assume.assumeTrue(LocalReminderManager.permissionGranted(activity))
        val work = WorkManager.getInstance(activity)
        val settings = activity.settingsRepository
        val enabledBefore = settings.isNotificationsEnabled.first()
        val repo = activity.userDataRepository
        repo.awaitReady()
        val original = repo.userProfile.value
        val prefs = activity.getSharedPreferences("local_reminders", 0)
        val sentBefore = prefs.getString("last_sent", null)
        val manager = activity.getSystemService(android.app.NotificationManager::class.java)
        val jobs = mutableListOf<java.util.UUID>()
        try {
            val today = java.time.LocalDate.now().toString()
            repo.restoreProfile(original.copy(lastLoginDate = today, currentStreak = 1, claimedDays = emptySet(),
                rewardClaims = original.rewardClaims.filterKeys { !it.endsWith(":daily_gift") } +
                    (com.mergeseven.game.data.model.DailyGifts.MIGRATION_KEY to 0)), clearClaims = true)
            repo.flush()
            settings.setNotificationsEnabled(true)
            prefs.edit().remove("last_sent").commit()
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            compose.waitUntil(10_000) {
                !androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
            }
            fun enqueue(): java.util.UUID {
                val request = androidx.work.OneTimeWorkRequestBuilder<com.mergeseven.game.core.liveops.PuzzleReminderWorker>().build()
                jobs += request.id
                work.enqueue(request).result.get()
                return request.id
            }
            val first = enqueue()
            compose.waitUntil(20_000) { work.getWorkInfoById(first).get()?.state?.isFinished == true }
            assertEquals(WorkInfo.State.SUCCEEDED, work.getWorkInfoById(first).get()!!.state)
            val notification = manager.activeNotifications.single { it.id == LocalReminderManager.NOTIFICATION }
            assertTrue(notification.notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("50 free coins"))
            assertNotNull(notification.notification.contentIntent)
            val second = enqueue()
            compose.waitUntil(20_000) { work.getWorkInfoById(second).get()?.state?.isFinished == true }
            assertEquals(notification.postTime, manager.activeNotifications.single { it.id == LocalReminderManager.NOTIFICATION }.postTime)
            assertEquals(original.coins, repo.coins())
        } finally {
            jobs.forEach { work.cancelWorkById(it).result.get() }
            manager.cancel(LocalReminderManager.NOTIFICATION)
            prefs.edit().apply { if (sentBefore == null) remove("last_sent") else putString("last_sent", sentBefore) }.commit()
            repo.restoreProfile(original, clearClaims = true); repo.flush()
            settings.setNotificationsEnabled(enabledBefore)
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            activity.reminderManager.sync()
        }
    }

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
