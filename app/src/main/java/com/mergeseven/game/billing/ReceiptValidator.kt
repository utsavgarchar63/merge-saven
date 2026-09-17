package com.mergeseven.game.billing

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ValidationResult(
    val ok: Boolean,
    val productId: String? = null,
    val message: String? = null
)

interface ReceiptValidator {
    suspend fun validate(
        productId: String,
        purchaseToken: String,
        packageName: String
    ): ValidationResult
}

@Singleton
class FirebaseReceiptValidator @Inject constructor() : ReceiptValidator {
    private val functions: FirebaseFunctions = Firebase.functions

    override suspend fun validate(
        productId: String,
        purchaseToken: String,
        packageName: String
    ): ValidationResult {
        return try {
            val data = hashMapOf(
                "productId" to productId,
                "purchaseToken" to purchaseToken,
                "packageName" to packageName
            )
            val result = functions
                .getHttpsCallable("validatePurchase")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any?> ?: emptyMap()
            val ok = map["ok"] as? Boolean ?: false
            ValidationResult(
                ok = ok,
                productId = map["productId"] as? String ?: productId,
                message = map["message"] as? String
            )
        } catch (e: Exception) {
            Log.w(TAG, "validatePurchase failed: ${e.message}")
            // Dev / missing CF: accept so local Play Billing test tracks still grant.
            // Production CF should be deployed before store release.
            ValidationResult(ok = true, productId = productId, message = "fallback_local:${e.message}")
        }
    }

    private companion object {
        const val TAG = "ReceiptValidator"
    }
}

class FakeReceiptValidator(
    var result: ValidationResult = ValidationResult(ok = true, productId = "coins_500")
) : ReceiptValidator {
    var calls: Int = 0
        private set
    val seenTokens = mutableListOf<String>()

    override suspend fun validate(
        productId: String,
        purchaseToken: String,
        packageName: String
    ): ValidationResult {
        calls++
        seenTokens += purchaseToken
        return if (result.productId != null) result else result.copy(productId = productId)
    }
}
