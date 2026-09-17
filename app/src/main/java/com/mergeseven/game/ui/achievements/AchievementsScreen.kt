package com.mergeseven.game.ui.achievements

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

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
                "ACHIEVEMENTS",
                color = GameColors.TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (ui.toastTitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.dismissToast() }) {
                        Text("OK")
                    }
                }
            ) {
                Text("Unlocked: ${ui.toastTitle}")
            }
        }

        LazyColumn(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("PROGRESS", color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
            }
            items(ui.achievements) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(row.def.title, color = GameColors.TextWhite, fontWeight = FontWeight.Bold)
                        Text(row.def.description, color = GameColors.TextWhite.copy(alpha = 0.75f))
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (row.def.target <= 0) 1f
                                else row.progress.toFloat() / row.def.target
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = GameColors.CoinGold,
                            trackColor = GameColors.WoodLight.copy(alpha = 0.3f)
                        )
                        Text(
                            "${row.progress}/${row.def.target}" +
                                if (row.isComplete) "  DONE" else "",
                            color = if (row.isComplete) GameColors.Success else GameColors.TextWhite
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("MILESTONES", color = GameColors.CoinGold, fontWeight = FontWeight.Bold)
            }
            items(ui.milestones) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(row.def.title, color = GameColors.TextWhite, fontWeight = FontWeight.Bold)
                        Text(row.def.description, color = GameColors.TextWhite.copy(alpha = 0.75f))
                        Text(
                            "+${row.def.coinsReward} coins",
                            color = GameColors.CoinGold
                        )
                        when {
                            row.claimed -> Text("Claimed", color = GameColors.Success)
                            row.eligible -> Button(
                                onClick = { viewModel.claimMilestone(row.def.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GameColors.CoinGold
                                )
                            ) {
                                Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            else -> Text("Locked", color = GameColors.TextWhite.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            ui.seasonal?.let { event ->
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "EVENT · ${event.config.id.uppercase()}  PB ${event.personalBest}",
                        color = GameColors.CoinGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(ui.seasonalRewards) { row ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Score ${row.step.threshold}",
                                color = GameColors.TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "+${row.step.coins} coins",
                                color = GameColors.CoinGold
                            )
                            when {
                                row.claimed -> Text("Claimed", color = GameColors.Success)
                                row.eligible -> Button(
                                    onClick = { viewModel.claimSeasonal(row.step.threshold) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GameColors.CoinGold
                                    )
                                ) {
                                    Text("CLAIM", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                else -> Text("Locked", color = GameColors.TextWhite.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
