package com.mergeseven.game.core.updates

import android.content.SharedPreferences
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UpdateStage { NONE, AVAILABLE, DOWNLOADING, DOWNLOADED }
data class PlayUpdateState(val stage: UpdateStage = UpdateStage.NONE, val busy: Boolean = false,
    val message: String? = null)

/** Flexible Play updates only: offline errors never block play or fabricate an update. */
class PlayUpdateController(private val manager: AppUpdateManager, private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis) {
    private val _state = MutableStateFlow(PlayUpdateState())
    val state = _state.asStateFlow()
    private var availableVersion = 0
    private var checking = false
    private var closed = false
    private val listener = InstallStateUpdatedListener { install ->
        if (!closed) when (install.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING ->
                _state.value = PlayUpdateState(UpdateStage.DOWNLOADING)
            InstallStatus.DOWNLOADED -> _state.value = PlayUpdateState(UpdateStage.DOWNLOADED)
            InstallStatus.FAILED -> _state.value = PlayUpdateState(UpdateStage.AVAILABLE, message = "Update could not download. Try again when online.")
            InstallStatus.CANCELED -> dismiss()
            InstallStatus.INSTALLED -> _state.value = PlayUpdateState()
        }
    }
    init { manager.registerListener(listener) }

    fun check() {
        if (checking || closed) return
        checking = true
        manager.appUpdateInfo.addOnSuccessListener { info ->
            checking = false
            if (closed) return@addOnSuccessListener
            availableVersion = info.availableVersionCode()
            _state.value = when {
                info.installStatus() == InstallStatus.DOWNLOADED -> PlayUpdateState(UpdateStage.DOWNLOADED)
                info.installStatus() in listOf(InstallStatus.DOWNLOADING, InstallStatus.PENDING) -> PlayUpdateState(UpdateStage.DOWNLOADING)
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                    UpdatePromptPolicy.shouldOffer(availableVersion, prefs.getInt("dismissed_version", 0), prefs.getLong("dismissed_at", 0), now()) ->
                    PlayUpdateState(UpdateStage.AVAILABLE)
                else -> PlayUpdateState()
            }
        }.addOnFailureListener { error ->
            checking = false
            Log.i("MergeSevenUpdate", "Play update check unavailable: ${error.javaClass.simpleName}")
            // Sideloaded/offline installations remain fully playable.
        }
    }

    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (_state.value.busy || _state.value.stage != UpdateStage.AVAILABLE || closed) return
        _state.value = _state.value.copy(busy = true, message = null)
        // Play requires a new AppUpdateInfo for each start/retry.
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (closed) return@addOnSuccessListener
            try {
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                    manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())) {
                    _state.value = _state.value.copy(busy = false)
                } else { _state.value = PlayUpdateState(); check() }
            } catch (error: Exception) { failed(error) }
        }.addOnFailureListener(::failed)
    }

    fun finish() {
        if (closed || _state.value.stage != UpdateStage.DOWNLOADED || _state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        manager.completeUpdate().addOnFailureListener(::failed)
    }

    fun dismiss() {
        if (closed) return
        // Never dismiss a completed download permanently; recheck reminds on next foreground.
        if (_state.value.stage == UpdateStage.AVAILABLE) prefs.edit().putInt("dismissed_version", availableVersion)
            .putLong("dismissed_at", now()).apply()
        _state.value = PlayUpdateState()
    }

    private fun failed(error: Exception) {
        if (closed) return
        Log.w("MergeSevenUpdate", "Update failed: ${error.javaClass.simpleName}")
        _state.value = _state.value.copy(busy = false, message = "Update unavailable right now. You can keep playing.")
    }

    fun close() { closed = true; manager.unregisterListener(listener) }
}

object UpdatePromptPolicy {
    fun shouldOffer(version: Int, dismissedVersion: Int, dismissedAt: Long, now: Long): Boolean =
        version > 0 && (version != dismissedVersion || now - dismissedAt >= 86_400_000L)
}
