package com.mergeseven.game.ads

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mergeseven.game.core.DateProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private val Context.adExposureStore by preferencesDataStore("ad_exposure_v2")
@Serializable
data class AdPolicyConfig(
    val graceRuns: Int = 3, val gracePlaytimeMs: Long = 600_000,
    val frequency: Int = 3, val cooldownMs: Long = 180_000,
    val windowMs: Long = 900_000, val windowCap: Int = 2, val dailyCap: Int = 6
) {
    fun bounded() = copy(graceRuns = graceRuns.coerceAtLeast(3),
        gracePlaytimeMs = gracePlaytimeMs.coerceAtLeast(600_000), frequency = frequency.coerceAtLeast(3),
        cooldownMs = cooldownMs.coerceAtLeast(180_000), windowMs = windowMs.coerceAtLeast(900_000),
        windowCap = windowCap.coerceIn(0, 2), dailyCap = dailyCap.coerceIn(0, 6))
}
data class AdExposure(
    val completedRuns: Int = 0, val campaignWins: Int = 0, val playtimeMs: Long = 0,
    val lastClosedMs: Long = 0, val interstitialTimes: List<Long> = emptyList(), val todayCount: Int = 0
)
fun decideInterstitial(state: AdExposure, config: AdPolicyConfig, now: Long): InterstitialDecision {
    val c = config.bounded()
    val reason = when {
        state.completedRuns <= c.graceRuns -> "grace_runs"
        state.playtimeMs < c.gracePlaytimeMs -> "grace_playtime"
        state.campaignWins < c.frequency -> "frequency"
        state.lastClosedMs > 0 && now - state.lastClosedMs < c.cooldownMs -> "cooldown"
        state.interstitialTimes.count { now - it in 0 until c.windowMs } >= c.windowCap -> "window_cap"
        state.todayCount >= c.dailyCap -> "daily_cap"
        else -> "ok"
    }
    return InterstitialDecision(reason == "ok", reason)
}
@Singleton
class AdExposureStore @Inject constructor(@ApplicationContext private val context: Context,
                                        private val dateProvider: DateProvider) {
    private val runs = intPreferencesKey("runs")
    private val wins = intPreferencesKey("wins")
    private val playtime = longPreferencesKey("playtime")
    private val closed = longPreferencesKey("closed")
    private val times = stringPreferencesKey("times")
    private val day = stringPreferencesKey("day")
    private val dayCount = intPreferencesKey("day_count")
    private val sessions = stringSetPreferencesKey("finished_sessions")
    suspend fun snapshot(): AdExposure {
        val p = context.adExposureStore.data.first()
        return AdExposure(p[runs] ?: 0, p[wins] ?: 0, p[playtime] ?: 0, p[closed] ?: 0,
            p[times].orEmpty().split(',').mapNotNull(String::toLongOrNull),
            if (p[day] == dateProvider.today()) p[dayCount] ?: 0 else 0)
    }
    suspend fun finishSession(id: String, campaignWon: Boolean) {
        context.adExposureStore.edit { p ->
            val finished = p[sessions].orEmpty()
            if (id !in finished) {
                p[sessions] = finished + id
                p[runs] = (p[runs] ?: 0) + 1
                if (campaignWon) p[wins] = (p[wins] ?: 0) + 1
            }
        }
    }
    suspend fun addPlaytime(ms: Long) {
        context.adExposureStore.edit { it[playtime] = (it[playtime] ?: 0) + ms.coerceAtLeast(0) }
    }
    suspend fun fullscreenClosed(now: Long = System.currentTimeMillis()) {
        context.adExposureStore.edit { it[closed] = now }
    }
    suspend fun interstitialShown(now: Long = System.currentTimeMillis()) {
        context.adExposureStore.edit { p ->
            p[wins] = 0
            val recent = p[times].orEmpty().split(',').mapNotNull(String::toLongOrNull)
                .filter { now - it in 0 until 86_400_000 }
            p[times] = (recent + now).joinToString(",")
            p[dayCount] = if (p[day] == dateProvider.today()) (p[dayCount] ?: 0) + 1 else 1
            p[day] = dateProvider.today()
        }
    }
}
