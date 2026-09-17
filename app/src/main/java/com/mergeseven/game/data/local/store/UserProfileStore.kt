package com.mergeseven.game.data.local.store

import com.mergeseven.game.core.DispatcherProvider
import com.mergeseven.game.data.local.dao.UserProfileDao
import com.mergeseven.game.data.local.entity.DailyChallengeEmbedded
import com.mergeseven.game.data.local.entity.DailyQuestEntity
import com.mergeseven.game.data.local.entity.UserProfileEntity
import com.mergeseven.game.data.model.DailyChallengeState
import com.mergeseven.game.data.model.DailyQuest
import com.mergeseven.game.data.model.UserProfile
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage boundary for [UserProfile].
 *
 * The repository owns the rules (streaks, claims, quest progress) and this owns the bytes. Keeping
 * them apart is what lets the streak and quest logic be unit tested on the JVM without Robolectric
 * or an instrumented database.
 */
interface UserProfileStore {
    /** Returns the stored profile, or null on first run. */
    suspend fun load(): UserProfile?

    suspend fun save(profile: UserProfile)
}

@Singleton
class RoomUserProfileStore @Inject constructor(
    private val dao: UserProfileDao,
    private val dispatchers: DispatcherProvider
) : UserProfileStore {

    override suspend fun load(): UserProfile? = withContext(dispatchers.io) {
        val entity = dao.getProfile() ?: return@withContext null
        val quests = dao.getQuests()
        UserProfile(
            coins = entity.coins,
            totalStars = entity.totalStars,
            currentStreak = entity.currentStreak,
            claimedDays = entity.claimedDays,
            lastLoginDate = entity.lastLoginDate,
            dailyQuests = quests.map { it.toDomain() },
            dailyChallenge = entity.dailyChallenge.toDomain(),
            xp = entity.xp,
            playerLevel = entity.playerLevel,
            equippedTileThemeId = entity.equippedTileThemeId,
            equippedBoardThemeId = entity.equippedBoardThemeId,
            totalMerges = entity.totalMerges,
            biggestTile = entity.biggestTile,
            longestChain = entity.longestChain,
            playtimeMs = entity.playtimeMs
        )
    }

    override suspend fun save(profile: UserProfile) = withContext(dispatchers.io) {
        dao.save(
            profile = profile.toEntity(),
            quests = profile.dailyQuests.mapIndexed { index, quest -> quest.toEntity(index) }
        )
    }
}

/** In-memory store for unit tests and Compose previews. */
class InMemoryUserProfileStore(
    initial: UserProfile? = null
) : UserProfileStore {

    private var stored: UserProfile? = initial

    var saveCount: Int = 0
        private set

    override suspend fun load(): UserProfile? = stored

    override suspend fun save(profile: UserProfile) {
        stored = profile
        saveCount++
    }
}

private fun UserProfile.toEntity() = UserProfileEntity(
    coins = coins,
    totalStars = totalStars,
    currentStreak = currentStreak,
    claimedDays = claimedDays,
    lastLoginDate = lastLoginDate,
    dailyChallenge = DailyChallengeEmbedded(
        dateSeed = dailyChallenge.dateSeed,
        title = dailyChallenge.title,
        targetScore = dailyChallenge.targetScore,
        isCompleted = dailyChallenge.isCompleted,
        bestScore = dailyChallenge.bestScore,
        coinsReward = dailyChallenge.coinsReward,
        starsReward = dailyChallenge.starsReward,
        attempts = dailyChallenge.attempts
    ),
    xp = xp,
    playerLevel = playerLevel,
    equippedTileThemeId = equippedTileThemeId,
    equippedBoardThemeId = equippedBoardThemeId,
    totalMerges = totalMerges,
    biggestTile = biggestTile,
    longestChain = longestChain,
    playtimeMs = playtimeMs
)

private fun DailyChallengeEmbedded.toDomain() = DailyChallengeState(
    dateSeed = dateSeed,
    title = title,
    targetScore = targetScore,
    isCompleted = isCompleted,
    bestScore = bestScore,
    coinsReward = coinsReward,
    starsReward = starsReward,
    attempts = attempts
)

private fun DailyQuest.toEntity(orderIndex: Int) = DailyQuestEntity(
    questId = id,
    title = title,
    description = description,
    currentProgress = currentProgress,
    targetProgress = targetProgress,
    coinsReward = coinsReward,
    starsReward = starsReward,
    isClaimed = isClaimed,
    orderIndex = orderIndex
)

private fun DailyQuestEntity.toDomain() = DailyQuest(
    id = questId,
    title = title,
    description = description,
    currentProgress = currentProgress,
    targetProgress = targetProgress,
    coinsReward = coinsReward,
    starsReward = starsReward,
    isClaimed = isClaimed
)
