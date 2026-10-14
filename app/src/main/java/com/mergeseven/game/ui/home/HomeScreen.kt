package com.mergeseven.game.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.layout.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.mergeseven.game.R

import com.mergeseven.game.ads.BannerAdHost

import com.mergeseven.game.game.modes.ModeIds

import com.mergeseven.game.ui.components.*

import com.mergeseven.game.ui.theme.GameColors

@Composable

fun HomeScreen(viewModel: HomeViewModel = hiltViewModel(), onPlayClick: () -> Unit = {},

    onContinueClick: (Int) -> Unit = {}, onContinueModeClick: (String, Int) -> Unit = { _, l -> onContinueClick(l) },

    onModeClick: (String) -> Unit = {}, onLevelsClick: () -> Unit = {}, onDailyClick: () -> Unit = {},

    onSettingsClick: () -> Unit = {}, onShopClick: () -> Unit = {}, onAchievementsClick: () -> Unit = {},

    onCosmeticsClick: () -> Unit = {}, onStatsClick: () -> Unit = {}) {

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    val resumable by viewModel.resumableGame.collectAsStateWithLifecycle()

    val level by viewModel.nextCampaignLevel.collectAsStateWithLifecycle(initialValue = 1)

    val cards by viewModel.modeCards.collectAsStateWithLifecycle()
    val shortWindow = LocalConfiguration.current.screenHeightDp < 400

    LaunchedEffect(Unit) { viewModel.refreshResumable() }

    WoodPage("Merge Seven", profile.coins, headerAction = {

        IconButton(onSettingsClick) { Icon(painterResource(R.drawable.icon_settings), "Settings", tint = GameColors.TextWhite) }

    }, footer = { BannerAdHost() }) {

        val banner: @Composable () -> Unit = {
        Surface(shape = RoundedCornerShape(20.dp)) {
            Image(
                painter = painterResource(R.drawable.home_brand_banner_v4),
                contentDescription = "Merge Seven. Place. Match. Merge.",
                modifier = Modifier.fillMaxWidth().aspectRatio(1024f / 500f),
                contentScale = ContentScale.Fit
            )
        }

        }
        val playCard: @Composable () -> Unit = {
        WoodPanel(compact = shortWindow) {

            Text(if (resumable != null) "Your board is waiting" else "Ready for your next merge?", style = MaterialTheme.typography.titleLarge)

            Text(resumable?.let {
                val title = cards.firstOrNull { c -> c.modeId == it.modeId }?.title ?: "Campaign"
                val stage = if (it.modeId == ModeIds.CAMPAIGN) " · Level ${it.level}" else ""
                "$title$stage · Score ${it.score}"
            }

                ?: "Campaign · Level $level", color = GameColors.TextWhite.copy(alpha = 0.8f))

            GoldButton(if (resumable != null) "Resume game" else "Play level $level") {

                resumable?.let { onContinueModeClick(it.modeId, it.level) } ?: onContinueClick(level)

            }

            TextButton(onLevelsClick, Modifier.fillMaxWidth()) { Text("Choose a campaign level", color = GameColors.CoinGold) }

        }

        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth / LocalDensity.current.fontScale >= (if (shortWindow) 380.dp else 600.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { banner() }
                    Box(Modifier.weight(1f)) { playCard() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { banner(); playCard() }
            }
        }

        WoodLink("Today's puzzle", "A fresh challenge with the same board for everyone", R.drawable.mode_daily_v3, onDailyClick)

        val descriptions = listOf("Follow the path, one satisfying merge at a time", "Build your best score without a timer",

            "Make every second count", "Relax, experiment, and undo freely", "A new puzzle every day", "Explore this week's personal challenge")

        val art = listOf(R.drawable.mode_campaign_v3, R.drawable.mode_endless_v3, R.drawable.mode_time_v3,

            R.drawable.mode_zen_v3, R.drawable.mode_daily_v3, R.drawable.mode_weekly_v3)

        ResponsiveCards(cards, minCardWidth = 220.dp, itemKey = { it.modeId }) { card ->
            val i = cards.indexOf(card)

            WoodLink(card.title, "${descriptions[i]} · Best ${card.bestScore}", art[i]) { onModeClick(card.modeId) }

        }

        Text("No purchases. Play at your pace; ad rewards are always optional.", style = MaterialTheme.typography.bodySmall,

            color = GameColors.TextWhite.copy(alpha = 0.8f))

    }

}

