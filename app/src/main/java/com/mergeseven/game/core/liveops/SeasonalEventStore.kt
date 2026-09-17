package com.mergeseven.game.core.liveops

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.di.PersistenceScope
import com.mergeseven.game.meta.UnlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveSeasonalEvent(
    val config: SeasonalEventConfig,
    val personalBest: Long,
    val claimedThresholds: Set<Long>
)

/**
 * Local seasonal event window + reward claims (AF6-10). No online leaderboard.
 */
fun interface SeasonalEventRecorder {
    fun recordScore(score: Long)
}

@Singleton
class SeasonalEventStore @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val featureFlags: FeatureFlags,
    private val dataStore: DataStore<Preferences>,
    private val userDataRepository: UserDataRepository,
    private val unlockService: UnlockService,
    private val dateProvider: DateProvider,
    @PersistenceScope private val scope: CoroutineScope
) : SeasonalEventRecorder {
    val active: StateFlow<ActiveSeasonalEvent?> =
        combine(remoteConfigRepository.revision, dataStore.data) { _, prefs ->
            resolveActive(prefs)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun recordScore(score: Long) {
        val event = active.value ?: return
        if (score <= event.personalBest) return
        scope.launch {
            dataStore.edit { prefs ->
                prefs[bestKey(event.config.id)] = score
            }
        }
    }

    suspend fun claim(threshold: Long): Boolean {
        val event = active.value ?: return false
        val step = event.config.rewardTrack.firstOrNull { it.threshold == threshold } ?: return false
        if (threshold in event.claimedThresholds) return false
        if (event.personalBest < threshold) return false
        dataStore.edit { prefs ->
            val key = claimedKey(event.config.id)
            val current = prefs[key].orEmpty()
            prefs[key] = current + threshold.toString()
        }
        if (step.coins > 0) userDataRepository.addCoins(step.coins)
        step.cosmeticId?.let { unlockService.grantFromEvent(it) }
        return true
    }

    private fun resolveActive(prefs: Preferences): ActiveSeasonalEvent? {
        if (!featureFlags.isEnabled(Feature.AF6)) return null
        val config = remoteConfigRepository.liveConfig.seasonalEvent() ?: return null
        if (!isInWindow(config)) return null
        val best = prefs[bestKey(config.id)] ?: 0L
        val claimed = prefs[claimedKey(config.id)].orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        return ActiveSeasonalEvent(config, best, claimed)
    }

    private fun isInWindow(config: SeasonalEventConfig): Boolean {
        val now = runCatching {
            LocalDate.parse(dateProvider.today()).atStartOfDay().toInstant(ZoneOffset.UTC)
        }.getOrElse { Instant.now() }
        val start = runCatching { Instant.parse(config.startIso) }.getOrNull() ?: return false
        val end = runCatching { Instant.parse(config.endIso) }.getOrNull() ?: return false
        return !now.isBefore(start) && now.isBefore(end)
    }

    private fun bestKey(eventId: String) = longPreferencesKey("event_${eventId}_best")
    private fun claimedKey(eventId: String) = stringSetPreferencesKey("event_${eventId}_claimed")
}
