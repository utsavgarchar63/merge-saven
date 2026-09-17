package com.mergeseven.game.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PendingPurchase(
    val productId: String,
    val purchaseToken: String,
    val packageName: String
)

private val Context.pendingPurchaseStore: DataStore<Preferences> by preferencesDataStore("pending_purchases")

@Singleton
class PendingPurchaseQueue @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringSetPreferencesKey("pending")

    suspend fun enqueue(item: PendingPurchase) {
        context.pendingPurchaseStore.edit { prefs ->
            val cur = prefs[key].orEmpty().toMutableSet()
            cur += json.encodeToString(item)
            prefs[key] = cur
        }
    }

    suspend fun all(): List<PendingPurchase> {
        val raw = context.pendingPurchaseStore.data.first()[key].orEmpty()
        return raw.mapNotNull { runCatching { json.decodeFromString(PendingPurchase.serializer(), it) }.getOrNull() }
    }

    suspend fun remove(token: String) {
        context.pendingPurchaseStore.edit { prefs ->
            val cur = prefs[key].orEmpty().toMutableSet()
            cur.removeAll { encoded ->
                runCatching {
                    json.decodeFromString(PendingPurchase.serializer(), encoded).purchaseToken == token
                }.getOrDefault(false)
            }
            prefs[key] = cur
        }
    }
}
