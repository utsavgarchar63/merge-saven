package com.mergeseven.game.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.view.ViewGroup
import android.util.Log
import com.mergeseven.game.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*
import com.google.android.gms.ads.rewarded.*
import com.mergeseven.game.R
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveOpsGates
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
    private val initialization = CompletableDeferred<Unit>()
    private val requested = mutableSetOf<AdPlacement>()
    private val loadGate = AdLoadGate()
    private val loadedAt = mutableMapOf<AdPlacement, Long>()
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var banner: AdView? = null
    private var bannerContainer: ViewGroup? = null
    private var bannerWidthDp = 0
    private var bannerBindingGeneration = 0
    private var generation = 0
    private val expiryJobs = mutableMapOf<AdPlacement, Job>()
    private var bannerRetry: Job? = null
    private val processLifecycle = ProcessLifecycleOwner.get().lifecycle
    private val qaFallback = mutableSetOf<AdPlacement>()
    init {
        scope.launch {
            combine(consentManager.canRequestAds, featureFlags.observe(Feature.AF9), liveOpsGates.revision) { can, enabled, _ ->
                can && enabled && liveOpsGates.adsAllowed()
            }.collect { allowed ->
                Log.i("MergeSevenAds", "request_gate allowed=$allowed consent=${consentManager.canRequestAds.value} feature=${featureFlags.isEnabled(Feature.AF9)} kill_switch=${!liveOpsGates.adsAllowed()}")
                if (allowed) requested.toList().forEach { preload(it) }
                else clearInventory()
            }
        }
        scope.launch {
            // Retry missing inventory on foreground entry and keep recovering after
            // extended outages. AdLoadGate enforces a shared, capped failure backoff.
            processLifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    requested.toList().forEach { preload(it) }
                    delay(30_000L)
                }
            }
        }
    }
    private fun enabled() = consentManager.canRequestAds.value &&
        featureFlags.isEnabled(Feature.AF9) && liveOpsGates.adsAllowed()
    private fun replenish(placement: AdPlacement) {
        loadGate.allowRetry(placement)
        requested.toList().forEach { preload(it) }
    }
    private suspend fun initialize() {
        if (!initialized) {
            initialized = true
            val devices = BuildConfig.AD_TEST_DEVICE_IDS.split(',').map(String::trim).filter(String::isNotEmpty)
            MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(devices).build())
            MobileAds.initialize(context) { status ->
                status.adapterStatusMap.forEach { (adapter, value) ->
                    Log.i("MergeSevenAds", "initialized adapter=$adapter state=${value.initializationState} latency_ms=${value.latency} description=${value.description}")
                }
                initialization.complete(Unit)
            }
        }
        // Suspend the request coroutine, never the UI, until every adapter has had its init chance.
        initialization.await()
    }
    private fun unit(placement: AdPlacement): String {
        val demo = inventoryKey(placement) in qaFallback
        return context.getString(when (placement) {
            AdPlacement.BANNER -> if (demo) R.string.admob_demo_banner_unit_id else R.string.admob_banner_unit_id
            AdPlacement.INTERSTITIAL -> if (demo) R.string.admob_demo_interstitial_unit_id else R.string.admob_interstitial_unit_id
            else -> if (demo) R.string.admob_demo_rewarded_unit_id else R.string.admob_rewarded_unit_id
        })
    }
    private fun inventorySource(p: AdPlacement) = when {
        inventoryKey(p) in qaFallback -> "qa_demo_fallback"
        BuildConfig.TEST_ADS -> "demo"
        else -> "production_units"
    }
    override fun isReady(placement: AdPlacement): Boolean = enabled() && placement in ready.value &&
        (placement == AdPlacement.BANNER || SystemClock.elapsedRealtime() - (loadedAt[inventoryKey(placement)] ?: 0) < 3_600_000)
    private fun telemetry(name: String, p: AdPlacement, extra: Map<String, Any?> = emptyMap()) {
        analytics.logEvent(name, mapOf("placement" to p.name.lowercase(), "inventory" to inventorySource(p)) + extra)
        if (name in setOf("ad_shown", "ad_impression", "ad_reward_granted", "ad_reward_failed", "ad_show_failed"))
            diagnostic(name, p)
    }
    private fun inventoryPlacements(p: AdPlacement): Set<AdPlacement> =
        if (inventoryKey(p) == AdPlacement.FUNDS_COINS) rewardedPlacements else setOf(p)
    private fun loaded(p: AdPlacement) {
        loadGate.finished(p)
        loadGate.allowRetry(p)
        val key = inventoryKey(p)
        if (p == AdPlacement.BANNER) { bannerRetry?.cancel(); bannerRetry = null }
        val timestamp = SystemClock.elapsedRealtime()
        loadedAt[key] = timestamp
        ready.value += inventoryPlacements(p)
        telemetry("ad_loaded", p)
        diagnostic("loaded", p, when (inventoryKey(p)) {
            AdPlacement.BANNER -> banner?.responseInfo
            AdPlacement.INTERSTITIAL -> interstitial?.responseInfo
            else -> rewarded?.responseInfo
        })
        expiryJobs.remove(key)?.cancel()
        if (p != AdPlacement.BANNER) expiryJobs[key] = scope.launch {
            delay(3_600_000L)
            if (loadedAt[key] == timestamp) {
                ready.value -= inventoryPlacements(p)
                loadedAt.remove(key)
                if (key == AdPlacement.FUNDS_COINS) rewarded = null else interstitial = null
                loadGate.allowRetry(p); preload(p)
            }
        }
    }
    private fun failed(p: AdPlacement, error: LoadAdError) {
        ready.value -= inventoryPlacements(p)
        telemetry("ad_load_failed", p, mapOf("code" to error.code))
        Log.w("MergeSevenAds", "load_failed placement=$p domain=${error.domain} code=${error.code} message=${error.message} cause=${error.cause}")
        diagnostic("load_failed", p, error.responseInfo)
        if (tryQaFallback(p)) return
        val retryDelay = loadGate.failed(p, SystemClock.elapsedRealtime())
        // Fullscreen inventory is maintained by the foreground loop. Banners need
        // their actual visible host and also recover beyond the old three-attempt limit.
        if (p == AdPlacement.BANNER) {
            bannerRetry?.cancel()
            val token = generation
            val retryBanner = banner
            bannerRetry = scope.launch {
                delay(retryDelay)
                while (token == generation && enabled() &&
                    (showing.value || !processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))) delay(1_000L)
                if (token == generation && enabled() && !showing.value) {
                    if (banner === retryBanner && bannerContainer?.isAttachedToWindow == true &&
                        bannerContainer?.isShown == true) {
                        diagnostic("retry_started", p)
                        retryBanner?.loadAd(AdRequest.Builder().build())
                    }
                }
            }
        }
    }
    private fun diagnostic(event: String, p: AdPlacement, response: ResponseInfo? = null) {
        Log.i("MergeSevenAds", "$event placement=$p profile=${inventorySource(p)} unit=${unit(p)} response_id=${response?.responseId} adapter=${response?.mediationAdapterClassName}")
        response?.adapterResponses?.forEach { adapter ->
            Log.i("MergeSevenAds", "adapter=${adapter.adapterClassName} latency_ms=${adapter.latencyMillis} error=${adapter.adError}")
        }
    }
    private fun paid(p: AdPlacement, value: AdValue) = telemetry("ad_paid", p,
        mapOf("value_micros" to value.valueMicros, "currency" to value.currencyCode,
            "precision" to value.precisionType))
    override fun preload(placement: AdPlacement) {
        scope.launch {
            requested += placement
            if (!enabled() || placement == AdPlacement.BANNER || isReady(placement) || showing.value ||
                !processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
            val now = SystemClock.elapsedRealtime()
            if (!loadGate.tryBegin(placement, now)) return@launch
            val token = generation
            initialize()
            if (token != generation) return@launch
            if (!enabled() || !processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                loadGate.finished(placement); loadGate.allowRetry(placement); return@launch
            }
            qaFallback.remove(inventoryKey(placement))
            loadFullscreen(placement, token)
        }
    }
    private fun tryQaFallback(p: AdPlacement): Boolean {
        val key = inventoryKey(p)
        if (!enabled() || !QaAdFallbackPolicy.allows(BuildConfig.DEBUG, BuildConfig.QA_AD_FALLBACK,
                !BuildConfig.TEST_ADS, AdRequest.Builder().build().isTestDevice(context), key in qaFallback)) return false
        val host = bannerContainer
        val owner = banner?.context as? Activity
        if (p == AdPlacement.BANNER && (host == null || owner == null)) return false
        qaFallback += key
        diagnostic("qa_fallback_started", p)
        if (p == AdPlacement.BANNER) requestBanner(requireNotNull(owner), requireNotNull(host), generation)
        else loadFullscreen(p, generation)
        return true
    }
    private fun loadFullscreen(placement: AdPlacement, token: Int) {
            if (token != generation || !enabled()) return
            telemetry("ad_load_started", placement)
            diagnostic("load_started", placement)
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
                        rewarded = ad; loaded(placement)
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) { if (token == generation) failed(placement, error) }
                })
            }
    }
    override suspend fun showRewarded(activity: Activity, placement: AdPlacement, onEarned: suspend () -> Unit): AdResult =
        withContext(Dispatchers.Main.immediate) {
            if (placement !in rewardedPlacements || !isReady(placement) || showing.value || !canShow(activity) || !showMutex.tryLock()) {
                preload(placement); return@withContext AdResult.NoFill
            }
            try {
                val ad = rewarded ?: return@withContext AdResult.NoFill
                rewarded = null
                expiryJobs.remove(AdPlacement.FUNDS_COINS)?.cancel()
                loadedAt.remove(AdPlacement.FUNDS_COINS)
                ready.value -= rewardedPlacements; showing.value = true
                ad.setOnPaidEventListener { paid(placement, it) }
                suspendCancellableCoroutine<AdResult> { cont ->
                    var earnedJob: Deferred<Boolean>? = null
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdImpression() { telemetry("ad_impression", placement) }
                        override fun onAdShowedFullScreenContent() { telemetry("ad_shown", placement) }
                        override fun onAdDismissedFullScreenContent() {
                            scope.launch {
                                exposure.fullscreenClosed(); showing.value = false
                                replenish(placement)
                                val earned = earnedJob?.await() == true
                                if (cont.isActive) cont.resume(if (earned) AdResult.Rewarded else AdResult.Dismissed)
                            }
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.w("MergeSevenAds", "show_failed placement=$placement domain=${error.domain} code=${error.code} message=${error.message}")
                            showing.value = false; telemetry("ad_show_failed", placement, mapOf("code" to error.code))
                            if (cont.isActive) cont.resume(AdResult.Failed("Ad unavailable"))
                            replenish(placement)
                        }
                    }
                    try {
                        ad.show(activity) {
                            if (earnedJob == null) earnedJob = scope.async {
                                runCatching { onEarned(); telemetry("ad_reward_granted", placement); true }
                                    .getOrElse { telemetry("ad_reward_failed", placement); false }
                            }
                        }
                    } catch (error: Exception) { showException(placement, error, cont) }
                }
            } finally { showMutex.unlock() }
        }
    override suspend fun showInterstitial(activity: Activity): AdResult = withContext(Dispatchers.Main.immediate) {
        if (!isReady(AdPlacement.INTERSTITIAL) || showing.value || !canShow(activity) || !showMutex.tryLock())
            return@withContext AdResult.NoFill
        try {
            val ad = interstitial ?: return@withContext AdResult.NoFill
            interstitial = null; ready.value -= AdPlacement.INTERSTITIAL; showing.value = true
            expiryJobs.remove(AdPlacement.INTERSTITIAL)?.cancel()
            loadedAt.remove(AdPlacement.INTERSTITIAL)
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
                            replenish(AdPlacement.INTERSTITIAL)
                            if (cont.isActive) cont.resume(AdResult.Completed)
                        }
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w("MergeSevenAds", "show_failed placement=INTERSTITIAL domain=${error.domain} code=${error.code} message=${error.message}")
                        showing.value = false
                        telemetry("ad_show_failed", AdPlacement.INTERSTITIAL, mapOf("code" to error.code))
                        if (cont.isActive) cont.resume(AdResult.Failed("Ad unavailable"))
                        replenish(AdPlacement.INTERSTITIAL)
                    }
                }
                try { ad.show(activity) }
                catch (error: Exception) { showException(AdPlacement.INTERSTITIAL, error, cont) }
            }
        } finally { showMutex.unlock() }
    }
    private fun canShow(activity: Activity): Boolean = !activity.isFinishing && !activity.isDestroyed &&
        ((activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
            ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) != false)

    private fun showException(placement: AdPlacement, error: Exception, cont: CancellableContinuation<AdResult>) {
        Log.w("MergeSevenAds", "show_exception placement=$placement type=${error.javaClass.simpleName}")
        showing.value = false
        telemetry("ad_show_failed", placement, mapOf("reason" to "sdk_exception"))
        if (cont.isActive) cont.resume(AdResult.Failed("Ad unavailable"))
        replenish(placement)
    }
    override fun bindBanner(activity: Activity, container: ViewGroup) {
        scope.launch {
            if (!enabled()) { unbindBanner(); return@launch }
            if (!container.isAttachedToWindow || activity.isDestroyed) { unbindBanner(container); return@launch }
            val width = (container.width / activity.resources.displayMetrics.density).toInt()
            if (width <= 0) return@launch // Wait for layout, never request using an unrelated screen width.
            if (bannerContainer === container && bannerWidthDp == width) return@launch
            unbindBanner()
            // Claim the host before suspending initialization, so recomposition cannot bind twice.
            bannerContainer = container
            bannerWidthDp = width
            val token = generation
            val bindingToken = bannerBindingGeneration
            initialize()
            if (token != generation || bindingToken != bannerBindingGeneration || bannerContainer !== container) return@launch
            val resumed = (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
                ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) != false
            if (!enabled() || !container.isAttachedToWindow || activity.isDestroyed || !resumed) {
                unbindBanner(container); return@launch
            }
            qaFallback.remove(AdPlacement.BANNER)
            requestBanner(activity, container, token)
        }
    }
    private fun requestBanner(activity: Activity, container: ViewGroup, token: Int) {
            if (token != generation || bannerContainer !== container || !enabled() ||
                !container.isAttachedToWindow || activity.isDestroyed) return
            banner?.destroy(); container.removeAllViews(); banner = null
            val width = bannerWidthDp
            val ad = AdView(activity)
            ad.apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, width))
                adUnitId = unit(AdPlacement.BANNER)
                adListener = object : AdListener() {
                    override fun onAdLoaded() { if (token == generation && banner === ad && bannerContainer === container) loaded(AdPlacement.BANNER) }
                    override fun onAdFailedToLoad(error: LoadAdError) { if (banner === ad) failed(AdPlacement.BANNER, error) }
                    override fun onAdImpression() { telemetry("ad_impression", AdPlacement.BANNER) }
                }
                setOnPaidEventListener { paid(AdPlacement.BANNER, it) }
            }
            banner = ad; bannerContainer = container; container.addView(ad)
            telemetry("ad_load_started", AdPlacement.BANNER)
            diagnostic("load_started", AdPlacement.BANNER)
            ad.loadAd(AdRequest.Builder().build())
    }
    override fun unbindBanner(container: ViewGroup?) {
        if (container != null && bannerContainer !== container) return
        bannerRetry?.cancel(); bannerRetry = null
        loadGate.allowRetry(AdPlacement.BANNER)
        bannerBindingGeneration++
        banner?.destroy(); bannerContainer?.removeAllViews(); banner = null; bannerContainer = null
        bannerWidthDp = 0
        ready.value -= AdPlacement.BANNER
    }
    override fun pauseBanner(container: ViewGroup?) { if (container == null || bannerContainer === container) banner?.pause() }
    override fun resumeBanner(container: ViewGroup?) {
        if (container != null && bannerContainer !== container) return
        if (enabled()) banner?.resume() else unbindBanner()
    }
    private fun clearInventory() {
        generation++; rewarded = null; interstitial = null; loadGate.clear(); loadedAt.clear(); qaFallback.clear()
        expiryJobs.values.forEach { it.cancel() }; expiryJobs.clear()
        ready.value = emptySet(); unbindBanner()
    }
    override fun destroy() { scope.launch { requested.clear(); clearInventory() } }
}
