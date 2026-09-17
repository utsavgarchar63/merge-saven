package com.mergeseven.game.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * UMP consent gate (AF9-11). No ad SDK initialize/load until [canRequestAds] is true.
 */
interface ConsentManager {
    val canRequestAds: StateFlow<Boolean>
    val resolved: StateFlow<Boolean>
    suspend fun gatherConsent(activity: Activity)
}

@Singleton
class UmpConsentManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ConsentManager {

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _resolved = MutableStateFlow(false)
    override val resolved: StateFlow<Boolean> = _resolved.asStateFlow()

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    override suspend fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        suspendCancellableCoroutine { cont ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Consent form error: ${formError.message}")
                        }
                        finish()
                        if (cont.isActive) cont.resume(Unit)
                    }
                },
                { error ->
                    Log.w(TAG, "Consent info update failed: ${error.message}")
                    finish()
                    if (cont.isActive) cont.resume(Unit)
                }
            )
        }
    }

    private fun finish() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _resolved.value = true
    }

    private companion object {
        const val TAG = "ConsentManager"
    }
}

/** JVM / unit-test consent that never talks to UMP. */
class FakeConsentManager(
    initiallyCanRequest: Boolean = true
) : ConsentManager {
    private val _can = MutableStateFlow(initiallyCanRequest)
    private val _resolved = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _can.asStateFlow()
    override val resolved: StateFlow<Boolean> = _resolved.asStateFlow()
    var gatherCalls: Int = 0
        private set

    override suspend fun gatherConsent(activity: Activity) {
        gatherCalls++
        _resolved.value = true
        _can.value = initiallyCanRequestFixed
    }

    private val initiallyCanRequestFixed = initiallyCanRequest

    fun setCanRequestAds(value: Boolean) {
        _can.value = value
        _resolved.value = true
    }
}
