package com.example

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MediaStoreVideo(
    val id: Long,
    val title: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val folderPath: String
)

class MainViewModel(
    application: Application,
    private val mediaDao: MediaDao,
    private val userPrefs: UserPreferencesRepository
) : AndroidViewModel(application) {

    val sortOrderFlow = userPrefs.userSettingsFlow.map { it.sortOrder }.distinctUntilChanged()

    val mediaList = sortOrderFlow.map { sortOrder -> 
        fetchMediaStoreVideos(sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun fetchMediaStoreVideos(sortOrder: Int): List<MediaStoreVideo> {
        val videoList = mutableListOf<MediaStoreVideo>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        )

        val sortColumn = when(sortOrder) {
            0 -> MediaStore.Video.Media.DISPLAY_NAME + " ASC"
            1 -> MediaStore.Video.Media.SIZE + " DESC"
            2 -> MediaStore.Video.Media.DURATION + " DESC"
            else -> MediaStore.Video.Media.DISPLAY_NAME + " ASC"
        }

        val context = getApplication<Application>()
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortColumn
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val data = cursor.getString(dataColumn) ?: ""
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val folderPath = data.substringBeforeLast('/')

                videoList += MediaStoreVideo(id, name, uri, duration, size, folderPath)
            }
        }
        return videoList
    }

    private val _currentUrl = MutableStateFlow("")
    val currentUrl = _currentUrl.asStateFlow()

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    class Factory(private val application: Application, private val mediaDao: MediaDao, private val userPrefs: UserPreferencesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, mediaDao, userPrefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
