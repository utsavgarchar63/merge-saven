package com.mergeseven.game.meta

data class MilestoneDef(
    val id: String,
    val title: String,
    val description: String,
    val targetMerges: Int? = null,
    val targetTile: Int? = null,
    val coinsReward: Int,
    val cosmeticRewardId: String? = null
)

object MilestoneCatalog {
    val all: List<MilestoneDef> = listOf(
        MilestoneDef(
            id = "merges_100",
            title = "Century",
            description = "Reach 100 lifetime merges",
            targetMerges = 100,
            coinsReward = 100
        ),
        MilestoneDef(
            id = "merges_1000",
            title = "Thousand Merges",
            description = "Reach 1,000 lifetime merges",
            targetMerges = 1_000,
            coinsReward = 500,
            cosmeticRewardId = "board_marble"
        ),
        MilestoneDef(
            id = "tile_512",
            title = "512 Club",
            description = "Create a 512 tile",
            targetTile = 512,
            coinsReward = 250,
            cosmeticRewardId = "tile_seasonal"
        )
    )

    fun unlockId(id: String): String = "milestone_$id"
}
