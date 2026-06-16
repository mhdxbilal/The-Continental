package com.example

import android.app.Application
import androidx.room.Room
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import coil3.video.VideoFrameDecoder
import okio.Path.Companion.toOkioPath

class TheContinentalApp : Application(), SingletonImageLoader.Factory {
    lateinit var database: MediaDatabase
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            MediaDatabase::class.java,
            "media_database"
        ).fallbackToDestructiveMigration().build()
        userPreferencesRepository = UserPreferencesRepository(dataStore)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }
}
