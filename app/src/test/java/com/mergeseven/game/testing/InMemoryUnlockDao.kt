package com.mergeseven.game.testing

import com.mergeseven.game.data.local.dao.UnlockDao
import com.mergeseven.game.data.local.entity.UnlockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Simple in-memory [UnlockDao] for unit tests. */
class InMemoryUnlockDao : UnlockDao {
    private val rows = MutableStateFlow<Map<String, UnlockEntity>>(emptyMap())

    override fun observeCategory(category: String): Flow<List<UnlockEntity>> =
        rows.map { map -> map.values.filter { it.category == category } }

    override suspend fun getAll(): List<UnlockEntity> = rows.value.values.toList()

    override suspend fun get(unlockId: String): UnlockEntity? = rows.value[unlockId]

    override suspend fun upsert(entity: UnlockEntity) {
        rows.value = rows.value + (entity.unlockId to entity)
    }

    override suspend fun delete(unlockId: String) {
        rows.value = rows.value - unlockId
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
    }
}
