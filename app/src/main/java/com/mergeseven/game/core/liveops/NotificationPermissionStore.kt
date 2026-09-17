package com.mergeseven.game.core.liveops

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the Android 13+ soft-ask notification dialog was shown (AF6-08).
 */
@Singleton
class NotificationPermissionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val softAskShown: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KEY_SOFT_ASK] ?: false }

    suspend fun wasSoftAskShown(): Boolean = softAskShown.first()

    suspend fun markSoftAskShown() {
        dataStore.edit { it[KEY_SOFT_ASK] = true }
    }

    private companion object {
        val KEY_SOFT_ASK = booleanPreferencesKey("notif_soft_ask_shown")
    }
}
