package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun liveChannelDao(): LiveChannelDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE live_channels ADD COLUMN logoUrl TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nova_video_player.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
