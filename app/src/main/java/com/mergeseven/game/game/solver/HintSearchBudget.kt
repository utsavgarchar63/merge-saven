package com.mergeseven.game.game.solver

/**
 * Hard wall-clock budget for a single solver invocation (AF4-03).
 */
class HintSearchBudget(
    private val budgetMs: Long,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) {
    private val deadline = nowMs() + budgetMs.coerceAtLeast(0L)

    fun hasTime(): Boolean = nowMs() < deadline

    fun expired(): Boolean = !hasTime()
}
