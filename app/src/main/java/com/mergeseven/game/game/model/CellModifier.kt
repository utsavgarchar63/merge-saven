package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/** Per-cell board modifiers (AF1-12). */
@Serializable
enum class CellModifierType {
    SCORE_PAD,
    SPAWN_VENT,
    LOCKED
}

@Serializable
data class CellModifier(
    val type: CellModifierType,
    val scoreBonus: Float = 1.5f
)
