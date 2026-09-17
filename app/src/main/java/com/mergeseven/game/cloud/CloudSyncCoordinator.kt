package com.mergeseven.game.cloud

import android.util.Log
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed class CloudUiEvent {
    data class Conflict(
        val local: ProgressSummary,
        val cloud: ProgressSummary
    ) : CloudUiEvent()

    data class FreshRestore(
        val cloud: ProgressSummary
    ) : CloudUiEvent()

    data object NeedsInteractiveSignIn : CloudUiEvent()

    data class Status(val message: String) : CloudUiEvent()
}

enum class ConflictChoice {
    KEEP_LOCAL,
    USE_CLOUD
}

/**
 * Orchestrates AF7 uploads, conflict detection, and fresh-install restore.
 * Never blocks gameplay; guest / AF7-off → no-ops.
 */
@Singleton
class CloudSyncCoordinator @Inject constructor(
    private val featureFlags: FeatureFlags,
    private val auth: PlayGamesAuth,
    private val cloudSave: CloudSaveRepository,
    private val builder: CloudSnapshotBuilder,
    private val applier: CloudSnapshotApplier,
    private val syncQueue: SyncQueue,
    private val economyNotifier: CloudEconomyNotifier,
    @PersistenceScope private val scope: CoroutineScope
) : CloudUploadTrigger {

    private val mutex = Mutex()
    private var backgroundJob: Job? = null
    private var pendingConflictLocal: CloudSnapshot? = null
    private var pendingConflictCloud: CloudSnapshot? = null
    private var freshCheckDone = false

    private val _events = MutableSharedFlow<CloudUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<CloudUiEvent> = _events.asSharedFlow()

    init {
        scope.launch {
            economyNotifier.ticks.collect {
                requestUpload(UploadReason.ECONOMY)
            }
        }
    }

    fun bootstrap() {
        if (!featureFlags.isEnabled(Feature.AF7)) return
        scope.launch {
            auth.signInSilently()
            drainQueueIfDue()
            maybeOfferFreshRestore()
        }
    }

    override fun requestUpload(reason: UploadReason) {
        if (!featureFlags.isEnabled(Feature.AF7)) return
        if (!auth.isSignedIn) return
        scope.launch {
            syncQueue.enqueue(reason)
            if (reason == UploadReason.BACKGROUND) {
                backgroundJob?.cancel()
                backgroundJob = scope.launch {
                    delay(BACKGROUND_DEBOUNCE_MS)
                    processQueue()
                }
            } else {
                processQueue()
            }
        }
    }

    fun onAppBackgrounded() {
        requestUpload(UploadReason.BACKGROUND)
    }

    suspend fun resolveConflict(choice: ConflictChoice) {
        mutex.withLock {
            val local = pendingConflictLocal ?: return
            val cloud = pendingConflictCloud ?: return
            when (choice) {
                ConflictChoice.KEEP_LOCAL -> {
                    applier.applyMergeSafe(chosen = local, other = cloud)
                    cloudSave.write(builder.build())
                }
                ConflictChoice.USE_CLOUD -> {
                    applier.applyMergeSafe(chosen = cloud, other = local)
                }
            }
            pendingConflictLocal = null
            pendingConflictCloud = null
            syncQueue.markSuccess()
        }
    }

    suspend fun resolveFreshRestore(useCloud: Boolean) {
        mutex.withLock {
            if (!useCloud) {
                pendingConflictCloud = null
                return
            }
            val cloud = pendingConflictCloud ?: cloudSave.read() ?: return
            applier.applyMergeSafe(chosen = cloud, other = null)
            pendingConflictCloud = null
        }
    }

    private suspend fun processQueue() {
        if (!featureFlags.isEnabled(Feature.AF7)) return
        if (!auth.isSignedIn) return
        mutex.withLock {
            val state = syncQueue.snapshot()
            if (state.pendingReasons.isEmpty()) return
            val now = System.currentTimeMillis()
            if (state.nextAttemptAtEpochMs > now && state.attemptCount > 0) return
            try {
                uploadAndReconcile()
                syncQueue.markSuccess()
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync failed: ${e.message}")
                syncQueue.markFailure(now)
                if (auth.lastAccount() == null) {
                    _events.tryEmit(CloudUiEvent.NeedsInteractiveSignIn)
                }
            }
        }
    }

    private suspend fun drainQueueIfDue() {
        val state = syncQueue.snapshot()
        if (state.pendingReasons.isEmpty()) return
        if (state.nextAttemptAtEpochMs > System.currentTimeMillis() && state.attemptCount > 0) return
        processQueue()
    }

    private suspend fun uploadAndReconcile() {
        val local = builder.build()
        val cloud = cloudSave.read()
        if (cloud == null) {
            cloudSave.write(local)
            return
        }
        when (ProgressCompare.compareSnapshots(local, cloud)) {
            Dominance.LOCAL -> {
                val merged = applier.mergeSafe(preferred = local, other = cloud)
                applier.applyExact(merged)
                cloudSave.write(builder.build())
            }
            Dominance.CLOUD -> {
                applier.applyMergeSafe(chosen = cloud, other = local)
            }
            Dominance.AMBIGUOUS -> {
                pendingConflictLocal = local
                pendingConflictCloud = cloud
                _events.tryEmit(
                    CloudUiEvent.Conflict(
                        local = ProgressCompare.tupleFrom(local).toSummary(),
                        cloud = ProgressCompare.tupleFrom(cloud).toSummary()
                    )
                )
            }
        }
    }

    private suspend fun maybeOfferFreshRestore() {
        if (freshCheckDone) return
        freshCheckDone = true
        if (!auth.isSignedIn) return
        val local = builder.build()
        if (!looksEmpty(local)) return
        val cloud = cloudSave.read() ?: return
        if (looksEmpty(cloud)) return
        pendingConflictCloud = cloud
        _events.tryEmit(
            CloudUiEvent.FreshRestore(ProgressCompare.tupleFrom(cloud).toSummary())
        )
    }

    private fun looksEmpty(snapshot: CloudSnapshot): Boolean {
        val p = snapshot.profile
        return p.totalStars == 0 &&
            p.totalMerges == 0 &&
            p.coins <= UserProfile.STARTING_COINS &&
            snapshot.levels.none { it.isCompleted } &&
            snapshot.unlocks.isEmpty()
    }

    private companion object {
        const val TAG = "CloudSync"
        const val BACKGROUND_DEBOUNCE_MS = 2_000L
    }
}

/**
 * Decouples durable economy writes from [CloudSyncCoordinator] (avoids a Hilt cycle through
 * [com.mergeseven.game.data.repository.UserDataRepository]).
 */
@Singleton
class CloudEconomyNotifier @Inject constructor() {
    private val _ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val ticks: SharedFlow<Unit> = _ticks.asSharedFlow()

    fun notifyChanged() {
        _ticks.tryEmit(Unit)
    }
}
