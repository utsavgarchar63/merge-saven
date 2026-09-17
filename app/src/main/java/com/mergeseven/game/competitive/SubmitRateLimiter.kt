package com.mergeseven.game.competitive

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.submitRateStore by preferencesDataStore(name = "af8_submit_rate")

/**
 * Per-mode daily submit caps (AF8-08).
 */
@Singleton
class SubmitRateLimiter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun tryAcquire(modeId: String, dayKey: String): Boolean {
        val countKey = intPreferencesKey("count_$modeId")
        val dayPref = stringPreferencesKey("day_$modeId")
        val prefs = context.submitRateStore.data.first()
        val storedDay = prefs[dayPref]
        val count = if (storedDay == dayKey) prefs[countKey] ?: 0 else 0
        if (count >= MAX_PER_DAY) return false
        context.submitRateStore.edit { edit ->
            edit[dayPref] = dayKey
            edit[countKey] = count + 1
        }
        return true
    }

    suspend fun lastSubmitAtMs(modeId: String): Long {
        val key = longPreferencesKey("last_ms_$modeId")
        return context.submitRateStore.data.first()[key] ?: 0L
    }

    suspend fun markSubmitted(modeId: String, nowMs: Long = System.currentTimeMillis()) {
        val key = longPreferencesKey("last_ms_$modeId")
        context.submitRateStore.edit { it[key] = nowMs }
    }

    companion object {
        const val MAX_PER_DAY = 20
        const val MIN_COOLDOWN_MS = 5_000L
    }
}
