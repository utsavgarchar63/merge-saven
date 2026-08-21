package com.mergeseven.game.game.rules

import com.mergeseven.game.core.Constants

/**
 * Merge rule configuration.
 * See Master Plan Sections 10, 90 (Q1-Q2).
 *
 * Defines the conditions under which tiles can merge.
 */
data class MergeRule(
    /** Minimum connected group size to trigger a merge. */
    val minMergeCount: Int = Constants.MIN_MERGE_COUNT,

    /** Whether 3+ (rather than exactly 3) triggers merge. Recommended: true. */
    val allowOverMerge: Boolean = true,

    /** The progression function: default is value * 2. */
    val nextValue: (Int) -> Int = { it * 2 }
) {
    /**
     * Check if a group of the given size qualifies for merging.
     */
    fun canMerge(groupSize: Int): Boolean {
        return groupSize >= minMergeCount
    }

    /**
     * Calculate the merged value from the source value.
     */
    fun mergedValue(sourceValue: Int): Int {
        return nextValue(sourceValue)
    }
}
