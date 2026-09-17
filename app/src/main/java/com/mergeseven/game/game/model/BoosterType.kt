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
    CONTINUE,

    /** Destroy one selected tile (AF3). */
    HAMMER,

    /** Raise a tile by one value tier (AF3). */
    VALUE_UP,

    /** Pull matching values together (AF3). */
    MAGNET,

    /** Pause the Time Attack clock briefly (AF3). */
    TIME_FREEZE
}
