package com.mergeseven.game.ui.cosmetics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.meta.CosmeticCatalog
import com.mergeseven.game.meta.CosmeticKind
import com.mergeseven.game.ui.theme.BoardTheme
import com.mergeseven.game.ui.theme.BoardThemes
import com.mergeseven.game.ui.theme.GameColors
import com.mergeseven.game.ui.theme.TileTheme
import com.mergeseven.game.ui.theme.TileThemes

@Composable
fun CosmeticsScreen(
    viewModel: CosmeticsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val previewDef = CosmeticCatalog.byId(ui.previewId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("BACK", color = GameColors.TextWhite, fontWeight = FontWeight.Bold)
            }
            Text(
                "COSMETICS",
                color = GameColors.TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = GameColors.WoodMid,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GameColors.CoinGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙 ", fontSize = 14.sp)
                    Text(
                        "${ui.coins}",
                        color = GameColors.CoinGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Skin Preview Banner
        CosmeticPreviewBanner(
            tab = ui.tab,
            previewId = ui.previewId,
            previewDef = previewDef,
            equippedTileId = ui.equippedTileThemeId,
            equippedBoardId = ui.equippedBoardThemeId
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Toggle Tabs (TILES / BOARDS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.selectTab(CosmeticKind.TILE) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (ui.tab == CosmeticKind.TILE) GameColors.CoinGold else GameColors.WoodLight
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (ui.tab == CosmeticKind.TILE) GameColors.WoodMid else Color.Transparent
                )
            ) {
                Text(
                    "TILES",
                    fontWeight = FontWeight.Bold,
                    color = if (ui.tab == CosmeticKind.TILE) GameColors.CoinGold else GameColors.TextWhite
                )
            }
            OutlinedButton(
                onClick = { viewModel.selectTab(CosmeticKind.BOARD) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (ui.tab == CosmeticKind.BOARD) GameColors.CoinGold else GameColors.WoodLight
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (ui.tab == CosmeticKind.BOARD) GameColors.WoodMid else Color.Transparent
                )
            ) {
                Text(
                    "BOARDS",
                    fontWeight = FontWeight.Bold,
                    color = if (ui.tab == CosmeticKind.BOARD) GameColors.CoinGold else GameColors.TextWhite
                )
            }
        }

        // Toast Message Notification
        ui.message?.let { msg ->
            Surface(
                color = GameColors.CoinGold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { viewModel.clearMessage() }
            ) {
                Text(
                    msg,
                    color = GameColors.CoinGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Catalog Item List
        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ui.rows) { row ->
                val isSelected = row.def.id == ui.previewId
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) GameColors.WoodMid.copy(alpha = 0.95f) else GameColors.WoodMid
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = if (isSelected) BorderStroke(2.dp, GameColors.CoinGold) else BorderStroke(1.dp, GameColors.WoodLight.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPreview(row.def.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Swatch Thumbnail
                        if (row.def.kind == CosmeticKind.TILE) {
                            TilePaletteSwatch(theme = TileThemes.of(row.def.id))
                        } else {
                            BoardPaletteSwatch(theme = BoardThemes.of(row.def.id))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Info Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    row.def.title,
                                    color = GameColors.TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "PREVIEWING",
                                        color = GameColors.CoinGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                when {
                                    row.equipped -> "Equipped"
                                    row.owned -> "Owned"
                                    row.def.unlockAtLevel > 0 -> "${row.def.coinCost} coins (or Lvl ${row.def.unlockAtLevel})"
                                    else -> "${row.def.coinCost} coins"
                                },
                                color = if (row.equipped) GameColors.Success else GameColors.TextWhite.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }

                        // Action Buttons
                        when {
                            row.equipped -> {
                                Surface(
                                    color = GameColors.Success.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GameColors.Success)
                                ) {
                                    Text(
                                        "EQUIPPED ✓",
                                        color = GameColors.Success,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            row.owned -> {
                                Button(
                                    onClick = { viewModel.equip(row.def.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("EQUIP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { viewModel.buy(row.def.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.WoodLight),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("BUY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top Dynamic Preview Card showing real styled tiles or board preview for the selected skin.
 */
@Composable
private fun CosmeticPreviewBanner(
    tab: CosmeticKind,
    previewId: String,
    previewDef: com.mergeseven.game.meta.CosmeticDef?,
    equippedTileId: String,
    equippedBoardId: String
) {
    val isEquipped = if (tab == CosmeticKind.TILE) previewId == equippedTileId else previewId == equippedBoardId
    val title = previewDef?.title ?: "Skin Preview"

    Card(
        colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, GameColors.WoodLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = GameColors.CoinGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (isEquipped) {
                    Surface(
                        color = GameColors.Success.copy(alpha = 0.25f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, GameColors.Success)
                    ) {
                        Text(
                            "ACTIVE SKIN",
                            color = GameColors.Success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Text(
                        "TAP TO PREVIEW",
                        color = GameColors.TextWhite.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tab == CosmeticKind.TILE) {
                val theme = TileThemes.of(previewId)
                // Render sample tiles row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(2, 4, 8, 16, 32).forEach { valNum ->
                        TileChipPreview(theme = theme, value = valNum)
                    }
                }
            } else {
                val theme = BoardThemes.of(previewId)
                // Render mini board preview
                BoardGridPreview(theme = theme)
            }
        }
    }
}

/**
 * Styled mini tile chip for preview header.
 */
@Composable
private fun TileChipPreview(theme: TileTheme, value: Int) {
    val bgColor = theme.tileColor(value)
    val txtColor = theme.tileTextColor(value)

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.5.dp, Color.White.copy(alpha = theme.strokeAlpha), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$value",
            color = txtColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

/**
 * Styled mini board grid preview for board cosmetics.
 */
@Composable
private fun BoardGridPreview(theme: BoardTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.overlayColor.compositeOverWood())
            .border(1.dp, GameColors.WoodLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty Cell
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.cellEmpty)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("EMPTY", color = GameColors.TextWhite.copy(alpha = 0.5f), fontSize = 8.sp)
            }

            // Highlighted Cell
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.cellHighlight)
                    .border(1.5.dp, GameColors.CoinGold, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("CELL", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }

            // Cell with Sample Tile
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.cellEmpty),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GameColors.TileRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text("8", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Hint Cell
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.cellHint)
                    .border(1.dp, Color.Cyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("HINT", color = Color.White, fontSize = 8.sp)
            }
        }
    }
}

/**
 * Multi-color palette swatch for Tile items in the catalog list.
 */
@Composable
private fun TilePaletteSwatch(theme: TileTheme) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(GameColors.WoodDark)
            .padding(3.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.tileColor(2))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.tileColor(4))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.tileColor(8))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.tileColor(16))
                )
            }
        }
    }
}

/**
 * Mini board preview swatch for Board items in the catalog list.
 */
@Composable
private fun BoardPaletteSwatch(theme: BoardTheme) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(theme.overlayColor.compositeOverWood())
            .border(1.dp, GameColors.WoodLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(theme.cellEmpty)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(theme.cellHighlight)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(theme.cellHint)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(15.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(theme.cellEmpty)
                )
            }
        }
    }
}

/**
 * Helper to blend board overlay color on wood dark background for crisp previews.
 */
private fun Color.compositeOverWood(): Color {
    val bg = GameColors.WoodDark
    val alpha = this.alpha
    val invAlpha = 1f - alpha
    return Color(
        red = this.red * alpha + bg.red * invAlpha,
        green = this.green * alpha + bg.green * invAlpha,
        blue = this.blue * alpha + bg.blue * invAlpha,
        alpha = 1f
    )
}
