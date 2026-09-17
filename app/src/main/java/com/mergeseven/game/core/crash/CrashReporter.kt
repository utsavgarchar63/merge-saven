package com.mergeseven.game.core.crash

/**
 * Crash / non-fatal reporting seam (AF6-03).
 */
interface CrashReporter {
    fun setCustomKey(key: String, value: String)
    fun setCustomKey(key: String, value: Int)
    fun setCustomKey(key: String, value: Long)
    fun log(message: String)
    fun recordNonFatal(throwable: Throwable, message: String? = null)

    fun updateSession(
        level: Int,
        mode: String,
        seed: Long,
        boardHash: String,
        lastAction: String
    ) {
        setCustomKey(KEY_LEVEL, level)
        setCustomKey(KEY_MODE, mode)
        setCustomKey(KEY_SEED, seed)
        setCustomKey(KEY_BOARD_HASH, boardHash)
        setCustomKey(KEY_LAST_ACTION, lastAction)
    }

    companion object {
        const val KEY_LEVEL = "level"
        const val KEY_MODE = "mode"
        const val KEY_SEED = "seed"
        const val KEY_BOARD_HASH = "board_hash"
        const val KEY_LAST_ACTION = "last_action"
    }
}
