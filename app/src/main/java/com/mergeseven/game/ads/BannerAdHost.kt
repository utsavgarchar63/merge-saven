package com.mergeseven.game.ads

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.billing.EntitlementStore
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.core.liveops.LiveOpsGates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BannerAdViewModel @Inject constructor(
    featureFlags: FeatureFlags,
    private val liveOpsGates: LiveOpsGates,
    entitlementStore: EntitlementStore,
    val adService: AdService
) : ViewModel() {
    val af9Enabled: StateFlow<Boolean> = featureFlags.enabledState(Feature.AF9, viewModelScope)
    val removeAds: StateFlow<Boolean> = entitlementStore.removeAdsOrPremium
    fun adsAllowed(): Boolean = liveOpsGates.adsAllowed()
}

@Composable
fun BannerAdHost(
    viewModel: BannerAdViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val af9 by viewModel.af9Enabled.collectAsStateWithLifecycle()
    val removeAds by viewModel.removeAds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    if (!af9 || removeAds || !viewModel.adsAllowed() || activity == null) return

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        update = { container ->
            viewModel.adService.bindBanner(activity, container)
        }
    )
    DisposableEffect(Unit) {
        onDispose { viewModel.adService.unbindBanner() }
    }
}
