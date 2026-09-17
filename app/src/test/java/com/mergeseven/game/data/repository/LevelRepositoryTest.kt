package com.mergeseven.game.data.repository

import com.mergeseven.game.data.local.store.InMemoryLevelProgressStore
import com.mergeseven.game.data.local.store.LevelProgress
import com.mergeseven.game.testing.TestPersistence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRepositoryTest {

    @Test
    fun `completed levels survive a restart`() {
        val store = InMemoryLevelProgressStore()

        TestPersistence.levelRepository(store).completeLevel(levelNum = 1, score = 1_500L, starsEarned = 2)

        val level = TestPersistence.levelRepository(store).getLevels(30).first()
        assertTrue(level.isCompleted)
        assertEquals(2, level.stars)
        assertEquals(1_500L, level.bestScore)
    }

    @Test
    fun `unlock pointer survives a restart`() = runTest {
        val store = InMemoryLevelProgressStore()

        TestPersistence.levelRepository(store).completeLevel(levelNum = 3, score = 900L, starsEarned = 1)

        val repository = TestPersistence.levelRepository(store)
        assertEquals(4, repository.highestUnlockedLevel.first())
        assertTrue(repository.getLevels(30)[3].isUnlocked)
        assertFalse(repository.getLevels(30)[4].isUnlocked)
    }

    @Test
    fun `only level one is unlocked on first run`() = runTest {
        val repository = TestPersistence.levelRepository()

        assertEquals(1, repository.highestUnlockedLevel.first())
        assertTrue(repository.getLevels(30)[0].isUnlocked)
        assertFalse(repository.getLevels(30)[1].isUnlocked)
    }

    @Test
    fun `a worse replay does not lower the record`() {
        val repository = TestPersistence.levelRepository()

        repository.completeLevel(levelNum = 1, score = 5_000L, starsEarned = 3)
        repository.completeLevel(levelNum = 1, score = 100L, starsEarned = 1)

        val level = repository.getLevels(30).first()
        assertEquals(3, level.stars)
        assertEquals(5_000L, level.bestScore)
    }

    @Test
    fun `total stars counts every completed level`() {
        val repository = TestPersistence.levelRepository()

        repository.completeLevel(levelNum = 1, score = 100L, starsEarned = 3)
        repository.completeLevel(levelNum = 2, score = 200L, starsEarned = 2)

        assertEquals(5, repository.totalStars(30))
    }

    @Test
    fun `total stars ignores levels outside the requested range`() {
        val repository = TestPersistence.levelRepository()

        repository.completeLevel(levelNum = 1, score = 100L, starsEarned = 3)
        repository.completeLevel(levelNum = 40, score = 200L, starsEarned = 3)

        assertEquals(3, repository.totalStars(30))
    }

    @Test
    fun `stored progress is kept when it beats what is in memory`() {
        val store = InMemoryLevelProgressStore(
            mapOf(1 to LevelProgress(levelNumber = 1, stars = 3, bestScore = 8_000L, isCompleted = true))
        )

        val level = TestPersistence.levelRepository(store).getLevels(30).first()

        assertEquals(3, level.stars)
        assertEquals(8_000L, level.bestScore)
    }

    @Test
    fun `unlockThrough marks prior levels complete so the target is unlocked`() = runTest {
        val repository = TestPersistence.levelRepository()

        repository.unlockThrough(5)

        assertEquals(5, repository.highestUnlockedLevel.first())
        assertTrue(repository.getLevels(30)[4].isUnlocked)
        assertFalse(repository.getLevels(30)[5].isUnlocked)
        assertTrue(repository.getLevels(30)[0].isCompleted)
        assertTrue(repository.getLevels(30)[3].isCompleted)
    }

    @Test
    fun `unlockThrough preserves existing stars`() {
        val repository = TestPersistence.levelRepository()
        repository.completeLevel(levelNum = 1, score = 9_000L, starsEarned = 3)

        repository.unlockThrough(3)

        val levelOne = repository.getLevels(30).first()
        assertEquals(3, levelOne.stars)
        assertEquals(9_000L, levelOne.bestScore)
    }
}
