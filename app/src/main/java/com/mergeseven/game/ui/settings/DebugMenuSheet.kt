package com.mergeseven.game.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.ui.theme.GameColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenuSheet(
    featureFlags: Map<Feature, Boolean>,
    unlockLevelInput: String,
    statusMessage: String?,
    difficultyProfile: String,
    onDismiss: () -> Unit,
    onGrantCoins: () -> Unit,
    onUnlockLevelInputChange: (String) -> Unit,
    onUnlockLevel: () -> Unit,
    onForceGameOver: () -> Unit,
    onToggleFeature: (Feature, Boolean) -> Unit,
    onDifficultyProfile: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GameColors.WoodDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "DEBUG MENU",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GameColors.CoinGold
            )

            if (statusMessage != null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = GameColors.TextWhite.copy(alpha = 0.85f)
                )
            }

            Button(
                onClick = onGrantCoins,
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant +1000 coins", color = Color.Black)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = unlockLevelInput,
                    onValueChange = onUnlockLevelInputChange,
                    label = { Text("Unlock through level") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onUnlockLevel) {
                    Text("Set", color = GameColors.CoinGold)
                }
            }

            Button(
                onClick = onForceGameOver,
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.Error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Force game over", color = Color.White)
            }

            Text(
                text = "DIFFICULTY PROFILE (AF4)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = GameColors.CoinGold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("EASY", "STANDARD", "HARD").forEach { id ->
                    val selected = difficultyProfile == id
                    OutlinedButton(
                        onClick = { onDifficultyProfile(id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (selected) GameColors.CoinGold else GameColors.TextWhite
                        )
                    ) {
                        Text(id.take(4), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "FEATURE FLAGS",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = GameColors.CoinGold
            )

            Feature.entries.forEach { feature ->
                SettingToggleRow(
                    title = feature.name,
                    subtitle = "Ship ${feature.name} dark until enabled",
                    checked = featureFlags[feature] == true,
                    onCheckedChange = { onToggleFeature(feature, it) }
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close", color = GameColors.CoinGold)
            }
        }
    }
}
