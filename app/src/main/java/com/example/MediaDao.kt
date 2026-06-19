package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE isPrivate = 0")
    fun getAllPublicMedia(): Flow<List<MediaEntity>>
    
    @Query("SELECT * FROM media_items WHERE isPrivate = 1")
    fun getAllPrivateMedia(): Flow<List<MediaEntity>>
    
    @Query("SELECT * FROM media_items")
    fun getAllMedia(): Flow<List<MediaEntity>>
    
    @Query("SELECT * FROM media_items WHERE isDownloaded = 0")
    fun getActiveDownloads(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity): Long

    @Update
    suspend fun updateMedia(media: MediaEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: Int)

    @Query("DELETE FROM media_items")
    suspend fun clearAllMedia()
}

