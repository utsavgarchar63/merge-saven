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
    val tutorialStep: Flow<Int> get() = kotlinx.coroutines.flow.flowOf(0)
    suspend fun setTutorialStep(step: Int) {}
    val isTutorialCompleted: Flow<Boolean>
    val isAnimatedGuideSeen: Flow<Boolean> get() = kotlinx.coroutines.flow.flowOf(true)
    val animatedGuidePage: Flow<Int> get() = kotlinx.coroutines.flow.flowOf(0)
    suspend fun setAnimatedGuideSeen(seen: Boolean) {}
    suspend fun setAnimatedGuidePage(page: Int) {}
    suspend fun resetTutorialGuide() {
        setTutorialStep(0); setAnimatedGuidePage(0); setAnimatedGuideSeen(false); setTutorialCompleted(false)
    }
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
        val KEY_GUIDE_SEEN = booleanPreferencesKey("animated_guide_seen")
        val KEY_GUIDE_PAGE = androidx.datastore.preferences.core.intPreferencesKey("animated_guide_page")
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
        preferences[PreferencesKeys.KEY_NOTIFICATIONS_ENABLED] ?: false
    }

    override val tutorialStep: Flow<Int> = prefsFlow.map { it[androidx.datastore.preferences.core.intPreferencesKey("tutorial_step")] ?: 0 }
    override suspend fun setTutorialStep(step: Int) {
        dataStore.edit { it[androidx.datastore.preferences.core.intPreferencesKey("tutorial_step")] = step.coerceIn(0, 3) }
    }

    override val isTutorialCompleted: Flow<Boolean> = prefsFlow.map { preferences ->
        preferences[PreferencesKeys.KEY_TUTORIAL_COMPLETED] ?: false
    }
    override val isAnimatedGuideSeen = prefsFlow.map { it[PreferencesKeys.KEY_GUIDE_SEEN] ?: false }
    override val animatedGuidePage = prefsFlow.map { (it[PreferencesKeys.KEY_GUIDE_PAGE] ?: 0).coerceIn(0, 2) }
    override suspend fun setAnimatedGuideSeen(seen: Boolean) {
        dataStore.edit { it[PreferencesKeys.KEY_GUIDE_SEEN] = seen }
    }
    override suspend fun setAnimatedGuidePage(page: Int) {
        dataStore.edit { it[PreferencesKeys.KEY_GUIDE_PAGE] = page.coerceIn(0, 2) }
    }
    override suspend fun resetTutorialGuide() {
        dataStore.edit {
            it[PreferencesKeys.KEY_GUIDE_SEEN] = false
            it[PreferencesKeys.KEY_GUIDE_PAGE] = 0
            it[androidx.datastore.preferences.core.intPreferencesKey("tutorial_step")] = 0
            it[PreferencesKeys.KEY_TUTORIAL_COMPLETED] = false
        }
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
