package com.mergeseven.game.game.repository

import com.mergeseven.game.game.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repository interface for game data persistence.
 * Maps between domain models and data entities.
 */
interface GameRepository {
    fun getActiveGame(): Flow<GameState?>
    suspend fun saveActiveGame(state: GameState)
    suspend fun clearActiveGame()
}

/**
 * Stub implementation of GameRepository for prototyping.
 */
class GameRepositoryImpl : GameRepository {
    override fun getActiveGame(): Flow<GameState?> = emptyFlow()
    override suspend fun saveActiveGame(state: GameState) {}
    override suspend fun clearActiveGame() {}
}
