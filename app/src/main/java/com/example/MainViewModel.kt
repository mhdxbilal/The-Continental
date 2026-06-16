package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val mediaDao: MediaDao,
    private val userPrefs: UserPreferencesRepository
) : AndroidViewModel(application) {

    val userSettings = userPrefs.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings(-1.0f, 50, 0, false, 0, false, "", "")
    )

    // Flow lists directly from secure DB
    val publicMedia = mediaDao.getAllPublicMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privateMedia = mediaDao.getAllPrivateMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads = mediaDao.getActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Equalizer levels
    private val _equalizerBand1 = MutableStateFlow(0.0f)
    val equalizerBand1 = _equalizerBand1.asStateFlow()

    private val _equalizerBand2 = MutableStateFlow(0.0f)
    val equalizerBand2 = _equalizerBand2.asStateFlow()

    private val _equalizerBand3 = MutableStateFlow(0.0f)
    val equalizerBand3 = _equalizerBand3.asStateFlow()

    private val _equalizerBand4 = MutableStateFlow(0.0f)
    val equalizerBand4 = _equalizerBand4.asStateFlow()

    private val _equalizerBand5 = MutableStateFlow(0.0f)
    val equalizerBand5 = _equalizerBand5.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("Flat")
    val equalizerPreset = _equalizerPreset.asStateFlow()

    fun updateBand1(v: Float) { _equalizerBand1.value = v; _equalizerPreset.value = "Custom" }
    fun updateBand2(v: Float) { _equalizerBand2.value = v; _equalizerPreset.value = "Custom" }
    fun updateBand3(v: Float) { _equalizerBand3.value = v; _equalizerPreset.value = "Custom" }
    fun updateBand4(v: Float) { _equalizerBand4.value = v; _equalizerPreset.value = "Custom" }
    fun updateBand5(v: Float) { _equalizerBand5.value = v; _equalizerPreset.value = "Custom" }

    fun applyPreset(preset: String) {
        _equalizerPreset.value = preset
        when (preset) {
            "Flat" -> {
                _equalizerBand1.value = 0.0f; _equalizerBand2.value = 0.0f; _equalizerBand3.value = 0.0f; _equalizerBand4.value = 0.0f; _equalizerBand5.value = 0.0f
            }
            "Bass Boost" -> {
                _equalizerBand1.value = 0.8f; _equalizerBand2.value = 0.6f; _equalizerBand3.value = 0.0f; _equalizerBand4.value = 0.0f; _equalizerBand5.value = -0.2f
            }
            "Vocal" -> {
                _equalizerBand1.value = -0.4f; _equalizerBand2.value = 0.1f; _equalizerBand3.value = 0.7f; _equalizerBand4.value = 0.5f; _equalizerBand5.value = -0.1f
            }
            "Rock" -> {
                _equalizerBand1.value = 0.5f; _equalizerBand2.value = 0.3f; _equalizerBand3.value = -0.2f; _equalizerBand4.value = 0.2f; _equalizerBand5.value = 0.6f
            }
            "Jazz" -> {
                _equalizerBand1.value = 0.3f; _equalizerBand2.value = 0.2f; _equalizerBand3.value = 0.1f; _equalizerBand4.value = 0.4f; _equalizerBand5.value = 0.2f
            }
        }
    }

    // Playback state parameters
    val isLooping = MutableStateFlow(false)
    val playbackSpeed = MutableStateFlow(1.0f)
    val activeDecoderMode = MutableStateFlow("HW+ Decoder") // HW, HW+, SW
    val selectedAspectRatio = MutableStateFlow("Default") // Default, 16:9, 4:3, 1:1
    val sleepTimerLeftMinutes = MutableStateFlow(0) // 0 means offline active. Options: 10, 15, 20, 30, 60 minutes.

    init {
        // Automatically pre-populate simulated assets on boot if library database empty
        viewModelScope.launch(Dispatchers.IO) {
            publicMedia.take(1).collect { currentList ->
                if (currentList.isEmpty()) {
                    val mockItems = listOf(
                        MediaEntity(
                            title = "Rebel Songs _ Google Searchlona Full Video Song _ Tamanna _ Latest Telugu Superhits_SriBala...080P_HD)",
                            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            durationMs = 268000,
                            fileSize = 139354000,
                            mimeType = "video/mp4",
                            folderName = "Media Download",
                            thumbnailUri = "https://picsum.photos/id/10/400/250",
                            isDownloaded = true
                        ),
                        MediaEntity(
                            title = "Aaj Ki Raat - 8K Video _ Stree 2 _ Tamannaah Bhatia _ Sachin-Jigar",
                            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                            durationMs = 195000,
                            fileSize = 48500000,
                            mimeType = "video/mp4",
                            folderName = "Movies",
                            thumbnailUri = "https://picsum.photos/id/20/400/250",
                            isDownloaded = true
                        ),
                        MediaEntity(
                            title = "tamannabhatiaoffl-20260531-0001 Local Camera Recording",
                            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            durationMs = 70000,
                            fileSize = 12500000,
                            mimeType = "video/mp4",
                            folderName = "Camera",
                            thumbnailUri = "https://picsum.photos/id/30/400/250",
                            isDownloaded = true
                        ),
                        MediaEntity(
                            title = "chassis.io-20260530-0001 Network Stream Analytics Log",
                            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                            durationMs = 150000,
                            fileSize = 34500000,
                            mimeType = "video/mp4",
                            folderName = "Media Download",
                            thumbnailUri = "https://picsum.photos/id/40/400/250",
                            isDownloaded = true
                        )
                    )
                    mockItems.forEach { mediaDao.insertMedia(it) }
                }
            }
        }
    }

    // Downloader system Simulator
    fun triggerDownload(url: String, title: String, isAudio: Boolean, resolution: String, fileSizeMb: Double) {
        val calculatedBytes = (fileSizeMb * 1024 * 1024).toLong()
        val targetFolderName = if (isAudio) "Music Download" else "Media Download"
        val fallbackTitle = title.ifEmpty { "Media_" + System.currentTimeMillis() }

        viewModelScope.launch(Dispatchers.IO) {
            val downloadItem = MediaEntity(
                title = fallbackTitle,
                uri = url.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                durationMs = 180000,
                fileSize = calculatedBytes,
                mimeType = if (isAudio) "audio/mp3" else "video/mp4",
                folderName = targetFolderName,
                isDownloaded = false,
                downloadProgress = 0f,
                downloadSpeedText = "Initializing...",
                isAudioOnly = isAudio,
                videoResolution = resolution,
                thumbnailUri = if (isAudio) "https://picsum.photos/id/150/400/250" else "https://picsum.photos/id/111/400/250"
            )

            mediaDao.insertMedia(downloadItem)

            // Dynamic live progress updates
            val simulationSteps = listOf(
                Pair(0.15f, "18.3 MB/s"),
                Pair(0.46f, "35.4 MB/s"),
                Pair(0.78f, "28.9 MB/s"),
                Pair(0.94f, "14.2 MB/s"),
                Pair(0.96f, "3.0 MB/s"),
                Pair(0.99f, "Converting...")
            )

            // Flow simulator
            var currentMedia = downloadItem
            // Fetch the assigned ID by querying DB
            var dbId = 0
            mediaDao.getAllPublicMedia().take(1).collect { list ->
                val highest = list.maxByOrNull { it.id }
                if (highest != null) {
                    dbId = highest.id
                }
            }
            if (dbId == 0) dbId = (Math.random() * 1000).toInt() + 5

            currentMedia = currentMedia.copy(id = dbId)

            for (step in simulationSteps) {
                delay(1200)
                currentMedia = currentMedia.copy(
                    downloadProgress = step.first,
                    downloadSpeedText = step.second
                )
                mediaDao.insertMedia(currentMedia)
            }

            delay(1500)
            // Finished
            currentMedia = currentMedia.copy(
                isDownloaded = true,
                downloadProgress = 1.0f,
                downloadSpeedText = ""
            )
            mediaDao.insertMedia(currentMedia)
        }
    }

    // Vault and PIN locks
    fun setupVaultPIN(pin: String) {
        viewModelScope.launch {
            userPrefs.updateVaultPin(pin)
        }
    }

    fun setupVaultEmail(email: String) {
        viewModelScope.launch {
            userPrefs.updateVaultEmail(email)
        }
    }

    fun toggleFileVault(media: MediaEntity, makePrivate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = media.copy(isPrivate = makePrivate)
            mediaDao.insertMedia(updated)
        }
    }

    fun removeFile(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaDao.deleteMedia(id)
        }
    }

    // Playlist systems
    fun setFilePlaylist(media: MediaEntity, playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = media.copy(playlistName = playlistName)
            mediaDao.insertMedia(updated)
        }
    }

    class Factory(
        private val application: Application,
        private val mediaDao: MediaDao,
        private val userPrefs: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, mediaDao, userPrefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
