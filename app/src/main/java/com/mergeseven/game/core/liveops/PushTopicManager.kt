package com.mergeseven.game.core.liveops

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscribes/unsubscribes FCM topics based on opt-in (AF6-07).
 */
@Singleton
class PushTopicManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val featureFlags: FeatureFlags,
    @PersistenceScope private val scope: CoroutineScope
) {
    fun onNewToken(@Suppress("UNUSED_PARAMETER") token: String) {
        scope.launch { syncTopics() }
    }

    fun syncTopicsAsync() {
        scope.launch { syncTopics() }
    }

    suspend fun syncTopics() {
        if (!featureFlags.isEnabled(Feature.AF6)) return
        val optedIn = settingsRepository.isNotificationsEnabled.first()
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        for (topic in TOPICS) {
            runCatching {
                if (optedIn) {
                    messaging.subscribeToTopic(topic).await()
                } else {
                    messaging.unsubscribeFromTopic(topic).await()
                }
            }.onFailure {
                Log.w(TAG, "Topic sync failed for $topic: ${it.message}")
            }
        }
    }

    companion object {
        private const val TAG = "PushTopicManager"
        val TOPICS = listOf(
            MergeSevenFirebaseMessagingService.TYPE_DAILY,
            MergeSevenFirebaseMessagingService.TYPE_STREAK,
            MergeSevenFirebaseMessagingService.TYPE_EVENT
        )
    }
}
