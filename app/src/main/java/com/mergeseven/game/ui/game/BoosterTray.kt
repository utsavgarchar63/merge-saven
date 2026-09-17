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
import androidx.compose.runtime.Composable
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

@Composable
fun BoosterTray(
    buttons: List<BoosterButtonUi>,
    af3Enabled: Boolean,
    onBooster: (BoosterType) -> Unit,
    modifier: Modifier = Modifier,
    largeTouchTargets: Boolean = false
) {
    val iconSize: Dp = if (largeTouchTargets) 56.dp else 48.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visible = if (af3Enabled) {
            buttons
        } else {
            buttons.filter {
                it.type in setOf(
                    BoosterType.UNDO,
                    BoosterType.SWAP,
                    BoosterType.RANDOMIZE,
                    BoosterType.REMOVE
                )
            }
        }
        visible.forEach { btn ->
            val label = labelFor(btn.type)
            BoosterButton(
                type = btn.type,
                label = label,
                enabled = btn.enabled,
                selected = btn.selected,
                costLabel = if (af3Enabled) {
                    if (btn.owned > 0) "×${btn.owned}" else "${btn.cost}"
                } else {
                    null
                },
                iconSize = iconSize,
                onClick = { onBooster(btn.type) }
            )
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
private fun BoosterButton(
    type: BoosterType,
    label: String,
    enabled: Boolean,
    selected: Boolean,
    costLabel: String?,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = buildString {
                    append(label)
                    if (costLabel != null) append(", ").append(costLabel)
                }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp)
            .heightIn(min = 48.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) {
                GameColors.CoinGold.copy(alpha = 0.45f)
            } else {
                GameColors.WoodDark.copy(alpha = 0.35f)
            },
            modifier = Modifier.size(iconSize)
        ) {
            GameIcon(
                resId = GameIcons.booster(type),
                contentDescription = null,
                modifier = Modifier.padding(2.dp),
                size = iconSize - 4.dp
            )
        }
        Text(
            text = label,
            color = GameColors.TextWhite.copy(alpha = 0.75f),
            fontSize = 11.sp,
            maxLines = 2
        )
        if (costLabel != null) {
            Text(
                text = costLabel,
                color = GameColors.CoinGold.copy(alpha = 0.9f),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
