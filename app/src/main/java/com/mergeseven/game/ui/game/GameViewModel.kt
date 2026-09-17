package com.mergeseven.game.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.ads.AdPlacement
import com.mergeseven.game.ads.AdPreloader
import com.mergeseven.game.ads.AdResult
import com.mergeseven.game.ads.AdService
import com.mergeseven.game.ads.InterstitialPolicy
import com.mergeseven.game.cloud.CloudUploadTrigger
import com.mergeseven.game.cloud.UploadReason
import com.mergeseven.game.competitive.CompetitiveScoreSubmitter
import com.mergeseven.game.competitive.GhostFrame
import com.mergeseven.game.competitive.GhostReplayController
import com.mergeseven.game.competitive.SessionReplayRecorder
import com.mergeseven.game.competitive.ShareRunData
import com.mergeseven.game.competitive.ShareRunUseCase
import com.mergeseven.game.competitive.SubmitStatus
import com.mergeseven.game.core.Constants
import android.content.Intent
import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.audio.AudioManager
import com.mergeseven.game.core.crash.BoardHash
import com.mergeseven.game.core.crash.CrashReporter
import com.mergeseven.game.core.debug.DebugCommand
import com.mergeseven.game.core.debug.DebugCommands
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveConfig
import com.mergeseven.game.core.liveops.SeasonalEventRecorder
import com.mergeseven.game.data.local.store.BoosterInventoryStore
import com.mergeseven.game.data.local.store.ModeRecordsStore
import com.mergeseven.game.data.preferences.ColourblindMode
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.LevelRepository
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.di.PersistenceScope
import com.mergeseven.game.game.boosters.BoosterCatalog
import com.mergeseven.game.game.boosters.BoosterDenyReason
import com.mergeseven.game.game.boosters.BoosterUseContext
import com.mergeseven.game.game.engine.GameEngine
import com.mergeseven.game.game.engine.HexGeometry
import com.mergeseven.game.game.engine.SpawnHints
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.game.model.CellModifier
import com.mergeseven.game.game.model.GameState
import com.mergeseven.game.game.model.HexCoord
import com.mergeseven.game.game.model.TilePiece
import com.mergeseven.game.game.model.TileTrait
import com.mergeseven.game.game.modes.GameMode
import com.mergeseven.game.game.modes.GameModeRegistry
import com.mergeseven.game.game.modes.ModeEndReason
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.modes.ModeSeeds
import com.mergeseven.game.game.modes.ModeSessionContext
import com.mergeseven.game.game.modes.ZenMode
import com.mergeseven.game.game.objectives.ObjectiveEvaluator
import com.mergeseven.game.game.objectives.ObjectiveProgress
import com.mergeseven.game.game.repository.GameRepository
import com.mergeseven.game.game.solver.CachedMoveSolver
import com.mergeseven.game.game.solver.DailySeedValidator
import com.mergeseven.game.game.solver.DeadlockPredictor
import com.mergeseven.game.game.solver.DifficultyProfileProvider
import com.mergeseven.game.game.solver.MoveHint
import com.mergeseven.game.meta.AchievementMetric
import com.mergeseven.game.meta.AchievementTracker
import com.mergeseven.game.meta.UnlockService
import com.mergeseven.game.ui.feel.JuiceController
import com.mergeseven.game.ui.theme.BoardThemes
import com.mergeseven.game.ui.theme.GameColors
import com.mergeseven.game.ui.theme.TileThemes
import com.mergeseven.game.ui.theme.withColourblindMode
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** Board tile ready for Canvas (value, colour, trait silhouette). */
data class TileUi(
    val value: Int,
    val color: Color,
    val trait: TileTrait = TileTrait.NORMAL,
    val freezeStage: Int = 0,
    val multiplierFactor: Int = 1
)

data class BoosterButtonUi(
    val type: BoosterType,
    val enabled: Boolean,
    val cost: Int,
    val owned: Int,
    val selected: Boolean = false
)

enum class AccessibilityAnnounceKind {
    PLACED,
    MERGED,
    INVALID,
    LEVEL_COMPLETE,
    GAME_OVER
}

data class AccessibilityAnnouncement(
    val kind: AccessibilityAnnounceKind,
    val score: Long = 0L,
    val nonce: Long = System.nanoTime()
)

/**
 * UI State exposed to the GameScreen.
 */
