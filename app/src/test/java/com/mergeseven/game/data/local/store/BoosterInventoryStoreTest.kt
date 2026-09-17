package com.mergeseven.game.data.local.store

import com.mergeseven.game.game.boosters.BoosterCatalog
import com.mergeseven.game.game.model.BoosterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoosterInventoryStoreTest {

    @Test
    fun `consume prefers inventory before coins would be needed`() = runTest {
        val store = InMemoryBoosterInventoryStore(
            initial = mapOf(BoosterType.UNDO to 2)
        )
        assertTrue(store.tryConsume(BoosterType.UNDO))
        assertEquals(1, store.count(BoosterType.UNDO))
        assertTrue(store.tryConsume(BoosterType.UNDO))
        assertEquals(0, store.count(BoosterType.UNDO))
        assertFalse(store.tryConsume(BoosterType.UNDO))
    }

    @Test
    fun `grant increases owned count`() = runTest {
        val store = InMemoryBoosterInventoryStore(initial = emptyMap())
        store.grant(BoosterType.HAMMER, 3)
        assertEquals(3, store.count(BoosterType.HAMMER))
    }
}

/** Lightweight fake for unit tests (no Room). */
class InMemoryBoosterInventoryStore(
    initial: Map<BoosterType, Int> = BoosterCatalog.specs.mapValues { it.value.startingOwned }
) : BoosterInventoryStore {
    private val _owned = MutableStateFlow(initial)
    override val owned: StateFlow<Map<BoosterType, Int>> = _owned.asStateFlow()
    override suspend fun loadAndSeed() = Unit
    override fun count(type: BoosterType): Int = _owned.value[type] ?: 0
    override suspend fun grant(type: BoosterType, amount: Int) {
        _owned.value = _owned.value + (type to count(type) + amount)
    }
    override suspend fun tryConsume(type: BoosterType): Boolean {
        val current = count(type)
        if (current <= 0) return false
        _owned.value = _owned.value + (type to current - 1)
        return true
    }
}
