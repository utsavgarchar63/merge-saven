package com.mergeseven.game.cloud

/**
 * ADV-005: progress score = (totalStars, completedLevelCount, totalMerges, coins).
 * Strictly higher lexicographic tuple wins; equal or crossed dimensions → [Dominance.AMBIGUOUS].
 */
data class ProgressTuple(
    val totalStars: Int,
    val completedLevelCount: Int,
    val totalMerges: Int,
    val coins: Int
) : Comparable<ProgressTuple> {
    override fun compareTo(other: ProgressTuple): Int =
        compareValuesBy(
            this,
            other,
            { it.totalStars },
            { it.completedLevelCount },
            { it.totalMerges },
            { it.coins }
        )

    fun toSummary() = ProgressSummary(
        totalStars = totalStars,
        completedLevelCount = completedLevelCount,
        totalMerges = totalMerges,
        coins = coins
    )
}

enum class Dominance {
    LOCAL,
    CLOUD,
    AMBIGUOUS
}

object ProgressCompare {
    fun tupleFrom(snapshot: CloudSnapshot): ProgressTuple = ProgressTuple(
        totalStars = snapshot.profile.totalStars,
        completedLevelCount = snapshot.levels.count { it.isCompleted },
        totalMerges = snapshot.profile.totalMerges,
        coins = snapshot.profile.coins
    )

    fun compare(local: ProgressTuple, cloud: ProgressTuple): Dominance {
        val cmp = local.compareTo(cloud)
        return when {
            cmp > 0 -> Dominance.LOCAL
            cmp < 0 -> Dominance.CLOUD
            else -> Dominance.AMBIGUOUS
        }
    }

    /**
     * Ambiguous when neither side strictly dominates on the tuple — including equal scores —
     * or when one side wins some merge-safe fields and loses others outside the tuple
     * (caller still uses tuple for auto-resolve).
     */
    fun compareSnapshots(local: CloudSnapshot, cloud: CloudSnapshot): Dominance =
        compare(tupleFrom(local), tupleFrom(cloud))
}
