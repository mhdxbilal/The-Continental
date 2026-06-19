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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class MainViewModel(
    application: Application,
    private val mediaDao: MediaDao,
    val userPrefs: UserPreferencesRepository
) : AndroidViewModel(application) {

    val userSettings = userPrefs.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
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
                            folderName = "Download",
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
                            folderName = "Download",
                            thumbnailUri = "https://picsum.photos/id/40/400/250",
                            isDownloaded = true
                        )
                    )
                    mockItems.forEach { mediaDao.insertMedia(it) }
                }
            }
        }
    }

    private val client = okhttp3.OkHttpClient()
    private var webSocket: okhttp3.WebSocket? = null

    init {
        viewModelScope.launch {
            userPrefs.userSettingsFlow
                .map { it.downloaderBackendUrl }
                .distinctUntilChanged()
                .collect { newUrl ->
                    connectWebSocket(newUrl)
                }
        }
    }

    private fun connectWebSocket(backendUrl: String) {
        webSocket?.cancel() // close existing socket if any
        val url = backendUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws"
        val request = okhttp3.Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                try {
                    val json = org.json.JSONObject(text)
                    val type = json.optString("type")
                    if (type == "progress") {
                        val percentStr = json.optString("percent").replace("%", "").replace("\u001B\\[.*?m".toRegex(), "").trim()
                        val speedStr = json.optString("speed")
                        val url = json.optString("url")
                        
                        val speedTxt = speedStr + " - " + json.optString("eta")
                        val pct = percentStr.toFloatOrNull()?.div(100f) ?: 0f

                        viewModelScope.launch(Dispatchers.IO) {
                            val active = mediaDao.getActiveDownloads().first()
                            val match = active.find { it.uri == url }
                            if (match != null) {
                                mediaDao.insertMedia(match.copy(
                                    downloadProgress = pct,
                                    downloadSpeedText = speedTxt
                                ))
                            }
                        }
                    } else if (type == "finished") {
                        val url = json.optString("url")
                        viewModelScope.launch(Dispatchers.IO) {
                            val active = mediaDao.getActiveDownloads().first()
                            val match = active.find { it.uri == url }
                            if (match != null) {
                                mediaDao.insertMedia(match.copy(
                                    downloadProgress = 1.0f,
                                    downloadSpeedText = "Finished",
                                    isDownloaded = true
                                ))
                            }
                        }
                    } else if (type == "error") {
                        val url = json.optString("url")
                        viewModelScope.launch(Dispatchers.IO) {
                            val active = mediaDao.getActiveDownloads().first()
                            val match = active.find { it.uri == url }
                            if (match != null) {
                                mediaDao.insertMedia(match.copy(
                                    downloadSpeedText = "Error: " + json.optString("message"),
                                    isDownloaded = false
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    // Downloader system Simulator
    fun triggerDownload(url: String, title: String, isAudio: Boolean, resolution: String, fileSizeMb: Double, ytDlpFormat: String = "bv*+ba/b", customDirectory: String = "") {
        val calculatedBytes = (fileSizeMb * 1024 * 1024).toLong()
        val targetFolderName = if (customDirectory.isNotEmpty()) customDirectory.substringAfterLast("/") else "Download"
        val fallbackTitle = title.ifEmpty { "Media_" + System.currentTimeMillis() }

        viewModelScope.launch(Dispatchers.IO) {
            val downloadItem = MediaEntity(
                title = fallbackTitle,
                uri = url.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                durationMs = 0,
                fileSize = 0,
                mimeType = if (isAudio) "audio/mp3" else "video/mp4",
                folderName = targetFolderName,
                isDownloaded = false,
                downloadProgress = 0f,
                downloadSpeedText = "Queued...",
                isAudioOnly = isAudio,
                videoResolution = resolution,
                thumbnailUri = if (isAudio) "https://picsum.photos/id/150/400/250" else "https://picsum.photos/id/111/400/250"
            )

            mediaDao.insertMedia(downloadItem)

            // Trigger backend
            val jsonObject = org.json.JSONObject()
            val urlsArray = org.json.JSONArray()
            urlsArray.put(url)
            jsonObject.put("urls", urlsArray)
            jsonObject.put("format", ytDlpFormat)
            if (customDirectory.isNotEmpty()) {
                jsonObject.put("custom_directory", customDirectory)
            }
            
            if (isAudio) {
                val postArray = org.json.JSONArray()
                val postObj = org.json.JSONObject()
                postObj.put("key", "FFmpegExtractAudio")
                postObj.put("preferredcodec", "mp3")
                postObj.put("preferredquality", "192")
                postArray.put(postObj)
                jsonObject.put("postprocessors", postArray)
            } else {
                jsonObject.put("postprocessors", org.json.JSONArray())
            }

            val jsonString = jsonObject.toString()
            val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

            val urlDownload = userSettings.value.downloaderBackendUrl + "/download/"
            val request = okhttp3.Request.Builder()
                .url(urlDownload)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        println("Failed to trigger download: " + response.message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
