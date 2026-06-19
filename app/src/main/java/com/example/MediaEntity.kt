package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val uri: String,
    val durationMs: Long = 0,
    val fileSize: Long = 0,
    val mimeType: String = "video/mp4",
    val folderName: String = "Download",
    val isPrivate: Boolean = false,
    val playlistName: String = "", // Playlist name relationship (e.g. "ggdea")
    val isDownloaded: Boolean = true, // true if finished, false if downloading
    val downloadProgress: Float = 1.0f, // 0.0 to 1.0
    val downloadSpeedText: String = "", // e.g., "18.3 MB/s"
    val isAudioOnly: Boolean = false,
    val videoResolution: String = "1080p",
    val thumbnailUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

