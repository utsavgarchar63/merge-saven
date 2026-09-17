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

    /**
     * Applied to a merge's score when the group contains at least one BOMB (AF1-03).
     * Keeps bomb clears from being an unbounded scoring exploit.
     */
    const val BOMB_SCORE_FACTOR = 0.5f

    // ──────────────────────────────────────────────
    // Economy (Section 21, Section 91)
    // ──────────────────────────────────────────────

    const val INITIAL_COIN_BALANCE = 100
    const val UNDO_COST = 50
    const val SWAP_COST = 40
    const val RANDOMIZE_COST = 80
    const val REMOVE_COST = 120
    const val CONTINUE_COST = 100
    const val CONTINUE_COST_CAP = 800
    const val CONTINUE_CLEAR_TILES = 5
    const val MAX_COIN_CONTINUES_PER_RUN = 2
    const val REWARDED_CONTINUE_LIMIT = 1
    const val HAMMER_COST = 150
    const val VALUE_UP_COST = 120
    const val MAGNET_COST = 180
    const val TIME_FREEZE_COST = 100
    const val TIME_FREEZE_MS = 15_000L
    const val MAX_UNDOS_PER_RUN = 5
    const val VALUE_UP_CAP = 2048
    const val DAILY_REWARD = 50
    const val REWARDED_COIN_GRANT = 100
    const val INSUFFICIENT_FUNDS_REWARD_COINS = 50
    /** AF9-08 daily coins while Premium subscription is active. */
    const val PREMIUM_DAILY_STIPEND_COINS = 75
    const val HINT_COST = 30
    const val HINT_COOLDOWN_MS = 10_000L
    const val MAX_HINTS_PER_RUN = 5
    const val HINT_SOLVER_BUDGET_MS = 30L
    const val DAILY_SEED_VALIDATE_BUDGET_MS = 200L
    const val DAILY_SEED_MAX_SALT = 63
    const val ADAPTIVE_SPAWN_FAIL_THRESHOLD = 3
    const val ADAPTIVE_SPAWN_BOOST = 4

    // ──────────────────────────────────────────────
    // Meta progression (AF5)
    // ──────────────────────────────────────────────

    const val XP_PER_MERGE = 2
    const val XP_PER_CHAIN_STEP = 1
    const val XP_LEVEL_CLEAR = 50
    const val XP_QUEST_CLAIM = 25

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

    // ──────────────────────────────────────────────
    // AF10 — Presentation & Game Feel
    // ──────────────────────────────────────────────

    const val AF10_PARTICLE_POOL_SIZE = 256
    const val AF10_PARTICLE_CAP_FULL = 120
    const val AF10_PARTICLE_CAP_HALF = 60
    const val AF10_PARTICLE_CAP_QUARTER = 30
    const val AF10_FRAME_BUDGET_MS = 17.5f
    const val AF10_MAX_SHAKE_PX = 12f
    const val AF10_MAX_HIT_STOP_MS = 80L
    const val AF10_SLOW_MO_CHAIN_MIN = 4
    const val AF10_SLOW_MO_SCALE = 0.55f
    const val AF10_SLOW_MO_MS = 400L
    const val AF10_COMBO_BANNER_MS = 900L
    const val AF10_SHAKE_DECAY_MS = 280L
    const val AF10_MUSIC_INTENSITY_DECAY_MS = 4_000L
    const val AF10_SFX_PITCH_MIN = 0.85f
    const val AF10_SFX_PITCH_MAX = 1.35f
    const val AF10_DROP_SNAP_MS = 280
    const val AF10_DROP_TRAIL_SAMPLES = 6
}
