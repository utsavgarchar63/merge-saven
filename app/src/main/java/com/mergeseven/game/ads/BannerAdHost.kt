package com.mergeseven.game.ads

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdSize
import com.mergeseven.game.core.flags.*
import com.mergeseven.game.core.liveops.LiveOpsGates
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BannerAdViewModel @Inject constructor(featureFlags: FeatureFlags,
    private val gates: LiveOpsGates, val consent: ConsentManager, val adService: AdService) : ViewModel() {
    val af9Enabled = featureFlags.enabledState(Feature.AF9, viewModelScope)
    val revision = gates.revision
    fun adsAllowed() = gates.adsAllowed()
}
private class BannerHostOwner { var container: FrameLayout? = null }

@Composable
fun BannerAdHost(viewModel: BannerAdViewModel = hiltViewModel(), modifier: Modifier = Modifier) {
    val enabled by viewModel.af9Enabled.collectAsStateWithLifecycle()
    val consent by viewModel.consent.canRequestAds.collectAsStateWithLifecycle()
    val revision by viewModel.revision.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity ?: return
    val allowed = remember(revision) { viewModel.adsAllowed() }
    if (!enabled || !consent || !allowed) return
    val owner = LocalLifecycleOwner.current
    val host = remember { BannerHostOwner() }
    BoxWithConstraints(modifier.fillMaxWidth().padding(top = 12.dp)) {
        val height = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, maxWidth.value.toInt()).height.dp
        AndroidView(factory = { FrameLayout(it).apply {
            host.container = this
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } }, modifier = Modifier.fillMaxWidth().height(height), update = { container ->
            container.post {
                if (container === host.container && container.isAttachedToWindow && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                    viewModel.adService.bindBanner(activity, container)
            }
        })
    }
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_PAUSE -> host.container?.let(viewModel.adService::pauseBanner)
            Lifecycle.Event.ON_RESUME -> host.container?.let { container ->
                container.post {
                    if (container.isAttachedToWindow && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        viewModel.adService.bindBanner(activity, container)
                        viewModel.adService.resumeBanner(container)
                    }
                }
            }
            else -> Unit
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); host.container?.let(viewModel.adService::unbindBanner) }
    }
}
