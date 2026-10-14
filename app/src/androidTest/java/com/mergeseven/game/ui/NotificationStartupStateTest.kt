package com.mergeseven.game.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.google.android.gms.ads.AdRequest
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.core.liveops.LocalReminderManager
import com.mergeseven.game.core.liveops.ReminderDependencies
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit snapshot after a native Allow/Don't allow action on an isolated test emulator. */
class NotificationStartupStateTest {
    @Test fun permissionChoicePersistsAndSchedulesMatchingWork(): Unit = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Explicit debug notification QA only", BuildConfig.DEBUG &&
            arguments.getString("startupNotificationQa") == "true")
        assumeTrue("Only SDK-marked test devices", AdRequest.Builder().build().isTestDevice(context))
        val expected = requireNotNull(arguments.getString("expectedEnabled")).toBooleanStrict()
        val settings = EntryPointAccessors.fromApplication(context, ReminderDependencies::class.java).settings()
        assertEquals(expected, settings.isNotificationsEnabled.first())
        val work = WorkManager.getInstance(context)
        val active = work.getWorkInfosForUniqueWork(LocalReminderManager.WORK).get().count { !it.state.isFinished }
        assertEquals(if (expected) 1 else 0, active)
        if (expected && android.os.Build.VERSION.SDK_INT >= 26) {
            assertNotNull(context.getSystemService(android.app.NotificationManager::class.java)
                .getNotificationChannel(LocalReminderManager.CHANNEL))
        }
        android.util.Log.i("NotificationStartupQA", "enabled=$expected active_reminders=$active")
    }
}
