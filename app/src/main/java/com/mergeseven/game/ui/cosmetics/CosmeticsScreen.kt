package com.mergeseven.game.ui.cosmetics

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.meta.CosmeticKind
import com.mergeseven.game.ui.theme.GameColors
import com.mergeseven.game.ui.theme.TileThemes

@Composable
fun CosmeticsScreen(
    viewModel: CosmeticsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val previewBitmap = remember {
        runCatching {
            context.assets.open("art/cosmetic_preview_sheet.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("BACK", color = GameColors.TextWhite)
            }
            Text(
                "COSMETICS",
                color = GameColors.TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text("${ui.coins} coins", color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
        }

        previewBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Cosmetic preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(vertical = 8.dp),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            "Preview skins below — equip owned themes anytime.",
            color = GameColors.TextWhite.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val previewThemeId = ui.rows.firstOrNull { it.equipped }?.def?.id ?: "classic"
            val theme = TileThemes.of(previewThemeId)
            listOf(2, 4, 8, 16).forEach { value ->
                Canvas(modifier = Modifier.size(36.dp)) {
                    drawCircle(theme.tileColor(value))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.selectTab(CosmeticKind.TILE) }) {
                Text(
                    "TILES",
                    color = if (ui.tab == CosmeticKind.TILE) GameColors.CoinGold else GameColors.TextWhite
                )
            }
            OutlinedButton(onClick = { viewModel.selectTab(CosmeticKind.BOARD) }) {
                Text(
                    "BOARDS",
                    color = if (ui.tab == CosmeticKind.BOARD) GameColors.CoinGold else GameColors.TextWhite
                )
            }
        }

        ui.message?.let { msg ->
            Text(
                msg,
                color = GameColors.CoinGold,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { viewModel.clearMessage() }
            )
        }

        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ui.rows) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (row.def.kind == CosmeticKind.TILE) {
                            val theme = TileThemes.of(row.def.id)
                            Canvas(modifier = Modifier.size(40.dp)) {
                                drawCircle(theme.tileColor(8))
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.def.title, color = GameColors.TextWhite, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    row.equipped -> "Equipped"
                                    row.owned -> "Owned"
                                    else -> "${row.def.coinCost} coins"
                                },
                                color = GameColors.TextWhite.copy(alpha = 0.7f)
                            )
                        }
                        when {
                            row.equipped -> Text("ON", color = GameColors.Success)
                            row.owned -> Button(
                                onClick = { viewModel.equip(row.def.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold)
                            ) {
                                Text("EQUIP", color = Color.Black)
                            }
                            else -> Button(
                                onClick = { viewModel.buy(row.def.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = GameColors.WoodLight)
                            ) {
                                Text("BUY", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
