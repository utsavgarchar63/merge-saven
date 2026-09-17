package com.mergeseven.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.ads.AdPlacement
import com.mergeseven.game.ads.AdPreloader
import com.mergeseven.game.ads.AdResult
import com.mergeseven.game.ads.AdService
import com.mergeseven.game.ads.BannerAdHost
import com.mergeseven.game.ads.InterstitialPolicy
import com.mergeseven.game.billing.OfferEngine
import com.mergeseven.game.billing.PurchaseGranter
import com.mergeseven.game.billing.RemoteOffer
import com.mergeseven.game.competitive.Tournament
import com.mergeseven.game.competitive.TournamentRepository
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.data.local.entity.ActiveGameEntity
import com.mergeseven.game.data.local.store.ModeRecordsStore
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.game.modes.ModeIds
import com.mergeseven.game.game.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A game the player can drop back into, summarised for the Home screen button. */
data class ResumableGame(
    val modeId: String,
    val level: Int,
    val score: Long
)

data class ModeCardUi(
    val modeId: String,
    val title: String,
    val bestScore: Long
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val gameRepository: GameRepository,
    private val modeRecordsStore: ModeRecordsStore,
    private val tournamentRepository: TournamentRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val featureFlags: FeatureFlags,
    private val settingsRepository: SettingsRepository,
    private val offerEngine: OfferEngine,
    private val purchaseGranter: PurchaseGranter,
    private val adService: AdService,
    private val adPreloader: AdPreloader,
    private val interstitialPolicy: InterstitialPolicy
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userDataRepository.userProfile

    val af2Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF2, viewModelScope)

    val af5Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF5, viewModelScope)

    val af8Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF8, viewModelScope)

    val af9Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF9, viewModelScope)

    val af10Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF10, viewModelScope)

    val reduceMotionEnabled: StateFlow<Boolean> = settingsRepository.isReduceMotionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val tournament: StateFlow<Tournament?> = tournamentRepository.current

    private val _remoteOffer = MutableStateFlow<RemoteOffer?>(null)
    val remoteOffer: StateFlow<RemoteOffer?> = _remoteOffer.asStateFlow()

    private val _stipendMessage = MutableStateFlow<String?>(null)
    val stipendMessage: StateFlow<String?> = _stipendMessage.asStateFlow()

    val modeCards: StateFlow<List<ModeCardUi>> = modeRecordsStore.records
        .map { records ->
            listOf(
                ModeIds.CAMPAIGN to "Campaign",
                ModeIds.ENDLESS to "Endless",
                ModeIds.TIME_ATTACK to "Time Attack",
                ModeIds.ZEN to "Zen",
                ModeIds.DAILY to "Daily Puzzle",
                ModeIds.WEEKLY to "Weekly"
            ).map { (id, title) ->
                ModeCardUi(id, title, records[id]?.bestScore ?: 0L)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _resumableGame = MutableStateFlow<ResumableGame?>(null)
    val resumableGame: StateFlow<ResumableGame?> = _resumableGame.asStateFlow()

    init {
        viewModelScope.launch { modeRecordsStore.load() }
        viewModelScope.launch { refreshResumable() }
        viewModelScope.launch {
            if (featureFlags.isEnabled(Feature.AF9)) {
                _remoteOffer.value = offerEngine.currentOffer()
                if (purchaseGranter.claimPremiumDailyStipend()) {
                    _stipendMessage.value = "Premium daily coins claimed"
                }
            }
        }
    }

    fun onReturnedFromSession(activity: android.app.Activity, sessionWon: Boolean) {
        viewModelScope.launch {
            interstitialPolicy.onSessionEnded()
            val decision = interstitialPolicy.evaluate(
                sessionWon = sessionWon,
                inTutorial = false,
                suppressAds = false
            )
            if (decision.allow) {
                adPreloader.warmInterstitial()
                if (adService.showInterstitial(activity) is AdResult.Completed) {
                    interstitialPolicy.onInterstitialShown()
                }
            }
        }
    }

    fun refreshTournament() {
        viewModelScope.launch { tournamentRepository.refresh() }
    }

    fun joinTournament(): Tournament? {
        val t = tournamentRepository.current.value ?: return null
        if (!t.isActive) return null
        analyticsTracker.logEvent(
            AnalyticsEvents.TOURNAMENT_JOINED,
            mapOf("id" to t.id)
        )
        return t
    }

    fun refreshResumable() {
        viewModelScope.launch {
            val slots = listOf(
                ActiveGameEntity.CAMPAIGN_SLOT to ModeIds.CAMPAIGN,
                ActiveGameEntity.ENDLESS_SLOT to ModeIds.ENDLESS,
                ActiveGameEntity.TIME_ATTACK_SLOT to ModeIds.TIME_ATTACK,
                ActiveGameEntity.ZEN_SLOT to ModeIds.ZEN,
                ActiveGameEntity.DAILY_SLOT to ModeIds.DAILY,
                ActiveGameEntity.WEEKLY_SLOT to ModeIds.WEEKLY
            )
            for ((slot, modeId) in slots) {
                val state = gameRepository.loadActiveGame(slot) ?: continue
                if (!state.isGameOver) {
                    _resumableGame.value = ResumableGame(modeId, state.level, state.score)
                    return@launch
                }
            }
            _resumableGame.value = null
        }
    }
}
