package com.mergeseven.game.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.mergeseven.game.competitive.LeaderboardId
import com.mergeseven.game.competitive.LeaderboardScope
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) {
                    GameIcon(
                        resId = GameIcons.Back,
                        contentDescription = "Back",
                        tint = GameColors.TextWhite,
                        size = 22.dp
                    )
                }
                Text(
                    text = "LEADERBOARDS",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!ui.enabled) {
                Text(
                    text = "Competitive features are off. Enable AF8 in the debug menu.",
                    color = GameColors.TextWhite.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeaderboardId.entries.forEach { board ->
                        FilterChip(
                            selected = ui.board == board,
                            onClick = { viewModel.selectBoard(board) },
                            label = { Text(board.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GameColors.CoinGold,
                                selectedLabelColor = Color.Black,
                                containerColor = GameColors.WoodMid,
                                labelColor = GameColors.TextWhite
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeaderboardScope.entries.forEach { scope ->
                        FilterChip(
                            selected = ui.scope == scope,
                            onClick = { viewModel.selectScope(scope) },
                            label = { Text(scope.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GameColors.CoinGold,
                                selectedLabelColor = Color.Black,
                                containerColor = GameColors.WoodMid,
                                labelColor = GameColors.TextWhite
                            )
                        )
                    }
                }

                when {
                    ui.loading && ui.entries.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GameColors.CoinGold)
                        }
                    }
                    ui.entries.isEmpty() -> {
                        Text(
                            text = if (ui.offlineSoft) {
                                "No scores yet. Play Endless, Daily, or Weekly — offline cache is empty."
                            } else {
                                "No scores for this board."
                            },
                            color = GameColors.TextWhite.copy(alpha = 0.65f),
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ui.entries, key = { "${it.rank}-${it.playerId}-${it.score}" }) { entry ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (entry.isLocalPlayer) {
                                        GameColors.CoinGold.copy(alpha = 0.25f)
                                    } else {
                                        GameColors.WoodMid.copy(alpha = 0.85f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = GameColors.WoodDark,
                                            modifier = Modifier.padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = "#${entry.rank}",
                                                color = GameColors.CoinGold,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 6.dp
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.displayName,
                                                color = GameColors.TextWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (entry.isLocalPlayer) {
                                                Text(
                                                    text = "You",
                                                    color = GameColors.CoinGold,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                        Text(
                                            text = entry.score.toString(),
                                            color = GameColors.CoinGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
