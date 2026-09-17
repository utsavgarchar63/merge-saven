package com.mergeseven.game.game.levels

import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.GameRandom
import com.mergeseven.game.game.model.BoardState
import com.mergeseven.game.game.model.CellModifier
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.game.objectives.LevelObjective
import com.mergeseven.game.game.objectives.StarRating
import com.mergeseven.game.game.rules.LevelPool
import javax.inject.Inject
import javax.inject.Singleton

data class PreparedLevelBoard(
    val board: BoardState,
    val targetValue: Int,
    val boardProfile: String,
    val startingBoardPreset: String,
    val objectives: List<LevelObjective> = emptyList(),
    val threeStarMoveCap: Int = StarRating.DEFAULT_THREE_STAR_MOVES,
    val twoStarMoveCap: Int = StarRating.DEFAULT_TWO_STAR_MOVES
)

/**
 * Builds the initial board for a level from JSON when [Feature.AF1] is on (AF1-10…15).
 * Falls back to the default radial empty board otherwise.
 */
@Singleton
class LevelBoardFactory @Inject constructor(
    private val boardEngine: BoardEngine,
    private val loader: LevelDefinitionLoader,
    private val featureFlags: FeatureFlags
) {

    fun prepare(level: Int, random: GameRandom): PreparedLevelBoard {
        val fallbackTarget = LevelPool.getLevel(level).target.toInt()
        if (!featureFlags.isEnabled(Feature.AF1)) {
            return defaultBoard(fallbackTarget)
        }

        val definition = loader.load(level) ?: return defaultBoard(fallbackTarget)
        return buildFromDefinition(definition, random, fallbackTarget)
            ?: defaultBoard(fallbackTarget)
    }

    fun buildFromDefinition(
        definition: LevelDefinition,
        random: GameRandom,
        fallbackTarget: Int = LevelPool.getLevel(definition.level).target.toInt()
    ): PreparedLevelBoard? {
        val shape = buildShape(definition.board) ?: return null
        val modifiers = definition.modifiers.associate { dto ->
            HexCoord(dto.q, dto.r) to CellModifier(type = dto.type, scoreBonus = dto.scoreBonus)
        }

        if (modifiers.keys.any { it !in shape.playableCells }) return null
        if (definition.startingTiles.any { HexCoord(it.q, it.r) !in shape.playableCells }) return null

        var board = shape.withModifiers(modifiers)
        for (dto in definition.startingTiles) {
            val cell = HexCoord(dto.q, dto.r)
            if (board.tileAt(cell) != null) return null
            board = board.withTile(
                Tile(
                    id = random.nextId(),
                    value = dto.value,
                    cell = cell,
                    trait = dto.trait,
                    freezeStage = if (dto.trait == TileTrait.FROZEN) {
                        dto.freezeStage.coerceIn(1, 2)
                    } else {
                        0
                    },
                    multiplierFactor = if (dto.trait == TileTrait.MULTIPLIER) {
                        dto.multiplierFactor.coerceIn(2, 3)
                    } else {
                        1
                    }
                )
            )
        }

        val targetValue = (definition.target ?: fallbackTarget.toLong()).toInt()
        val objectives = definition.objectives.ifEmpty {
            listOf(LevelObjective.ReachValue(id = "reach_target", value = targetValue))
        }
        val three = definition.starMoves?.three ?: StarRating.DEFAULT_THREE_STAR_MOVES
        val two = definition.starMoves?.two ?: StarRating.DEFAULT_TWO_STAR_MOVES

        val profile = "level_%02d".format(definition.level)
        return PreparedLevelBoard(
            board = board,
            targetValue = targetValue,
            boardProfile = profile,
            startingBoardPreset = profile,
            objectives = objectives,
            threeStarMoveCap = three.coerceAtLeast(1),
            twoStarMoveCap = two.coerceAtLeast(three.coerceAtLeast(1))
        )
    }

    private fun buildShape(template: BoardTemplateDto): BoardState? = when (template.mode) {
        BoardTemplateMode.RADIUS_MINUS_HOLES -> {
            val holes = template.holes.map { HexCoord(it.q, it.r) }.toSet()
            boardEngine.createBoard(radius = template.radius, blockedCells = holes)
        }

        BoardTemplateMode.EXPLICIT -> {
            if (template.cells.isEmpty()) {
                null
            } else {
                val playable = template.cells.map { HexCoord(it.q, it.r) }.toSet()
                boardEngine.createBoard(playableCells = playable)
            }
        }
    }

    private fun defaultBoard(targetValue: Int) = PreparedLevelBoard(
        board = boardEngine.createBoard(),
        targetValue = targetValue,
        boardProfile = "default",
        startingBoardPreset = "default",
        objectives = listOf(LevelObjective.ReachValue(id = "reach_target", value = targetValue)),
        threeStarMoveCap = StarRating.DEFAULT_THREE_STAR_MOVES,
        twoStarMoveCap = StarRating.DEFAULT_TWO_STAR_MOVES
    )
}
