package com.mergeseven.game.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.data.model.DailyChallengeState
import com.mergeseven.game.data.model.DailyQuest
import com.mergeseven.game.ui.theme.GameColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    viewModel: DailyViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onStartDailyChallenge: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val rewards = viewModel.getDailyRewards()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // ─── Top Bar ─────────────────────────────────
            DailyTopBar(
                totalStars = userProfile.totalStars,
                coins = userProfile.coins,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Daily Challenge Banner ─────────────
                DailyChallengeCard(
                    challengeState = userProfile.dailyChallenge,
                    onStartChallenge = onStartDailyChallenge
                )

                // ─── 7-Day Login Streak Section ─────────
                Text(
                    text = "7-DAY LOGIN STREAK REWARDS",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.CoinGold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                DailyStreakGrid(
                    rewards = rewards,
                    onClaim = { item -> viewModel.claimReward(item) }
                )

                // ─── Daily Quests Section ────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DAILY QUESTS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.CoinGold
                        )

                        userProfile.dailyQuests.forEach { quest ->
                            QuestRow(
                                quest = quest,
                                onClaim = { viewModel.claimQuest(quest.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTopBar(
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
            text = "DAILY",
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
private fun DailyChallengeCard(
    challengeState: DailyChallengeState,
    onStartChallenge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GameColors.WoodLight.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GameColors.WoodMid,
                            GameColors.TileGold
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(2.dp, GameColors.CoinGold, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = challengeState.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.CoinGold
                        )
                        if (challengeState.dateSeed.isNotEmpty()) {
                            Text(
                                text = "Seed: ${challengeState.dateSeed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GameColors.TextWhite.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (challengeState.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GameColors.CoinGold
                        ) {
                            Text(
                                text = "COMPLETED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Goal: Reach ${challengeState.targetScore} pts • Best: ${challengeState.bestScore}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GameColors.TextWhite
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bonus: 🪙 +${challengeState.coinsReward} • ⭐ +${challengeState.starsReward}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GameColors.CoinGold
                    )

                    Button(
                        onClick = onStartChallenge,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (challengeState.isCompleted) "PLAY AGAIN" else "PLAY",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStreakGrid(
    rewards: List<DailyRewardItem>,
    onClaim: (DailyRewardItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Rows of 3 items
        rewards.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        DailyRewardCard(
                            item = item,
                            onClaim = { onClaim(item) }
                        )
                    }
                }
                // Fill space if row has less than 3 items
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DailyRewardCard(
    item: DailyRewardItem,
    onClaim: () -> Unit
) {
    val isClaimed = item.isClaimed
    val isAvailable = item.isAvailable
    val isJackpot = item.day == 7

    val bgColor = when {
        isClaimed -> GameColors.WoodDark.copy(alpha = 0.6f)
        isAvailable -> GameColors.WoodLight.copy(alpha = 0.5f)
        else -> GameColors.WoodDark.copy(alpha = 0.4f)
    }

    val borderColor = when {
        isJackpot && isAvailable -> GameColors.CoinGold
        isAvailable -> GameColors.CoinGold
        isClaimed -> GameColors.WoodLight.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .background(bgColor, shape = RoundedCornerShape(16.dp))
            .border(if (isAvailable) 2.dp else 0.dp, borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(enabled = isAvailable, onClick = onClaim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = if (isJackpot) "DAY 7 🏆" else "DAY ${item.day}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isAvailable) GameColors.CoinGold else GameColors.TextWhite.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "🪙 +${item.coins}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = GameColors.CoinGold,
                fontSize = 12.sp
            )

            if (item.stars > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GameColors.CoinGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "+${item.stars}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GameColors.TextWhite,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                isClaimed -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Claimed",
                        tint = GameColors.CoinGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                isAvailable -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GameColors.CoinGold
                    ) {
                        Text(
                            text = "CLAIM",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = GameColors.TextWhite.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestRow(
    quest: DailyQuest,
    onClaim: () -> Unit
) {
    val isCompleted = quest.isCompleted
    val isClaimed = quest.isClaimed
    val canClaim = isCompleted && !isClaimed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GameColors.WoodDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = quest.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = GameColors.TextWhite
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (quest.currentProgress.toFloat() / quest.targetProgress).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = GameColors.CoinGold,
                    trackColor = GameColors.WoodLight.copy(alpha = 0.3f)
                )
                Text(
                    text = "${quest.currentProgress}/${quest.targetProgress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GameColors.TextWhite.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            Text(
                text = "Reward: 🪙 +${quest.coinsReward}" + if (quest.starsReward > 0) " • ⭐ +${quest.starsReward}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = GameColors.CoinGold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        when {
            isClaimed -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GameColors.WoodLight.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Claimed",
                            tint = GameColors.CoinGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "CLAIMED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.TextWhite.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            canClaim -> {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CLAIM",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        fontSize = 11.sp
                    )
                }
            }
            else -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GameColors.WoodLight.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GameColors.TextWhite.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

