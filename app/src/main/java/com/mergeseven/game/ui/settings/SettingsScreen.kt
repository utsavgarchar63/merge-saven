package com.mergeseven.game.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.ui.theme.GameColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // ─── Top Bar ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(GameColors.WoodMid, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GameColors.TextWhite
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )
            }

            // ─── Settings Content List ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio & Sensory Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionHeader(icon = Icons.Default.VolumeUp, title = "AUDIO & SENSORY")

                        SettingToggleRow(
                            title = "Sound Effects",
                            subtitle = "Tile place, merge & combo sounds",
                            checked = uiState.isSoundEnabled,
                            onCheckedChange = { viewModel.toggleSound(it) }
                        )

                        HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = "Background Music",
                            subtitle = "Relaxing ambient puzzle theme",
                            checked = uiState.isMusicEnabled,
                            onCheckedChange = { viewModel.toggleMusic(it) }
                        )

                        HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = "Haptic Vibrations",
                            subtitle = "Tactile feedback on tile merge",
                            checked = uiState.isHapticsEnabled,
                            onCheckedChange = { viewModel.toggleHaptics(it) }
                        )
                    }
                }

                // Preferences Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionHeader(icon = Icons.Default.Notifications, title = "NOTIFICATIONS")

                        SettingToggleRow(
                            title = "Daily Reminders",
                            subtitle = "Alerts for streak rewards & daily challenge",
                            checked = uiState.isNotificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) }
                        )
                    }
                }

                // Reset Game Data Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(icon = Icons.Default.Refresh, title = "DATA & PROGRESS")

                        Text(
                            text = "Reset saved user data (coins, stars, daily streak) back to starting defaults.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.TextWhite.copy(alpha = 0.7f)
                        )

                        Button(
                            onClick = { viewModel.onResetClicked() },
                            colors = ButtonDefaults.buttonColors(containerColor = GameColors.Error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "RESET GAME DATA",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // About & App Version Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SectionHeader(icon = Icons.Default.Info, title = "ABOUT MERGE SEVEN")

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Merge Seven Hexagon Puzzle",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GameColors.CoinGold
                        )

                        Text(
                            text = uiState.appVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = GameColors.TextWhite.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Reset Confirmation Dialog
        if (uiState.showResetDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissResetDialog() },
                title = {
                    Text(
                        text = "Reset Saved Progress?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GameColors.TextWhite
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset all coins, total stars, and streak data back to initial defaults? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GameColors.TextWhite.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmResetData() },
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.Error)
                    ) {
                        Text("Reset", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissResetDialog() }) {
                        Text("Cancel", color = GameColors.CoinGold)
                    }
                },
                containerColor = GameColors.WoodDark,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GameColors.CoinGold,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = GameColors.CoinGold
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = GameColors.TextWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = GameColors.TextWhite.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = GameColors.CoinGold,
                uncheckedThumbColor = GameColors.TextWhite,
                uncheckedTrackColor = GameColors.WoodDark
            )
        )
    }
}
