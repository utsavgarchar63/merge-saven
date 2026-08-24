package com.mergeseven.game.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.ui.theme.GameColors

/**
 * Clean & Focused Home Screen.
 * Displays Title Logo, Coins & Stars stats, PLAY, LEVELS, and DAILY buttons.
 * (Shop button removed as requested).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlayClick: () -> Unit = {},
    onLevelsClick: () -> Unit = {},
    onDailyClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GameColors.WoodDark,
                        GameColors.WoodMid,
                        GameColors.WoodDark
                    )
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top User Stats Bar (Coins & Stars)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GameColors.WoodLight.copy(alpha = 0.3f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            text = "${userProfile.totalStars}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.TextWhite
                        )
                    }
                }

                // Coins Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GameColors.WoodLight.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙 ${userProfile.coins}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.CoinGold
                        )
                    }
                }
            }

            // Central Logo Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MERGE\nSEVEN",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 56.sp
                    ),
                    color = GameColors.CoinGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "HEXAGON MERGE PUZZLE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }

            // Main Action Buttons (PLAY, LEVELS, DAILY)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PLAY button (Primary Glowing Gold)
                HomeButton(
                    text = "PLAY NOW",
                    onClick = onPlayClick,
                    primary = true
                )

                // Secondary buttons
                HomeButton(text = "LEVELS MAP", onClick = onLevelsClick)
                HomeButton(text = "DAILY REWARDS", onClick = onDailyClick)

                Spacer(modifier = Modifier.height(4.dp))

                // Settings
                TextButton(onClick = onSettingsClick) {
                    Text(
                        text = "SETTINGS",
                        color = GameColors.TextWhite.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (primary) 58.dp else 48.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) GameColors.CoinGold
            else GameColors.WoodLight.copy(alpha = 0.35f),
            contentColor = if (primary) Color.Black
            else GameColors.TextWhite
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (primary) 20.sp else 16.sp
            )
        )
    }
}
