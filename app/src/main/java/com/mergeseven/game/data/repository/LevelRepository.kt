package com.mergeseven.game.data.repository

import com.mergeseven.game.game.rules.LevelPool
import com.mergeseven.game.game.rules.LevelRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Level progression data item.
 */
data class LevelItem(
    val rule: LevelRule,
    val isUnlocked: Boolean,
    val stars: Int, // 0 to 3 stars
    val bestScore: Long,
    val isCompleted: Boolean
)

/**
 * Repository for managing level progression, star ratings, and unlock states.
 */
@Singleton
class LevelRepository @Inject constructor() {

    private val _highestUnlockedLevel = MutableStateFlow(1)
    val highestUnlockedLevel: Flow<Int> = _highestUnlockedLevel.asStateFlow()

    private val levelStars = mutableMapOf<Int, Int>()
    private val levelScores = mutableMapOf<Int, Long>()

    fun getLevels(totalLevels: Int = 30): List<LevelItem> {
        val maxUnlocked = _highestUnlockedLevel.value
        return (1..totalLevels).map { levelNum ->
            val rule = LevelPool.getLevel(levelNum)
            val isUnlocked = levelNum <= maxUnlocked
            val stars = levelStars[levelNum] ?: 0
            val bestScore = levelScores[levelNum] ?: 0L
            val isCompleted = stars > 0 || levelNum < maxUnlocked

            LevelItem(
                rule = rule,
                isUnlocked = isUnlocked,
                stars = stars,
                bestScore = bestScore,
                isCompleted = isCompleted
            )
        }
    }

    fun completeLevel(levelNum: Int, score: Long, starsEarned: Int) {
        val currentStars = levelStars[levelNum] ?: 0
        if (starsEarned > currentStars) {
            levelStars[levelNum] = starsEarned
        }
        val currentScore = levelScores[levelNum] ?: 0L
        if (score > currentScore) {
            levelScores[levelNum] = score
        }

        // Unlock next level
        if (levelNum >= _highestUnlockedLevel.value) {
            _highestUnlockedLevel.update { levelNum + 1 }
        }
    }

    fun totalStars(totalLevels: Int = 30): Int {
        return (1..totalLevels).sumOf { levelStars[it] ?: 0 }
    }
}
