package com.mergeseven.game.game.solver

import kotlinx.coroutines.flow.StateFlow

/**
 * Seam for AF6 Remote Config to override the local difficulty curve.
 */
interface DifficultyProfileProvider {
    val profile: StateFlow<DifficultyProfile>
    fun current(): DifficultyProfile = profile.value
    suspend fun setProfile(id: DifficultyId)
}
