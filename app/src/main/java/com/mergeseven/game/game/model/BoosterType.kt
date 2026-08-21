package com.mergeseven.game.game.model

/**
 * Available booster types.
 * See Master Plan Section 22.
 *
 * Each booster must define: cost, availability, validation, effect,
 * animation, analytics event, and failure case.
 */
enum class BoosterType {
    /** Exchange a tile/piece with another eligible piece/value. */
    SWAP,

    /** Replace the current piece with another valid generated piece. */
    RANDOMIZE,

    /** Remove one selected tile from the board. */
    REMOVE,

    /** Restore the previous game state. */
    UNDO,

    /** After game over, restore a playable state. */
    CONTINUE
}
