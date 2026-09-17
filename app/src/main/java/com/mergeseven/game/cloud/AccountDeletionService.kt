package com.mergeseven.game.cloud

import com.mergeseven.game.data.local.dao.ActiveGameDao
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes Merge Seven progress on this device and the cloud save slot, then signs out.
 * Does not delete the player's Google account.
 */
@Singleton
class AccountDeletionService @Inject constructor(
    private val applier: CloudSnapshotApplier,
    private val cloudSave: CloudSaveRepository,
    private val syncQueue: SyncQueue,
    private val auth: PlayGamesAuth,
    private val activeGameDao: ActiveGameDao,
    @PersistenceScope private val scope: CoroutineScope
) {
    suspend fun deleteLocalAndCloudData() {
        applier.wipeMetaProgress()
        listOf(
            ActiveGameEntity.CAMPAIGN_SLOT,
            ActiveGameEntity.ENDLESS_SLOT,
            ActiveGameEntity.TIME_ATTACK_SLOT,
            ActiveGameEntity.ZEN_SLOT,
            ActiveGameEntity.DAILY_SLOT,
            ActiveGameEntity.WEEKLY_SLOT
        ).forEach { slot ->
            runCatching { activeGameDao.delete(slot) }
        }
        runCatching { cloudSave.deleteSlot() }
        syncQueue.clear()
        auth.signOut()
    }

    fun deleteAsync(onDone: (() -> Unit)? = null) {
        scope.launch {
            deleteLocalAndCloudData()
            onDone?.invoke()
        }
    }
}
