package com.mergeseven.game.cloud

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceIdStore {
    suspend fun getOrCreate(): String
}

private val Context.deviceIdDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_device_id")

@Singleton
class DataStoreDeviceIdStore @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceIdStore {
    private val key = stringPreferencesKey("device_id")

    override suspend fun getOrCreate(): String {
        val existing = context.deviceIdDataStore.data.first()[key]
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        context.deviceIdDataStore.edit { it[key] = created }
        return created
    }
}

class FixedDeviceIdStore(private val id: String) : DeviceIdStore {
    override suspend fun getOrCreate(): String = id
}
