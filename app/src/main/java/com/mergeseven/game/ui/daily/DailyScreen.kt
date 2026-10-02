package com.mergeseven.game.ui.daily
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.R
import com.mergeseven.game.ads.AdPlacement
import com.mergeseven.game.ui.components.*
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun DailyScreen(viewModel: DailyViewModel = hiltViewModel(), onBackClick: () -> Unit = {},
    onStartDailyChallenge: () -> Unit = {}, onStartWeekly: () -> Unit = {}, onSettings: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val availability by viewModel.adAvailability.collectAsStateWithLifecycle()
    val busy by viewModel.adBusy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? android.app.Activity
    val giftColumns = if (LocalConfiguration.current.screenWidthDp / LocalDensity.current.fontScale >= 360) 2 else 1
    LaunchedEffect(Unit) { viewModel.refreshDailyCheck(); viewModel.warmAds() }
    WoodPage("Challenges", profile.coins, headerAction = { TextButton(onSettings) { Text("Settings") } }) {
        WoodPanel {
            Text("Today's puzzle", style = MaterialTheme.typography.headlineMedium)
            Text("${profile.dailyChallenge.dateSeed} · Reach ${profile.dailyChallenge.targetScore} points")
            Text(if (viewModel.extraClaimed() && profile.dailyChallenge.attempts == 0)
                "Bonus reward attempt ready. Your recorded score stays ${profile.dailyChallenge.bestScore}."
                else if (profile.dailyChallenge.attempts > 0 || profile.dailyChallenge.isCompleted)
                "Recorded score: ${profile.dailyChallenge.bestScore}. Further runs are practice."
                else "Your first run records today's score. Reach the goal to earn ${profile.dailyChallenge.coinsReward} coins.")
            GoldButton(if (profile.dailyChallenge.attempts > 0) "Practice today's puzzle" else "Play today's puzzle", !busy, onStartDailyChallenge)
            if (!viewModel.extraClaimed() && profile.dailyChallenge.attempts > 0 && !profile.dailyChallenge.isCompleted) {
                OutlinedButton({ activity?.let(viewModel::watchAdForExtraAttempt) }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = !busy && activity != null && AdPlacement.EXTRA_DAILY in availability) {
                    Text("Watch ad · Get one more try for ${profile.dailyChallenge.coinsReward} coins")
                }
            }
        }
        WoodLink("Weekly challenge", "A shared board. A new chance to beat your best.", R.drawable.mode_weekly_v3, onStartWeekly)
        status?.let { Text(it, color = GameColors.CoinGold) }
        Text("Your daily gifts", style = MaterialTheme.typography.titleLarge)
        Text("Claim available gifts free. Missing a day never blocks play.")
        viewModel.getDailyRewards().chunked(giftColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { gift ->
                    WoodPanel(Modifier.weight(1f)) {
                        Text("Day ${gift.day}", style = MaterialTheme.typography.titleMedium)
                        Text("${gift.coins} coins" + if (gift.stars > 0) " · ${gift.stars} stars" else "")
                        OutlinedButton({ viewModel.claimReward(gift) }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = gift.isAvailable && !busy) {
                            Text(if (gift.isClaimed) "Claimed" else if (gift.isAvailable) "Claim free" else "Coming up")
                        }
                    }
                }
                if (row.size < giftColumns) Spacer(Modifier.weight(1f))
            }
        }
        Text("Daily quests", style = MaterialTheme.typography.titleLarge)
        profile.dailyQuests.forEach { quest ->
            WoodPanel {
                Text(quest.title, style = MaterialTheme.typography.titleMedium)
                Text(quest.description)
                LinearProgressIndicator(progress = { (quest.currentProgress.toFloat() / quest.targetProgress.coerceAtLeast(1)).coerceIn(0f,1f) },
                    modifier = Modifier.fillMaxWidth(), color = GameColors.CoinGold)
                Text("${quest.currentProgress}/${quest.targetProgress} · ${quest.coinsReward} coins")
                OutlinedButton({ viewModel.claimQuest(quest.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = quest.isCompleted && !quest.isClaimed && !busy) { Text(if (quest.isClaimed) "Claimed" else "Claim reward") }
            }
        }
    }
}
