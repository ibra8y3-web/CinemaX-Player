package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.LiveChannelDao
import com.example.data.dao.VideoDao
import com.example.data.model.BookmarkEntity
import com.example.data.model.DownloadTaskEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.LiveStreamEntity
import com.example.data.model.PlayHistoryEntity
import com.example.data.model.VaultItemEntity

@Database(
    entities = [
        PlayHistoryEntity::class,
        BookmarkEntity::class,
        FavoriteEntity::class,
        VaultItemEntity::class,
        DownloadTaskEntity::class,
        LiveStreamEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun liveChannelDao(): LiveChannelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nova_video_player.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