data class GameUiState(
    val isLoading: Boolean = true,
    val level: Int = 1,
    val score: Long = 0,
    val coins: Int = 0,
    val targetValue: Int = 16,
    val boardRadius: Int = Constants.DEFAULT_BOARD_RADIUS,
    val boardCells: Set<HexCoord> = emptySet(),
    val cellModifiers: Map<HexCoord, CellModifier> = emptyMap(),
    val tiles: Map<HexCoord, TileUi> = emptyMap(),
    val trayPieces: List<TilePiece?> = listOf(null, null, null),
    val selectedSlotIndex: Int = 0,
    val isGameOver: Boolean = false,
    val isLevelComplete: Boolean = false,
    val starsEarned: Int = 0,
    val objectiveChips: List<ObjectiveProgress> = emptyList(),
    val canUndo: Boolean = false,
    val hoveredCells: List<Pair<HexCoord, Boolean>> = emptyList(),
    val modeId: String = ModeIds.CAMPAIGN,
    val showObjectives: Boolean = true,
    val showTimer: Boolean = false,
    val showTarget: Boolean = true,
    val timeRemainingMs: Long = 0L,
    val paletteKey: String = "default",
    val suppressAds: Boolean = false,
    val af3Enabled: Boolean = false,
    val boosterButtons: List<BoosterButtonUi> = emptyList(),
    val pendingBooster: BoosterType? = null,
    val continueCoinCost: Int = Constants.CONTINUE_COST,
    val canCoinContinue: Boolean = false,
    val canRewardedContinue: Boolean = false,
    val showInsufficientFunds: Boolean = false,
    val confirmBooster: BoosterType? = null,
    val af4Enabled: Boolean = false,
    val hintCells: List<HexCoord> = emptyList(),
    val canUseHint: Boolean = false,
    val showDeadlockWarning: Boolean = false,
    val hintsUsedThisRun: Int = 0,
    val boardThemeId: String = "wood",
    val tileThemeId: String = "classic",
    val achievementToast: String? = null,
    val af8Enabled: Boolean = false,
    val shareReady: Boolean = false,
    val scoreSubmitStatus: String? = null,
    val ghostFrames: List<GhostFrame> = emptyList(),
    val showGhostOverlay: Boolean = false,
    val af9Enabled: Boolean = false,
    val canDoubleCoins: Boolean = false,
    val canWatchHintAd: Boolean = false,
    val adStatusMessage: String? = null,
    val af11Enabled: Boolean = false,
    val colourblindMode: ColourblindMode = ColourblindMode.OFF,
    val largeTouchTargets: Boolean = false,
    val accessibilityAnnouncement: AccessibilityAnnouncement? = null
) {
    val currentPiece: TilePiece?
        get() = trayPieces.getOrNull(selectedSlotIndex) ?: trayPieces.firstOrNull { it != null }

    val nextPieces: List<TilePiece>
        get() = trayPieces.filterNotNull()
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameEngine: GameEngine,
    private val audioManager: AudioManager,
    private val levelRepository: LevelRepository,
    private val gameRepository: GameRepository,
    private val userDataRepository: UserDataRepository,
    private val modeRecordsStore: ModeRecordsStore,
    private val modeRegistry: GameModeRegistry,
    private val analyticsTracker: AnalyticsTracker,
    private val dateProvider: DateProvider,
    private val debugCommands: DebugCommands,
    private val featureFlags: FeatureFlags,
    private val boosterInventory: BoosterInventoryStore,
    private val cachedMoveSolver: CachedMoveSolver,
    private val deadlockPredictor: DeadlockPredictor,
    private val dailySeedValidator: DailySeedValidator,
    private val difficultyProfileProvider: DifficultyProfileProvider,
    private val achievementTracker: AchievementTracker,
    private val unlockService: UnlockService,
    private val crashReporter: CrashReporter,
    private val liveConfig: LiveConfig,
    private val seasonalEventStore: SeasonalEventRecorder,
    private val cloudUploadTrigger: CloudUploadTrigger,
    private val sessionReplayRecorder: SessionReplayRecorder,
    private val competitiveScoreSubmitter: CompetitiveScoreSubmitter,
    private val shareRunUseCase: ShareRunUseCase,
    private val ghostReplayController: GhostReplayController,
    private val adService: AdService,
    private val adPreloader: AdPreloader,
    private val interstitialPolicy: InterstitialPolicy,
    private val settingsRepository: SettingsRepository,
    private val juiceController: JuiceController,
    @PersistenceScope private val persistenceScope: CoroutineScope
) : ViewModel() {

    private var colourblindMode: ColourblindMode = ColourblindMode.OFF
    private var largeTouchTargets: Boolean = false
    private var af11Enabled: Boolean = false

    private val selectedModeId: String =
        savedStateHandle.get<String>("mode") ?: ModeIds.CAMPAIGN
    private val selectedLevelId: Int = savedStateHandle.get<Int>("levelId") ?: 1
    private val navSeed: Long = savedStateHandle.get<Long>("seed") ?: -1L

    private val mode: GameMode = modeRegistry.get(selectedModeId)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    val juiceUiState = juiceController.uiState
    val juiceControllerPublic: JuiceController get() = juiceController

    private var currentGameState: GameState? = null
    private var sessionEndedHandled = false
    private var timerJob: Job? = null
    private var hintPrefetchJob: Job? = null
    private var lastHintAtMs: Long = 0L
    private var hintsUsedThisRun: Int = 0
    private var deadlockWarningSuppressed: Boolean = false
    private var sessionStartedAtMs: Long = 0L
    private var currentSessionSeed: Long = 0L

    private fun liveBoosterCost(type: BoosterType): Int =
        if (featureFlags.isEnabled(Feature.AF6)) liveConfig.boosterCost(type)
        else BoosterCatalog.spec(type).coinCost

    private fun liveHintCost(): Int =
        if (featureFlags.isEnabled(Feature.AF6)) liveConfig.hintCost()
        else Constants.HINT_COST

    private fun liveContinueCost(continuesUsed: Int): Int {
        val base = if (featureFlags.isEnabled(Feature.AF6)) {
            liveConfig.boosterCost(BoosterType.CONTINUE)
        } else {
            Constants.CONTINUE_COST
        }
        var cost = base
        repeat(continuesUsed.coerceAtLeast(0)) {
            cost = (cost * 2).coerceAtMost(Constants.CONTINUE_COST_CAP)
        }
        return cost
    }

    private fun updateCrashContext(state: GameState, lastAction: String) {
        crashReporter.updateSession(
            level = state.level,
            mode = mode.id,
            seed = currentSessionSeed,
            boardHash = BoardHash.of(state.board),
            lastAction = lastAction
        )
    }

    private val saveRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        debugCommands.registerGameSession()
        launchAutosave()
        listenForDebugCommands()
        viewModelScope.launch { modeRecordsStore.load() }
        viewModelScope.launch { boosterInventory.loadAndSeed() }

        viewModelScope.launch {
            val saved = gameRepository.loadActiveGame(mode.saveSlotId)
            val canResume = saved != null &&
                !saved.isGameOver &&
                saved.modeId == mode.id &&
                (mode.id != ModeIds.CAMPAIGN || saved.level == selectedLevelId) &&
                dailyWeeklyStillValid(saved)
            if (canResume) {
                val outcome = mode.afterMove(
                    saved!!,
                    com.mergeseven.game.game.model.GameResult(saved, emptyList()),
                    gameEngine
                )
                sessionReplayRecorder.markIncomplete()
                currentSessionSeed = saved.rng.seed
                applyState(outcome.state, outcome.endReason)
                if (outcome.endReason == null) restartTimerIfNeeded()
            } else {
                startNewGame(selectedLevelId)
            }
        }

        audioManager.startMusic()
        juiceController.bind(viewModelScope)
        juiceController.af10Enabled = featureFlags.isEnabled(Feature.AF10)
        viewModelScope.launch {
            featureFlags.observe(Feature.AF10).collect { enabled ->
                juiceController.af10Enabled = enabled
                if (!enabled) juiceController.resetSession()
            }
        }

        viewModelScope.launch {
            combine(
                featureFlags.observe(Feature.AF11),
                settingsRepository.colourblindMode,
                settingsRepository.largeTouchTargets
            ) { af11, cb, large ->
                Triple(af11, cb, large)
            }.collect { (af11, cb, large) ->
                af11Enabled = af11
                colourblindMode = if (af11) cb else ColourblindMode.OFF
                largeTouchTargets = af11 && large
                currentGameState?.let { state ->
                    updateUiFromState(
                        state,
                        isComplete = _uiState.value.isLevelComplete,
                        stars = _uiState.value.starsEarned,
                        isGameOver = _uiState.value.isGameOver
                    )
                } ?: _uiState.update {
                    it.copy(
                        af11Enabled = af11Enabled,
                        colourblindMode = colourblindMode,
                        largeTouchTargets = largeTouchTargets
                    )
                }
            }
        }
    }

    private fun dailyWeeklyStillValid(saved: GameState): Boolean {
        return when (saved.modeId) {
            ModeIds.DAILY -> saved.sessionDateKey == dateProvider.today()
            ModeIds.WEEKLY -> saved.sessionDateKey == ModeSeeds.weekKey(dateProvider.today())
            else -> true
        }
    }

    private fun listenForDebugCommands() {
        viewModelScope.launch {
            debugCommands.commands.collect { command ->
                when (command) {
                    DebugCommand.ForceGameOver -> forceGameOverFromDebug()
                }
            }
        }
    }

    private fun forceGameOverFromDebug() {
        val state = currentGameState ?: return
        if (state.isGameOver || _uiState.value.isLevelComplete) return
        handleEnded(state.copy(isGameOver = true), ModeEndReason.LOST)
    }

    fun startNewGame(levelId: Int = selectedLevelId) {
        sessionEndedHandled = false
        hintsUsedThisRun = 0
        lastHintAtMs = 0L
        deadlockWarningSuppressed = false
        cachedMoveSolver.invalidate()
        sessionStartedAtMs = System.currentTimeMillis()
        viewModelScope.launch {
            if (featureFlags.isEnabled(Feature.AF5)) {
                achievementTracker.load()
                unlockService.loadAndSeed()
            }
        }
        val targetScore = userDataRepository.userProfile.value.dailyChallenge.targetScore
        val seed = when {
            navSeed != -1L -> navSeed
            mode.id == ModeIds.DAILY -> {
                val date = dateProvider.today()
                if (featureFlags.isEnabled(Feature.AF4)) {
                    dailySeedValidator.resolveSeed(date, targetScore)
                } else {
                    ModeSeeds.dailySeed(date)
                }
            }
            mode.id == ModeIds.WEEKLY -> ModeSeeds.weeklySeed(ModeSeeds.weekKey(dateProvider.today()))
            else -> Random.nextLong()
        }
        val dateKey = when (mode.id) {
            ModeIds.DAILY -> dateProvider.today()
            ModeIds.WEEKLY -> ModeSeeds.weekKey(dateProvider.today())
            else -> ""
        }
        val ctx = ModeSessionContext(
            levelId = levelId,
            seed = seed,
            bestScore = modeRecordsStore.bestScore(mode.id),
            dateKey = dateKey,
            targetScore = targetScore,
            gameEngine = gameEngine
        )
        val initial = decorateAf4Spawn(mode.createSession(ctx))
        currentSessionSeed = seed
        sessionReplayRecorder.start(seed = seed, level = initial.level)
        updateCrashContext(initial, "session_start")
        analyticsTracker.logEvent(
            AnalyticsEvents.GAME_START,
            mapOf(
                "mode" to mode.id,
                "level" to levelId.toString(),
                "seed" to seed.toString()
            )
        )
        if (mode.id == ModeIds.DAILY) {
            analyticsTracker.logEvent(AnalyticsEvents.DAILY_STARTED, mapOf("level" to levelId.toString()))
        }
        if (mode.id == ModeIds.CAMPAIGN) {
            analyticsTracker.logEvent(AnalyticsEvents.LEVEL_STARTED, mapOf("level" to levelId.toString()))
        }
        applyState(initial, endReason = null)
        audioManager.startMusic()
        restartTimerIfNeeded()
    }

    fun onNextLevel() {
        val nextLevel = _uiState.value.level + 1
        startNewGame(nextLevel)
    }

    fun onPause() {
        audioManager.pauseMusic()
        timerJob?.cancel()
    }

    fun onResume() {
        audioManager.startMusic()
        restartTimerIfNeeded()
    }

    fun onStopped() {
        if (featureFlags.isEnabled(Feature.AF5) && sessionStartedAtMs > 0L) {
            val elapsed = System.currentTimeMillis() - sessionStartedAtMs
            userDataRepository.recordLifetimeStats(playtimeDeltaMs = elapsed)
            sessionStartedAtMs = System.currentTimeMillis()
        }
        audioManager.pauseMusic()
        timerJob?.cancel()
        persistNow()
    }

    private fun restartTimerIfNeeded() {
        timerJob?.cancel()
        if (!mode.hud.showTimer) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TIMER_TICK_MS)
                val state = currentGameState ?: continue
                if (state.isGameOver || _uiState.value.isLevelComplete) break
                val outcome = mode.onTimerTick(state, TIMER_TICK_MS, gameEngine)
                applyState(outcome.state, outcome.endReason)
                if (outcome.endReason != null) break
            }
        }
    }

    private fun applyState(gameState: GameState, endReason: ModeEndReason?) {
        if (endReason != null && !sessionEndedHandled) {
            handleEnded(gameState, endReason)
            return
        }
        updateUiFromState(gameState, isComplete = false, stars = 0, isGameOver = gameState.isGameOver)
        if (gameState.isGameOver) {
            clearSave()
        } else {
            saveRequests.tryEmit(Unit)
        }
    }

    private fun handleEnded(gameState: GameState, endReason: ModeEndReason) {
        sessionEndedHandled = true
        timerJob?.cancel()
        val effects = mode.onSessionEnded(gameState, endReason)
        val isComplete = endReason == ModeEndReason.WON
        val stars = effects.starsEarned

        if (isComplete && mode.id == ModeIds.CAMPAIGN) {
            levelRepository.completeLevel(gameState.level, gameState.score, stars)
            audioManager.playSoundCombo()
            if (featureFlags.isEnabled(Feature.AF7)) {
                cloudUploadTrigger.requestUpload(UploadReason.LEVEL_COMPLETE)
            }
            if (featureFlags.isEnabled(Feature.AF3)) {
                persistenceScope.launch { boosterInventory.grant(BoosterType.UNDO, 1) }
            }
            if (featureFlags.isEnabled(Feature.AF5)) {
                userDataRepository.addXp(Constants.XP_LEVEL_CLEAR)
                analyticsTracker.logEvent(AnalyticsEvents.XP_GAINED, mapOf("source" to "level_clear"))
                viewModelScope.launch {
                    val completedLevels = levelRepository.getLevels().count { it.isCompleted }
                    val newly = achievementTracker.reportMetric(
                        AchievementMetric.LEVELS_CLEARED,
                        completedLevels
                    )
                    newly.forEach { def ->
                        def.cosmeticRewardId?.let { unlockService.grantCosmetic(it) }
                        analyticsTracker.logEvent(
                            AnalyticsEvents.ACHIEVEMENT_UNLOCKED,
                            mapOf("id" to def.id)
                        )
                    }
                    unlockService.applyLevelUnlocks(userDataRepository.userProfile.value.playerLevel)
                }
            }
        } else if (!isComplete && mode.id == ModeIds.CAMPAIGN) {
            levelRepository.recordLevelFail(gameState.level)
        }

        if (featureFlags.isEnabled(Feature.AF5)) {
            val elapsed = if (sessionStartedAtMs > 0L) {
                System.currentTimeMillis() - sessionStartedAtMs
            } else {
                0L
            }
            userDataRepository.recordLifetimeStats(
                biggestTile = gameState.board.activeTiles().maxOfOrNull { it.value } ?: 0,
                playtimeDeltaMs = elapsed
            )
            viewModelScope.launch {
                achievementTracker.reportMetric(
                    AchievementMetric.SCORE,
                    gameState.score.toInt()
                )
                achievementTracker.reportMetric(
                    AchievementMetric.STREAK,
                    userDataRepository.userProfile.value.currentStreak
                )
                achievementTracker.reportMetric(
                    AchievementMetric.BIGGEST_TILE,
                    userDataRepository.userProfile.value.biggestTile
                )
            }
        }

        if (effects.recordBestScore) {
            persistenceScope.launch {
                modeRecordsStore.recordRun(mode.id, gameState.score)
            }
        }
        if (effects.finishDailyAttempt) {
            userDataRepository.finishDailyAttempt(gameState.score.toInt())
        }
        if (effects.finishWeeklyAttempt) {
            userDataRepository.finishWeeklyAttempt(
                weekKey = gameState.sessionDateKey.ifEmpty {
                    ModeSeeds.weekKey(dateProvider.today())
                },
                score = gameState.score.toInt()
            )
        }

        val eventName = if (isComplete) AnalyticsEvents.LEVEL_COMPLETE else AnalyticsEvents.GAME_OVER
        analyticsTracker.logEvent(
            eventName,
            mapOf(
                "mode" to mode.id,
                "level" to gameState.level.toString(),
                "score" to gameState.score.toString(),
                "stars" to stars.toString()
            )
        )

        updateUiFromState(
            gameState = gameState,
            isComplete = isComplete,
            stars = stars,
            isGameOver = !isComplete
        )
        clearSave()

        if (featureFlags.isEnabled(Feature.AF8)) {
            viewModelScope.launch {
                val result = competitiveScoreSubmitter.maybeSubmit(gameState, mode.id)
                _uiState.update {
                    it.copy(
                        shareReady = true,
                        scoreSubmitStatus = when (result.status) {
                            SubmitStatus.ACCEPTED -> "Score submitted"
                            SubmitStatus.NEEDS_SIGN_IN -> "Sign in to submit scores"
                            SubmitStatus.RATE_LIMITED -> "Submit rate limited"
                            SubmitStatus.REJECTED -> "Score rejected: ${result.reason}"
                            SubmitStatus.SKIPPED -> null
                        }
                    )
                }
            }
        }
    }

    fun createShareIntent(): Intent? {
        if (!featureFlags.isEnabled(Feature.AF8)) return null
        val state = currentGameState ?: return null
        return shareRunUseCase.createShareIntent(
            ShareRunData(
                score = state.score,
                maxTile = state.board.activeTiles().maxOfOrNull { it.value } ?: 0,
                modeLabel = mode.id,
                dateLabel = state.sessionDateKey.ifEmpty { ModeSeeds.formatToday() }
            )
        )
    }

    fun loadGhostReplay() {
        if (!featureFlags.isEnabled(Feature.AF8) || mode.id != ModeIds.DAILY) return
        val dateKey = currentGameState?.sessionDateKey ?: dateProvider.today()
        viewModelScope.launch {
            val frames = ghostReplayController.loadDailyLeaderFrames(dateKey)
            _uiState.update {
                it.copy(ghostFrames = frames, showGhostOverlay = frames.isNotEmpty())
            }
        }
    }

    fun dismissGhostOverlay() {
        _uiState.update { it.copy(showGhostOverlay = false) }
    }

    fun dismissScoreSubmitStatus() {
        _uiState.update { it.copy(scoreSubmitStatus = null) }
    }

    private fun updateUiFromState(
        gameState: GameState,
        isComplete: Boolean,
        stars: Int,
        isGameOver: Boolean
    ) {
        val walletCoins = if (featureFlags.isEnabled(Feature.AF3)) {
            userDataRepository.coins()
        } else {
            gameState.coins
        }
        val syncedBase = if (featureFlags.isEnabled(Feature.AF3)) {
            gameState.copy(coins = walletCoins)
        } else {
            gameState
        }
        val synced = decorateAf4Spawn(syncedBase)
        currentGameState = synced
        val profile = userDataRepository.userProfile.value
        val baseTheme = if (featureFlags.isEnabled(Feature.AF5)) {
            TileThemes.of(profile.equippedTileThemeId)
        } else {
            TileThemes.of("classic")
        }
        val tileTheme = baseTheme.withColourblindMode(colourblindMode)
        val tilesUi = synced.board.activeTiles().associate { tile ->
            tile.cell to TileUi(
                value = tile.value,
                color = tileTheme.tileColor(tile.value),
                trait = tile.trait,
                freezeStage = tile.freezeStage,
                multiplierFactor = tile.multiplierFactor
            )
        }
        val availableSlot = if (synced.trayPieces.getOrNull(_uiState.value.selectedSlotIndex) != null) {
            _uiState.value.selectedSlotIndex
        } else {
            synced.trayPieces.indexOfFirst { it != null }.coerceAtLeast(0)
        }
        val chips = if (mode.hud.showObjectives) {
            ObjectiveEvaluator.allProgress(synced)
        } else {
            emptyList()
        }
        val af3 = featureFlags.isEnabled(Feature.AF3)
        val af4 = featureFlags.isEnabled(Feature.AF4)
        val af8 = featureFlags.isEnabled(Feature.AF8)
        val continueCost = liveContinueCost(synced.continuesUsed)
        val canCoinContinue = af3 &&
            synced.isGameOver &&
            synced.continuesUsed < Constants.MAX_COIN_CONTINUES_PER_RUN &&
            (boosterInventory.count(BoosterType.CONTINUE) > 0 || walletCoins >= continueCost)
        val canRewarded = af3 &&
            synced.isGameOver &&
            synced.rewardedContinuesUsed < Constants.REWARDED_CONTINUE_LIMIT

        val nearDeadlock = af4 &&
            !isComplete &&
            !isGameOver &&
            deadlockPredictor.isNearDeadlock(synced)
        if (!nearDeadlock) {
            deadlockWarningSuppressed = false
        }
        val showDeadlock = nearDeadlock && !deadlockWarningSuppressed

        val hintCost = liveHintCost()
        val now = System.currentTimeMillis()
        val canHint = af4 &&
            !isGameOver &&
            !isComplete &&
            hintsUsedThisRun < Constants.MAX_HINTS_PER_RUN &&
            now - lastHintAtMs >= Constants.HINT_COOLDOWN_MS &&
            walletCoins >= hintCost

        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                level = synced.level,
                score = synced.score,
                coins = walletCoins,
                targetValue = synced.targetValue,
                boardRadius = synced.board.displayRadius().coerceAtLeast(1),
                boardCells = synced.board.playableCells,
                cellModifiers = synced.board.cellModifiers,
                tiles = tilesUi,
                trayPieces = synced.trayPieces,
                selectedSlotIndex = availableSlot,
                isGameOver = isGameOver && !isComplete,
                isLevelComplete = isComplete,
                starsEarned = stars,
                objectiveChips = chips,
                canUndo = synced.previousState != null && !synced.undoBlockedUntilMove,
                modeId = mode.id,
                showObjectives = mode.hud.showObjectives,
                showTimer = mode.hud.showTimer,
                showTarget = mode.hud.showTarget,
                timeRemainingMs = synced.timeRemainingMs,
                paletteKey = mode.hud.paletteKey,
                suppressAds = mode.hud.suppressAds,
                af3Enabled = af3,
                boosterButtons = buildBoosterButtons(synced, af3, currentState.pendingBooster),
                continueCoinCost = continueCost,
                canCoinContinue = canCoinContinue,
                canRewardedContinue = canRewarded,
                pendingBooster = currentState.pendingBooster,
                af4Enabled = af4,
                af8Enabled = af8,
                hintCells = if (af4) currentState.hintCells else emptyList(),
                canUseHint = canHint,
                showDeadlockWarning = showDeadlock,
                hintsUsedThisRun = hintsUsedThisRun,
                boardThemeId = if (featureFlags.isEnabled(Feature.AF5)) {
                    profile.equippedBoardThemeId
                } else {
                    "wood"
                },
                tileThemeId = tileTheme.id,
                achievementToast = if (featureFlags.isEnabled(Feature.AF5)) {
                    achievementTracker.newlyCompleted.value?.title
                } else {
                    null
                },
                af9Enabled = featureFlags.isEnabled(Feature.AF9),
                canDoubleCoins = featureFlags.isEnabled(Feature.AF9) && isComplete,
                canWatchHintAd = featureFlags.isEnabled(Feature.AF9) &&
                    featureFlags.isEnabled(Feature.AF4) &&
                    !isGameOver &&
                    !isComplete,
                af11Enabled = af11Enabled,
                colourblindMode = colourblindMode,
                largeTouchTargets = largeTouchTargets,
                accessibilityAnnouncement = currentState.accessibilityAnnouncement
            )
        }
        if ((isGameOver || isComplete) && featureFlags.isEnabled(Feature.AF9)) {
            adPreloader.warmGameOver()
        }
        if (af4 && !isGameOver && !isComplete) {
            prefetchHint(synced)
        } else {
            cachedMoveSolver.invalidate()
        }
    }

    private fun buildBoosterButtons(
        state: GameState,
        af3: Boolean,
        pending: BoosterType?
    ): List<BoosterButtonUi> {
        val trayTypes = listOf(
            BoosterType.UNDO,
            BoosterType.SWAP,
            BoosterType.RANDOMIZE,
            BoosterType.REMOVE,
            BoosterType.HAMMER,
            BoosterType.VALUE_UP,
            BoosterType.MAGNET,
            BoosterType.TIME_FREEZE
        )
        return trayTypes.map { type ->
            val spec = BoosterCatalog.spec(type)
            val owned = boosterInventory.count(type)
            val ctx = useContextFor(type, state)
            val deny = if (!af3 && type in setOf(
                    BoosterType.HAMMER, BoosterType.VALUE_UP, BoosterType.MAGNET, BoosterType.TIME_FREEZE
                )
            ) {
                BoosterDenyReason.WRONG_MODE
            } else if (!af3) {
                when (type) {
                    BoosterType.UNDO -> if (state.previousState == null) {
                        BoosterDenyReason.UNDO_EMPTY
                    } else {
                        BoosterDenyReason.OK
                    }
                    else -> BoosterDenyReason.OK
                }
            } else {
                BoosterCatalog.canUse(type, state, ctx)
            }
            BoosterButtonUi(
                type = type,
                enabled = deny == BoosterDenyReason.OK && !state.isGameOver,
                cost = liveBoosterCost(type),
                owned = owned,
                selected = pending == type
            )
        }
    }

    private fun useContext(state: GameState) = BoosterUseContext(
        owned = 0, // checked per-type in canUse via affordability with owned injected below
        coins = userDataRepository.coins(),
        nowMs = System.currentTimeMillis(),
        unlimitedUndo = mode.hud.unlimitedUndo,
        undosUsedThisRun = state.undosUsedThisRun,
        undoBlockedUntilMove = state.undoBlockedUntilMove,
        continuesUsed = state.continuesUsed,
        rewardedContinuesUsed = state.rewardedContinuesUsed
    )

    private fun useContextFor(type: BoosterType, state: GameState) = useContext(state).copy(
        owned = boosterInventory.count(type)
    )

    @OptIn(FlowPreview::class)
    private fun launchAutosave() = viewModelScope.launch {
        saveRequests.debounce(AUTOSAVE_DEBOUNCE_MS).collect { persistNow() }
    }

    private fun persistNow() {
        val state = currentGameState ?: return
        if (state.isGameOver) return
        val undoDepth = if (mode.hud.unlimitedUndo) ZenMode.UNDO_DEPTH else Constants.MAX_UNDO_HISTORY
        persistenceScope.launch {
            // Snapshot depth is applied inside toSnapshot default; for Zen we need deeper history
            // in-memory — persist uses MAX_UNDO_HISTORY unless we pass via repository.
            gameRepository.saveActiveGame(state, mode.saveSlotId)
        }
        // Keep in-memory undo deep for Zen regardless of disk cap.
        if (mode.hud.unlimitedUndo) {
            // no-op: previousState chain already built in engine
        }
        @Suppress("UNUSED_VARIABLE")
        val ignored = undoDepth
    }

    private fun clearSave() {
        persistenceScope.launch { gameRepository.clearActiveGame(mode.saveSlotId) }
    }

    fun onSelectTraySlot(index: Int) {
        val state = currentGameState ?: return
        if (index !in state.trayPieces.indices) return
        _uiState.update { it.copy(selectedSlotIndex = index) }
    }

    fun onRotateTraySlot(index: Int) {
        val state = currentGameState ?: return
        sessionReplayRecorder.recordRotate(index)
        val newState = gameEngine.rotateTrayPiece(state, index)
        applyState(newState, endReason = null)
    }

    fun onCellHover(origin: HexCoord?, slotIndex: Int = _uiState.value.selectedSlotIndex) {
        val state = currentGameState ?: return
        val piece = state.trayPieces.getOrNull(slotIndex)

        if (origin == null || piece == null) {
            _uiState.update { it.copy(hoveredCells = emptyList()) }
            return
        }

        val canPlace = gameEngine.canPlace(state, piece, origin)
        val hovered = piece.absoluteCells(origin).map { (coord, _) ->
            coord to canPlace
        }
        _uiState.update { it.copy(hoveredCells = hovered) }
    }

    fun onDropOnCell(
        origin: HexCoord,
        slotIndex: Int = _uiState.value.selectedSlotIndex,
        dropBoardLocal: Offset? = null,
        boardWidth: Float = 0f,
        boardHeight: Float = 0f
    ) {
        onCellHover(null, slotIndex)
        placeAt(origin, slotIndex, dropBoardLocal, boardWidth, boardHeight)
    }

    fun dismissAccessibilityAnnouncement() {
        _uiState.update { it.copy(accessibilityAnnouncement = null) }
    }

    fun onCellTapped(cell: HexCoord) {
        val pending = _uiState.value.pendingBooster
        if (pending != null) {
            executeBooster(pending, target = cell)
            return
        }
        placeAt(cell, _uiState.value.selectedSlotIndex)
    }

    fun clearDropSnap(id: Long) {
        juiceController.clearDropSnap(id)
    }

    private fun placeAt(
        origin: HexCoord,
        slotIndex: Int,
        dropBoardLocal: Offset? = null,
        boardWidth: Float = 0f,
        boardHeight: Float = 0f
    ) {
        val state = currentGameState ?: return
        if (state.isGameOver || _uiState.value.isLevelComplete) return
        val piece = state.trayPieces.getOrNull(slotIndex) ?: return
        if (!gameEngine.canPlace(state, piece, origin)) {
            announce(AccessibilityAnnounceKind.INVALID)
            juiceController.onInvalidPlacement()
            return
        }

        clearHintHighlight()
        cachedMoveSolver.invalidate()
        sessionReplayRecorder.recordPlace(slotIndex, origin)
        val result = gameEngine.placePiece(state, piece, origin, slotIndex)
        dispatchFeel(result, piece, origin, dropBoardLocal, boardWidth, boardHeight)
        analyticsTracker.logEvent(
            AnalyticsEvents.PIECE_PLACED,
            mapOf("mode" to mode.id, "level" to state.level.toString())
        )
        val hadMerge = result.events.any { it is com.mergeseven.game.game.model.GameEvent.MergeCompleted }
        val hadChain = result.events.any { it is com.mergeseven.game.game.model.GameEvent.ChainCompleted }
        if (hadMerge) {
            analyticsTracker.logEvent(
                AnalyticsEvents.MERGE_COMPLETED,
                mapOf("mode" to mode.id, "level" to state.level.toString())
            )
        }
        if (hadChain) {
            analyticsTracker.logEvent(
                AnalyticsEvents.CHAIN_COMPLETED,
                mapOf("mode" to mode.id, "level" to state.level.toString())
            )
        }
        updateCrashContext(result.state, "place")
        if (featureFlags.isEnabled(Feature.AF5)) {
            applyAf5FromResult(result)
        }
        if (featureFlags.isEnabled(Feature.AF6)) {
            seasonalEventStore.recordScore(result.state.score)
        }
        val outcome = mode.afterMove(state, result, gameEngine)
        applyState(decorateAf4Spawn(outcome.state), outcome.endReason)
        when (outcome.endReason) {
            ModeEndReason.WON -> announce(AccessibilityAnnounceKind.LEVEL_COMPLETE, result.state.score)
            ModeEndReason.LOST -> announce(AccessibilityAnnounceKind.GAME_OVER, result.state.score)
            null -> if (hadMerge) {
                announce(AccessibilityAnnounceKind.MERGED, result.state.score)
            } else {
                announce(AccessibilityAnnounceKind.PLACED, result.state.score)
            }
        }
    }

    private fun announce(kind: AccessibilityAnnounceKind, score: Long = 0L) {
        if (!af11Enabled) return
        _uiState.update {
            it.copy(accessibilityAnnouncement = AccessibilityAnnouncement(kind, score))
        }
    }

    private fun applyAf5FromResult(result: com.mergeseven.game.game.model.GameResult) {
        val merges = result.events.filterIsInstance<com.mergeseven.game.game.model.GameEvent.MergeCompleted>()
        val chains = result.events.filterIsInstance<com.mergeseven.game.game.model.GameEvent.ChainCompleted>()
        val mergeCount = merges.size
        val maxResultTile = merges.maxOfOrNull { it.resultTile.value } ?: 0
        val maxChain = chains.maxOfOrNull { it.chainLength } ?: 0
        if (mergeCount > 0) {
            userDataRepository.addXp(Constants.XP_PER_MERGE * mergeCount)
            analyticsTracker.logEvent(AnalyticsEvents.XP_GAINED, mapOf("source" to "merge"))
        }
        if (maxChain > 1) {
            userDataRepository.addXp(Constants.XP_PER_CHAIN_STEP * (maxChain - 1))
            analyticsTracker.logEvent(AnalyticsEvents.XP_GAINED, mapOf("source" to "chain"))
        }
        userDataRepository.recordLifetimeStats(
            mergesDelta = mergeCount,
            biggestTile = maxResultTile,
            chainLength = maxChain
        )
        viewModelScope.launch {
            if (mergeCount > 0) {
                val newly = achievementTracker.reportMetric(AchievementMetric.MERGES, mergeCount)
                newly.forEach { def ->
                    def.cosmeticRewardId?.let { unlockService.grantCosmetic(it) }
                    analyticsTracker.logEvent(
                        AnalyticsEvents.ACHIEVEMENT_UNLOCKED,
                        mapOf("id" to def.id)
                    )
                }
            }
            if (maxResultTile > 0) {
                achievementTracker.reportMetric(AchievementMetric.BIGGEST_TILE, maxResultTile)
            }
            unlockService.applyLevelUnlocks(userDataRepository.userProfile.value.playerLevel)
            achievementTracker.newlyCompleted.value?.title?.let { title ->
                _uiState.update { it.copy(achievementToast = title) }
            }
        }
    }

    fun dismissAchievementToast() {
        achievementTracker.consumeToast()
        _uiState.update { it.copy(achievementToast = null) }
    }

    private fun dispatchFeel(
        result: com.mergeseven.game.game.model.GameResult,
        piece: TilePiece,
        origin: HexCoord,
        dropBoardLocal: Offset?,
        boardWidth: Float,
        boardHeight: Float
    ) {
        val theme = TileThemes.of(_uiState.value.tileThemeId).withColourblindMode(colourblindMode)
        val dropTargets = if (boardWidth > 0f && boardHeight > 0f) {
            val hexSize = HexGeometry.calculateHexSize(
                _uiState.value.boardRadius,
                boardWidth,
                boardHeight,
                8f
            )
            val cx = boardWidth / 2f
            val cy = boardHeight / 2f
            piece.absoluteCells(origin).map { (coord, value) ->
                val (px, py) = HexGeometry.hexToPixel(coord, hexSize, cx, cy)
                Offset(px, py) to theme.tileColor(value)
            }
        } else {
            emptyList()
        }
        juiceController.dispatch(
            result = result,
            dropFrom = dropBoardLocal,
            dropTargets = dropTargets
        )
    }

    private fun handleResultAudio(result: com.mergeseven.game.game.model.GameResult) {
        juiceController.onLegacyAudio(result)
    }

    fun dismissDeadlockWarning() {
        deadlockWarningSuppressed = true
        _uiState.update { it.copy(showDeadlockWarning = false) }
    }

    fun onHintClick() {
        if (!featureFlags.isEnabled(Feature.AF4)) return
        val state = currentGameState ?: return
        if (state.isGameOver || _uiState.value.isLevelComplete) return
        val now = System.currentTimeMillis()
        if (hintsUsedThisRun >= Constants.MAX_HINTS_PER_RUN) return
        if (now - lastHintAtMs < Constants.HINT_COOLDOWN_MS) return

        if (!userDataRepository.trySpendCoins(liveHintCost())) {
            _uiState.update { it.copy(showInsufficientFunds = true) }
            return
        }

        viewModelScope.launch { deliverHint(refundOnMiss = true) }
    }

    fun onHintWithRewardedAd(activity: android.app.Activity) {
        if (!featureFlags.isEnabled(Feature.AF4)) return
        if (!featureFlags.isEnabled(Feature.AF9)) {
            onHintClick()
            return
        }
        val state = currentGameState ?: return
        if (state.isGameOver || _uiState.value.isLevelComplete) return
        if (hintsUsedThisRun >= Constants.MAX_HINTS_PER_RUN) return
        viewModelScope.launch {
            adPreloader.warmHint()
            analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_STARTED, mapOf("source" to "hint"))
            when (adService.showRewarded(activity, AdPlacement.FREE_HINT)) {
                AdResult.Rewarded -> {
                    analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_COMPLETED, mapOf("source" to "hint"))
                    deliverHint(refundOnMiss = false)
                }
                else -> _uiState.update { it.copy(adStatusMessage = "No ad available") }
            }
        }
    }

    private suspend fun deliverHint(refundOnMiss: Boolean) {
        val state = currentGameState ?: return
        val profile = difficultyProfileProvider.current()
        val hint = cachedMoveSolver.peekCached(state)
            ?: cachedMoveSolver.findBest(state, profile)
        if (hint == null) {
            if (refundOnMiss) userDataRepository.addCoins(liveHintCost())
            _uiState.update { it.copy(coins = userDataRepository.coins()) }
            return
        }
        applyHint(hint)
        lastHintAtMs = System.currentTimeMillis()
        hintsUsedThisRun++
        analyticsTracker.logEvent(
            AnalyticsEvents.HINT_USED,
            mapOf(
                "mode" to mode.id,
                "level" to state.level.toString()
            )
        )
        _uiState.update {
            it.copy(
                coins = userDataRepository.coins(),
                hintsUsedThisRun = hintsUsedThisRun,
                canUseHint = false
            )
        }
    }

    private fun applyHint(hint: MoveHint) {
        val state = currentGameState ?: return
        val piece = state.trayPieces.getOrNull(hint.slotIndex)?.copy(rotation = hint.rotation)
            ?: return
        val cells = piece.absoluteCells(hint.origin).map { it.first }
        val newTray = state.trayPieces.toMutableList()
        if (hint.slotIndex in newTray.indices) {
            newTray[hint.slotIndex] = piece
        }
        currentGameState = state.copy(trayPieces = newTray)
        _uiState.update {
            it.copy(
                selectedSlotIndex = hint.slotIndex,
                hintCells = cells,
                trayPieces = newTray
            )
        }
    }

    private fun clearHintHighlight() {
        _uiState.update { it.copy(hintCells = emptyList()) }
    }

    private fun prefetchHint(state: GameState) {
        hintPrefetchJob?.cancel()
        hintPrefetchJob = viewModelScope.launch {
            cachedMoveSolver.findBest(state, difficultyProfileProvider.current())
        }
    }

    private fun decorateAf4Spawn(state: GameState): GameState {
        val baseWeights = if (featureFlags.isEnabled(Feature.AF6)) {
            liveConfig.spawnWeights()
        } else {
            emptyMap()
        }
        if (!featureFlags.isEnabled(Feature.AF4)) {
            return state.copy(spawnValueBoost = emptyMap(), spawnBaseWeights = baseWeights)
        }
        val profile = difficultyProfileProvider.current()
        val failCount = if (mode.id == ModeIds.CAMPAIGN) {
            levelRepository.failCount(state.level)
        } else {
            0
        }
        val boost = if (
            profile.adaptiveSpawnEnabled &&
            failCount >= Constants.ADAPTIVE_SPAWN_FAIL_THRESHOLD
        ) {
            state.board.activeTiles()
                .map { it.value }
                .filter { it in setOf(2, 4, 8, 16) }
                .distinct()
                .associateWith { Constants.ADAPTIVE_SPAWN_BOOST }
        } else {
            emptyMap()
        }
        val tier = if (mode.id == ModeIds.ENDLESS) {
            (state.moves / com.mergeseven.game.game.modes.EndlessMode.MOVES_PER_TIER +
                profile.spawnTierOffset).coerceAtLeast(0)
        } else {
            state.spawnTier
        }
        return state.copy(
            spawnValueBoost = boost,
            spawnTier = tier,
            spawnBaseWeights = baseWeights
        )
    }

    fun onBoosterUndo() = requestBooster(BoosterType.UNDO)
    fun onBoosterShuffle() = requestBooster(BoosterType.RANDOMIZE)
    fun onBoosterRotate() = requestBooster(BoosterType.SWAP)
    fun onBoosterSwap() = onBoosterRotate()
    fun onBoosterRemoveHighest() = requestBooster(BoosterType.REMOVE)
    fun onBoosterHammer() = requestBooster(BoosterType.HAMMER)
    fun onBoosterValueUp() = requestBooster(BoosterType.VALUE_UP)
    fun onBoosterMagnet() = requestBooster(BoosterType.MAGNET)
    fun onBoosterTimeFreeze() = requestBooster(BoosterType.TIME_FREEZE)

    fun dismissInsufficientFunds() {
        _uiState.update { it.copy(showInsufficientFunds = false) }
    }

    fun dismissConfirmBooster() {
        _uiState.update { it.copy(confirmBooster = null) }
    }

    fun confirmPendingBooster() {
        val type = _uiState.value.confirmBooster ?: return
        _uiState.update { it.copy(confirmBooster = null) }
        executeBooster(type, target = null)
    }

    fun onRewardedCoinsStub() {
        // Legacy entry; prefer onWatchFundsAd when AF9 is on.
        userDataRepository.addCoins(Constants.INSUFFICIENT_FUNDS_REWARD_COINS)
        _uiState.update { it.copy(showInsufficientFunds = false, coins = userDataRepository.coins()) }
        currentGameState = currentGameState?.copy(coins = userDataRepository.coins())
    }

    fun onWatchFundsAd(activity: android.app.Activity) {
        if (!featureFlags.isEnabled(Feature.AF9)) {
            onRewardedCoinsStub()
            return
        }
        viewModelScope.launch {
            analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_STARTED, mapOf("source" to "funds"))
            when (adService.showRewarded(activity, AdPlacement.FUNDS_COINS)) {
                AdResult.Rewarded -> {
                    analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_COMPLETED, mapOf("source" to "funds"))
                    userDataRepository.addCoins(Constants.INSUFFICIENT_FUNDS_REWARD_COINS)
                    _uiState.update {
                        it.copy(showInsufficientFunds = false, coins = userDataRepository.coins())
                    }
                    currentGameState = currentGameState?.copy(coins = userDataRepository.coins())
                }
                else -> _uiState.update { it.copy(adStatusMessage = "No ad available") }
            }
        }
    }

    fun onContinueWithCoins() {
        viewModelScope.launch { executeContinue(rewarded = false) }
    }

    fun onContinueWithRewardedAd() {
        viewModelScope.launch { executeContinue(rewarded = true) }
    }

    fun onContinueWithRewardedAd(activity: android.app.Activity) {
        if (!featureFlags.isEnabled(Feature.AF9)) {
            onContinueWithRewardedAd()
            return
        }
        viewModelScope.launch {
            analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_STARTED, mapOf("source" to "continue"))
            when (adService.showRewarded(activity, AdPlacement.CONTINUE)) {
                AdResult.Rewarded -> executeContinue(rewarded = true)
                else -> _uiState.update { it.copy(adStatusMessage = "No ad available") }
            }
        }
    }

    fun onDoubleCoinsAd(activity: android.app.Activity) {
        if (!featureFlags.isEnabled(Feature.AF9)) return
        if (!_uiState.value.isLevelComplete && !_uiState.value.isGameOver) return
        viewModelScope.launch {
            analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_STARTED, mapOf("source" to "double_coins"))
            when (adService.showRewarded(activity, AdPlacement.DOUBLE_COINS)) {
                AdResult.Rewarded -> {
                    analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_COMPLETED, mapOf("source" to "double_coins"))
                    val grant = Constants.REWARDED_COIN_GRANT
                    userDataRepository.addCoins(grant)
                    _uiState.update {
                        it.copy(coins = userDataRepository.coins(), canDoubleCoins = false)
                    }
                }
                else -> _uiState.update { it.copy(adStatusMessage = "No ad available") }
            }
        }
    }

    fun dismissAdStatus() {
        _uiState.update { it.copy(adStatusMessage = null) }
    }

    /** Call when leaving a finished session toward Home (AF9-03). */
    suspend fun maybeShowInterstitial(activity: android.app.Activity, sessionWon: Boolean) {
        interstitialPolicy.onSessionEnded()
        val decision = interstitialPolicy.evaluate(
            sessionWon = sessionWon,
            inTutorial = false,
            suppressAds = mode.hud.suppressAds
        )
        if (!decision.allow) return
        adPreloader.warmInterstitial()
        when (adService.showInterstitial(activity)) {
            AdResult.Completed -> interstitialPolicy.onInterstitialShown()
            else -> Unit
        }
    }

    fun onBoosterRemoveTile(cell: HexCoord) {
        executeBooster(BoosterType.REMOVE, target = cell)
    }

    private fun requestBooster(type: BoosterType) {
        val state = currentGameState ?: return
        if (state.isGameOver || _uiState.value.isLevelComplete) return
        val af3 = featureFlags.isEnabled(Feature.AF3)
        val spec = BoosterCatalog.spec(type)

        if (!af3) {
            if (type in setOf(
                    BoosterType.HAMMER,
                    BoosterType.VALUE_UP,
                    BoosterType.MAGNET,
                    BoosterType.TIME_FREEZE
                )
            ) {
                return
            }
            executeBooster(type, target = null, free = true)
            return
        }

        val deny = BoosterCatalog.canUse(
            type,
            state,
            useContextFor(type, state),
            costOverride = if (type == BoosterType.CONTINUE) {
                liveContinueCost(state.continuesUsed)
            } else {
                liveBoosterCost(type)
            }
        )
        if (deny == BoosterDenyReason.INSUFFICIENT_FUNDS) {
            _uiState.update { it.copy(showInsufficientFunds = true) }
            return
        }
        if (deny != BoosterDenyReason.OK) return

        if (spec.requiresTargetCell) {
            _uiState.update { it.copy(pendingBooster = type) }
            return
        }

        if (boosterInventory.count(type) <= 0 &&
            !(type == BoosterType.UNDO && mode.hud.unlimitedUndo)
        ) {
            _uiState.update { it.copy(confirmBooster = type) }
            return
        }
        executeBooster(type, target = null)
    }

    private fun executeBooster(type: BoosterType, target: HexCoord?, free: Boolean = false) {
        viewModelScope.launch {
            val state = currentGameState ?: return@launch
            val af3 = featureFlags.isEnabled(Feature.AF3) && !free
            if (af3) {
                val deny = BoosterCatalog.canUse(
            type,
            state,
            useContextFor(type, state),
            costOverride = if (type == BoosterType.CONTINUE) {
                liveContinueCost(state.continuesUsed)
            } else {
                liveBoosterCost(type)
            }
        )
                if (deny == BoosterDenyReason.INSUFFICIENT_FUNDS) {
                    _uiState.update { it.copy(showInsufficientFunds = true) }
                    return@launch
                }
                if (deny != BoosterDenyReason.OK) return@launch
                val paid = payForBooster(type, state)
                if (!paid) {
                    _uiState.update { it.copy(showInsufficientFunds = true) }
                    return@launch
                }
            }

            when (type) {
                BoosterType.UNDO -> {
                    var next = gameEngine.undo(state)
                    if (af3 && !mode.hud.unlimitedUndo && next !== state) {
                        next = next.copy(undosUsedThisRun = state.undosUsedThisRun + 1)
                    }
                    if (next !== state) sessionReplayRecorder.recordUndo()
                    applyState(next, null)
                    analyticsTracker.logEvent(
                        AnalyticsEvents.UNDO_USED,
                        mapOf("mode" to mode.id)
                    )
                }
                BoosterType.SWAP -> {
                    sessionReplayRecorder.recordRotate(_uiState.value.selectedSlotIndex)
                    applyState(
                        gameEngine.rotateTrayPiece(state, _uiState.value.selectedSlotIndex),
                        null
                    )
                }
                BoosterType.RANDOMIZE -> {
                    sessionReplayRecorder.recordShuffle()
                    applyState(gameEngine.shuffleTray(state), null)
                }
                BoosterType.REMOVE -> {
                    val cell = target
                        ?: state.board.activeTiles().maxByOrNull { it.value }?.cell
                        ?: return@launch
                    sessionReplayRecorder.recordRemove(cell)
                    applyState(gameEngine.removeTile(state, cell).copy(previousState = state), null)
                }
                BoosterType.HAMMER -> {
                    val cell = target ?: return@launch
                    applyState(gameEngine.hammerTile(state, cell).state, null)
                }
                BoosterType.VALUE_UP -> {
                    val cell = target ?: return@launch
                    applyState(gameEngine.valueUpTile(state, cell).state, null)
                }
                BoosterType.MAGNET -> {
                    val cell = target ?: return@launch
                    applyState(gameEngine.magnetPull(state, cell).state, null)
                }
                BoosterType.TIME_FREEZE -> {
                    applyState(gameEngine.timeFreeze(state, Constants.TIME_FREEZE_MS).state, null)
                }
                BoosterType.CONTINUE -> Unit
            }
            _uiState.update { it.copy(pendingBooster = null) }
            analyticsTracker.logEvent(
                AnalyticsEvents.BOOSTER_USED,
                mapOf("booster_type" to type.name.lowercase(), "mode" to mode.id)
            )
            updateCrashContext(currentGameState ?: state, "booster_${type.name.lowercase()}")
        }
    }

    private suspend fun payForBooster(type: BoosterType, state: GameState): Boolean {
        if (type == BoosterType.UNDO && mode.hud.unlimitedUndo) return true
        if (boosterInventory.tryConsume(type)) return true
        val cost = if (type == BoosterType.CONTINUE) {
            liveContinueCost(state.continuesUsed)
        } else {
            liveBoosterCost(type)
        }
        return userDataRepository.trySpendCoins(cost)
    }

    private suspend fun executeContinue(rewarded: Boolean) {
        val state = currentGameState ?: return
        if (!state.isGameOver) return
        if (!featureFlags.isEnabled(Feature.AF3)) return
        if (rewarded) {
            if (state.rewardedContinuesUsed >= Constants.REWARDED_CONTINUE_LIMIT) return
        } else {
            if (state.continuesUsed >= Constants.MAX_COIN_CONTINUES_PER_RUN) return
            val paid = payForBooster(BoosterType.CONTINUE, state)
            if (!paid) {
                _uiState.update { it.copy(showInsufficientFunds = true) }
                return
            }
        }
        val result = gameEngine.continueAfterGameOver(
            state = state,
            clearCount = Constants.CONTINUE_CLEAR_TILES,
            rewarded = rewarded
        )
        sessionEndedHandled = false
        applyState(result.state.copy(coins = userDataRepository.coins()), null)
        analyticsTracker.logEvent(
            AnalyticsEvents.CONTINUE_USED,
            mapOf(
                "mode" to mode.id,
                "rewarded" to rewarded.toString()
            )
        )
        if (rewarded) {
            analyticsTracker.logEvent(AnalyticsEvents.REWARD_AD_COMPLETED, mapOf("source" to "continue"))
        }
        restartTimerIfNeeded()
    }

    override fun onCleared() {
        super.onCleared()
        debugCommands.unregisterGameSession()
        timerJob?.cancel()
        persistNow()
        audioManager.pauseMusic()
    }

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 400L
        const val TIMER_TICK_MS = 1_000L
    }
}
