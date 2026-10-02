package com.mergeseven.game.data.local.store

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import com.mergeseven.game.game.boosters.BoosterCatalog
import com.mergeseven.game.game.model.BoosterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface BoosterInventoryStore {
    val owned: StateFlow<Map<BoosterType, Int>>
    suspend fun loadAndSeed()
    fun count(type: BoosterType): Int
    suspend fun grantOnce(type: BoosterType, amount: Int, key: String): Boolean { grant(type, amount); return true }
    suspend fun grant(type: BoosterType, amount: Int)
    /** Returns true if an owned charge was consumed. */
    suspend fun tryConsume(type: BoosterType): Boolean
}

@Singleton
class RoomBoosterInventoryStore @Inject constructor(
    private val unlockDao: UnlockDao,
    private val dispatchers: DispatcherProvider,
    private val featureFlags: com.mergeseven.game.core.flags.FeatureFlags,
) : BoosterInventoryStore {

    private val _owned = MutableStateFlow<Map<BoosterType, Int>>(emptyMap())
    override val owned: StateFlow<Map<BoosterType, Int>> = _owned.asStateFlow()

    private val inventoryMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun refreshLocked() {
        for ((type, spec) in BoosterCatalog.specs) unlockDao.seedBooster(BoosterCatalog.unlockId(type), spec.startingOwned)
        _owned.value = unlockDao.getAll().filter { it.category == UnlockEntity.CATEGORY_BOOSTER }
            .mapNotNull { entity ->
                runCatching { BoosterType.valueOf(entity.unlockId.removePrefix("booster_").uppercase()) }
                    .getOrNull()?.let { it to entity.quantity }
            }.toMap()
    }

    override suspend fun loadAndSeed() = withContext(dispatchers.io) {
        inventoryMutex.withLock { refreshLocked() }
    }
    override fun count(type: BoosterType): Int = _owned.value[type] ?: 0

    override suspend fun grantOnce(type: BoosterType, amount: Int, key: String): Boolean = withContext(dispatchers.io) {
        inventoryMutex.withLock {
            refreshLocked()
            val granted = unlockDao.grantOnce("reward:$key", BoosterCatalog.unlockId(type), amount)
            refreshLocked()

            granted
        }
    }
    override suspend fun grant(type: BoosterType, amount: Int) = withContext(dispatchers.io) {
        inventoryMutex.withLock {
            refreshLocked()
            unlockDao.addBooster(BoosterCatalog.unlockId(type), amount)
            refreshLocked()

        }
    }
    override suspend fun tryConsume(type: BoosterType): Boolean = withContext(dispatchers.io) {
        inventoryMutex.withLock {
            refreshLocked()
            val consumed = unlockDao.consumeBooster(BoosterCatalog.unlockId(type))
            refreshLocked()

            consumed
        }
    }

}
