package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

fun LazyListScope.controlsTab(settings: UserSettings, repo: UserPreferencesRepository, scope: CoroutineScope) {
    item {
        SettingsCheckboxItem(
            title = "Seek buttons",
            subtitle = "Show rewind and fast forward buttons on the video interface",
            isChecked = settings.seekButtons,
            onCheckedChange = { scope.launch { repo.updateSeekButtons(it) } }
        )
        SettingsTextValueItem("Forward/backward time delay", "${settings.forwardBackwardDelay} seconds")
        SettingsTextValueItem("Long tap forward/backward time delay", "${settings.longTapDelay} seconds")
        
        SettingsSliderItem(
            title = "Video player controls hiding delay",
            value = settings.controlsHidingDelay.toFloat(),
            valueRange = 1f..10f,
            valueSuffix = "s",
            onValueChange = { scope.launch { repo.updateControlsHidingDelay(it.toInt()) } }
        )
        
        SettingsCheckboxItem(
            title = "Videos transition",
            subtitle = "Show new video title on transition",
            isChecked = settings.videosTransition,
            onCheckedChange = { scope.launch { repo.updateVideosTransition(it) } }
        )
        
        SettingsCheckboxItem(
            title = "Lock with sensor",
            subtitle = "When the orientation is locked, use the sensor to allow the reverse orientation",
            isChecked = settings.lockWithSensor,
            onCheckedChange = { scope.launch { repo.updateLockWithSensor(it) } }
        )
        
        SettingsTextValueItem("Screen Orientation", settings.screenOrientation)
        SettingsTextValueItem("Double tap time delay", "${settings.doubleTapDelay} seconds")
        
        SettingsCheckboxItem(
            title = "Double tap to play/pause",
            subtitle = "Double tap on screen center to play or pause",
            isChecked = settings.doubleTapToPlayPause,
            onCheckedChange = { scope.launch { repo.updateDoubleTapToPlayPause(it) } }
        )
    }
}

fun LazyListScope.gesturesTab(settings: UserSettings, repo: UserPreferencesRepository, scope: CoroutineScope) {
    item {
        SettingsCheckboxItem(
            title = "Volume gesture",
            subtitle = "Control volume by gesture during video playback",
            isChecked = settings.volumeGesture,
            onCheckedChange = { scope.launch { repo.updateVolumeGesture(it) } }
        )
        SettingsCheckboxItem(
            title = "Brightness gesture",
            subtitle = "Control brightness by gesture during video playback",
            isChecked = settings.brightnessGesture,
            onCheckedChange = { scope.launch { repo.updateBrightnessGesture(it) } }
        )
        SettingsCheckboxItem(
            title = "Save brightness level",
            subtitle = "Keep brightness level between media",
            isChecked = settings.saveBrightnessLevel,
            onCheckedChange = { scope.launch { repo.updateSaveBrightnessLevel(it) } }
        )
        SettingsCheckboxItem(
            title = "Save volume level",
            subtitle = "Keep volume level consistent when opening media",
            isChecked = settings.saveVolumeLevel,
            onCheckedChange = { scope.launch { repo.updateSaveVolumeLevel(it) } }
        )
        SettingsCheckboxItem(
            title = "Swipe to seek",
            subtitle = "Swipe your finger across the screen to seek",
            isChecked = settings.swipeToSeek,
            onCheckedChange = { scope.launch { repo.updateSwipeToSeek(it) } }
        )
        SettingsCheckboxItem(
            title = "Two finger zoom",
            subtitle = "Zoom in and out with two fingers",
            isChecked = settings.twoFingerZoom,
            onCheckedChange = { scope.launch { repo.updateTwoFingerZoom(it) } }
        )
        SettingsCheckboxItem(
            title = "Double tap to seek",
            subtitle = "Double tap on screen edges to seek by 10 seconds",
            isChecked = settings.doubleTapToSeek,
            onCheckedChange = { scope.launch { repo.updateDoubleTapToSeek(it) } }
        )
        SettingsSliderItem(
            title = "Swipe gesture sensitivity",
            value = settings.swipeGestureSensitivity,
            valueRange = 0.1f..3.0f,
            valueSuffix = "x",
            isFloat = true,
            onValueChange = { scope.launch { repo.updateSwipeGestureSensitivity(it) } }
        )
    }
}

