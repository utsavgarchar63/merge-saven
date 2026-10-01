package com.mergeseven.game.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*
import com.google.android.gms.ads.rewarded.*
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.R
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveOpsGates
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Consent-gated inventory. Show never waits for a network load. */
@Singleton
class AdMobAdService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentManager: ConsentManager,
    private val featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    private val exposure: AdExposureStore,
    private val analytics: AnalyticsTracker
) : AdService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val showMutex = Mutex()
    private val ready = MutableStateFlow<Set<AdPlacement>>(emptySet())
    override val availability = ready.asStateFlow()
    private val showing = MutableStateFlow(false)
    override val fullscreenShowing = showing.asStateFlow()
    private var initialized = false
    private val requested = mutableSetOf<AdPlacement>()
    private val loading = mutableSetOf<AdPlacement>()
    private val loadedAt = mutableMapOf<AdPlacement, Long>()
    private val lastAttempt = mutableMapOf<AdPlacement, Long>()
    private val rewarded = mutableMapOf<AdPlacement, RewardedAd>()
    private var interstitial: InterstitialAd? = null
    private var banner: AdView? = null
    private var bannerContainer: ViewGroup? = null
    private var generation = 0
    init {
        scope.launch {
            combine(consentManager.canRequestAds, featureFlags.observe(Feature.AF9), liveOpsGates.revision) { can, enabled, _ ->
                can && enabled && liveOpsGates.adsAllowed()
            }.collect { allowed ->
                if (allowed) requested.toList().forEach { preload(it) }
                else clearInventory()
            }
        }
    }
    private fun enabled() = consentManager.canRequestAds.value &&
        featureFlags.isEnabled(Feature.AF9) && liveOpsGates.adsAllowed()
    private fun initialize() { if (!initialized) { MobileAds.initialize(context) {}; initialized = true } }
    private fun unit(placement: AdPlacement): String = if (BuildConfig.DEBUG) when (placement) {
        AdPlacement.BANNER -> "ca-app-pub-3940256099942544/6300978111"
        AdPlacement.INTERSTITIAL -> "ca-app-pub-3940256099942544/1033173712"
        else -> "ca-app-pub-3940256099942544/5224354917"
    } else context.getString(when (placement) {
        AdPlacement.BANNER -> R.string.admob_banner_unit_id
        AdPlacement.INTERSTITIAL -> R.string.admob_interstitial_unit_id
        else -> R.string.admob_rewarded_unit_id
    })
    override fun isReady(placement: AdPlacement): Boolean = enabled() && placement in ready.value &&
        (placement == AdPlacement.BANNER || System.currentTimeMillis() - (loadedAt[placement] ?: 0) < 3_600_000)
    private fun telemetry(name: String, p: AdPlacement, extra: Map<String, Any?> = emptyMap()) =
        analytics.logEvent(name, mapOf("placement" to p.name.lowercase()) + extra)
    private fun loaded(p: AdPlacement) {
        loading.remove(p); loadedAt[p] = System.currentTimeMillis(); ready.value += p
        telemetry("ad_loaded", p)
    }
    private fun failed(p: AdPlacement, error: LoadAdError) {
        loading.remove(p); ready.value -= p
        telemetry("ad_load_failed", p, mapOf("code" to error.code))
    }
    private fun paid(p: AdPlacement, value: AdValue) = telemetry("ad_paid", p,
        mapOf("value_micros" to value.valueMicros, "currency" to value.currencyCode,
            "precision" to value.precisionType))
    override fun preload(placement: AdPlacement) {
        scope.launch {
            requested += placement
            if (!enabled() || placement == AdPlacement.BANNER || isReady(placement) || placement in loading) return@launch
            val now = System.currentTimeMillis()
            if (now - (lastAttempt[placement] ?: 0) < 30_000) return@launch
            initialize(); loading += placement; lastAttempt[placement] = now
            val token = generation
            telemetry("ad_load_started", placement)
            if (placement == AdPlacement.INTERSTITIAL) {
                InterstitialAd.load(context, unit(placement), AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        if (token != generation || !enabled()) return
                        interstitial = ad; ad.setOnPaidEventListener { paid(placement, it) }; loaded(placement)
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) { if (token == generation) failed(placement, error) }
                })
            } else {
                RewardedAd.load(context, unit(placement), AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (token != generation || !enabled()) return
                        rewarded[placement] = ad; ad.setOnPaidEventListener { paid(placement, it) }; loaded(placement)
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) { if (token == generation) failed(placement, error) }
                })
            }
        }
    }
    override suspend fun showRewarded(activity: Activity, placement: AdPlacement, onEarned: suspend () -> Unit): AdResult =
        withContext(Dispatchers.Main.immediate) {
            if (!isReady(placement) || showing.value || activity.isFinishing || activity.isDestroyed || !showMutex.tryLock()) {
                preload(placement); return@withContext AdResult.NoFill
            }
            try {
                val ad = rewarded.remove(placement) ?: return@withContext AdResult.NoFill
                ready.value -= placement; showing.value = true
                suspendCancellableCoroutine<AdResult> { cont ->
                    var earnedJob: Deferred<Boolean>? = null
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdImpression() { telemetry("ad_impression", placement) }
                        override fun onAdShowedFullScreenContent() { telemetry("ad_shown", placement) }
                        override fun onAdDismissedFullScreenContent() {
                            scope.launch {
                                exposure.fullscreenClosed(); showing.value = false
                                val earned = earnedJob?.await() == true
                                if (cont.isActive) cont.resume(if (earned) AdResult.Rewarded else AdResult.Dismissed)
                                lastAttempt.remove(placement); preload(placement)
                            }
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            showing.value = false; telemetry("ad_show_failed", placement, mapOf("code" to error.code))
                            if (cont.isActive) cont.resume(AdResult.Failed("Ad unavailable"))
                        }
                    }
                    ad.show(activity) {
                        if (earnedJob == null) earnedJob = scope.async {
                            runCatching { onEarned(); telemetry("ad_reward_granted", placement); true }
                                .getOrElse { telemetry("ad_reward_failed", placement); false }
                        }
                    }
                }
            } finally { showMutex.unlock() }
        }
    override suspend fun showInterstitial(activity: Activity): AdResult = withContext(Dispatchers.Main.immediate) {
        if (!isReady(AdPlacement.INTERSTITIAL) || showing.value || activity.isFinishing || activity.isDestroyed || !showMutex.tryLock())
            return@withContext AdResult.NoFill
        try {
            val ad = interstitial ?: return@withContext AdResult.NoFill
            interstitial = null; ready.value -= AdPlacement.INTERSTITIAL; showing.value = true
            suspendCancellableCoroutine<AdResult> { cont ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdImpression() { telemetry("ad_impression", AdPlacement.INTERSTITIAL) }
                    override fun onAdShowedFullScreenContent() {
                        telemetry("ad_shown", AdPlacement.INTERSTITIAL)
                        scope.launch { exposure.interstitialShown() }
                    }
                    override fun onAdDismissedFullScreenContent() {
                        scope.launch {
                            exposure.fullscreenClosed(); showing.value = false
                            if (cont.isActive) cont.resume(AdResult.Completed)
                            lastAttempt.remove(AdPlacement.INTERSTITIAL); preload(AdPlacement.INTERSTITIAL)
                        }
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        showing.value = false
                        if (cont.isActive) cont.resume(AdResult.Failed("Ad unavailable"))
                    }
                }
                ad.show(activity)
            }
        } finally { showMutex.unlock() }
    }
    override fun bindBanner(activity: Activity, container: ViewGroup) {
        scope.launch {
            if (!enabled() || !container.isAttachedToWindow || activity.isDestroyed) { unbindBanner(); return@launch }
            if (bannerContainer === container && banner != null) return@launch
            unbindBanner(); initialize()
            val density = activity.resources.displayMetrics.density
            val width = ((container.width.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels) / density).toInt()
            val token = generation
            val ad = AdView(activity).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width))
                adUnitId = unit(AdPlacement.BANNER)
                adListener = object : AdListener() {
                    override fun onAdLoaded() { if (token == generation) loaded(AdPlacement.BANNER) }
                    override fun onAdFailedToLoad(error: LoadAdError) { failed(AdPlacement.BANNER, error) }
                    override fun onAdImpression() { telemetry("ad_impression", AdPlacement.BANNER) }
                }
                setOnPaidEventListener { paid(AdPlacement.BANNER, it) }
            }
            banner = ad; bannerContainer = container; container.addView(ad)
            ad.loadAd(AdRequest.Builder().build())
        }
    }
    override fun unbindBanner() {
        banner?.destroy(); bannerContainer?.removeAllViews(); banner = null; bannerContainer = null
        ready.value -= AdPlacement.BANNER
    }
    override fun pauseBanner() { banner?.pause() }
    override fun resumeBanner() { if (enabled()) banner?.resume() else unbindBanner() }
    private fun clearInventory() {
        generation++; rewarded.clear(); interstitial = null; loading.clear(); loadedAt.clear()
        ready.value = emptySet(); unbindBanner()
    }
    override fun destroy() { scope.launch { clearInventory() } }
}
