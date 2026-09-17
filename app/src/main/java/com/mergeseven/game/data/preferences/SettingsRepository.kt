package com.mergeseven.game.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for app settings (DataStore).
 */
interface SettingsRepository {
    val isSoundEnabled: Flow<Boolean>
    val isMusicEnabled: Flow<Boolean>
    val isHapticsEnabled: Flow<Boolean>
    val isReduceMotionEnabled: Flow<Boolean>
    val isNotificationsEnabled: Flow<Boolean>
    val isTutorialCompleted: Flow<Boolean>
    val colourblindMode: Flow<ColourblindMode>
    val largeTouchTargets: Flow<Boolean>

    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setMusicEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setReduceMotionEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setTutorialCompleted(completed: Boolean)
    suspend fun setColourblindMode(mode: ColourblindMode)
    suspend fun setLargeTouchTargets(enabled: Boolean)
    suspend fun resetSettings()
}

/**
 * Production implementation backed by Jetpack DataStore Preferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_REDUCE_MOTION_ENABLED = booleanPreferencesKey("reduce_motion_enabled")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val KEY_COLOURBLIND_MODE = stringPreferencesKey("colourblind_mode")
        val KEY_LARGE_TOUCH_TARGETS = booleanPreferencesKey("large_touch_targets")
    }

    private val prefsFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val isSoundEnabled: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_SOUND_ENABLED] ?: true
    }

    override val isMusicEnabled: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_MUSIC_ENABLED] ?: true
    }

    override val isHapticsEnabled: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_HAPTICS_ENABLED] ?: true
    }

    override val isReduceMotionEnabled: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_REDUCE_MOTION_ENABLED] ?: false
    }

    override val isNotificationsEnabled: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    override val isTutorialCompleted: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_TUTORIAL_COMPLETED] ?: false
    }

    override val colourblindMode: Flow<ColourblindMode> = prefsFlow.map { preferences ->
        ColourblindMode.fromStorage(preferences[PreferencesKeys.KEY_COLOURBLIND_MODE])
    }

    override val largeTouchTargets: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_LARGE_TOUCH_TARGETS] ?: false
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SOUND_ENABLED] = enabled
        }
    }

    override suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MUSIC_ENABLED] = enabled
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_HAPTICS_ENABLED] = enabled
        }
    }

    override suspend fun setReduceMotionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_REDUCE_MOTION_ENABLED] = enabled
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun setTutorialCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_TUTORIAL_COMPLETED] = completed
        }
    }

    override suspend fun setColourblindMode(mode: ColourblindMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_COLOURBLIND_MODE] = mode.name
        }
    }

    override suspend fun setLargeTouchTargets(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_LARGE_TOUCH_TARGETS] = enabled
        }
    }

    override suspend fun resetSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
