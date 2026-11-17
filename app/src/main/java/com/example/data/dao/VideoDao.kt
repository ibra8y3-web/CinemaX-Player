package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BookmarkEntity
import com.example.data.model.DownloadTaskEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.PlayHistoryEntity
import com.example.data.model.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    // History
    @Query("SELECT * FROM play_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY lastPlayedTimestamp DESC")
    suspend fun getAllHistoryList(): List<PlayHistoryEntity>

    @Query("SELECT * FROM play_history WHERE videoUri = :uri LIMIT 1")
    suspend fun getHistoryForUri(uri: String): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHistory(history: PlayHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHistory(histories: List<PlayHistoryEntity>)

    @Query("DELETE FROM play_history WHERE videoUri = :uri")
    suspend fun deleteHistory(uri: String)

    @Query("DELETE FROM play_history")
    suspend fun clearAllHistory()

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY dateAdded DESC")
    suspend fun getAllFavoritesList(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoUri = :uri)")
    fun isFavorite(uri: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE videoUri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE videoUri = :uri ORDER BY timestampMs ASC")
    fun getBookmarksForVideo(uri: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarksList(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

    // Vault
    @Query("SELECT * FROM vault_items ORDER BY encryptedAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items ORDER BY encryptedAt DESC")
    suspend fun getAllVaultItemsList(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVaultItems(items: List<VaultItemEntity>)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: String)

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsList(): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(task: DownloadTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDownloads(tasks: List<DownloadTaskEntity>)

    @Query("UPDATE downloads SET progressPercent = :progress, status = :status WHERE id = :id")
    suspend fun updateDownloadProgress(id: Long, progress: Int, status: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)
}
