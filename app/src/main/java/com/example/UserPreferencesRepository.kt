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
    val seekButtons: Boolean = true,
    val forwardBackwardDelay: Int = 3,
    val longTapDelay: Int = 4,
    val controlsHidingDelay: Int = 3,
    val videosTransition: Boolean = true,
    val lockWithSensor: Boolean = false,
    val screenOrientation: String = "Auto / Sensor",
    val doubleTapDelay: Int = 3,
    val doubleTapToPlayPause: Boolean = true,

    val volumeGesture: Boolean = true,
    val brightnessGesture: Boolean = true,
    val saveBrightnessLevel: Boolean = true,
    val saveVolumeLevel: Boolean = true,
    val swipeToSeek: Boolean = true,
    val twoFingerZoom: Boolean = true,
    val doubleTapToSeek: Boolean = true,
    val swipeGestureSensitivity: Float = 1.0f,

    val enableAudioEqualizer: Boolean = false,
    val acousticEqualizerPreset: String = "Flat",
    val band60Hz: Float = 0.0f,
    val band230Hz: Float = 0.0f,
    val band910Hz: Float = 0.0f,
    val band4kHz: Float = 0.0f,
    val band14kHz: Float = 0.0f,

    val displayModeLayout: String = "Sleek Grid View",
    val showOnlyFavorites: Boolean = false,
    val defaultPlaybackTapAction: String = "Play Entire List Sequentially",
    val sortMediaAssetsBy: String = "Media Title / Alphabetical",
    val sortDirectionOrder: String = "Ascending",
    val groupMediaItemsBy: String = "Group by Folder",

    val autoRescan: Boolean = true,
    val fileAccessMode: String = "MediaStore (Recommended/Default)",
    val tunneledPlayback: Boolean = false,

    val brightness: Float = -1f,
    val volume: Int = 50,
    val orientation: Int = 0,
    val isGridMode: Boolean = false,
    val sortOrder: Int = 0,
    val deepProcessingMode: Boolean = false,
    val vaultPin: String = "",
    val vaultEmail: String = ""
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val SEEK_BUTTONS = booleanPreferencesKey("seek_buttons")
        val FORWARD_BACKWARD_DELAY = intPreferencesKey("forward_backward_delay")
        val LONG_TAP_DELAY = intPreferencesKey("long_tap_delay")
        val CONTROLS_HIDING_DELAY = intPreferencesKey("controls_hiding_delay")
        val VIDEOS_TRANSITION = booleanPreferencesKey("videos_transition")
        val LOCK_WITH_SENSOR = booleanPreferencesKey("lock_with_sensor")
        val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")
        val DOUBLE_TAP_DELAY = intPreferencesKey("double_tap_delay")
        val DOUBLE_TAP_TO_PLAY_PAUSE = booleanPreferencesKey("double_tap_to_play_pause")

        val VOLUME_GESTURE = booleanPreferencesKey("volume_gesture")
        val BRIGHTNESS_GESTURE = booleanPreferencesKey("brightness_gesture")
        val SAVE_BRIGHTNESS_LEVEL = booleanPreferencesKey("save_brightness_level")
        val SAVE_VOLUME_LEVEL = booleanPreferencesKey("save_volume_level")
        val SWIPE_TO_SEEK = booleanPreferencesKey("swipe_to_seek")
        val TWO_FINGER_ZOOM = booleanPreferencesKey("two_finger_zoom")
        val DOUBLE_TAP_TO_SEEK = booleanPreferencesKey("double_tap_to_seek")
        val SWIPE_GESTURE_SENSITIVITY = floatPreferencesKey("swipe_gesture_sensitivity")

        val ENABLE_AUDIO_EQUALIZER = booleanPreferencesKey("enable_audio_equalizer")
        val ACOUSTIC_EQUALIZER_PRESET = stringPreferencesKey("acoustic_equalizer_preset")
        val BAND_60HZ = floatPreferencesKey("band_60hz")
        val BAND_230HZ = floatPreferencesKey("band_230hz")
        val BAND_910HZ = floatPreferencesKey("band_910hz")
        val BAND_4KHZ = floatPreferencesKey("band_4khz")
        val BAND_14KHZ = floatPreferencesKey("band_14khz")

        val DISPLAY_MODE_LAYOUT = stringPreferencesKey("display_mode_layout")
        val SHOW_ONLY_FAVORITES = booleanPreferencesKey("show_only_favorites")
        val DEFAULT_PLAYBACK_TAP_ACTION = stringPreferencesKey("default_playback_tap_action")
        val SORT_MEDIA_ASSETS_BY = stringPreferencesKey("sort_media_assets_by")
        val SORT_DIRECTION_ORDER = stringPreferencesKey("sort_direction_order")
        val GROUP_MEDIA_ITEMS_BY = stringPreferencesKey("group_media_items_by")

        val AUTO_RESCAN = booleanPreferencesKey("auto_rescan")
        val FILE_ACCESS_MODE = stringPreferencesKey("file_access_mode")
        val TUNNELED_PLAYBACK = booleanPreferencesKey("tunneled_playback")

        // Legacy fields mapping
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val VOLUME = intPreferencesKey("volume")
        val ORIENTATION = intPreferencesKey("orientation")
        val IS_GRID_MODE = booleanPreferencesKey("is_grid_mode")
        val SORT_ORDER = intPreferencesKey("sort_order")
        val DEEP_PROCESSING_MODE = booleanPreferencesKey("deep_processing_mode")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val VAULT_EMAIL = stringPreferencesKey("vault_email")
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
            UserSettings(
                seekButtons = preferences[Keys.SEEK_BUTTONS] ?: true,
                forwardBackwardDelay = preferences[Keys.FORWARD_BACKWARD_DELAY] ?: 3,
                longTapDelay = preferences[Keys.LONG_TAP_DELAY] ?: 4,
                controlsHidingDelay = preferences[Keys.CONTROLS_HIDING_DELAY] ?: 3,
                videosTransition = preferences[Keys.VIDEOS_TRANSITION] ?: true,
                lockWithSensor = preferences[Keys.LOCK_WITH_SENSOR] ?: false,
                screenOrientation = preferences[Keys.SCREEN_ORIENTATION] ?: "Auto / Sensor",
                doubleTapDelay = preferences[Keys.DOUBLE_TAP_DELAY] ?: 3,
                doubleTapToPlayPause = preferences[Keys.DOUBLE_TAP_TO_PLAY_PAUSE] ?: true,

                volumeGesture = preferences[Keys.VOLUME_GESTURE] ?: true,
                brightnessGesture = preferences[Keys.BRIGHTNESS_GESTURE] ?: true,
                saveBrightnessLevel = preferences[Keys.SAVE_BRIGHTNESS_LEVEL] ?: true,
                saveVolumeLevel = preferences[Keys.SAVE_VOLUME_LEVEL] ?: true,
                swipeToSeek = preferences[Keys.SWIPE_TO_SEEK] ?: true,
                twoFingerZoom = preferences[Keys.TWO_FINGER_ZOOM] ?: true,
                doubleTapToSeek = preferences[Keys.DOUBLE_TAP_TO_SEEK] ?: true,
                swipeGestureSensitivity = preferences[Keys.SWIPE_GESTURE_SENSITIVITY] ?: 1.0f,

                enableAudioEqualizer = preferences[Keys.ENABLE_AUDIO_EQUALIZER] ?: false,
                acousticEqualizerPreset = preferences[Keys.ACOUSTIC_EQUALIZER_PRESET] ?: "Flat",
                band60Hz = preferences[Keys.BAND_60HZ] ?: 0.0f,
                band230Hz = preferences[Keys.BAND_230HZ] ?: 0.0f,
                band910Hz = preferences[Keys.BAND_910HZ] ?: 0.0f,
                band4kHz = preferences[Keys.BAND_4KHZ] ?: 0.0f,
                band14kHz = preferences[Keys.BAND_14KHZ] ?: 0.0f,

                displayModeLayout = preferences[Keys.DISPLAY_MODE_LAYOUT] ?: "Sleek Grid View",
                showOnlyFavorites = preferences[Keys.SHOW_ONLY_FAVORITES] ?: false,
                defaultPlaybackTapAction = preferences[Keys.DEFAULT_PLAYBACK_TAP_ACTION] ?: "Play Entire List Sequentially",
                sortMediaAssetsBy = preferences[Keys.SORT_MEDIA_ASSETS_BY] ?: "Media Title / Alphabetical",
                sortDirectionOrder = preferences[Keys.SORT_DIRECTION_ORDER] ?: "Ascending",
                groupMediaItemsBy = preferences[Keys.GROUP_MEDIA_ITEMS_BY] ?: "Group by Folder",

                autoRescan = preferences[Keys.AUTO_RESCAN] ?: true,
                fileAccessMode = preferences[Keys.FILE_ACCESS_MODE] ?: "MediaStore (Recommended/Default)",
                tunneledPlayback = preferences[Keys.TUNNELED_PLAYBACK] ?: false,

                brightness = preferences[Keys.BRIGHTNESS] ?: -1.0f,
                volume = preferences[Keys.VOLUME] ?: 50,
                orientation = preferences[Keys.ORIENTATION] ?: 0,
                isGridMode = preferences[Keys.IS_GRID_MODE] ?: false,
                sortOrder = preferences[Keys.SORT_ORDER] ?: 0,
                deepProcessingMode = preferences[Keys.DEEP_PROCESSING_MODE] ?: false,
                vaultPin = preferences[Keys.VAULT_PIN] ?: "",
                vaultEmail = preferences[Keys.VAULT_EMAIL] ?: ""
            )
        }
        
    suspend fun <T> updateSetting(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun updateBrightness(brightness: Float) = updateSetting(Keys.BRIGHTNESS, brightness)
    suspend fun updateVolume(volume: Int) = updateSetting(Keys.VOLUME, volume)
    suspend fun updateOrientation(orientation: Int) = updateSetting(Keys.ORIENTATION, orientation)
    suspend fun updateIsGridMode(isGridMode: Boolean) = updateSetting(Keys.IS_GRID_MODE, isGridMode)
    suspend fun updateSortOrder(sortOrder: Int) = updateSetting(Keys.SORT_ORDER, sortOrder)
    suspend fun updateDeepProcessingMode(enabled: Boolean) = updateSetting(Keys.DEEP_PROCESSING_MODE, enabled)
    suspend fun updateVaultPin(pin: String) = updateSetting(Keys.VAULT_PIN, pin)
    suspend fun updateVaultEmail(email: String) = updateSetting(Keys.VAULT_EMAIL, email)
    
    // Auto-generated accessors for UI
    suspend fun updateSeekButtons(v: Boolean) = updateSetting(Keys.SEEK_BUTTONS, v)
    suspend fun updateForwardBackwardDelay(v: Int) = updateSetting(Keys.FORWARD_BACKWARD_DELAY, v)
    suspend fun updateLongTapDelay(v: Int) = updateSetting(Keys.LONG_TAP_DELAY, v)
    suspend fun updateControlsHidingDelay(v: Int) = updateSetting(Keys.CONTROLS_HIDING_DELAY, v)
    suspend fun updateVideosTransition(v: Boolean) = updateSetting(Keys.VIDEOS_TRANSITION, v)
    suspend fun updateLockWithSensor(v: Boolean) = updateSetting(Keys.LOCK_WITH_SENSOR, v)
    suspend fun updateScreenOrientation(v: String) = updateSetting(Keys.SCREEN_ORIENTATION, v)
    suspend fun updateDoubleTapDelay(v: Int) = updateSetting(Keys.DOUBLE_TAP_DELAY, v)
    suspend fun updateDoubleTapToPlayPause(v: Boolean) = updateSetting(Keys.DOUBLE_TAP_TO_PLAY_PAUSE, v)

    suspend fun updateVolumeGesture(v: Boolean) = updateSetting(Keys.VOLUME_GESTURE, v)
    suspend fun updateBrightnessGesture(v: Boolean) = updateSetting(Keys.BRIGHTNESS_GESTURE, v)
    suspend fun updateSaveBrightnessLevel(v: Boolean) = updateSetting(Keys.SAVE_BRIGHTNESS_LEVEL, v)
    suspend fun updateSaveVolumeLevel(v: Boolean) = updateSetting(Keys.SAVE_VOLUME_LEVEL, v)
    suspend fun updateSwipeToSeek(v: Boolean) = updateSetting(Keys.SWIPE_TO_SEEK, v)
    suspend fun updateTwoFingerZoom(v: Boolean) = updateSetting(Keys.TWO_FINGER_ZOOM, v)
    suspend fun updateDoubleTapToSeek(v: Boolean) = updateSetting(Keys.DOUBLE_TAP_TO_SEEK, v)
    suspend fun updateSwipeGestureSensitivity(v: Float) = updateSetting(Keys.SWIPE_GESTURE_SENSITIVITY, v)

    suspend fun updateEnableAudioEqualizer(v: Boolean) = updateSetting(Keys.ENABLE_AUDIO_EQUALIZER, v)
    suspend fun updateAcousticEqualizerPreset(v: String) = updateSetting(Keys.ACOUSTIC_EQUALIZER_PRESET, v)
    suspend fun updateBand60Hz(v: Float) = updateSetting(Keys.BAND_60HZ, v)
    suspend fun updateBand230Hz(v: Float) = updateSetting(Keys.BAND_230HZ, v)
    suspend fun updateBand910Hz(v: Float) = updateSetting(Keys.BAND_910HZ, v)
    suspend fun updateBand4kHz(v: Float) = updateSetting(Keys.BAND_4KHZ, v)
    suspend fun updateBand14kHz(v: Float) = updateSetting(Keys.BAND_14KHZ, v)

    suspend fun updateDisplayModeLayout(v: String) = updateSetting(Keys.DISPLAY_MODE_LAYOUT, v)
    suspend fun updateShowOnlyFavorites(v: Boolean) = updateSetting(Keys.SHOW_ONLY_FAVORITES, v)
    suspend fun updateDefaultPlaybackTapAction(v: String) = updateSetting(Keys.DEFAULT_PLAYBACK_TAP_ACTION, v)
    suspend fun updateSortMediaAssetsBy(v: String) = updateSetting(Keys.SORT_MEDIA_ASSETS_BY, v)
    suspend fun updateSortDirectionOrder(v: String) = updateSetting(Keys.SORT_DIRECTION_ORDER, v)
    suspend fun updateGroupMediaItemsBy(v: String) = updateSetting(Keys.GROUP_MEDIA_ITEMS_BY, v)

    suspend fun updateAutoRescan(v: Boolean) = updateSetting(Keys.AUTO_RESCAN, v)
    suspend fun updateFileAccessMode(v: String) = updateSetting(Keys.FILE_ACCESS_MODE, v)
    suspend fun updateTunneledPlayback(v: Boolean) = updateSetting(Keys.TUNNELED_PLAYBACK, v)
}
