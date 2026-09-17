package com.mergeseven.game.game.boosters

import com.mergeseven.game.core.Constants
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoosterCatalogTest {

    private fun emptyState(
        modeId: String = ModeIds.CAMPAIGN,
        previous: GameState? = null,
        isGameOver: Boolean = false
    ): GameState {
        val board = BoardEngine().createBoard(radius = 2)
        return GameState.initial(board = board, trayPieces = emptyList(), modeId = modeId)
            .copy(previousState = previous, isGameOver = isGameOver)
    }

    @Test
    fun `every booster type has a spec`() {
        BoosterType.entries.forEach { type ->
            assertTrue(BoosterCatalog.specs.containsKey(type))
            assertEquals(type, BoosterCatalog.spec(type).type)
        }
    }

    @Test
    fun `continue cost doubles then caps`() {
        assertEquals(100, BoosterCatalog.continueCoinCost(0))
        assertEquals(200, BoosterCatalog.continueCoinCost(1))
        assertEquals(400, BoosterCatalog.continueCoinCost(2))
        assertEquals(800, BoosterCatalog.continueCoinCost(3))
        assertEquals(Constants.CONTINUE_COST_CAP, BoosterCatalog.continueCoinCost(10))
    }

    @Test
    fun `time freeze is denied outside time attack`() {
        val state = emptyState(modeId = ModeIds.ENDLESS)
        val deny = BoosterCatalog.canUse(
            BoosterType.TIME_FREEZE,
            state,
            BoosterUseContext(owned = 1, coins = 999, nowMs = 0L)
        )
        assertEquals(BoosterDenyReason.WRONG_MODE, deny)
    }

    @Test
    fun `time freeze is allowed in time attack when owned`() {
        val state = emptyState(modeId = ModeIds.TIME_ATTACK)
        val deny = BoosterCatalog.canUse(
            BoosterType.TIME_FREEZE,
            state,
            BoosterUseContext(owned = 1, coins = 0, nowMs = 0L)
        )
        assertEquals(BoosterDenyReason.OK, deny)
    }

    @Test
    fun `undo without funds or inventory is denied`() {
        val state = emptyState(previous = emptyState())
        val deny = BoosterCatalog.canUse(
            BoosterType.UNDO,
            state,
            BoosterUseContext(owned = 0, coins = 10, nowMs = 0L)
        )
        assertEquals(BoosterDenyReason.INSUFFICIENT_FUNDS, deny)
    }

    @Test
    fun `zen unlimited undo bypasses spend and cap`() {
        val state = emptyState(previous = emptyState())
        val deny = BoosterCatalog.canUse(
            BoosterType.UNDO,
            state,
            BoosterUseContext(
                owned = 0,
                coins = 0,
                nowMs = 0L,
                unlimitedUndo = true,
                undosUsedThisRun = 99
            )
        )
        assertEquals(BoosterDenyReason.OK, deny)
    }

    @Test
    fun `undo blocked after revive until next move`() {
        val state = emptyState(previous = emptyState())
        val deny = BoosterCatalog.canUse(
            BoosterType.UNDO,
            state,
            BoosterUseContext(
                owned = 1,
                coins = 999,
                nowMs = 0L,
                undoBlockedUntilMove = true
            )
        )
        assertEquals(BoosterDenyReason.UNDO_BLOCKED, deny)
    }
}
