package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val brightness: Float,
    val volume: Int,
    val orientation: Int,
    val isGridMode: Boolean,
    val sortOrder: Int, // 0: Name, 1: Size, 2: Duration
    val deepProcessingMode: Boolean
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val VOLUME = intPreferencesKey("volume")
        val ORIENTATION = intPreferencesKey("orientation")
        val IS_GRID_MODE = booleanPreferencesKey("is_grid_mode")
        val SORT_ORDER = intPreferencesKey("sort_order")
        val DEEP_PROCESSING_MODE = booleanPreferencesKey("deep_processing_mode")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val brightness = preferences[Keys.BRIGHTNESS] ?: -1.0f // -1 means system default
            val volume = preferences[Keys.VOLUME] ?: 50 // Default 50%
            val orientation = preferences[Keys.ORIENTATION] ?: 0 // 0 means default/unspecified
            val isGridMode = preferences[Keys.IS_GRID_MODE] ?: false
            val sortOrder = preferences[Keys.SORT_ORDER] ?: 0
            val deepProcessingMode = preferences[Keys.DEEP_PROCESSING_MODE] ?: false
            UserSettings(brightness, volume, orientation, isGridMode, sortOrder, deepProcessingMode)
        }

    suspend fun updateBrightness(brightness: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.BRIGHTNESS] = brightness
        }
    }

    suspend fun updateVolume(volume: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.VOLUME] = volume
        }
    }

    suspend fun updateOrientation(orientation: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.ORIENTATION] = orientation
        }
    }
    
    suspend fun updateIsGridMode(isGridMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_GRID_MODE] = isGridMode
        }
    }

    suspend fun updateSortOrder(sortOrder: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.SORT_ORDER] = sortOrder
        }
    }
    
    suspend fun updateDeepProcessingMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DEEP_PROCESSING_MODE] = enabled
        }
    }
}
