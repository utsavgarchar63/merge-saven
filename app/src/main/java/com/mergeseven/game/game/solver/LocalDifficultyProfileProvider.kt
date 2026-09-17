package com.mergeseven.game.game.solver

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDifficultyProfileProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @PersistenceScope private val scope: CoroutineScope
) : DifficultyProfileProvider {

    override val profile: StateFlow<DifficultyProfile> = dataStore.data
        .map { prefs ->
            val raw = prefs[KEY] ?: DifficultyId.STANDARD.name
            val id = runCatching { DifficultyId.valueOf(raw) }.getOrDefault(DifficultyId.STANDARD)
            DifficultyProfiles.of(id)
        }
        .stateIn(scope, SharingStarted.Eagerly, DifficultyProfiles.STANDARD)

    override suspend fun setProfile(id: DifficultyId) {
        dataStore.edit { it[KEY] = id.name }
    }

    fun setProfileAsync(id: DifficultyId) {
        scope.launch { setProfile(id) }
    }

    private companion object {
        val KEY = stringPreferencesKey("difficulty_profile")
    }
}
