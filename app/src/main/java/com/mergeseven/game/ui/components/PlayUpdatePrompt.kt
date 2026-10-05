package com.mergeseven.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.mergeseven.game.core.updates.PlayUpdateState
import com.mergeseven.game.core.updates.UpdateStage

/** Reserves space above navigation, so neither banners nor game controls are obscured. */
@Composable
fun PlayUpdatePrompt(state: PlayUpdateState, onUpdate: () -> Unit, onRestart: () -> Unit, onLater: () -> Unit) {
    if (state.stage == UpdateStage.NONE) return
    val textHeight = (LocalConfiguration.current.screenHeightDp * 0.22f).coerceIn(56f, 120f).dp
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        WoodPanel(Modifier.widthIn(max = 560.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(Modifier.heightIn(max = textHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(when (state.stage) {
                UpdateStage.DOWNLOADED -> "Update ready"
                UpdateStage.DOWNLOADING -> "Downloading update"
                else -> "A new Merge Seven update is available"
            }, style = MaterialTheme.typography.titleMedium)
            Text(state.message ?: when (state.stage) {
                UpdateStage.DOWNLOADED -> "Restart when you're ready to use the update. Your progress is saved."
                UpdateStage.DOWNLOADING -> "You can keep playing while Google Play downloads it."
                else -> "Get the latest improvements from Google Play."
            }, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.stage == UpdateStage.DOWNLOADING) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onLater, Modifier.weight(1f).heightIn(min = 48.dp), enabled = !state.busy) { Text("Later") }
                if (state.stage != UpdateStage.DOWNLOADING) Button(
                    if (state.stage == UpdateStage.DOWNLOADED) onRestart else onUpdate,
                    Modifier.weight(1f).heightIn(min = 48.dp), enabled = !state.busy
                ) { Text(if (state.stage == UpdateStage.DOWNLOADED) "Restart" else "Update") }
            }
        }
    }
}
