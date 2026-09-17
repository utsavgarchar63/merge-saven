package com.mergeseven.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mergeseven.game.ads.ConsentManager
import com.mergeseven.game.app.AppNavGraph
import com.mergeseven.game.billing.BillingRepository
import com.mergeseven.game.billing.EntitlementStore
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.ui.theme.MergeSevenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity entry point for Merge Seven.
 * Uses Jetpack Compose for all UI rendering.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var featureFlags: FeatureFlags
    @Inject lateinit var billingRepository: BillingRepository
    @Inject lateinit var entitlementStore: EntitlementStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MergeSevenTheme {
                LaunchedEffect(Unit) {
                    if (featureFlags.isEnabled(Feature.AF9)) {
                        consentManager.gatherConsent(this@MainActivity)
                        entitlementStore.load()
                        billingRepository.start()
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
