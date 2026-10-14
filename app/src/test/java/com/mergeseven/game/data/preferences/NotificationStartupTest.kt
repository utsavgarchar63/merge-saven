package com.mergeseven.game.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NotificationStartupTest {
    @get:Rule val files = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun repository(): SettingsRepositoryImpl = SettingsRepositoryImpl(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(files.root, "settings.preferences_pb") }))
    @After fun close() { scope.cancel() }

    @Test fun freshStartupIsClaimedOnlyOnceEvenBeforePermissionCallback(): Unit = runTest {
        val settings = repository()
        assertFalse(settings.isNotificationsEnabled.first())
        assertTrue(settings.beginNotificationStartup())
        assertFalse(settings.beginNotificationStartup())
        settings.setNotificationsEnabled(true)
        assertTrue(settings.isNotificationsEnabled.first())
        assertFalse(settings.beginNotificationStartup())
    }

    @Test fun existingOptOutSurvivesUpgrade(): Unit = runTest {
        val settings = repository()
        settings.setNotificationsEnabled(false)
        assertFalse(settings.beginNotificationStartup())
        assertFalse(settings.isNotificationsEnabled.first())
    }

    @Test fun existingOptInDoesNotPromptAgain(): Unit = runTest {
        val settings = repository()
        settings.setNotificationsEnabled(true)
        assertFalse(settings.beginNotificationStartup())
        assertTrue(settings.isNotificationsEnabled.first())
    }

    @Test fun denialAndSettingsResetDoNotReopenStartupPrompt(): Unit = runTest {
        val settings = repository()
        settings.beginNotificationStartup()
        settings.setNotificationsEnabled(false)
        settings.resetSettings()
        assertFalse(settings.beginNotificationStartup())
        assertFalse(settings.isNotificationsEnabled.first())
    }

    @Test fun concurrentLaunchesCannotBothClaimThePrompt(): Unit = runTest {
        val settings = repository()
        val claims = List(4) { async { settings.beginNotificationStartup() } }.awaitAll()
        assertEquals(1, claims.count { it })
    }
}
