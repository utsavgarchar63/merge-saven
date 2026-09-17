package com.mergeseven.game.billing

import com.mergeseven.game.game.model.BoosterType

enum class ProductKind {
    CONSUMABLE_COINS,
    CONSUMABLE_BOOSTERS,
    NON_CONSUMABLE_REMOVE_ADS,
    SUBSCRIPTION_PREMIUM
}

data class CatalogProduct(
    val productId: String,
    val title: String,
    val subtitle: String,
    val kind: ProductKind,
    val coins: Int = 0,
    val boosters: Map<BoosterType, Int> = emptyMap()
)

object ProductCatalog {
    const val COINS_500 = "coins_500"
    const val COINS_2000 = "coins_2000"
    const val PACK_UNDO = "pack_undo"
    const val PACK_MIXED = "pack_mixed"
    const val REMOVE_ADS = "remove_ads"
    const val PREMIUM = "merge_seven_premium"

    val all: List<CatalogProduct> = listOf(
        CatalogProduct(COINS_500, "500 Coins", "Top up your wallet", ProductKind.CONSUMABLE_COINS, coins = 500),
        CatalogProduct(COINS_2000, "2000 Coins", "Best value pack", ProductKind.CONSUMABLE_COINS, coins = 2000),
        CatalogProduct(
            PACK_UNDO,
            "Undo Pack ×5",
            "Stock up on undos",
            ProductKind.CONSUMABLE_BOOSTERS,
            boosters = mapOf(BoosterType.UNDO to 5)
        ),
        CatalogProduct(
            PACK_MIXED,
            "Booster Bundle",
            "Hammer + Value Up + Magnet",
            ProductKind.CONSUMABLE_BOOSTERS,
            boosters = mapOf(
                BoosterType.HAMMER to 2,
                BoosterType.VALUE_UP to 2,
                BoosterType.MAGNET to 1
            )
        ),
        CatalogProduct(REMOVE_ADS, "Remove Ads", "No interstitial or banner", ProductKind.NON_CONSUMABLE_REMOVE_ADS),
        CatalogProduct(
            PREMIUM,
            "Merge Seven Premium",
            "Ad-free + daily coins + exclusive skin",
            ProductKind.SUBSCRIPTION_PREMIUM
        )
    )

    fun byId(id: String): CatalogProduct? = all.firstOrNull { it.productId == id }

    fun inAppProductIds(): List<String> =
        all.filter { it.kind != ProductKind.SUBSCRIPTION_PREMIUM }.map { it.productId }

    fun subscriptionIds(): List<String> = listOf(PREMIUM)
}
