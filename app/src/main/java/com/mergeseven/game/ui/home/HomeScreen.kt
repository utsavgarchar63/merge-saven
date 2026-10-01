package com.mergeseven.game.ui.home



import androidx.compose.foundation.Image

import androidx.compose.foundation.layout.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

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

    onCosmeticsClick: () -> Unit = {}, onStatsClick: () -> Unit = {}, onLeaderboardsClick: () -> Unit = {},

    onTournamentPlay: (Long) -> Unit = {}) {

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    val resumable by viewModel.resumableGame.collectAsStateWithLifecycle()

    val level by viewModel.nextCampaignLevel.collectAsStateWithLifecycle(initialValue = 1)

    val cards by viewModel.modeCards.collectAsStateWithLifecycle()

    var moreModes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshResumable(); viewModel.refreshTournament() }

    WoodPage("Merge Seven", profile.coins, headerAction = {

        IconButton(onSettingsClick) { Icon(painterResource(R.drawable.icon_settings), "Settings", tint = GameColors.TextWhite) }

    }, footer = { BannerAdHost() }) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(20.dp)) {

            Image(painterResource(R.drawable.logo_mark_v2), null, Modifier.size(92.dp))

            Column(Modifier.weight(1f)) {

                Text("A little puzzle. A big chain.", style = MaterialTheme.typography.headlineMedium)

                Text("Place · Match · Merge", color = GameColors.CoinGold)

            }

        }

        WoodPanel {

            Text(if (resumable != null) "Your board is waiting" else "Ready for your next merge?", style = MaterialTheme.typography.titleLarge)

            Text(resumable?.let { "${cards.firstOrNull { c -> c.modeId == it.modeId }?.title ?: "Campaign"} · Level ${it.level} · Score ${it.score}" }

                ?: "Campaign · Level $level", color = GameColors.TextWhite.copy(alpha = 0.8f))

            GoldButton(if (resumable != null) "Resume game" else "Play level $level") {

                resumable?.let { onContinueModeClick(it.modeId, it.level) } ?: onContinueClick(level)

            }

            TextButton(onLevelsClick, Modifier.fillMaxWidth()) { Text("Choose a campaign level", color = GameColors.CoinGold) }

        }

        WoodLink("Today's puzzle", "A fresh challenge with the same board for everyone", R.drawable.mode_daily_v2, onDailyClick)

        OutlinedButton({ moreModes = !moreModes }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {

            Text(if (moreModes) "Hide game modes" else "Explore all six modes")

        }

        if (moreModes) {

            val descriptions = listOf("Follow the path, one satisfying merge at a time", "Build your best score without a timer",

                "Make every second count", "Relax, experiment, and undo freely", "A new puzzle every day", "Compete in this week's challenge")

            val art = listOf(R.drawable.mode_campaign_v2, R.drawable.mode_endless_v2, R.drawable.mode_time_attack_v2,

                R.drawable.mode_zen_v2, R.drawable.mode_daily_v2, R.drawable.mode_weekly_v2)

            cards.forEachIndexed { i, card ->

                WoodLink(card.title, "${descriptions[i]} · Best ${card.bestScore}", art[i]) { onModeClick(card.modeId) }

            }

        }

        Text("No purchases. Play at your pace; ad rewards are always optional.", style = MaterialTheme.typography.bodySmall,

            color = GameColors.TextWhite.copy(alpha = 0.8f))

    }

}

