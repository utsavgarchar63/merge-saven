package com.mergeseven.game.data.repository

import android.util.Log
import com.mergeseven.game.cloud.CloudEconomyNotifier
import com.mergeseven.game.core.DateProvider
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.data.local.store.UserProfileStore
import com.mergeseven.game.data.model.DailyChallengeState
import com.mergeseven.game.data.model.UserProfile
import com.mergeseven.game.data.model.defaultDailyQuests
import com.mergeseven.game.di.PersistenceScope
import com.mergeseven.game.meta.XpCurve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the player's coins, stars, streak, quests, and daily challenge.
 *
 * Reads are served from an in-memory [StateFlow] so the UI and callers stay synchronous; every
 * mutation is written through to [store]. Loading from disk is asynchronous, which creates a short
 * window at startup where a caller could mutate a profile that has not been hydrated yet — see
 * [mutate] for how that is handled without losing the mutation or the stored balance.
 */
@Singleton
class UserDataRepository @Inject constructor(
    private val store: UserProfileStore,
    @PersistenceScope private val scope: CoroutineScope,
    private val dateProvider: DateProvider,
    private val featureFlags: FeatureFlags,
    private val cloudEconomyNotifier: CloudEconomyNotifier
) {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val lock = Any()

    @Volatile
    private var hydrated = false

    /** Mutations that arrived before the stored profile was read, replayed on top of it. */
    private val pendingMutations = mutableListOf<(UserProfile) -> UserProfile>()

    init {
        scope.launch {
            val stored = store.load()
            val replayed = synchronized(lock) {
                val base = stored ?: UserProfile()
                val result = pendingMutations.fold(base) { profile, mutation -> mutation(profile) }
                pendingMutations.clear()
                hydrated = true
                result
            }
            _userProfile.value = replayed
            store.save(replayed)
            checkDailyLogin()
        }
    }

    fun addCoins(amount: Int) = mutate(notifyEconomy = amount != 0) { profile ->
        profile.copy(coins = profile.coins + amount)
    }

    /**
     * Atomically deducts [amount] if the wallet has enough. Returns false without changing balance
     * when funds are insufficient (AF3).
     */
    fun trySpendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        var spent = false
        mutate(notifyEconomy = false) { profile ->
            if (profile.coins < amount) {
                spent = false
                profile
            } else {
                spent = true
                profile.copy(coins = profile.coins - amount)
            }
        }
        if (spent && featureFlags.isEnabled(Feature.AF7)) {
            cloudEconomyNotifier.notifyChanged()
        }
        return spent
    }

    fun coins(): Int = _userProfile.value.coins

    fun addStars(amount: Int) = mutate { profile ->
        profile.copy(totalStars = profile.totalStars + amount)
    }

    fun checkDailyLogin(todayDate: String = dateProvider.today()) = mutate { profile ->
        when {
            profile.lastLoginDate.isEmpty() -> profile.copy(
                lastLoginDate = todayDate,
                dailyChallenge = profile.dailyChallenge.copy(dateSeed = todayDate)
            )

            profile.lastLoginDate != todayDate -> {
                val lastDate = runCatching { LocalDate.parse(profile.lastLoginDate) }.getOrNull()
                val parsedToday = runCatching { LocalDate.parse(todayDate) }.getOrNull()
                val isConsecutive = lastDate != null &&
                    parsedToday != null &&
                    lastDate.plusDays(1) == parsedToday

                val hasFinishedCycle = profile.claimedDays.contains(FINAL_STREAK_DAY)
                val newStreak = if (isConsecutive && !hasFinishedCycle) profile.currentStreak else 1
                val newClaimed = if (isConsecutive && !hasFinishedCycle) profile.claimedDays else emptySet()

                profile.copy(
                    lastLoginDate = todayDate,
                    currentStreak = newStreak,
                    claimedDays = newClaimed,
                    dailyQuests = defaultDailyQuests(),
                    dailyChallenge = DailyChallengeState(dateSeed = todayDate)
                )
            }

            profile.dailyChallenge.dateSeed.isEmpty() ->
                profile.copy(dailyChallenge = profile.dailyChallenge.copy(dateSeed = todayDate))

            else -> profile
        }
    }

    fun claimDailyReward(day: Int, coinsReward: Int, starsReward: Int) =
        mutate(notifyEconomy = true) { profile ->
            if (day in profile.claimedDays) return@mutate profile

            val nextStreak = if (day >= profile.currentStreak && day < FINAL_STREAK_DAY) {
                day + 1
            } else {
                profile.currentStreak
            }
            profile.copy(
                coins = profile.coins + coinsReward,
                totalStars = profile.totalStars + starsReward,
                claimedDays = profile.claimedDays + day,
                currentStreak = nextStreak
            )
        }

    fun updateQuestProgress(questId: String, progressIncrement: Int) = mutate { profile ->
        profile.copy(
            dailyQuests = profile.dailyQuests.map { quest ->
                if (quest.id == questId && !quest.isClaimed) {
                    quest.copy(
                        currentProgress = (quest.currentProgress + progressIncrement)
                            .coerceAtMost(quest.targetProgress)
                    )
                } else {
                    quest
                }
            }
        )
    }

    fun claimQuestReward(questId: String) = mutate(notifyEconomy = true) { profile ->
        val quest = profile.dailyQuests.firstOrNull { it.id == questId }
        if (quest == null || !quest.isCompleted || quest.isClaimed) return@mutate profile

        profile.copy(
            coins = profile.coins + quest.coinsReward,
            totalStars = profile.totalStars + quest.starsReward,
            dailyQuests = profile.dailyQuests.map {
                if (it.id == questId) it.copy(isClaimed = true) else it
            }
        )
    }

    fun completeDailyChallenge(score: Int) = finishDailyAttempt(score)

    /**
     * ADV-003: first finished run of the day locks the official score and may award rewards;
     * later finishes that day are practice (attempts++ only).
     */
    fun finishDailyAttempt(score: Int) = mutate { profile ->
        val challenge = profile.dailyChallenge
        if (challenge.attempts > 0) {
            return@mutate profile.copy(
                dailyChallenge = challenge.copy(attempts = challenge.attempts + 1)
            )
        }
        val metTarget = score >= challenge.targetScore
        profile.copy(
            coins = profile.coins + if (metTarget) challenge.coinsReward else 0,
            totalStars = profile.totalStars + if (metTarget) challenge.starsReward else 0,
            dailyChallenge = challenge.copy(
                bestScore = score,
                isCompleted = metTarget,
                attempts = 1
            )
        )
    }

    fun finishWeeklyAttempt(weekKey: String, score: Int) {
        // Weekly PB/runs live in ModeRecordsStore (AF2-09); no profile schema change required.
        Log.d(TAG, "Weekly attempt finished week=$weekKey score=$score")
    }

    /** AF5-01: grant XP and level up. Returns levels gained. */
    fun addXp(amount: Int): Int {
        var levelsGained = 0
        mutate { profile ->
            val (xp, level, gained) = XpCurve.apply(profile.xp, profile.playerLevel, amount)
            levelsGained = gained
            profile.copy(xp = xp, playerLevel = level)
        }
        return levelsGained
    }

    fun recordLifetimeStats(
        mergesDelta: Int = 0,
        biggestTile: Int = 0,
        chainLength: Int = 0,
        playtimeDeltaMs: Long = 0L
    ) = mutate { profile ->
        profile.copy(
            totalMerges = profile.totalMerges + mergesDelta.coerceAtLeast(0),
            biggestTile = maxOf(profile.biggestTile, biggestTile),
            longestChain = maxOf(profile.longestChain, chainLength),
            playtimeMs = profile.playtimeMs + playtimeDeltaMs.coerceAtLeast(0L)
        )
    }

    fun equipTileTheme(id: String) = mutate(notifyEconomy = true) { it.copy(equippedTileThemeId = id) }

    fun equipBoardTheme(id: String) = mutate(notifyEconomy = true) { it.copy(equippedBoardThemeId = id) }

    /** AF9-02: reset daily challenge attempts so another official run is allowed. */
    fun grantExtraDailyAttempt() = mutate(notifyEconomy = false) { profile ->
        profile.copy(
            dailyChallenge = profile.dailyChallenge.copy(attempts = 0)
        )
    }

    /** AF7: replace in-memory + Room profile from a cloud restore (skips economy upload). */
    fun replaceFromCloud(profile: UserProfile) {
        synchronized(lock) {
            pendingMutations.clear()
            hydrated = true
        }
        _userProfile.value = profile
        scope.launch { store.save(profile) }
    }

    /**
     * Applies [block] to the in-memory profile immediately and persists the result.
     *
     * Before hydration completes the mutation is also recorded, so that when the stored profile
     * arrives it can be replayed on top of it. Without that, a coin grant in the first moments after
     * launch would either be overwritten by the load or would overwrite the stored balance.
     */
    private fun mutate(
        notifyEconomy: Boolean = false,
        block: (UserProfile) -> UserProfile
    ) {
        synchronized(lock) {
            if (!hydrated) pendingMutations += block
        }
        val updated = _userProfile.updateAndGet(block)
        if (hydrated) {
            scope.launch { store.save(updated) }
        }
        if (notifyEconomy && featureFlags.isEnabled(Feature.AF7)) {
            cloudEconomyNotifier.notifyChanged()
        }
    }

    private companion object {
        /** Length of the daily reward cycle, after which the streak restarts. */
        const val FINAL_STREAK_DAY = 7
        const val TAG = "UserDataRepository"
    }
}
