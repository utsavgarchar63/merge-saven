package com.mergeseven.game.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.core.liveops.NotificationPermissionStore
import com.mergeseven.game.core.liveops.PushTopicManager
import com.mergeseven.game.ui.theme.GameColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val permissionStore: NotificationPermissionStore,
    private val pushTopicManager: PushTopicManager,
    featureFlags: FeatureFlags
) : ViewModel() {
    val af6Enabled: StateFlow<Boolean> =
        featureFlags.enabledState(Feature.AF6, viewModelScope)

    suspend fun shouldSoftAsk(): Boolean =
        af6Enabled.value && !permissionStore.wasSoftAskShown()

    fun markShown() {
        viewModelScope.launch { permissionStore.markSoftAskShown() }
    }

    fun syncTopics() {
        pushTopicManager.syncTopicsAsync()
    }
}

/**
 * Soft-ask before the Android 13+ system notification prompt (AF6-08).
 * Shown at most once when AF6 is enabled.
 */
@Composable
fun NotificationSoftAskHost(
    viewModel: NotificationPermissionViewModel = hiltViewModel()
) {
    val af6 by viewModel.af6Enabled.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.syncTopics()
    }

    LaunchedEffect(af6) {
        if (!af6) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < 33) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) return@LaunchedEffect
        if (viewModel.shouldSoftAsk()) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                viewModel.markShown()
            },
            title = { Text("Stay in the loop", color = GameColors.TextWhite) },
            text = {
                Text(
                    "Get a gentle reminder for daily puzzles, streak risk, and seasonal events. You can turn this off anytime in Settings.",
                    color = GameColors.TextWhite.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.markShown()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text("Enable", color = GameColors.CoinGold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.markShown()
                    }
                ) {
                    Text("Not now", color = GameColors.TextWhite)
                }
            },
            containerColor = GameColors.WoodMid
        )
    }
}
