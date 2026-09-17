package com.mergeseven.game.core.flags

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.mergeseven.game.BuildConfig
import com.mergeseven.game.di.FeatureFlagsStore
import com.mergeseven.game.di.PersistenceScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed flags for DEBUG builds. In release, every read is hard-false and writes are
 * no-ops — preferences on disk cannot turn a phase on.
 */
@Singleton
class LocalFeatureFlags(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val isDebug: Boolean
) : FeatureFlags {

    @Inject
    constructor(
        @FeatureFlagsStore dataStore: DataStore<Preferences>,
        @PersistenceScope scope: CoroutineScope
    ) : this(dataStore, scope, BuildConfig.DEBUG)

    private val overrides = MutableStateFlow(allOn())

    init {
        scope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) emit(emptyPreferences()) else throw exception
                }
                .collect { prefs ->
                    overrides.value = Feature.entries.associateWith { feature ->
                        prefs[key(feature)] ?: true
                    }
                }
        }
    }

    override fun isEnabled(feature: Feature): Boolean {
        return overrides.value[feature] ?: true
    }

    override fun observe(feature: Feature): Flow<Boolean> {
        return overrides.asStateFlow().map { it[feature] ?: true }
    }

    override suspend fun setEnabled(feature: Feature, enabled: Boolean) {
        overrides.update { it + (feature to enabled) }
        dataStore.edit { prefs ->
            prefs[key(feature)] = enabled
        }
    }

    override fun snapshot(): Map<Feature, Boolean> {
        return overrides.value.toMap()
    }

    private fun key(feature: Feature) = booleanPreferencesKey(feature.name)

    private companion object {
        fun allOn(): Map<Feature, Boolean> = Feature.entries.associateWith { true }
    }
}

/** In-memory flags for unit tests. Defaults off; [isDebug] controls whether writes stick. */
class InMemoryFeatureFlags(
    private val isDebug: Boolean = true,
    initial: Map<Feature, Boolean> = emptyMap()
) : FeatureFlags {

    private val state = MutableStateFlow(
        Feature.entries.associateWith { feature -> initial[feature] == true }
    )

    override fun isEnabled(feature: Feature): Boolean {
        if (!isDebug) return false
        return state.value[feature] == true
    }

    override fun observe(feature: Feature): Flow<Boolean> {
        if (!isDebug) return flowOf(false)
        return state.asStateFlow().map { it[feature] == true }
    }

    override suspend fun setEnabled(feature: Feature, enabled: Boolean) {
        if (!isDebug) return
        state.update { it + (feature to enabled) }
    }

    override fun snapshot(): Map<Feature, Boolean> {
        if (!isDebug) return Feature.entries.associateWith { false }
        return state.value.toMap()
    }
}
