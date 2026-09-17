package com.mergeseven.game.competitive

import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.modes.ModeSeeds
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.replay.Replay
import com.mergeseven.game.game.replay.ReplayAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSanityCheckerTest {

    private val checker = ScoreSanityChecker()

    @Test
    fun acceptsValidEndless() {
        val result = checker.check(
            ScoreSubmission(
                modeId = ModeIds.ENDLESS,
                score = 1200,
                seed = 42L,
                replay = Replay(42L, 1, emptyList()),
                maxTile = 64,
                moves = 20,
                clientVersion = "0.1.0"
            )
        )
        assertEquals(SanityResult.Ok, result)
    }

    @Test
    fun rejectsNegativeScore() {
        val result = checker.check(
            ScoreSubmission(
                modeId = ModeIds.ENDLESS,
                score = -1,
                seed = 1L,
                replay = Replay(1L, 1, emptyList()),
                maxTile = 2,
                moves = 1,
                clientVersion = "0.1.0"
            )
        )
        assertTrue(result is SanityResult.Rejected)
    }

    @Test
    fun rejectsDailySeedMismatch() {
        val date = "2026-09-09"
        val result = checker.check(
            ScoreSubmission(
                modeId = ModeIds.DAILY,
                score = 100,
                seed = 999L,
                dateKey = date,
                replay = Replay(999L, 1, emptyList()),
                maxTile = 16,
                moves = 5,
                clientVersion = "0.1.0"
            )
        )
        assertTrue(result is SanityResult.Rejected)
        assertEquals("daily_seed_mismatch", (result as SanityResult.Rejected).reason)
    }

    @Test
    fun acceptsMatchingDailySeed() {
        val date = "2026-09-09"
        val seed = ModeSeeds.dailySeed(date)
        val result = checker.check(
            ScoreSubmission(
                modeId = ModeIds.DAILY,
                score = 500,
                seed = seed,
                dateKey = date,
                replay = Replay(seed, 1, emptyList()),
                maxTile = 32,
                moves = 10,
                clientVersion = "0.1.0"
            )
        )
        assertEquals(SanityResult.Ok, result)
    }
}

class SessionReplayRecorderTest {

    @Test
    fun buildsReplayFromActions() {
        val recorder = SessionReplayRecorder()
        recorder.start(seed = 7L, level = 1)
        recorder.recordPlace(0, HexCoord(0, 0))
        recorder.recordRotate(1)
        recorder.recordShuffle()
        recorder.recordUndo()
        val replay = recorder.buildReplay()!!
        assertEquals(7L, replay.seed)
        assertEquals(4, replay.actions.size)
        assertTrue(replay.actions[0] is ReplayAction.Place)
    }

    @Test
    fun incompleteBlocksSubmit() {
        val recorder = SessionReplayRecorder()
        recorder.start(1L, 1)
        recorder.markIncomplete()
        assertEquals(false, recorder.canSubmit())
        assertEquals(null, recorder.buildReplay())
    }
}
