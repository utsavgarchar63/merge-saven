package com.mergeseven.game.core.liveops

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.core.crash.CrashReporter
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.LocalFeatureFlags
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-blocking Remote Config fetch (AF6-04/05).
 *
 * Startup reads last-activated values immediately. Fetch runs on IO and never blocks
 * Application.onCreate. Activate refreshes [LiveConfig] for the current session when safe.
 */
@Singleton
class RemoteConfigRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val localFeatureFlags: LocalFeatureFlags,
    private val dispatchers: DispatcherProvider,
    private val crashReporter: CrashReporter,
    @PersistenceScope private val scope: CoroutineScope
) {
    private val values = MutableStateFlow(LiveConfigDefaults.defaultStringMap())
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    val liveConfig: LiveConfig = ParsedLiveConfig(
        strings = { values.value },
        revision = revision
    )

    fun snapshotStrings(): Map<String, String> = values.value

    fun bootstrapFromActivatedCache() {
        runCatching {
            remoteConfig.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(
                        if (BuildConfig.DEBUG) 0L else FETCH_INTERVAL_PROD_SEC
                    )
                    .build()
            )
            remoteConfig.setDefaultsAsync(LiveConfigDefaults.defaultStringMap())
            pullActivatedIntoMemory()
        }.onFailure {
            crashReporter.recordNonFatal(it, "RC bootstrap failed")
            Log.w(TAG, "RC bootstrap failed: ${it.message}")
        }
    }

    /** Fire-and-forget; safe to call from Application.onCreate. */
    fun fetchAsync() {
        if (!localFeatureFlags.isEnabled(Feature.AF6)) return
        scope.launch {
            withContext(dispatchers.io) {
                withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                    runCatching {
                        remoteConfig.fetchAndActivate().await()
                        pullActivatedIntoMemory()
                    }.onFailure {
                        crashReporter.recordNonFatal(it, "RC fetch failed")
                        Log.w(TAG, "RC fetch failed: ${it.message}")
                    }
                }
            }
        }
    }

    private fun pullActivatedIntoMemory() {
        val next = LiveConfigDefaults.defaultStringMap().toMutableMap()
        for (key in next.keys) {
            val remote = remoteConfig.getString(key)
            if (remote.isNotBlank()) next[key] = remote
        }
        values.value = next
        _revision.value = _revision.value + 1
    }

    companion object {
        private const val TAG = "RemoteConfigRepository"
        private const val FETCH_INTERVAL_PROD_SEC = 12L * 60L * 60L
        private const val FETCH_TIMEOUT_MS = 8_000L
    }
}
