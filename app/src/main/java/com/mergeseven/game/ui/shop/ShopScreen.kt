package com.mergeseven.game.ui.shop
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.R
import com.mergeseven.game.ads.*
import com.mergeseven.game.game.boosters.BoosterCatalog
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.ui.components.*
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun ShopScreen(viewModel: ShopViewModel = hiltViewModel(), onBackClick: () -> Unit = {},
    onCosmeticsClick: () -> Unit = {}, onDailyClick: () -> Unit = {}) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val inventory by viewModel.owned.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? android.app.Activity
    var confirm by remember { mutableStateOf<BoosterType?>(null) }
    LaunchedEffect(Unit) { viewModel.onOpened() }
    WoodPage("Rewards", profile.coins, footer = { BannerAdHost() }) {
        Text("Everything is earned through play", style = MaterialTheme.typography.headlineMedium)
        Text("Collect free rewards, build your booster collection, and make the table yours.")
        WoodLink("Daily gifts and quests", "Claim your free rewards in Challenges", R.drawable.art_gift_v2, onDailyClick)
        WoodPanel {
            Image(painterResource(R.drawable.art_coins_v2), null, Modifier.size(80.dp))
            Text("A little coin boost", style = MaterialTheme.typography.titleLarge)
            Text("${viewModel.claimsLeft()} of 3 optional coin rewards left today")
            GoldButton(if (busy) "Claiming reward…" else if (AdPlacement.FUNDS_COINS in availability) "Watch ad · Get 100 coins" else "Ad unavailable · Try again",
                enabled = !busy && activity != null && viewModel.claimsLeft() > 0) { activity?.let(viewModel::watchEarnAd) }
            Text("Ads are optional. Campaign levels and daily quests also earn coins.", style = MaterialTheme.typography.bodySmall)
        }
        status?.let { Text(it, color = GameColors.CoinGold) }
        Text("Booster collection", style = MaterialTheme.typography.titleLarge)
        BoosterCatalog.specs.values.forEach { spec ->
            WoodPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GameIcon(GameIcons.booster(spec.type), null, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(boosterName(spec.type), style = MaterialTheme.typography.titleMedium)
                        Text(boosterDescription(spec.type), style = MaterialTheme.typography.bodyMedium)
                        Text("Owned ${inventory[spec.type] ?: 0}", color = GameColors.CoinGold)
                    }
                }
                OutlinedButton({ confirm = spec.type }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
                    Text("Get one · ${viewModel.cost(spec.type)} coins")
                }
            }
        }
        WoodLink("Cosmetics", "Unlock styles with coins and milestones", R.drawable.art_celebration_v2, onCosmeticsClick)
    }
    confirm?.let { type -> AlertDialog(onDismissRequest = { confirm = null },
        title = { Text("Get ${boosterName(type)}?") },
        text = { Text("Spend ${viewModel.cost(type)} coins to add one charge. Owned charges are used before coins during play.") },
        confirmButton = { TextButton({ confirm = null; viewModel.buyBooster(type) }) { Text("Use coins") } },
        dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } }) }
}
fun boosterName(type: BoosterType) = when(type) {
    BoosterType.RANDOMIZE -> "Shuffle"
    else -> type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
fun boosterDescription(type: BoosterType) = when(type) {
    BoosterType.UNDO -> "Take back your last placement"
    BoosterType.SWAP -> "Rotate your selected piece"
    BoosterType.RANDOMIZE -> "Refresh the pieces in your tray"
    BoosterType.REMOVE -> "Clear one tile from the board"
    BoosterType.HAMMER -> "Break a selected obstacle"
    BoosterType.VALUE_UP -> "Increase the value of a selected tile"
    BoosterType.MAGNET -> "Gather matching tiles around a target"
    BoosterType.TIME_FREEZE -> "Pause the Time Attack countdown for 15 seconds"
    BoosterType.CONTINUE -> "Clear space and continue a finished run"
}
