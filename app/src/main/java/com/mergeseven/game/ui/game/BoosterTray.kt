package com.mergeseven.game.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import com.mergeseven.game.ui.shop.boosterName
import com.mergeseven.game.ui.shop.boosterDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mergeseven.game.R
import com.mergeseven.game.game.model.BoosterType
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.theme.GameColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoosterTray(
    buttons: List<BoosterButtonUi>,
    af3Enabled: Boolean,
    onBooster: (BoosterType) -> Unit,
    modifier: Modifier = Modifier,
    largeTouchTargets: Boolean = false,
    verticalLayout: Boolean = false,
    extraContent: @Composable (() -> Unit) -> Unit = {}
) {
    var more by remember { mutableStateOf(false) }
    val primary = setOf(BoosterType.UNDO, BoosterType.RANDOMIZE, BoosterType.REMOVE)
    val shown = buttons.filter { it.type in primary }
    val stackedLabels = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 360 ||
        androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    if (verticalLayout) {
        Column(modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.forEach { btn -> PrimaryBoosterCard(btn, onBooster, Modifier.fillMaxWidth(), false) }
            if (af3Enabled) TextButton({ more = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("More") }
        }
    } else {
        Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            shown.forEach { btn -> PrimaryBoosterCard(btn, onBooster, Modifier.weight(1f), stackedLabels) }
            if (af3Enabled) TextButton({ more = true }, Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)) { Text("More") }
        }
    }

    if (more) ModalBottomSheet(onDismissRequest = { more = false }, containerColor = GameColors.WoodDark) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hints and boosters", style = MaterialTheme.typography.headlineSmall)
            extraContent { more = false }
            buttons.filter { stackedLabels || it.type !in primary }.forEach { btn ->
                OutlinedButton({ more = false; onBooster(btn.type) }, enabled = btn.enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                    GameIcon(GameIcons.booster(btn.type), null, size = 32.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(boosterName(btn.type))
                        Text(boosterDescription(btn.type), style = MaterialTheme.typography.bodySmall)
                        Text(if (btn.unlimited) "Unlimited · No coins or charges used" else if (btn.owned > 0) "Owned ${btn.owned}" else "${btn.cost} coins", color = GameColors.CoinGold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun labelFor(type: BoosterType): String = when (type) {
    BoosterType.UNDO -> stringResource(R.string.booster_undo)
    BoosterType.SWAP -> stringResource(R.string.booster_swap)
    BoosterType.RANDOMIZE -> stringResource(R.string.booster_randomize)
    BoosterType.REMOVE -> stringResource(R.string.booster_remove)
    BoosterType.HAMMER -> stringResource(R.string.booster_hammer)
    BoosterType.VALUE_UP -> stringResource(R.string.booster_value_up)
    BoosterType.MAGNET -> stringResource(R.string.booster_magnet)
    BoosterType.TIME_FREEZE -> stringResource(R.string.booster_time_freeze)
    BoosterType.CONTINUE -> stringResource(R.string.booster_continue)
}

@Composable
private fun PrimaryBoosterCard(btn: BoosterButtonUi, onBooster: (BoosterType) -> Unit,
    modifier: Modifier, stacked: Boolean) {
    Surface(onClick = { onBooster(btn.type) }, enabled = btn.enabled,
        modifier = modifier.heightIn(min = 56.dp).semantics(mergeDescendants = true) {
            contentDescription = "${boosterName(btn.type)}. ${boosterDescription(btn.type)}. " +
                if (btn.unlimited) "Unlimited. No coins or charges used." else "Owned ${btn.owned}. ${btn.cost} coins if no charge."
        }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = GameColors.WoodMid) {
        val quantity = if (btn.unlimited) "Unlimited" else if (btn.owned > 0) "${btn.owned} owned" else "${btn.cost} coins"
        if (stacked) Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameIcon(GameIcons.booster(btn.type), null, size = 24.dp)
            Text(when(btn.type) {
                BoosterType.RANDOMIZE -> "Mix"
                BoosterType.REMOVE -> "Clear"
                else -> labelFor(btn.type)
            }, style = MaterialTheme.typography.labelSmall)
            Text(if (btn.unlimited) "Free" else if (btn.owned > 0) btn.owned.toString() else "${btn.cost}c",
                style = MaterialTheme.typography.labelSmall, color = GameColors.CoinGold)
        } else Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GameIcon(GameIcons.booster(btn.type), null, size = 24.dp)
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(labelFor(btn.type), style = MaterialTheme.typography.labelMedium)
                Text(quantity, style = MaterialTheme.typography.labelSmall, color = GameColors.CoinGold)
            }
        }
    }
}
