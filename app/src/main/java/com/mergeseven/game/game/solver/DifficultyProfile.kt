package com.mergeseven.game.game.solver

/**
 * Tunable weights for [BoardEvaluator] (AF4-01 / AF4-08).
 */
data class EvaluatorWeights(
    val openCells: Double = 1.0,
    val fillPenalty: Double = 40.0,
    val clusterPair: Double = 3.0,
    val maxTile: Double = 0.15,
    val targetProgress: Double = 0.02,
    val lockedPenalty: Double = 2.0
)

enum class DifficultyId {
    EASY,
    STANDARD,
    HARD
}

data class DifficultyProfile(
    val id: DifficultyId,
    val evaluatorWeights: EvaluatorWeights,
    val solverDepth: Int,
    val adaptiveSpawnEnabled: Boolean,
    val spawnTierOffset: Int
)

object DifficultyProfiles {
    val EASY = DifficultyProfile(
        id = DifficultyId.EASY,
        evaluatorWeights = EvaluatorWeights(openCells = 1.2, fillPenalty = 30.0, clusterPair = 3.5),
        solverDepth = 2,
        adaptiveSpawnEnabled = true,
        spawnTierOffset = -1
    )
    val STANDARD = DifficultyProfile(
        id = DifficultyId.STANDARD,
        evaluatorWeights = EvaluatorWeights(),
        solverDepth = 1,
        adaptiveSpawnEnabled = false,
        spawnTierOffset = 0
    )
    val HARD = DifficultyProfile(
        id = DifficultyId.HARD,
        evaluatorWeights = EvaluatorWeights(openCells = 0.8, fillPenalty = 50.0, clusterPair = 2.5),
        solverDepth = 1,
        adaptiveSpawnEnabled = false,
        spawnTierOffset = 1
    )

    fun of(id: DifficultyId): DifficultyProfile = when (id) {
        DifficultyId.EASY -> EASY
        DifficultyId.STANDARD -> STANDARD
        DifficultyId.HARD -> HARD
    }
}
