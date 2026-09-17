package com.mergeseven.game.data.local.store

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import com.mergeseven.game.game.boosters.BoosterCatalog
import com.mergeseven.game.game.model.BoosterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface BoosterInventoryStore {
    val owned: StateFlow<Map<BoosterType, Int>>
    suspend fun loadAndSeed()
    fun count(type: BoosterType): Int
    suspend fun grant(type: BoosterType, amount: Int)
    /** Returns true if an owned charge was consumed. */
    suspend fun tryConsume(type: BoosterType): Boolean
}

@Singleton
class RoomBoosterInventoryStore @Inject constructor(
    private val unlockDao: UnlockDao,
    private val dispatchers: DispatcherProvider,
    private val featureFlags: com.mergeseven.game.core.flags.FeatureFlags,
    private val cloudEconomyNotifier: com.mergeseven.game.cloud.CloudEconomyNotifier
) : BoosterInventoryStore {

    private val _owned = MutableStateFlow<Map<BoosterType, Int>>(emptyMap())
    override val owned: StateFlow<Map<BoosterType, Int>> = _owned.asStateFlow()

    override suspend fun loadAndSeed() = withContext(dispatchers.io) {
        val existing = unlockDao.getAll()
            .filter { it.category == UnlockEntity.CATEGORY_BOOSTER }
            .associate { entity ->
                val typeName = entity.unlockId.removePrefix("booster_").uppercase()
                val type = runCatching { BoosterType.valueOf(typeName) }.getOrNull()
                type to entity.quantity
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }

        val seeded = BoosterCatalog.specs.mapValues { (type, spec) ->
            existing[type] ?: spec.startingOwned
        }
        for ((type, qty) in seeded) {
            if (type !in existing) {
                unlockDao.upsert(
                    UnlockEntity(
                        unlockId = BoosterCatalog.unlockId(type),
                        category = UnlockEntity.CATEGORY_BOOSTER,
                        quantity = qty,
                        unlockedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        _owned.value = seeded
    }

    override fun count(type: BoosterType): Int = _owned.value[type] ?: 0

    override suspend fun grant(type: BoosterType, amount: Int) = withContext(dispatchers.io) {
        if (amount <= 0) return@withContext
        val next = count(type) + amount
        unlockDao.upsert(
            UnlockEntity(
                unlockId = BoosterCatalog.unlockId(type),
                category = UnlockEntity.CATEGORY_BOOSTER,
                quantity = next,
                unlockedAt = System.currentTimeMillis()
            )
        )
        _owned.value = _owned.value + (type to next)
        if (featureFlags.isEnabled(com.mergeseven.game.core.flags.Feature.AF7)) {
            cloudEconomyNotifier.notifyChanged()
        }
    }

    override suspend fun tryConsume(type: BoosterType): Boolean = withContext(dispatchers.io) {
        val current = count(type)
        if (current <= 0) return@withContext false
        val next = current - 1
        unlockDao.upsert(
            UnlockEntity(
                unlockId = BoosterCatalog.unlockId(type),
                category = UnlockEntity.CATEGORY_BOOSTER,
                quantity = next,
                unlockedAt = System.currentTimeMillis()
            )
        )
        _owned.value = _owned.value + (type to next)
        if (featureFlags.isEnabled(com.mergeseven.game.core.flags.Feature.AF7)) {
            cloudEconomyNotifier.notifyChanged()
        }
        true
    }
}
