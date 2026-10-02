package com.mergeseven.game.ui.settings

import androidx.compose.ui.res.painterResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mergeseven.game.R
import com.mergeseven.game.data.preferences.ColourblindMode
import com.mergeseven.game.ui.components.GameIcon
import com.mergeseven.game.ui.components.GameIcons
import com.mergeseven.game.ui.theme.GameColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onCosmeticsClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onReplayTutorial: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val privacyRequired by viewModel.privacyOptionsRequired.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.toggleNotifications(granted)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameColors.WoodDark)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
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
                        .size(48.dp)
                        .background(GameColors.WoodMid, CircleShape)
                ) {
                    GameIcon(
                        resId = GameIcons.Back,
                        contentDescription = stringResource(R.string.back),
                        tint = GameColors.TextWhite,
                        size = 24.dp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.settings_title).uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = GameColors.TextWhite
                )
            }

            // ─── Settings Content List ───────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
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
                        SectionHeader(icon = Icons.AutoMirrored.Filled.VolumeUp, title = stringResource(R.string.settings_section_audio))

                        SettingToggleRow(
                            title = stringResource(R.string.settings_sound),
                            subtitle = stringResource(R.string.settings_sound_subtitle),
                            checked = uiState.isSoundEnabled,
                            onCheckedChange = { viewModel.toggleSound(it) }
                        )

                        HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = stringResource(R.string.settings_music),
                            subtitle = stringResource(R.string.settings_music_subtitle),
                            checked = uiState.isMusicEnabled,
                            onCheckedChange = { viewModel.toggleMusic(it) }
                        )

                        HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = stringResource(R.string.settings_haptics),
                            subtitle = stringResource(R.string.settings_haptics_subtitle),
                            checked = uiState.isHapticsEnabled,
                            onCheckedChange = { viewModel.toggleHaptics(it) }
                        )

                        HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                        SettingToggleRow(
                            title = stringResource(R.string.settings_reduce_motion),
                            subtitle = stringResource(R.string.settings_reduce_motion_subtitle),
                            checked = uiState.isReduceMotionEnabled,
                            onCheckedChange = { viewModel.toggleReduceMotion(it) }
                        )
                    }
                }

                if (uiState.af11Enabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = GameColors.WoodMid)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionHeader(
                                icon = Icons.Default.AccessibilityNew,
                                title = stringResource(R.string.settings_section_accessibility)
                            )

                            Text(
                                text = stringResource(R.string.settings_colourblind),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = GameColors.TextWhite
                            )
                            Text(
                                text = stringResource(R.string.settings_colourblind_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = GameColors.TextWhite.copy(alpha = 0.7f)
                            )
                            ColourblindModeRow(
                                selected = uiState.colourblindMode,
                                onSelect = viewModel::setColourblindMode
                            )

                            HorizontalDivider(color = GameColors.WoodDark.copy(alpha = 0.5f))

                            SettingToggleRow(
                                title = stringResource(R.string.settings_large_touch),
                                subtitle = stringResource(R.string.settings_large_touch_subtitle),
                                checked = uiState.largeTouchTargets,
                                onCheckedChange = { viewModel.toggleLargeTouchTargets(it) }
                            )
                        }
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
                        SectionHeader(icon = Icons.Default.Notifications, title = stringResource(R.string.settings_section_notifications))

                        SettingToggleRow(
                            title = stringResource(R.string.settings_notifications),
                            subtitle = stringResource(R.string.settings_notifications_subtitle),
                            checked = uiState.isNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && android.os.Build.VERSION.SDK_INT >= 33 &&
                                    !com.mergeseven.game.core.liveops.LocalReminderManager.permissionGranted(context))
                                    notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                else viewModel.toggleNotifications(enabled)
                            }
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

                        if (privacyRequired) OutlinedButton(
                            onClick = { (context as? android.app.Activity)?.let(viewModel::showPrivacyOptions) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Icon(painterResource(R.drawable.icon_privacy), null)
                                Spacer(Modifier.width(8.dp)); Text("Ad privacy options")
                            }
                        OutlinedButton(onClick = { viewModel.replayTutorial(); onReplayTutorial() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Icon(painterResource(R.drawable.icon_help), null)
                                Spacer(Modifier.width(8.dp)); Text("Replay how to play")
                            }

                        Text(
                            text = "Reset coins to 100, total stars to zero, and app preferences to their defaults.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GameColors.TextWhite.copy(alpha = 0.7f)
                        )

                        Button(
                            onClick = { viewModel.onResetClicked() },
                            colors = ButtonDefaults.buttonColors(containerColor = GameColors.Error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "RESET WALLET & SETTINGS",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                Button(
                    onClick = onShopClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.CoinGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "REWARDS",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }

                OutlinedButton(
                    onClick = onAchievementsClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ACHIEVEMENTS", color = GameColors.TextWhite)
                }
                                OutlinedButton(
                    onClick = onStatsClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("STATS", color = GameColors.TextWhite)
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
                            color = GameColors.CoinGold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = uiState.appVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = GameColors.TextWhite.copy(alpha = 0.6f),
                            modifier = if (uiState.debugMenuAvailable) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { viewModel.onVersionLongPressed() }
                                )
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }

        if (uiState.showDebugMenu) {
            DebugMenuSheet(
                featureFlags = uiState.featureFlags,
                unlockLevelInput = uiState.unlockLevelInput,
                statusMessage = uiState.debugStatusMessage,
                difficultyProfile = uiState.difficultyProfile,
                onDismiss = { viewModel.dismissDebugMenu() },
                onGrantCoins = { viewModel.grantDebugCoins() },
                onUnlockLevelInputChange = { viewModel.onUnlockLevelInputChange(it) },
                onUnlockLevel = { viewModel.unlockThroughLevel() },
                onForceGameOver = { viewModel.forceGameOver() },
                onToggleFeature = { feature, enabled -> viewModel.setFeatureEnabled(feature, enabled) },
                onDifficultyProfile = { viewModel.setDifficultyProfile(it) }
            )
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
                        text = "Reset coins to 100, total stars to zero, and app preferences? This action cannot be undone.",
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
private fun ColourblindModeRow(
    selected: ColourblindMode,
    onSelect: (ColourblindMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ColourblindMode.entries.forEach { mode ->
            val label = when (mode) {
                ColourblindMode.OFF -> stringResource(R.string.colourblind_off)
                ColourblindMode.DEUTERANOPIA -> stringResource(R.string.colourblind_deuteranopia)
                ColourblindMode.PROTANOPIA -> stringResource(R.string.colourblind_protanopia)
                ColourblindMode.TRITANOPIA -> stringResource(R.string.colourblind_tritanopia)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = GameColors.CoinGold,
                        unselectedColor = GameColors.TextWhite.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = label,
                    color = GameColors.TextWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
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
internal fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = GameColors.CoinGold,
                uncheckedThumbColor = GameColors.TextWhite,
                uncheckedTrackColor = GameColors.WoodDark
            )
        )
    }
}
