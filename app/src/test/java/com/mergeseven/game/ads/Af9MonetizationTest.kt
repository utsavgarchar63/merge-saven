package com.mergeseven.game.ads
import com.mergeseven.game.testing.TestPersistence
import com.mergeseven.game.data.local.store.InMemoryUserProfileStore
import com.mergeseven.game.economy.RewardRules
import com.mergeseven.game.game.modes.ModeIds
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class Af9MonetizationTest {
    @Test fun economyTelemetryCountsSuccessfulChangesOnly() = runTest {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        val recorder = object : com.mergeseven.game.core.analytics.AnalyticsTracker {
            override fun logEvent(name: String, params: Map<String, Any?>) { events += name to params }
            override fun setUserProperty(name: String, value: String?) = Unit
        }
        val repo = com.mergeseven.game.data.repository.UserDataRepository(
            InMemoryUserProfileStore(), backgroundScope, com.mergeseven.game.core.DateProvider { TestPersistence.TODAY },
            com.mergeseven.game.core.flags.InMemoryFeatureFlags(isDebug = true),
            com.mergeseven.game.cloud.CloudEconomyNotifier(), recorder)
        assertTrue(repo.claimReward("run:double", 30))
        assertFalse(repo.claimReward("run:double", 30))
        assertFalse(repo.trySpendCoins(1000))
        assertTrue(repo.trySpendCoins(10))
        assertEquals(listOf("coin_source", "coin_sink"), events.map { it.first })
        assertEquals(listOf(30L, 10L), events.map { it.second["amount"] })
    }
    @Test fun firstClearSecondaryClaimRejectsAnotherRun() = runTest {
        val repo = TestPersistence.userDataRepository()
        val before = repo.coins()
        assertTrue(repo.claimReward("run1:win", 100, extraKey = "level:1:first"))
        assertFalse(repo.claimReward("run2:win", 100, extraKey = "level:1:first"))
        assertEquals(before + 100, repo.coins())
    }
    @Test fun olderCloudSnapshotCannotRemoveClaims() = runTest {
        val repo = TestPersistence.userDataRepository()
        val old = repo.userProfile.value
        repo.claimReward("run:double", 30)
        repo.replaceFromCloud(old)
        repo.flush()
        assertFalse(repo.claimReward("run:double", 30))
        assertEquals(old.coins + 30, repo.coins())
    }
    @Test fun bonusDailyAttemptCanEarnMissedPrizeOnceWithoutChangingOfficialScore() = runTest {
        val repo = TestPersistence.userDataRepository()
        repo.finishDailyAttempt(10)
        val before = repo.coins()
        val challenge = repo.userProfile.value.dailyChallenge
        repo.claimReward("${TestPersistence.TODAY}:extra_daily", 0, extraDailyAttempt = true)
        repo.finishDailyAttempt(challenge.targetScore)
        assertEquals(before + challenge.coinsReward, repo.coins())
        assertEquals(10, repo.userProfile.value.dailyChallenge.bestScore)
        assertTrue(repo.userProfile.value.dailyChallenge.isCompleted)
        repo.finishDailyAttempt(challenge.targetScore)
        assertEquals(before + challenge.coinsReward, repo.coins())
    }
    @Test fun noFillAndDismissNeverEarn() = runTest {
        var earned = 0
        for (result in listOf(AdResult.NoFill, AdResult.Dismissed, AdResult.Failed("offline"))) {
            val ads = FakeAdService(rewardedResult = result)
            assertEquals(result, ads.showRewarded(android.app.Activity(), AdPlacement.FUNDS_COINS) { earned++ })
        }
        assertEquals(0, earned)
    }
    @Test fun earnedCallbackRunsBeforeShowReturns() = runTest {
        var earned = false
        val result = FakeAdService().showRewarded(android.app.Activity(), AdPlacement.FUNDS_COINS) { earned = true }
        assertTrue(earned); assertEquals(AdResult.Rewarded, result)
    }
    @Test fun claimsSurviveRestartAndConcurrentDuplicate() = runTest {
        val store = InMemoryUserProfileStore()
        val repo = TestPersistence.userDataRepository(store = store)
        val before = repo.coins()
        coroutineScope { repeat(10) { launch { repo.claimReward("run:double", 30) } } }
        assertEquals(before + 30, repo.coins())
        val restored = TestPersistence.userDataRepository(store = store)
        assertFalse(restored.claimReward("run:double", 30))
        assertEquals(before + 30, restored.coins())
        assertEquals(30, store.load()!!.rewardClaims["run:double"])
    }
    @Test fun coinPlacementsShareThreeDailyClaims() = runTest {
        val repo = TestPersistence.userDataRepository()
        val before = repo.coins()
        assertTrue(repo.claimReward("day:coin:1",100,"day:coin:",3))
        assertTrue(repo.claimReward("day:coin:2",50,"day:coin:",3))
        assertTrue(repo.claimReward("day:coin:3",100,"day:coin:",3))
        assertFalse(repo.claimReward("day:coin:4",100,"day:coin:",3))
        assertEquals(before+250, repo.coins())
        assertTrue(repo.claimReward("next:coin:1",100,"next:coin:",3))
    }
    @Test fun earnedDailyRetryDoesNotRegrantOfficialPrize() = runTest {
        val repo = TestPersistence.userDataRepository()
        val target = repo.userProfile.value.dailyChallenge.targetScore
        repo.finishDailyAttempt(target)
        val wallet = repo.coins()
        val official = repo.userProfile.value.dailyChallenge.bestScore
        assertTrue(repo.claimReward("${TestPersistence.TODAY}:extra_daily",0,extraDailyAttempt = true))
        repo.finishDailyAttempt(target + 1000)
        assertEquals(wallet, repo.coins())
        assertEquals(official, repo.userProfile.value.dailyChallenge.bestScore)
        assertFalse(repo.claimReward("${TestPersistence.TODAY}:extra_daily",0,extraDailyAttempt = true))
    }
    @Test fun actualBaseRewardIsDoubled() = runTest {
        val repo = TestPersistence.userDataRepository()
        val before = repo.coins()
        val replay = RewardRules.resultCoins(ModeIds.CAMPAIGN,true,false,20,900)
        assertEquals(30,replay)
        repo.claimReward("run:base",replay)
        repo.claimReward("run:double",replay)
        assertEquals(before+60,repo.coins())
        assertEquals(0,RewardRules.resultCoins(ModeIds.ENDLESS,false,false,9,5000))
        assertEquals(50,RewardRules.resultCoins(ModeIds.TIME_ATTACK,false,false,10,99999))
    }
    @Test fun interstitialBoundaries() {
        val now = 2_000_000L
        val eligible = AdExposure(completedRuns=4,campaignWins=3,playtimeMs=600_000)
        fun reason(s: AdExposure) = decideInterstitial(s,AdPolicyConfig(),now).reason
        assertEquals("ok",reason(eligible))
        assertEquals("grace_runs",reason(eligible.copy(completedRuns=3)))
        assertEquals("grace_playtime",reason(eligible.copy(playtimeMs=599_999)))
        assertEquals("frequency",reason(eligible.copy(campaignWins=2)))
        assertEquals("cooldown",reason(eligible.copy(lastClosedMs=now-179_999)))
        assertEquals("ok",reason(eligible.copy(lastClosedMs=now-180_000)))
        assertEquals("window_cap",reason(eligible.copy(interstitialTimes=listOf(now-1,now-899_999))))
        assertEquals("ok",reason(eligible.copy(interstitialTimes=listOf(now-1,now-900_000))))
        assertEquals("daily_cap",reason(eligible.copy(todayCount=6)))
    }
    @Test fun lossTutorialAndModeSuppression() = runTest {
        val policy = FakeInterstitialPolicy(true)
        assertFalse(policy.evaluate(false,false,false).allow)
        assertFalse(policy.evaluate(true,true,false).allow)
        assertFalse(policy.evaluate(true,false,true).allow)
    }
    @Test fun malformedAdConfigKeepsSafeDefaults() {
        val config = com.mergeseven.game.core.liveops.ParsedLiveConfig(
            { mapOf("ad_policy_json" to "{bad", "ad_frequency" to "-1") },
            kotlinx.coroutines.flow.MutableStateFlow(0L))
        assertEquals(AdPolicyConfig(),config.adPolicy())
        assertEquals(AdPolicyConfig(),AdPolicyConfig(graceRuns=0,gracePlaytimeMs=0,frequency=0,
            cooldownMs=0,windowMs=0,windowCap=99,dailyCap=99).bounded())
    }
}
