package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items")
    fun getAllMedia(): Flow<List<MediaEntity>>
    
    @Query("SELECT * FROM media_items WHERE durationMs > 1200000") // > 20 mins
    fun getLongFormContent(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items ORDER BY id DESC LIMIT 50")
    fun getRecentAdditions(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMedia(id: Int)
}
