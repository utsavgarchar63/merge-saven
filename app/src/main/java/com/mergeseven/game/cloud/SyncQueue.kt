package com.mergeseven.game.cloud

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

enum class UploadReason {
    LEVEL_COMPLETE,
    ECONOMY,
    BACKGROUND,
    MANUAL
}

@Serializable
data class SyncQueueState(
    val pendingReasons: List<String> = emptyList(),
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMs: Long = 0L
)

interface SyncQueue {
    suspend fun enqueue(reason: UploadReason)
    suspend fun snapshot(): SyncQueueState
    suspend fun markFailure(nowEpochMs: Long = System.currentTimeMillis())
    suspend fun markSuccess()
    suspend fun clear()
    fun backoffMs(attemptCount: Int): Long
}

private val Context.syncQueueDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_sync_queue")

@Singleton
class DataStoreSyncQueue @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncQueue {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateKey = stringPreferencesKey("state")
    private val nextKey = longPreferencesKey("next_attempt")

    override suspend fun enqueue(reason: UploadReason) {
        val current = snapshot()
        val reasons = (current.pendingReasons + reason.name).distinct()
        persist(current.copy(pendingReasons = reasons))
    }

    override suspend fun snapshot(): SyncQueueState {
        val prefs = context.syncQueueDataStore.data.first()
        val raw = prefs[stateKey] ?: return SyncQueueState()
        return runCatching { json.decodeFromString(SyncQueueState.serializer(), raw) }
            .getOrElse { SyncQueueState() }
    }

    override suspend fun markFailure(nowEpochMs: Long) {
        val current = snapshot()
        val attempts = current.attemptCount + 1
        persist(
            current.copy(
                attemptCount = attempts,
                nextAttemptAtEpochMs = nowEpochMs + backoffMs(attempts)
            )
        )
    }

    override suspend fun markSuccess() {
        persist(SyncQueueState())
    }

    override suspend fun clear() {
        context.syncQueueDataStore.edit { it.clear() }
    }

    /** Exponential backoff: min(60s * 2^n, 1h). */
    override fun backoffMs(attemptCount: Int): Long {
        val n = attemptCount.coerceAtLeast(0)
        val raw = (60_000.0 * 2.0.pow(n.toDouble())).toLong()
        return min(raw, 3_600_000L)
    }

    private suspend fun persist(state: SyncQueueState) {
        context.syncQueueDataStore.edit { prefs ->
            prefs[stateKey] = json.encodeToString(SyncQueueState.serializer(), state)
            prefs[nextKey] = state.nextAttemptAtEpochMs
        }
    }
}

class InMemorySyncQueue : SyncQueue {
    var state: SyncQueueState = SyncQueueState()

    override suspend fun enqueue(reason: UploadReason) {
        state = state.copy(
            pendingReasons = (state.pendingReasons + reason.name).distinct()
        )
    }

    override suspend fun snapshot(): SyncQueueState = state

    override suspend fun markFailure(nowEpochMs: Long) {
        val attempts = state.attemptCount + 1
        state = state.copy(
            attemptCount = attempts,
            nextAttemptAtEpochMs = nowEpochMs + backoffMs(attempts)
        )
    }

    override suspend fun markSuccess() {
        state = SyncQueueState()
    }

    override suspend fun clear() {
        state = SyncQueueState()
    }

    override fun backoffMs(attemptCount: Int): Long {
        val n = attemptCount.coerceAtLeast(0)
        val raw = (60_000.0 * 2.0.pow(n.toDouble())).toLong()
        return min(raw, 3_600_000L)
    }
}
