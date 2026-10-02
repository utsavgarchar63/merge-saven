package com.mergeseven.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import com.mergeseven.game.ads.ConsentManager
import com.mergeseven.game.app.AppNavGraph
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.ui.theme.MergeSevenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity entry point for Merge Seven.
 * Uses Jetpack Compose for all UI rendering.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val reminderRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private fun routeReminder(intent: android.content.Intent?) {
        if (intent?.action == "com.mergeseven.game.OPEN_DAILY") reminderRoute.value = com.mergeseven.game.app.Routes.DAILY
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent); setIntent(intent); routeReminder(intent)
    }
    @Inject lateinit var reminderManager: com.mergeseven.game.core.liveops.LocalReminderManager
    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var featureFlags: FeatureFlags
    @Inject lateinit var audioManager: AudioManager
    @Inject lateinit var settingsRepository: com.mergeseven.game.data.preferences.SettingsRepository
    @Inject lateinit var foregroundActivity: com.mergeseven.game.core.ForegroundActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        foregroundActivity.attach(this)
        routeReminder(intent)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.rgb(40, 24, 15))
        )

        setContent {
            MergeSevenTheme {
                LaunchedEffect(Unit) {
                    if (featureFlags.isEnabled(Feature.AF9)) {
                        consentManager.gatherConsent(this@MainActivity)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val requested by reminderRoute.collectAsState()
                    AppNavGraph(requestedRoute = requested, onRouteHandled = { reminderRoute.value = null; intent?.action = null })
                }
            }
        }
    }

    /** Start background music whenever the app comes to the foreground. */
    override fun onResume() {
        super.onResume()
        audioManager.setAppForeground(true)
        lifecycleScope.launch {
            val music = settingsRepository.isMusicEnabled.first()
            val sound = settingsRepository.isSoundEnabled.first()
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                audioManager.setSoundEnabled(sound)
                audioManager.setMusicEnabled(music)
            }
        }
        reminderManager.syncAsync()
    }

    /** Pause music whenever the app goes to the background or loses focus. */
    override fun onPause() {
        super.onPause()
        audioManager.setAppForeground(false)
    }

    override fun onDestroy() {
        foregroundActivity.detach(this)
        super.onDestroy()
    }
}
