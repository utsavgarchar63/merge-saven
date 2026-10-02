package com.mergeseven.game.ui.levels

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mergeseven.game.ui.components.CoinIcon
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.components.StarIcon
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
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()
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
                columns = GridCells.Adaptive(96.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.6f)),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                uiState.levels.chunked(10).forEachIndexed { chapter, levels ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text("Chapter ${chapter + 1}", style = MaterialTheme.typography.titleLarge)
                            Text("Levels ${levels.first().rule.level}–${levels.last().rule.level}", color = GameColors.CoinGold)
                        }
                    }
                    items(levels, key = { it.rule.level }) { levelItem ->
                        LevelNodeCard(levelItem, onClick = { viewModel.onSelectLevel(levelItem) })
                    }
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
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val compactHeader = maxWidth / LocalDensity.current.fontScale < 320.dp
        val balances: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                StarIcon(size = 18.dp)
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
                CoinIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$coins",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.CoinGold
                )
            }
        }

            }
        }
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick,
                    modifier = Modifier.size(48.dp).background(GameColors.WoodMid, CircleShape)) {
                    GameIcon(resId = GameIcons.Back, contentDescription = "Back",
                        tint = GameColors.TextWhite, size = 24.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text("LEVELS", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite)
                if (!compactHeader) balances()
            }
            if (compactHeader) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                    balances()
                }
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
                        StarIcon(
                            tint = if (isStarred) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.2f),
                            size = 14.dp
                        )
                    }
                }
            } else {
                GameIcon(
                    resId = GameIcons.Lock,
                    contentDescription = "Locked",
                    tint = GameColors.TextWhite.copy(alpha = 0.3f),
                    size = 28.dp
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * .8f).dp)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
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
                        StarIcon(
                            tint = if (isStarred) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.3f),
                            size = 24.dp
                        )
                    }
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GameIcon(
                            resId = GameIcons.Play,
                            contentDescription = null,
                            tint = Color.Black,
                            size = 24.dp
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
