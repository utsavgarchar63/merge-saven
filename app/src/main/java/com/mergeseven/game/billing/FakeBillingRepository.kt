package com.mergeseven.game.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Controllable billing for JVM tests. */
class FakeBillingRepository(
    var purchaseResult: PurchaseResult = PurchaseResult.Success("coins_500", "token_1"),
    var products: List<ProductDetailsUi> = ProductCatalog.all.map {
        ProductDetailsUi(it.productId, it.title, "$0.99")
    },
    var restoreIds: List<String> = emptyList()
) : BillingRepository {
    var startCalls: Int = 0
    var purchaseCalls: Int = 0
    var pendingCalls: Int = 0
    private val owned = MutableStateFlow<Set<String>>(emptySet())
    val ownedProducts: StateFlow<Set<String>> = owned.asStateFlow()

    override suspend fun start() {
        startCalls++
    }

    override suspend fun queryProducts(): List<ProductDetailsUi> = products

    override suspend fun purchase(activity: android.app.Activity, productId: String): PurchaseResult {
        purchaseCalls++
        val result = purchaseResult
        if (result is PurchaseResult.Success) {
            owned.value = owned.value + productId
        }
        return result
    }

    override suspend fun restorePurchases(): List<String> {
        owned.value = owned.value + restoreIds
        return restoreIds
    }

    override suspend fun processPending() {
        pendingCalls++
    }
}
