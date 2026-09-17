package com.mergeseven.game.competitive

import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.ChainReactionEngine
import com.mergeseven.game.game.engine.GameEngineImpl
import com.mergeseven.game.game.engine.GameOverEngine
import com.mergeseven.game.game.engine.MergeEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.engine.ScoreEngine
import com.mergeseven.game.game.engine.SpawnEngine
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.modes.ModeSeeds
import com.mergeseven.game.game.replay.Replay
import com.mergeseven.game.game.replay.ReplayRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDailyScoreValidatorTest {

    private val scoreEngine = ScoreEngine()
    private val mergeEngine = MergeEngine(scoreEngine)
    private val placementEngine = PlacementEngine()
    private val engine = GameEngineImpl(
        boardEngine = BoardEngine(),
        mergeEngine = mergeEngine,
        placementEngine = placementEngine,
        chainReactionEngine = ChainReactionEngine(mergeEngine),
        scoreEngine = scoreEngine,
        spawnEngine = SpawnEngine(),
        gameOverEngine = GameOverEngine(placementEngine)
    )

    @Test
    fun rejectsShadowBanned() = runBlocking {
        val validator = FakeDailyScoreValidator(ReplayRunner(engine), shadowBanned = setOf("bad"))
        val date = "2026-09-09"
        val seed = ModeSeeds.dailySeed(date)
        val outcome = validator.validate(
            ScoreSubmission(
                modeId = ModeIds.DAILY,
                score = 0,
                seed = seed,
                dateKey = date,
                replay = Replay(seed, 1, emptyList()),
                maxTile = 0,
                moves = 0,
                clientVersion = "test"
            ),
            playerId = "bad"
        )
        assertFalse(outcome.accepted)
        assertEquals("shadow_banned", outcome.reason)
    }

    @Test
    fun acceptsFaithfulEmptyReplayScoreZero() = runBlocking {
        val validator = FakeDailyScoreValidator(ReplayRunner(engine))
        val date = "2026-09-09"
        val seed = ModeSeeds.dailySeed(date)
        val outcome = validator.validate(
            ScoreSubmission(
                modeId = ModeIds.DAILY,
                score = 0,
                seed = seed,
                dateKey = date,
                replay = Replay(seed, 1, emptyList()),
                maxTile = 0,
                moves = 0,
                clientVersion = "test"
            ),
            playerId = "ok"
        )
        assertTrue(outcome.accepted)
    }

    @Test
    fun rejectsClaimedScoreMismatch() = runBlocking {
        val validator = FakeDailyScoreValidator(ReplayRunner(engine))
        val date = "2026-09-09"
        val seed = ModeSeeds.dailySeed(date)
        val outcome = validator.validate(
            ScoreSubmission(
                modeId = ModeIds.DAILY,
                score = 99999,
                seed = seed,
                dateKey = date,
                replay = Replay(seed, 1, emptyList()),
                maxTile = 0,
                moves = 0,
                clientVersion = "test"
            ),
            playerId = "ok"
        )
        assertFalse(outcome.accepted)
        assertEquals("score_mismatch", outcome.reason)
    }
}
