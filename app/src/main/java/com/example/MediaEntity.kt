package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val uri: String,
    val durationMs: Long = 0,
    val mimeType: String = ""
)
