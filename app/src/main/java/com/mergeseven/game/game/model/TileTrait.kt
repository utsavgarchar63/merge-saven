package com.mergeseven.game.game.model

import kotlinx.serialization.Serializable

/**
 * Special tile traits (AF1). Distinct silhouettes are drawn in Canvas — not colour-only (AF11).
 *
 * Behaviour is encoded in [com.mergeseven.game.game.engine.traits.TraitInteractions], not in this enum.
 */
@Serializable
enum class TileTrait {
    NORMAL,
    BOMB,
    WILDCARD,
    FROZEN,
    STONE,
    MULTIPLIER
}
