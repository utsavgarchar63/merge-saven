package com.mergeseven.game.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val p = ui.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackClick) {
                Text("BACK", color = GameColors.TextWhite)
            }
            Text(
                "STATS",
                color = GameColors.TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                StatCard("Lifetime") {
                    Text("Merges: ${p.totalMerges}", color = GameColors.TextWhite)
                    Text("Biggest tile: ${p.biggestTile}", color = GameColors.TextWhite)
                    Text("Longest chain: ${p.longestChain}", color = GameColors.TextWhite)
                    Text(
                        "Playtime: ${p.playtimeMs / 60_000L} min",
                        color = GameColors.TextWhite
                    )
                    Text("Campaign stars: ${ui.campaignStars}", color = GameColors.CoinGold)
                }
            }
            items(ui.modeRows) { row ->
                StatCard(row.title) {
                    Text("Best: ${row.bestScore}", color = GameColors.TextWhite)
                    Text("Runs: ${row.runs}", color = GameColors.TextWhite.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
