package com.mergeseven.game.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.mergeseven.game.data.local.store.UserProfileStore
import com.mergeseven.game.data.model.UserProfile
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prepares local storage on first launch and on upgrade.
 *
 * There is deliberately no data to migrate from the builds before this change: coins, stars and
 * level progress were held in memory and never reached disk, so an upgrading player starts from the
 * same seed values as a new one. What this class does provide is the versioned hook where real
 * upgrade steps go, and the guarantee that the profile row exists before anything reads it.
 *
 * Add an upgrade step by raising [SEED_VERSION] and handling the previous value in [seedIfNeeded].
 */
@Singleton
class PersistenceSeeder @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val userProfileStore: UserProfileStore
) {

    suspend fun seedIfNeeded() {
        val appliedVersion = runCatching { readAppliedVersion() }
            .getOrElse { error ->
                Log.w(TAG, "Could not read seed version; treating as first run", error)
                NOT_SEEDED
            }

        if (appliedVersion >= SEED_VERSION) return

        runCatching {
            if (userProfileStore.load() == null) {
                userProfileStore.save(UserProfile())
            }
            dataStore.edit { preferences -> preferences[KEY_SEED_VERSION] = SEED_VERSION }
        }.onFailure { error ->
            // Seeding is best-effort: the repositories fall back to defaults, so a failure here
            // must not stop the app from starting. It will be retried on the next launch.
            Log.e(TAG, "Seeding local storage failed", error)
        }
    }

    private suspend fun readAppliedVersion(): Int =
        dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { preferences -> preferences[KEY_SEED_VERSION] ?: NOT_SEEDED }
            .first()

    private companion object {
        const val TAG = "PersistenceSeeder"
        const val SEED_VERSION = 1
        const val NOT_SEEDED = 0
        val KEY_SEED_VERSION = intPreferencesKey("persistence_seed_version")
    }
}
