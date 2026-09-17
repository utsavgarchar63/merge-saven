package com.mergeseven.game.game.engine

import com.mergeseven.game.game.model.RngState

/**
 * The only source of randomness the game engine is allowed to use.
 *
 * Deliberately not `kotlin.random.Random`: that hides its state, so a game using it could never be
 * resumed or replayed. This is a SplitMix64 generator whose entire state is one `Long`, which means
 * the position in the sequence can be saved to disk and picked back up exactly ([snapshot]).
 *
 * Create one per engine operation from the state's [RngState], use it, then write [snapshot] back
 * into the new game state. It is mutable and not thread safe, which is fine because it never
 * outlives a single call.
 */
class GameRandom(state: RngState) {

    private val seed: Long = state.seed
    private var cursor: Long = state.cursor
    private var draws: Int = state.draws
    private var nextId: Long = state.nextEntityId

    /** Draws the next value in the sequence. */
    fun nextLong(): Long {
        cursor += GOLDEN_GAMMA
        draws++
        return mix(cursor)
    }

    /**
     * Draws a value in `0 until bound`.
     *
     * Uses a plain remainder, which is very slightly biased towards low values when [bound] is not
     * a power of two. For spawn weights and shape selection that bias is far below anything a
     * player could perceive, and the simplicity keeps the sequence easy to reason about.
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        return (nextLong().toULong() % bound.toULong()).toInt()
    }

    /** Hands out the next tile or piece id. Does not consume a random value. */
    fun nextId(): Long = nextId++

    /** Captures the position so it can be stored and later restored. */
    fun snapshot(): RngState = RngState(
        seed = seed,
        cursor = cursor,
        draws = draws,
        nextEntityId = nextId
    )

    private companion object {
        /** SplitMix64 increment: the 64-bit golden ratio, 0x9E3779B97F4A7C15. */
        const val GOLDEN_GAMMA: Long = -0x61c8864680b583ebL

        fun mix(value: Long): Long {
            var z = value
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
            return z xor (z ushr 31)
        }
    }
}
