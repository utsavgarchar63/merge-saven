package com.mergeseven.game.ui.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.data.repository.LevelItem
import com.mergeseven.game.ui.theme.GameColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsScreen(
    viewModel: LevelsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onStartLevel: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ─── Top Bar ─────────────────────────────────
            LevelsTopBar(
                totalStars = uiState.totalStars,
                coins = uiState.coins,
                onBackClick = onBackClick
            )

            Text(
                text = "LEVEL ROADMAP",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GameColors.CoinGold,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // ─── Level Grid ──────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(uiState.levels) { levelItem ->
                    LevelNodeCard(
                        levelItem = levelItem,
                        onClick = { viewModel.onSelectLevel(levelItem) }
                    )
                }
            }
        }

        // ─── Level Detail Dialog ────────────────────────
        val selectedLevel = uiState.selectedLevel
        if (selectedLevel != null) {
            LevelDetailModal(
                levelItem = selectedLevel,
                onDismiss = { viewModel.dismissDetailModal() },
                onPlay = {
                    viewModel.dismissDetailModal()
                    onStartLevel(selectedLevel.rule.level)
                }
            )
        }
    }
}

@Composable
private fun LevelsTopBar(
    totalStars: Int,
    coins: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .background(GameColors.WoodMid, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = GameColors.TextWhite
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "LEVELS",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = GameColors.TextWhite,
            modifier = Modifier.weight(1f)
        )

        // Stars Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GameColors.WoodMid.copy(alpha = 0.8f),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Stars",
                    tint = GameColors.CoinGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$totalStars",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )
            }
        }

        // Coins Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GameColors.WoodMid.copy(alpha = 0.8f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🪙 $coins",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.CoinGold
                )
            }
        }
    }
}

@Composable
private fun LevelNodeCard(
    levelItem: LevelItem,
    onClick: () -> Unit
) {
    val isUnlocked = levelItem.isUnlocked
    val isCompleted = levelItem.isCompleted

    val bgColor = when {
        !isUnlocked -> GameColors.WoodDark.copy(alpha = 0.5f)
        isCompleted -> GameColors.WoodMid.copy(alpha = 0.85f)
        else -> GameColors.WoodLight.copy(alpha = 0.4f)
    }

    val borderColor = when {
        !isUnlocked -> Color.Transparent
        isCompleted -> GameColors.CoinGold
        else -> GameColors.CoinGold.copy(alpha = 0.7f)
    }

    val borderWidth = if (isUnlocked) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(bgColor, shape = RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "${levelItem.rule.level}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )

                Text(
                    text = "Target: ${levelItem.rule.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GameColors.CoinGold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stars rating
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..3) {
                        val isStarred = i <= levelItem.stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isStarred) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.2f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = GameColors.TextWhite.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Level ${levelItem.rule.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GameColors.TextWhite.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun LevelDetailModal(
    levelItem: LevelItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = GameColors.CoinGold,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${levelItem.rule.level}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }

                Text(
                    text = "LEVEL ${levelItem.rule.level}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )

                Text(
                    text = "Target Goal: Merge up to ${levelItem.rule.target}",
                    style = MaterialTheme.typography.titleMedium,
                    color = GameColors.CoinGold
                )

                if (levelItem.bestScore > 0) {
                    Text(
                        text = "Best Score: ${levelItem.bestScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GameColors.TextWhite.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isStarred = i <= levelItem.stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isStarred) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START LEVEL",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
