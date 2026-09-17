package com.mergeseven.game.billing

sealed class PurchaseResult {
    data class Success(val productId: String, val purchaseToken: String) : PurchaseResult()
    data object Cancelled : PurchaseResult()
    data object Pending : PurchaseResult()
    data class Failed(val message: String) : PurchaseResult()
    data object Unavailable : PurchaseResult()
}

data class ProductDetailsUi(
    val productId: String,
    val title: String,
    val priceLabel: String
)

interface BillingRepository {
    suspend fun start()
    suspend fun queryProducts(): List<ProductDetailsUi>
    suspend fun purchase(activity: android.app.Activity, productId: String): PurchaseResult
    suspend fun restorePurchases(): List<String>
    suspend fun processPending()
}

class NoOpBillingRepository : BillingRepository {
    override suspend fun start() = Unit
    override suspend fun queryProducts(): List<ProductDetailsUi> = emptyList()
    override suspend fun purchase(activity: android.app.Activity, productId: String): PurchaseResult =
        PurchaseResult.Unavailable

    override suspend fun restorePurchases(): List<String> = emptyList()
    override suspend fun processPending() = Unit
}
