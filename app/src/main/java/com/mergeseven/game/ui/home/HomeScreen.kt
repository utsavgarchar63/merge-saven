package com.mergeseven.game.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mergeseven.game.ui.theme.GameColors

/**
 * Home screen layout.
 * See Master Plan Section 27.
 *
 * Structure:
 * Logo
 * Best Score
 * Coins
 * [ PLAY ]
 * [ LEVELS ]
 * [ DAILY ]
 * [ SHOP ]
 * Settings
 */
@Composable
fun HomeScreen(
    onPlayClick: () -> Unit = {},
    onLevelsClick: () -> Unit = {},
    onDailyClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / Title
            Text(
                text = "MERGE\nSEVEN",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 54.sp
                ),
                color = GameColors.CoinGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Best Score
            Text(
                text = "Best Score: 0",
                style = MaterialTheme.typography.titleMedium,
                color = GameColors.TextWhite.copy(alpha = 0.8f)
            )

            // Coins
            Text(
                text = "🪙 100",
                style = MaterialTheme.typography.titleMedium,
                color = GameColors.CoinGold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PLAY button
            HomeButton(
                text = "PLAY",
                onClick = onPlayClick,
                primary = true
            )

            // Secondary buttons
            HomeButton(text = "LEVELS", onClick = onLevelsClick)
            HomeButton(text = "DAILY", onClick = onDailyClick)
            HomeButton(text = "SHOP", onClick = onShopClick)

            Spacer(modifier = Modifier.height(8.dp))

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
            .height(if (primary) 60.dp else 48.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) GameColors.TileGold
            else GameColors.WoodLight.copy(alpha = 0.3f),
            contentColor = if (primary) GameColors.TextDark
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
