package com.mergeseven.game.game.levels

import com.mergeseven.game.game.model.CellModifierType
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.game.objectives.LevelObjective
import com.mergeseven.game.game.objectives.StarRating
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LevelDefinition(
    val level: Int,
    val target: Long? = null,
    val board: BoardTemplateDto,
    val startingTiles: List<StartingTileDto> = emptyList(),
    val modifiers: List<ModifierDto> = emptyList(),
    val objectives: List<LevelObjective> = emptyList(),
    val starMoves: StarMovesDto? = null
)

@Serializable
data class StarMovesDto(
    val three: Int = StarRating.DEFAULT_THREE_STAR_MOVES,
    val two: Int = StarRating.DEFAULT_TWO_STAR_MOVES
)

@Serializable
data class BoardTemplateDto(
    val mode: BoardTemplateMode,
    val radius: Int = 4,
    val holes: List<AxialDto> = emptyList(),
    val cells: List<AxialDto> = emptyList()
)

@Serializable
enum class BoardTemplateMode {
    @SerialName("radius_minus_holes")
    RADIUS_MINUS_HOLES,

    @SerialName("explicit")
    EXPLICIT
}

@Serializable
data class AxialDto(val q: Int, val r: Int)

@Serializable
data class StartingTileDto(
    val q: Int,
    val r: Int,
    val value: Int = 0,
    val trait: TileTrait = TileTrait.NORMAL,
    val freezeStage: Int = 0,
    val multiplierFactor: Int = 1
)

@Serializable
data class ModifierDto(
    val q: Int,
    val r: Int,
    val type: CellModifierType,
    val scoreBonus: Float = 1.5f
)
