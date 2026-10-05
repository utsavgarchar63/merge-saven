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
    val privacyOptionsRequired: StateFlow<Boolean> get() = kotlinx.coroutines.flow.MutableStateFlow(false)
    suspend fun showPrivacyOptions(activity: Activity) {}
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

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired = _privacyOptionsRequired.asStateFlow()

    override suspend fun showPrivacyOptions(activity: Activity) {
        suspendCancellableCoroutine<Unit> { cont ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w(TAG, "Privacy options error code=${error.errorCode}: ${error.message}")
                finish()
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    override suspend fun gatherConsent(activity: Activity) {
        _resolved.value = false
        val params = ConsentRequestParameters.Builder().build()
        suspendCancellableCoroutine { cont ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Consent form error code=${formError.errorCode}: ${formError.message}")
                        }
                        finish()
                        if (cont.isActive) cont.resume(Unit)
                    }
                },
                { error ->
                    Log.w(TAG, "Consent info update failed code=${error.errorCode}: ${error.message}")
                    finish()
                    if (cont.isActive) cont.resume(Unit)
                }
            )
            // UMP permits using its previous-session status after starting this update.
            // Do not wait for a slow network response when the SDK already allows ads.
            refreshPermission()
        }
    }

    private fun refreshPermission() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _canRequestAds.value = consentInformation.canRequestAds()
    }

    private fun finish() {
        refreshPermission()
        _resolved.value = true
        Log.i(TAG, "Consent resolved status=${consentInformation.consentStatus} can_request_ads=${_canRequestAds.value} privacy_options_required=${_privacyOptionsRequired.value}")
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
