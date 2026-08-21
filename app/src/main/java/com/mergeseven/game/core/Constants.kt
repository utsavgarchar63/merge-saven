package com.mergeseven.game.core

/**
 * Centralized game configuration constants.
 * See Master Plan Section 10 (Merge Rule), Section 18 (Spawn), Section 20 (Scoring),
 * Section 39 (Animation Timings), Section 91 (Balancing Table).
 *
 * All gameplay-affecting values should be defined here, never scattered across UI classes.
 * See Master Plan Section 101.
 */
object Constants {

    // ──────────────────────────────────────────────
    // Merge Rules
    // ──────────────────────────────────────────────

    /** Minimum number of connected same-value tiles required to trigger a merge. */
    const val MIN_MERGE_COUNT = 3

    // ──────────────────────────────────────────────
    // Board
    // ──────────────────────────────────────────────

    /** Default board radius in hex rings from center. */
    const val DEFAULT_BOARD_RADIUS = 4

    // ──────────────────────────────────────────────
    // Piece Queue
    // ──────────────────────────────────────────────

    /** Number of visible pieces in the queue. */
    const val PIECE_QUEUE_SIZE = 3

    /** Number of rotations supported (60° increments). */
    const val ROTATION_STEPS = 6

    // ──────────────────────────────────────────────
    // Spawn Weights (prototype — Section 18)
    // ──────────────────────────────────────────────

    val SPAWN_WEIGHTS = mapOf(
        2 to 35,
        4 to 35,
        8 to 20,
        16 to 8,
        32 to 2
    )

    // ──────────────────────────────────────────────
    // Scoring (Section 20)
    // ──────────────────────────────────────────────

    /**
     * Chain multipliers applied to successive merges in a single turn.
     * 1st merge = 1.0x, 2nd = 1.25x, 3rd = 1.5x, 4th+ = 2.0x
     */
    val CHAIN_MULTIPLIERS = listOf(1.0f, 1.25f, 1.5f, 2.0f)

    // ──────────────────────────────────────────────
    // Economy (Section 21, Section 91)
    // ──────────────────────────────────────────────

    const val INITIAL_COIN_BALANCE = 100
    const val UNDO_COST = 50
    const val RANDOMIZE_COST = 80
    const val REMOVE_COST = 120
    const val CONTINUE_COST = 100
    const val REWARDED_CONTINUE_LIMIT = 1
    const val DAILY_REWARD = 50

    // ──────────────────────────────────────────────
    // Undo (Section 23)
    // ──────────────────────────────────────────────

    /** Maximum number of undo states kept in history. */
    const val MAX_UNDO_HISTORY = 3

    // ──────────────────────────────────────────────
    // Animation Timings (Section 39) — in milliseconds
    // ──────────────────────────────────────────────

    const val ANIM_TAP_SCALE_MS = 100
    const val ANIM_TILE_PLACE_MS = 150
    const val ANIM_TILE_SLIDE_MS = 200
    const val ANIM_MERGE_MOVE_MS = 150
    const val ANIM_MERGE_POP_MS = 140
    const val ANIM_NEW_TILE_MS = 130
    const val ANIM_LEVEL_COMPLETE_MS = 700
    const val ANIM_DIALOG_OPEN_MS = 215
}
