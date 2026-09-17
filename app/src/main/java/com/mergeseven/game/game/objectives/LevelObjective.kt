package com.mergeseven.game.game.objectives

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Level win conditions (AF1-13). All listed objectives must pass (AF1-14).
 */
@Serializable
sealed interface LevelObjective {
    val id: String

    @Serializable
    @SerialName("reach_value")
    data class ReachValue(
        override val id: String,
        val value: Int
    ) : LevelObjective

    @Serializable
    @SerialName("clear_all_stone")
    data class ClearAllStone(
        override val id: String
    ) : LevelObjective

    @Serializable
    @SerialName("survive_moves")
    data class SurviveMoves(
        override val id: String,
        val moves: Int
    ) : LevelObjective

    @Serializable
    @SerialName("score_in_moves")
    data class ScoreInMoves(
        override val id: String,
        val score: Long,
        val moves: Int
    ) : LevelObjective

    @Serializable
    @SerialName("collect_value")
    data class CollectValue(
        override val id: String,
        val tileValue: Int,
        val count: Int
    ) : LevelObjective
}

data class ObjectiveProgress(
    val id: String,
    val label: String,
    val current: Long,
    val target: Long,
    val isComplete: Boolean,
    val isFailed: Boolean = false
)
