package com.mergeseven.game.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.mergeseven.game.R
import com.mergeseven.game.billing.EntitlementStore
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveOpsGates
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AdMob-backed [AdService] (AF9-01). Loads only after consent + AF9 + adsAllowed.
 * Remove Ads / Premium suppress banner and interstitial only (AF9-10).
 */
@Singleton
class AdMobAdService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
    private val featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    private val entitlementStore: EntitlementStore
) : AdService {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()
    @Volatile private var adsInitialized = false
    private val rewardedByPlacement = mutableMapOf<AdPlacement, RewardedAd?>()
    private var interstitialAd: InterstitialAd? = null
    private var bannerView: AdView? = null

    private fun adsEnabled(): Boolean =
        featureFlags.isEnabled(Feature.AF9) &&
            liveOpsGates.adsAllowed() &&
            consentManager.canRequestAds.value

    private fun suppressNonRewarded(): Boolean =
        entitlementStore.removeAdsOrPremium.value

    private fun ensureInitialized() {
        if (adsInitialized || !adsEnabled()) return
        MobileAds.initialize(context) {}
        adsInitialized = true
    }

    override fun isReady(placement: AdPlacement): Boolean {
        if (!adsEnabled()) return false
        return when (placement) {
            AdPlacement.INTERSTITIAL -> interstitialAd != null && !suppressNonRewarded()
            AdPlacement.BANNER -> !suppressNonRewarded()
            else -> rewardedByPlacement[placement] != null
        }
    }

    override fun preload(placement: AdPlacement) {
        if (!adsEnabled()) return
        if (placement == AdPlacement.BANNER) return
        if (placement == AdPlacement.INTERSTITIAL && suppressNonRewarded()) return
        ensureInitialized()
        mainHandler.post {
            when (placement) {
                AdPlacement.INTERSTITIAL -> loadInterstitial()
                AdPlacement.BANNER -> Unit
                else -> loadRewarded(placement)
            }
        }
    }

    override suspend fun showRewarded(activity: Activity, placement: AdPlacement): AdResult {
        if (!adsEnabled()) return AdResult.NoFill
        ensureInitialized()
        return mutex.withLock {
            val existing = rewardedByPlacement[placement]
            if (existing == null) {
                loadRewardedBlocking(placement)
            }
            val ad = rewardedByPlacement[placement] ?: return@withLock AdResult.NoFill
            rewardedByPlacement[placement] = null
            showRewardedBlocking(activity, ad).also {
                // Warm next
                mainHandler.post { loadRewarded(placement) }
            }
        }
    }

    override suspend fun showInterstitial(activity: Activity): AdResult {
        if (!adsEnabled() || suppressNonRewarded()) return AdResult.NoFill
        ensureInitialized()
        return mutex.withLock {
            if (interstitialAd == null) loadInterstitialBlocking()
            val ad = interstitialAd ?: return@withLock AdResult.NoFill
            interstitialAd = null
            showInterstitialBlocking(activity, ad).also {
                mainHandler.post { loadInterstitial() }
            }
        }
    }

    override fun bindBanner(activity: Activity, container: ViewGroup) {
        if (!adsEnabled() || suppressNonRewarded()) {
            container.removeAllViews()
            return
        }
        ensureInitialized()
        mainHandler.post {
            container.removeAllViews()
            val view = AdView(activity).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = context.getString(R.string.admob_banner_unit_id)
                loadAd(AdRequest.Builder().build())
            }
            bannerView = view
            container.addView(view)
        }
    }

    override fun unbindBanner() {
        mainHandler.post {
            bannerView?.destroy()
            bannerView = null
        }
    }

    override fun destroy() {
        unbindBanner()
        rewardedByPlacement.clear()
        interstitialAd = null
    }

    private fun rewardedUnitId(): String =
        context.getString(R.string.admob_rewarded_unit_id)

    private fun interstitialUnitId(): String =
        context.getString(R.string.admob_interstitial_unit_id)

    private fun loadRewarded(placement: AdPlacement) {
        RewardedAd.load(
            context,
            rewardedUnitId(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedByPlacement[placement] = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Rewarded load fail ${placement}: ${error.message}")
                    rewardedByPlacement[placement] = null
                }
            }
        )
    }

    private suspend fun loadRewardedBlocking(placement: AdPlacement) =
        suspendCancellableCoroutine { cont ->
            RewardedAd.load(
                context,
                rewardedUnitId(),
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedByPlacement[placement] = ad
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedByPlacement[placement] = null
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            )
        }

    private suspend fun showRewardedBlocking(activity: Activity, ad: RewardedAd): AdResult =
        suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) {
                        cont.resume(if (earned) AdResult.Rewarded else AdResult.Dismissed)
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdResult.Failed(error.message ?: "show failed"))
                }
            }
            ad.show(activity) {
                earned = true
            }
        }

    private fun loadInterstitial() {
        InterstitialAd.load(
            context,
            interstitialUnitId(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private suspend fun loadInterstitialBlocking() =
        suspendCancellableCoroutine { cont ->
            InterstitialAd.load(
                context,
                interstitialUnitId(),
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            )
        }

    private suspend fun showInterstitialBlocking(activity: Activity, ad: InterstitialAd): AdResult =
        suspendCancellableCoroutine { cont ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) cont.resume(AdResult.Completed)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdResult.Failed(error.message ?: "show failed"))
                }
            }
            ad.show(activity)
        }

    private companion object {
        const val TAG = "AdMobAdService"
    }
}
