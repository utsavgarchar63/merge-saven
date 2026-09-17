package com.mergeseven.game.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.mergeseven.game.core.analytics.AnalyticsEvents
import com.mergeseven.game.core.analytics.AnalyticsTracker
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.liveops.LiveOpsGates
import com.mergeseven.game.di.PersistenceScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    private val receiptValidator: ReceiptValidator,
    private val purchaseGranter: PurchaseGranter,
    private val pendingQueue: PendingPurchaseQueue,
    private val entitlementStore: EntitlementStore,
    private val analyticsTracker: AnalyticsTracker,
    @PersistenceScope private val scope: CoroutineScope
) : BillingRepository, PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private val productCache = mutableMapOf<String, ProductDetails>()
    private var purchaseDeferred: CompletableDeferred<PurchaseResult>? = null

    private fun enabled(): Boolean =
        featureFlags.isEnabled(Feature.AF9) && liveOpsGates.iapAllowed()

    override suspend fun start() {
        if (!enabled()) return
        entitlementStore.load()
        ensureClient()
        processPending()
        queryOwnedAndSync()
    }

    private suspend fun ensureClient() = withContext(Dispatchers.Main) {
        if (billingClient?.isReady == true) return@withContext
        val client = BillingClient.newBuilder(context)
            .setListener(this@PlayBillingRepository)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        billingClient = client
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    override suspend fun queryProducts(): List<ProductDetailsUi> {
        if (!enabled()) return emptyList()
        ensureClient()
        val client = billingClient ?: return emptyList()
        val inApp = queryDetails(client, ProductCatalog.inAppProductIds(), BillingClient.ProductType.INAPP)
        val subs = queryDetails(client, ProductCatalog.subscriptionIds(), BillingClient.ProductType.SUBS)
        (inApp + subs).forEach { productCache[it.productId] = it }
        return (inApp + subs).map { details ->
            val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases
                    ?.pricingPhaseList?.firstOrNull()?.formattedPrice
                ?: ""
            ProductDetailsUi(
                productId = details.productId,
                title = details.title,
                priceLabel = price
            )
        }
    }

    private suspend fun queryDetails(
        client: BillingClient,
        ids: List<String>,
        type: String
    ): List<ProductDetails> {
        if (ids.isEmpty()) return emptyList()
        val products = ids.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(type)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        return suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, productDetailsList ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(productDetailsList.orEmpty())
                } else {
                    Log.w(TAG, "queryProductDetails ${result.debugMessage}")
                    cont.resume(emptyList())
                }
            }
        }
    }

    override suspend fun purchase(activity: Activity, productId: String): PurchaseResult {
        if (!enabled()) return PurchaseResult.Unavailable
        ensureClient()
        analyticsTracker.logEvent(AnalyticsEvents.PURCHASE_STARTED, mapOf("product" to productId))
        val details = productCache[productId] ?: run {
            queryProducts()
            productCache[productId]
        } ?: return PurchaseResult.Failed("Unknown product")
        val client = billingClient ?: return PurchaseResult.Unavailable
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { builder.setOfferToken(it) }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(builder.build()))
            .build()
        val deferred = CompletableDeferred<PurchaseResult>()
        purchaseDeferred = deferred
        val launch = client.launchBillingFlow(activity, flowParams)
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseDeferred = null
            return PurchaseResult.Failed(launch.debugMessage)
        }
        return deferred.await()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = purchaseDeferred
        when (result.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                deferred?.complete(PurchaseResult.Cancelled)
                purchaseDeferred = null
            }
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    deferred?.complete(PurchaseResult.Pending)
                    purchaseDeferred = null
                } else {
                    scope.launch {
                        val first = purchases.first()
                        val handled = handlePurchases(purchases)
                        deferred?.complete(
                            if (handled) {
                                PurchaseResult.Success(
                                    productId = first.products.firstOrNull().orEmpty(),
                                    purchaseToken = first.purchaseToken
                                )
                            } else {
                                PurchaseResult.Pending
                            }
                        )
                        purchaseDeferred = null
                    }
                }
            }
            else -> {
                deferred?.complete(PurchaseResult.Failed(result.debugMessage))
                purchaseDeferred = null
            }
        }
    }

    override suspend fun restorePurchases(): List<String> {
        if (!enabled()) return emptyList()
        ensureClient()
        return queryOwnedAndSync()
    }

    override suspend fun processPending() {
        if (!enabled()) return
        for (item in pendingQueue.all()) {
            val validation = receiptValidator.validate(item.productId, item.purchaseToken, item.packageName)
            if (validation.ok) {
                purchaseGranter.grant(item.productId, item.purchaseToken)
                pendingQueue.remove(item.purchaseToken)
            }
        }
    }

    private suspend fun queryOwnedAndSync(): List<String> {
        val client = billingClient ?: return emptyList()
        val granted = mutableListOf<String>()
        for (type in listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS)) {
            val purchases = queryPurchases(client, type)
            if (handlePurchases(purchases)) {
                granted += purchases.flatMap { it.products }
            }
        }
        val subs = queryPurchases(client, BillingClient.ProductType.SUBS)
        val premiumActive = subs.any {
            it.products.contains(ProductCatalog.PREMIUM) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (!premiumActive && entitlementStore.premium.value) {
            entitlementStore.grantPremium(active = false)
        }
        return granted.distinct()
    }

    private suspend fun queryPurchases(client: BillingClient, type: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(type).build()
            ) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(list)
                } else {
                    cont.resume(emptyList())
                }
            }
        }

    private suspend fun handlePurchases(purchases: List<Purchase>): Boolean {
        var any = false
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            val productId = purchase.products.firstOrNull() ?: continue
            pendingQueue.enqueue(
                PendingPurchase(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    packageName = context.packageName
                )
            )
            val validation = receiptValidator.validate(
                productId,
                purchase.purchaseToken,
                context.packageName
            )
            if (!validation.ok) continue
            val granted = purchaseGranter.grant(productId, purchase.purchaseToken)
            if (granted) {
                acknowledge(purchase)
                pendingQueue.remove(purchase.purchaseToken)
                analyticsTracker.logEvent(
                    AnalyticsEvents.PURCHASE_COMPLETED,
                    mapOf("product" to productId)
                )
                any = true
            }
        }
        return any
    }

    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        suspendCancellableCoroutine { cont ->
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "ack failed ${result.debugMessage}")
                }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private companion object {
        const val TAG = "PlayBilling"
    }
}