fun LazyListScope.equalizerTab(settings: UserSettings, repo: UserPreferencesRepository, scope: CoroutineScope) {
    item {
        SettingsCheckboxItem(
            title = "Enable Audio Equalizer",
            subtitle = "Apply custom audio frequency filters during media playback",
            isChecked = settings.enableAudioEqualizer,
            onCheckedChange = { scope.launch { repo.updateEnableAudioEqualizer(it) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Acoustic Equalizer Presets", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal list of presets
        Row(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val presets = listOf("Flat", "Bass Boost", "Vocal", "Rock", "Jazz")
            presets.forEach { preset ->
                val isSelected = settings.acousticEqualizerPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) CyberCyan else Color(0xFF1E2A2E))
                        .clickable { scope.launch { repo.updateAcousticEqualizerPreset(preset) } }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(preset, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("5-Band Graphic Equalizer", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(24.dp))
        
        // Equalizer bands (vertical sliders in a row)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EqBand(label = "60 Hz")
            EqBand(label = "230 Hz")
            EqBand(label = "910 Hz")
            EqBand(label = "4 kHz")
            EqBand(label = "14 kHz")
        }
    }
}

fun LazyListScope.displayAndSortTab(settings: UserSettings, repo: UserPreferencesRepository, scope: CoroutineScope) {
    item {
        SettingsTextValueItem("Display Mode Layout", settings.displayModeLayout)
        SettingsCheckboxItem(
            title = "Show Only Favorites",
            subtitle = "Filter and view only key-marked favorite media assets on the dashboard",
            isChecked = settings.showOnlyFavorites,
            onCheckedChange = { scope.launch { repo.updateShowOnlyFavorites(it) } }
        )
        SettingsTextValueItem("Default Playback Tap Action", settings.defaultPlaybackTapAction)
        SettingsTextValueItem("Sort Media Assets By", settings.sortMediaAssetsBy)
        SettingsTextValueItem("Sort Direction Order", settings.sortDirectionOrder)
        SettingsTextValueItem("Group Media Items By", settings.groupMediaItemsBy)
    }
}

fun LazyListScope.libraryTab(settings: UserSettings, repo: UserPreferencesRepository, scope: CoroutineScope) {
    item {
        Text("Media Library", color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        SettingsTextValueItem("Media library folders", "Select directories to include in the media library", CyberCyan)
        SettingsTextValueItem("Excluded Directories", "Specify folders to ignore during media scanning", CyberCyan)
        SettingsTextValueItem("Hidden Directories", "Manage folders that are hidden on the UI dashboards", CyberCyan)
        SettingsTextValueItem("Storage Permission Status", "Manage access to device storage for media scanning", CyberCyan)
        
        SettingsCheckboxItem(
            title = "Auto rescan",
            subtitle = "Automatically scan device for new or deleted media at application startup",
            isChecked = settings.autoRescan,
            onCheckedChange = { scope.launch { repo.updateAutoRescan(it) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Manual Scanning Shortcuts", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp))
        SettingsTextValueItem("Run Automated Storage Sync", "Instantly scan local files on your internal storage", CyberCyan)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Advanced Playback Decoders", color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        SettingsTextValueItem("File Access Mode", settings.fileAccessMode, CyberCyan)
        
        SettingsCheckboxItem(
            title = "Tunneled Playback",
            subtitle = "Enable hardware video tunneling to optimize playbacks of 4K/HDR content on supported TVs",
            isChecked = settings.tunneledPlayback,
            onCheckedChange = { scope.launch { repo.updateTunneledPlayback(it) } }
        )
    }
}

fun LazyListScope.downloaderTab(settings: UserSettings, repo: UserPreferencesRepository, scope: CoroutineScope) {
    item {
        Text("Downloader Settings", color = CyberCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
        
        SettingsTextInputItem(
            title = "Backend URL",
            value = settings.downloaderBackendUrl,
            onValueChange = { scope.launch { repo.updateDownloaderBackendUrl(it) } }
        )
        
        SettingsCheckboxItem(
            title = "Auto Start Downloader",
            subtitle = "Automatically start downloading queued items",
            isChecked = settings.autoStartDownloader,
            onCheckedChange = { scope.launch { repo.updateAutoStartDownloader(it) } }
        )

        SettingsSliderItem(
            title = "Simultaneous Downloads",
            value = settings.downloadSimultaneously.toFloat(),
            valueRange = 1f..10f,
            valueSuffix = " files",
            onValueChange = { scope.launch { repo.updateDownloadSimultaneously(it.toInt()) } }
        )

        SettingsCheckboxItem(
            title = "Download Over WiFi Only",
            subtitle = "Pause downloads when on cellular data",
            isChecked = settings.downloadOverWifiOnly,
            onCheckedChange = { scope.launch { repo.updateDownloadOverWifiOnly(it) } }
        )
    }
}


@Composable
fun EqBand(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("+0.0", color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))
        // Simulated vertical slider
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E2A2E)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.DarkGray)
                    .border(4.dp, Color.LightGray, androidx.compose.foundation.shape.CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun SettingsCheckboxItem(title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        // Custom Checkbox mapping to the screenshot
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(2.dp, if (isChecked) CyberCyan else Color.Gray, RoundedCornerShape(4.dp))
                .background(if (isChecked) CyberCyan else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp)) 
                // Alternatively use a Check icon, but the screenshot might be using checkboxes.
            }
        }
    }
}

@Composable
fun SettingsTextValueItem(title: String, value: String, valueColor: Color = CyberCyan) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 12.sp)
    }
}

@Composable
fun SettingsTextInputItem(title: String, value: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
    }
}

@Composable
fun SettingsSliderItem(title: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, valueSuffix: String, isFloat: Boolean = false, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = Color(0xFF1E2A2E)
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                if (isFloat) String.format("%.1f$valueSuffix", value) else "${value.toInt()}$valueSuffix", 
                color = CyberCyan, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}
