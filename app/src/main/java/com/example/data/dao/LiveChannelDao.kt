package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LiveStreamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveChannelDao {

    @Query("SELECT * FROM live_channels ORDER BY isFavorite DESC, category ASC, id ASC")
    fun getAllChannelsFlow(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_channels")
    suspend fun getAllChannelsSync(): List<LiveStreamEntity>

    @Query("SELECT COUNT(*) FROM live_channels")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<LiveStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(channels: List<LiveStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: LiveStreamEntity)

    @Query("DELETE FROM live_channels WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM live_channels WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    @Query("DELETE FROM live_channels WHERE isCustom = 0")
    suspend fun deleteCatalogChannels(): Int

    @Query("DELETE FROM live_channels WHERE failureCount >= :maxFailures")
    suspend fun deleteDeadChannels(maxFailures: Int = 1): Int

    @Query("UPDATE live_channels SET isHealthy = :isHealthy, failureCount = :failureCount, lastCheckedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateHealth(id: String, isHealthy: Boolean, failureCount: Int, timestamp: Long)

    @Query("UPDATE live_channels SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("DELETE FROM live_channels")
    suspend fun clearAll()
}
