package com.mergeseven.game.ui.profile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.R
import com.mergeseven.game.ui.home.HomeViewModel
import com.mergeseven.game.ui.components.*
@Composable
fun ProfileScreen(viewModel: HomeViewModel = hiltViewModel(), onAchievements: () -> Unit,
    onStats: () -> Unit, onCosmetics: () -> Unit, onLeaderboards: () -> Unit, onSettings: () -> Unit) {
    val p by viewModel.userProfile.collectAsStateWithLifecycle()
    WoodPage("Your collection", p.coins, headerAction = { TextButton(onSettings) { Text("Settings") } }) {
        WoodPanel {
            Text("Level ${p.playerLevel}", style = MaterialTheme.typography.headlineMedium)
            Text("${p.xp} XP · ${p.totalMerges} merges · Best tile ${p.biggestTile}")
        }
        WoodLink("Achievements", "Celebrate your puzzle milestones", R.drawable.art_achievement_v2, onAchievements)
        WoodLink("Statistics", "Your scores, chains, and time well spent", onClick = onStats)
        WoodLink("Cosmetics", "Make the table your own with coins and play", R.drawable.art_celebration_v2, onCosmetics)
        WoodLink("Leaderboards", "Compare your best runs", R.drawable.mode_weekly_v2, onLeaderboards)
    }
}
