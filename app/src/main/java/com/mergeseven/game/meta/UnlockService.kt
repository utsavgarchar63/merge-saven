package com.mergeseven.game.meta

import com.mergeseven.game.cloud.CloudEconomyNotifier
import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import com.mergeseven.game.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockService @Inject constructor(
    private val unlockDao: UnlockDao,
    private val dispatchers: DispatcherProvider,
    private val userDataRepository: UserDataRepository,
    private val featureFlags: FeatureFlags,
    private val cloudEconomyNotifier: CloudEconomyNotifier
) {
    private val _owned = MutableStateFlow<Set<String>>(emptySet())
    val owned: StateFlow<Set<String>> = _owned.asStateFlow()

    private val _claimedMilestones = MutableStateFlow<Set<String>>(emptySet())
    val claimedMilestones: StateFlow<Set<String>> = _claimedMilestones.asStateFlow()

    suspend fun loadAndSeed() = withContext(dispatchers.io) {
        val rows = unlockDao.getAll()
        val cosmetics = rows
            .filter { it.category == UnlockEntity.CATEGORY_COSMETIC && it.quantity > 0 }
            .map { it.unlockId.removePrefix("cosmetic_") }
            .toSet()
        val seeded = cosmetics.toMutableSet()
        for (def in CosmeticCatalog.all.filter { it.startingOwned }) {
            if (def.id !in seeded) {
                grantCosmeticInternal(def.id, notify = false)
                seeded += def.id
            }
        }
        _owned.value = seeded
        _claimedMilestones.value = rows
            .filter { it.category == UnlockEntity.CATEGORY_MILESTONE && it.quantity > 0 }
            .map { it.unlockId.removePrefix("milestone_") }
            .toSet()
    }

    fun owns(id: String): Boolean =
        id in _owned.value || CosmeticCatalog.byId(id)?.startingOwned == true

    suspend fun grantCosmetic(id: String) = withContext(dispatchers.io) {
        grantCosmeticInternal(id)
        _owned.value = _owned.value + id
    }

    /** AF6 seam — same as grant for now. */
    suspend fun grantFromEvent(id: String) = grantCosmetic(id)

    suspend fun tryBuyCosmetic(id: String): Boolean = withContext(dispatchers.io) {
        if (owns(id)) return@withContext true
        val def = CosmeticCatalog.byId(id) ?: return@withContext false
        if (def.coinCost <= 0) {
            grantCosmetic(id)
            return@withContext true
        }
        if (!userDataRepository.trySpendCoins(def.coinCost)) return@withContext false
        grantCosmetic(id)
        true
    }

    suspend fun applyLevelUnlocks(playerLevel: Int) = withContext(dispatchers.io) {
        for (def in CosmeticCatalog.all) {
            if (def.unlockAtLevel > 0 && playerLevel >= def.unlockAtLevel && !owns(def.id)) {
                grantCosmetic(id = def.id)
            }
        }
    }

    fun isMilestoneClaimed(id: String): Boolean = id in _claimedMilestones.value

    fun isMilestoneEligible(def: MilestoneDef, totalMerges: Int, biggestTile: Int): Boolean {
        if (isMilestoneClaimed(def.id)) return false
        val mergesOk = def.targetMerges?.let { totalMerges >= it } ?: true
        val tileOk = def.targetTile?.let { biggestTile >= it } ?: true
        return mergesOk && tileOk
    }

    suspend fun claimMilestone(def: MilestoneDef): Boolean = withContext(dispatchers.io) {
        if (isMilestoneClaimed(def.id)) return@withContext false
        val profile = userDataRepository.userProfile.value
        if (!isMilestoneEligible(def, profile.totalMerges, profile.biggestTile)) {
            return@withContext false
        }
        unlockDao.upsert(
            UnlockEntity(
                unlockId = MilestoneCatalog.unlockId(def.id),
                category = UnlockEntity.CATEGORY_MILESTONE,
                quantity = 1,
                unlockedAt = System.currentTimeMillis()
            )
        )
        _claimedMilestones.value = _claimedMilestones.value + def.id
        if (def.coinsReward > 0) userDataRepository.addCoins(def.coinsReward)
        def.cosmeticRewardId?.let { grantCosmetic(it) }
        notifyEconomy()
        true
    }

    private suspend fun grantCosmeticInternal(id: String, notify: Boolean = true) {
        unlockDao.upsert(
            UnlockEntity(
                unlockId = CosmeticCatalog.unlockId(id),
                category = UnlockEntity.CATEGORY_COSMETIC,
                quantity = 1,
                unlockedAt = System.currentTimeMillis()
            )
        )
        if (notify) notifyEconomy()
    }

    private fun notifyEconomy() {
        if (featureFlags.isEnabled(Feature.AF7)) {
            cloudEconomyNotifier.notifyChanged()
        }
    }
}
