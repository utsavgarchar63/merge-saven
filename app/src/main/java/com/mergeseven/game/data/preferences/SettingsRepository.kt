package com.mergeseven.game.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repository interface for app settings (DataStore).
 */
interface SettingsRepository {
    val isSoundEnabled: Flow<Boolean>
    val isMusicEnabled: Flow<Boolean>
    val isHapticsEnabled: Flow<Boolean>
    val isTutorialCompleted: Flow<Boolean>

    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setTutorialCompleted(completed: Boolean)
}

/**
 * Stub implementation for prototyping.
 */
class SettingsRepositoryImpl : SettingsRepository {
    override val isSoundEnabled: Flow<Boolean> = emptyFlow()
    override val isMusicEnabled: Flow<Boolean> = emptyFlow()
    override val isHapticsEnabled: Flow<Boolean> = emptyFlow()
    override val isTutorialCompleted: Flow<Boolean> = emptyFlow()

    override suspend fun setSoundEnabled(enabled: Boolean) {}
    override suspend fun setMusicEnabled(enabled: Boolean) {}
    override suspend fun setHapticsEnabled(enabled: Boolean) {}
    override suspend fun setTutorialCompleted(completed: Boolean) {}
}
