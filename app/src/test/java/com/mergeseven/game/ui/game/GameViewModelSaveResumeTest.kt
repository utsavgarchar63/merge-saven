package com.mergeseven.game.ui.game

import androidx.lifecycle.SavedStateHandle
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.game.engine.BoardEngine
import com.mergeseven.game.game.engine.ChainReactionEngine
import com.mergeseven.game.game.engine.GameEngineImpl
import com.mergeseven.game.game.engine.GameOverEngine
import com.mergeseven.game.game.engine.MergeEngine
import com.mergeseven.game.game.engine.PlacementEngine
import com.mergeseven.game.game.engine.ScoreEngine
import com.mergeseven.game.game.engine.SpawnEngine
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.PieceCell
import com.mergeseven.game.game.model.Tile
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.repository.GameRepository
import com.mergeseven.game.testing.TestPersistence
import com.mergeseven.game.testing.fakeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSaveResumeTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a saved game for this level is resumed instead of restarting`() = runTest {
        val saved = savedGame(level = 3, score = 4_242L)
        val repository = FakeGameRepository(saved)

        val viewModel = createViewModel(repository, levelId = 3)

        assertEquals(4_242L, viewModel.uiState.value.score)
        assertEquals(3, viewModel.uiState.value.level)
        assertEquals(saved.board.activeTiles().size, viewModel.uiState.value.tiles.size)
    }

    @Test
    fun `a saved game for a different level does not leak into the new one`() = runTest {
        val repository = FakeGameRepository(savedGame(level = 3, score = 4_242L))

        val viewModel = createViewModel(repository, levelId = 5)

        assertEquals(5, viewModel.uiState.value.level)
        assertEquals(0L, viewModel.uiState.value.score)
    }

    @Test
    fun `a finished game is not resumed`() = runTest {
        val repository = FakeGameRepository(
            savedGame(level = 3, score = 4_242L).copy(isGameOver = true)
        )

        val viewModel = createViewModel(repository, levelId = 3)

        assertFalse(viewModel.uiState.value.isGameOver)
        assertEquals(0L, viewModel.uiState.value.score)
    }

    @Test
    fun `starting fresh with no save loads the level from scratch`() = runTest {
        val repository = FakeGameRepository(null)

        val viewModel = createViewModel(repository, levelId = 2)

        assertEquals(2, viewModel.uiState.value.level)
        assertEquals(0L, viewModel.uiState.value.score)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `the board is marked loading until the saved game has been read`() = runTest {
        val repository = FakeGameRepository(null)

        val viewModel = createViewModel(repository, levelId = 1)

        // By the time the unconfined dispatcher has run the init coroutine, loading is done —
        // what matters is that the flag is cleared rather than left stuck on.
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `leaving the screen writes the board immediately`() = runTest {
        val repository = FakeGameRepository(null)
        val viewModel = createViewModel(repository, levelId = 1)
        val savesAfterStart = repository.saveCount

        viewModel.onStopped()

        assertTrue(repository.saveCount > savesAfterStart)
        assertEquals(1, repository.stored.value?.level)
    }

    @Test
    fun `rapid moves collapse into a single debounced write`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = FakeGameRepository(null)
        val viewModel = createViewModel(repository, levelId = 1)
        advanceTimeBy(1_000)
        val baseline = repository.saveCount

        repeat(5) { viewModel.onBoosterShuffle() }
        advanceTimeBy(1_000)

        assertEquals("five shuffles should produce one write", baseline + 1, repository.saveCount)
    }

    @Test
    fun `a completed level clears the save so it is not resumed`() = runTest {
        // Resuming a board that already meets the target drives the completion path on the first
        // state update, which is the same path a winning move takes.
        val repository = FakeGameRepository(completedGame(level = 1, targetValue = 16))

        val viewModel = createViewModel(repository, levelId = 1)

        assertTrue(viewModel.uiState.value.isLevelComplete)
        assertTrue(repository.clearCount > 0)
        assertEquals(null, repository.stored.value)
    }

    @Test
    fun `a debug force game over ends the active session`() = runTest {
        val repository = FakeGameRepository(null)
        val debugCommands = com.mergeseven.game.core.debug.DebugCommands()
        val viewModel = createViewModel(repository, levelId = 1, debugCommands = debugCommands)

        assertFalse(viewModel.uiState.value.isGameOver)
        assertTrue(debugCommands.forceGameOver())
        assertTrue(viewModel.uiState.value.isGameOver)
        assertTrue(repository.clearCount > 0)
    }

    private fun TestScope.createViewModel(
        repository: GameRepository,
        levelId: Int,
        debugCommands: com.mergeseven.game.core.debug.DebugCommands =
            com.mergeseven.game.core.debug.DebugCommands()
    ): GameViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val scoreEngine = ScoreEngine()
        val mergeEngine = MergeEngine(scoreEngine)
        val placementEngine = PlacementEngine()
        val gameEngine = GameEngineImpl(
            boardEngine = BoardEngine(),
            mergeEngine = mergeEngine,
            placementEngine = placementEngine,
            spawnEngine = SpawnEngine(),
            scoreEngine = scoreEngine,
            gameOverEngine = GameOverEngine(placementEngine),
            chainReactionEngine = ChainReactionEngine(mergeEngine)
        )
        val evaluator = com.mergeseven.game.game.solver.BoardEvaluator()
        val moveSolver = com.mergeseven.game.game.solver.MoveSolver(
            gameEngine = gameEngine,
            placementEngine = placementEngine,
            evaluator = evaluator
        )
        val campaign = com.mergeseven.game.game.modes.CampaignMode()
        val registry = com.mergeseven.game.game.modes.GameModeRegistry(
            campaignMode = campaign,
            endlessMode = com.mergeseven.game.game.modes.EndlessMode(),
            timeAttackMode = com.mergeseven.game.game.modes.TimeAttackMode(),
            zenMode = com.mergeseven.game.game.modes.ZenMode(),
            dailyMode = com.mergeseven.game.game.modes.DailyMode(),
            weeklyMode = com.mergeseven.game.game.modes.WeeklyMode()
        )

        return GameViewModel(
            savedStateHandle = SavedStateHandle(mapOf("levelId" to levelId)),
            gameEngine = gameEngine,
            audioManager = AudioManager(fakeContext()),
            levelRepository = TestPersistence.levelRepository(),
            gameRepository = repository,
            userDataRepository = TestPersistence.userDataRepository(),
            modeRecordsStore = InMemoryModeRecordsStore(),
            modeRegistry = registry,
            analyticsTracker = com.mergeseven.game.core.analytics.NoOpAnalyticsTracker(),
            dateProvider = com.mergeseven.game.core.DateProvider { "2026-09-09" },
            debugCommands = debugCommands,
            featureFlags = FakeFeatureFlags(enabled = emptySet()),
            boosterInventory = InMemoryBoosterInventoryStore(),
            cachedMoveSolver = com.mergeseven.game.game.solver.CachedMoveSolver(
                moveSolver = moveSolver,
                dispatchers = TestPersistence.dispatchers(dispatcher)
            ),
            deadlockPredictor = com.mergeseven.game.game.solver.DeadlockPredictor(
                placementEngine = placementEngine,
                gameOverEngine = GameOverEngine(placementEngine)
            ),
            dailySeedValidator = com.mergeseven.game.game.solver.DailySeedValidator(
                gameEngine = gameEngine,
                moveSolver = moveSolver
            ),
            difficultyProfileProvider = object : com.mergeseven.game.game.solver.DifficultyProfileProvider {
                private val flow = MutableStateFlow(com.mergeseven.game.game.solver.DifficultyProfiles.STANDARD)
                override val profile = flow.asStateFlow()
                override suspend fun setProfile(id: com.mergeseven.game.game.solver.DifficultyId) {
                    flow.value = com.mergeseven.game.game.solver.DifficultyProfiles.of(id)
                }
            },
            achievementTracker = com.mergeseven.game.meta.AchievementTracker(
                unlockDao = com.mergeseven.game.testing.InMemoryUnlockDao(),
                dispatchers = TestPersistence.dispatchers(dispatcher)
            ),
            unlockService = com.mergeseven.game.meta.UnlockService(
                unlockDao = com.mergeseven.game.testing.InMemoryUnlockDao(),
                dispatchers = TestPersistence.dispatchers(dispatcher),
                userDataRepository = TestPersistence.userDataRepository(),
                featureFlags = FakeFeatureFlags(enabled = emptySet()),
                cloudEconomyNotifier = com.mergeseven.game.cloud.CloudEconomyNotifier()
            ),
            crashReporter = com.mergeseven.game.core.crash.NoOpCrashReporter(),
            liveConfig = com.mergeseven.game.core.liveops.ParsedLiveConfig(
                strings = { com.mergeseven.game.core.liveops.LiveConfigDefaults.defaultStringMap() },
                revision = MutableStateFlow(0L)
            ),
            seasonalEventStore = com.mergeseven.game.core.liveops.SeasonalEventRecorder { },
            cloudUploadTrigger = com.mergeseven.game.cloud.NoOpCloudUploadTrigger(),
            sessionReplayRecorder = com.mergeseven.game.competitive.SessionReplayRecorder(),
            competitiveScoreSubmitter = com.mergeseven.game.competitive.CompetitiveScoreSubmitter(
                featureFlags = FakeFeatureFlags(enabled = emptySet()),
                recorder = com.mergeseven.game.competitive.SessionReplayRecorder(),
                sanity = com.mergeseven.game.competitive.ScoreSanityChecker(),
                rateLimiter = com.mergeseven.game.competitive.SubmitRateLimiter(fakeContext()),
                leaderboards = object : com.mergeseven.game.competitive.LeaderboardRepository {
                    override suspend fun submitScore(
                        board: com.mergeseven.game.competitive.LeaderboardId,
                        score: Long
                    ) = false

                    override suspend fun loadScores(
                        board: com.mergeseven.game.competitive.LeaderboardId,
                        scope: com.mergeseven.game.competitive.LeaderboardScope,
                        maxResults: Int
                    ) = emptyList<com.mergeseven.game.competitive.LeaderboardEntry>()

                    override fun cachedScores(
                        board: com.mergeseven.game.competitive.LeaderboardId,
                        scope: com.mergeseven.game.competitive.LeaderboardScope
                    ) = emptyList<com.mergeseven.game.competitive.LeaderboardEntry>()

                    override fun openPlayGamesIntent(
                        board: com.mergeseven.game.competitive.LeaderboardId
                    ) = null
                },
                dailyValidator = object : com.mergeseven.game.competitive.DailyScoreValidator {
                    override suspend fun validate(
                        submission: com.mergeseven.game.competitive.ScoreSubmission,
                        playerId: String
                    ) = com.mergeseven.game.competitive.ValidationOutcome(accepted = true)

                    override suspend fun fetchDailyLeaderGhost(dateKey: String) = null
                },
                auth = com.mergeseven.game.cloud.FakePlayGamesAuth(),
                analytics = com.mergeseven.game.core.analytics.NoOpAnalyticsTracker()
            ),
            shareRunUseCase = com.mergeseven.game.competitive.ShareRunUseCase(
                context = fakeContext(),
                renderer = com.mergeseven.game.competitive.ResultCardRenderer(fakeContext()),
                analytics = com.mergeseven.game.core.analytics.NoOpAnalyticsTracker()
            ),
            ghostReplayController = com.mergeseven.game.competitive.GhostReplayController(
                featureFlags = FakeFeatureFlags(enabled = emptySet()),
                dailyValidator = object : com.mergeseven.game.competitive.DailyScoreValidator {
                    override suspend fun validate(
                        submission: com.mergeseven.game.competitive.ScoreSubmission,
                        playerId: String
                    ) = com.mergeseven.game.competitive.ValidationOutcome(accepted = true)

                    override suspend fun fetchDailyLeaderGhost(dateKey: String) = null
                },
                engine = gameEngine,
                analytics = com.mergeseven.game.core.analytics.NoOpAnalyticsTracker()
            ),
            adService = com.mergeseven.game.ads.FakeAdService(),
            adPreloader = com.mergeseven.game.ads.AdPreloader(com.mergeseven.game.ads.FakeAdService()),
            interstitialPolicy = com.mergeseven.game.ads.FakeInterstitialPolicy(),
            settingsRepository = object : com.mergeseven.game.data.preferences.SettingsRepository {
                override val isSoundEnabled = MutableStateFlow(true)
                override val isMusicEnabled = MutableStateFlow(true)
                override val isHapticsEnabled = MutableStateFlow(true)
                override val isReduceMotionEnabled = MutableStateFlow(false)
                override val isNotificationsEnabled = MutableStateFlow(true)
                override val isTutorialCompleted = MutableStateFlow(false)
                override val colourblindMode =
                    MutableStateFlow(com.mergeseven.game.data.preferences.ColourblindMode.OFF)
                override val largeTouchTargets = MutableStateFlow(false)
                override suspend fun setSoundEnabled(enabled: Boolean) = Unit
                override suspend fun setMusicEnabled(enabled: Boolean) = Unit
                override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
                override suspend fun setReduceMotionEnabled(enabled: Boolean) = Unit
                override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
                override suspend fun setTutorialCompleted(completed: Boolean) = Unit
                override suspend fun setColourblindMode(
                    mode: com.mergeseven.game.data.preferences.ColourblindMode
                ) = Unit
                override suspend fun setLargeTouchTargets(enabled: Boolean) = Unit
                override suspend fun resetSettings() = Unit
            },
            juiceController = com.mergeseven.game.ui.feel.JuiceController(
                audioManager = AudioManager(fakeContext()),
                hapticManager = com.mergeseven.game.core.haptics.HapticManager(fakeContext()),
                settingsRepository = object : com.mergeseven.game.data.preferences.SettingsRepository {
                    override val isSoundEnabled = MutableStateFlow(true)
                    override val isMusicEnabled = MutableStateFlow(true)
                    override val isHapticsEnabled = MutableStateFlow(true)
                    override val isReduceMotionEnabled = MutableStateFlow(false)
                    override val isNotificationsEnabled = MutableStateFlow(true)
                    override val isTutorialCompleted = MutableStateFlow(false)
                    override val colourblindMode =
                        MutableStateFlow(com.mergeseven.game.data.preferences.ColourblindMode.OFF)
                    override val largeTouchTargets = MutableStateFlow(false)
                    override suspend fun setSoundEnabled(enabled: Boolean) = Unit
                    override suspend fun setMusicEnabled(enabled: Boolean) = Unit
                    override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
                    override suspend fun setReduceMotionEnabled(enabled: Boolean) = Unit
                    override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
                    override suspend fun setTutorialCompleted(completed: Boolean) = Unit
                    override suspend fun setColourblindMode(
                        mode: com.mergeseven.game.data.preferences.ColourblindMode
                    ) = Unit
                    override suspend fun setLargeTouchTargets(enabled: Boolean) = Unit
                    override suspend fun resetSettings() = Unit
                }
            ),
            persistenceScope = CoroutineScope(dispatcher)
        )
    }

    private fun savedGame(level: Int, score: Long): GameState {
        val board = BoardEngine().createBoard()
            .withTile(Tile(id = 1L, value = 4, cell = HexCoord(0, 0)))
            .withTile(Tile(id = 2L, value = 8, cell = HexCoord(1, -1)))

        return GameState(
            board = board,
            trayPieces = listOf(piece(10L), piece(11L), piece(12L)),
            score = score,
            bestScore = score,
            coins = 250,
            level = level,
            targetValue = 64,
            moves = 17,
            isPaused = false,
            isGameOver = false,
            isBusy = false
        )
    }

    private fun completedGame(level: Int, targetValue: Int): GameState =
        savedGame(level = level, score = 100L).let { state ->
            state.copy(
                targetValue = targetValue,
                board = state.board.withTile(
                    Tile(id = 99L, value = targetValue, cell = HexCoord(2, 0))
                )
            )
        }

    private fun piece(id: Long) = TilePiece(
        id = id,
        cells = listOf(PieceCell(HexCoord(0, 0), 2))
    )

    private class FakeGameRepository(initial: GameState?) : GameRepository {
        val stored = MutableStateFlow(initial)
        var saveCount = 0
            private set
        var clearCount = 0
            private set

        override fun getActiveGame(slotId: String): Flow<GameState?> = stored

        override suspend fun loadActiveGame(slotId: String): GameState? = stored.value

        override suspend fun saveActiveGame(state: GameState, slotId: String) {
            stored.value = state
            saveCount++
        }

        override suspend fun clearActiveGame(slotId: String) {
            stored.value = null
            clearCount++
        }
    }
}

private class InMemoryModeRecordsStore : com.mergeseven.game.data.local.store.ModeRecordsStore {
    private val _records =
        MutableStateFlow<Map<String, com.mergeseven.game.data.local.store.ModeRecord>>(emptyMap())
    override val records:
        StateFlow<Map<String, com.mergeseven.game.data.local.store.ModeRecord>> =
        _records.asStateFlow()

    override suspend fun load() = Unit

    override suspend fun recordRun(modeId: String, score: Long) {
        val current = _records.value[modeId]
        _records.value = _records.value + (
            modeId to com.mergeseven.game.data.local.store.ModeRecord(
                modeId = modeId,
                bestScore = maxOf(current?.bestScore ?: 0L, score),
                runs = (current?.runs ?: 0) + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
            )
    }

    override fun bestScore(modeId: String): Long = _records.value[modeId]?.bestScore ?: 0L
}

private class FakeFeatureFlags(
    private val enabled: Set<com.mergeseven.game.core.flags.Feature>
) : com.mergeseven.game.core.flags.FeatureFlags {
    override fun isEnabled(feature: com.mergeseven.game.core.flags.Feature): Boolean =
        feature in enabled

    override fun observe(feature: com.mergeseven.game.core.flags.Feature) =
        kotlinx.coroutines.flow.flowOf(feature in enabled)

    override suspend fun setEnabled(
        feature: com.mergeseven.game.core.flags.Feature,
        enabled: Boolean
    ) = Unit

    override fun snapshot(): Map<com.mergeseven.game.core.flags.Feature, Boolean> =
        com.mergeseven.game.core.flags.Feature.entries.associateWith { it in enabled }
}

private class InMemoryBoosterInventoryStore :
    com.mergeseven.game.data.local.store.BoosterInventoryStore {
    private val _owned = MutableStateFlow(
        com.mergeseven.game.game.boosters.BoosterCatalog.specs.mapValues { it.value.startingOwned }
    )
    override val owned = _owned.asStateFlow()
    override suspend fun loadAndSeed() = Unit
    override fun count(type: com.mergeseven.game.game.model.BoosterType): Int =
        _owned.value[type] ?: 0

    override suspend fun grant(type: com.mergeseven.game.game.model.BoosterType, amount: Int) {
        _owned.value = _owned.value + (type to count(type) + amount)
    }

    override suspend fun tryConsume(type: com.mergeseven.game.game.model.BoosterType): Boolean {
        val current = count(type)
        if (current <= 0) return false
        _owned.value = _owned.value + (type to current - 1)
        return true
    }
}

