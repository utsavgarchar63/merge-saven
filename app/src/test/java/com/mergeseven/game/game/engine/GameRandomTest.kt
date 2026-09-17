package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.RngState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRandomTest {

    @Test
    fun `the same seed produces the same sequence`() {
        val first = GameRandom(RngState.fromSeed(42L)).take(50)
        val second = GameRandom(RngState.fromSeed(42L)).take(50)

        assertEquals(first, second)
    }

    @Test
    fun `different seeds produce different sequences`() {
        val first = GameRandom(RngState.fromSeed(42L)).take(50)
        val second = GameRandom(RngState.fromSeed(43L)).take(50)

        assertNotEquals(first, second)
    }

    @Test
    fun `a restored snapshot continues where it left off`() {
        val original = GameRandom(RngState.fromSeed(7L))
        original.take(10)
        val snapshot = original.snapshot()

        val expected = original.take(10)
        val restored = GameRandom(snapshot).take(10)

        assertEquals(expected, restored)
    }

    @Test
    fun `a restored snapshot does not replay what was already drawn`() {
        val original = GameRandom(RngState.fromSeed(7L))
        val alreadySeen = original.take(10)

        val restored = GameRandom(original.snapshot()).take(10)

        assertTrue(alreadySeen.intersect(restored.toSet()).isEmpty())
    }

    @Test
    fun `bounded draws stay in range`() {
        val random = GameRandom(RngState.fromSeed(99L))

        repeat(1_000) {
            val value = random.nextInt(7)
            assertTrue("$value out of range", value in 0..6)
        }
    }

    @Test
    fun `bounded draws cover the range`() {
        val random = GameRandom(RngState.fromSeed(99L))

        val seen = (1..1_000).map { random.nextInt(6) }.toSet()

        assertEquals("every bucket should appear over 1000 draws", 6, seen.size)
    }

    @Test
    fun `ids are unique and survive a snapshot`() {
        val random = GameRandom(RngState.fromSeed(3L))
        val before = List(5) { random.nextId() }

        val after = List(5) { GameRandom(random.snapshot()).nextId() }

        assertEquals(5, before.toSet().size)
        assertTrue("ids must not be reissued after a restore", before.none { it in after })
    }

    @Test
    fun `drawing an id does not disturb the value sequence`() {
        val withoutIds = GameRandom(RngState.fromSeed(5L)).take(5)

        val withIds = GameRandom(RngState.fromSeed(5L)).let { random ->
            (1..5).map {
                random.nextId()
                random.nextLong()
            }
        }

        assertEquals(withoutIds, withIds)
    }

    @Test
    fun `the seed is preserved across snapshots for bug reports`() {
        val random = GameRandom(RngState.fromSeed(1234L))
        random.take(20)

        assertEquals(1234L, random.snapshot().seed)
    }

    @Test
    fun `the draw count tracks how far the run has advanced`() {
        val random = GameRandom(RngState.fromSeed(1L))

        random.take(13)

        assertEquals(13, random.snapshot().draws)
    }

    private fun GameRandom.take(count: Int): List<Long> = (1..count).map { nextLong() }
}
