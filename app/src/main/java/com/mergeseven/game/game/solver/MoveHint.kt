package com.mergeseven.game.game.solver

import com.mergeseven.game.game.model.HexCoord

/**
 * Suggested tray placement from the solver (AF4-02).
 */
data class MoveHint(
    val slotIndex: Int,
    val origin: HexCoord,
    val rotation: Int,
    val score: Double
)
