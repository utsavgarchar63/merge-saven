package com.mergeseven.game.game.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScoreEngineTest {

    private lateinit var scoreEngine: ScoreEngine

    @Before
    fun setup() {
        scoreEngine = ScoreEngine()
    }

    @Test
    fun `test base merge score calculation`() {
        // Base score is mergedValue * tileCount
        
        // 3x 4s merge to 8
        // Score = 8 * 3 = 24
        assertEquals(24L, scoreEngine.calculateMergeScore(mergedValue = 8, tileCount = 3))
        
        // 4x 8s merge to 16
        // Score = 16 * 4 = 64
        assertEquals(64L, scoreEngine.calculateMergeScore(mergedValue = 16, tileCount = 4))
    }

    @Test
    fun `test chain multipliers`() {
        // Assuming Constants.CHAIN_MULTIPLIERS = [1.0f, 1.5f, 2.0f, 3.0f, 5.0f]
        
        // Chain index 0 (1.0x) -> 8 * 3 = 24
        assertEquals(24L, scoreEngine.calculateMergeScore(mergedValue = 8, tileCount = 3, chainIndex = 0))
        
        // Chain index 1 (1.5x) -> (8 * 3) * 1.5 = 36
        assertEquals(36L, scoreEngine.calculateMergeScore(mergedValue = 8, tileCount = 3, chainIndex = 1))
        
        // Chain index 2 (2.0x) -> (8 * 3) * 2.0 = 48
        assertEquals(48L, scoreEngine.calculateMergeScore(mergedValue = 8, tileCount = 3, chainIndex = 2))
    }

    @Test
    fun `test coin reward rules`() {
        // 3 tiles -> 1 coin
        assertEquals(1, scoreEngine.calculateMergeCoins(mergedValue = 8, tileCount = 3))
        
        // 4 tiles -> 2 coins
        assertEquals(2, scoreEngine.calculateMergeCoins(mergedValue = 16, tileCount = 4))
        
        // 5+ tiles -> 3 coins
        assertEquals(3, scoreEngine.calculateMergeCoins(mergedValue = 32, tileCount = 5))
        assertEquals(3, scoreEngine.calculateMergeCoins(mergedValue = 64, tileCount = 7))
    }

    @Test
    fun `test coin chain bonus`() {
        // 3 tiles (1 coin) + chain index 1 (1 coin bonus) = 2 coins
        assertEquals(2, scoreEngine.calculateMergeCoins(mergedValue = 8, tileCount = 3, chainIndex = 1))
        
        // 4 tiles (2 coins) + chain index 2 (2 coin bonus) = 4 coins
        assertEquals(4, scoreEngine.calculateMergeCoins(mergedValue = 16, tileCount = 4, chainIndex = 2))
    }
}
