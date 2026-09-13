package com.example.baseredy.flashnews.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "custom_rss_feeds")
data class CustomRssFeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val category: String = "General",
    val region: String = "RO",
    val isFollowed: Boolean = true,
    val isCustomUrl: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface CustomRssFeedDao {
    @Query("SELECT * FROM custom_rss_feeds ORDER BY addedAt DESC")
    fun getAllFeeds(): Flow<List<CustomRssFeedEntity>>

    @Query("SELECT * FROM custom_rss_feeds WHERE isFollowed = 1")
    fun getFollowedFeeds(): Flow<List<CustomRssFeedEntity>>

    @Query("SELECT * FROM custom_rss_feeds WHERE isFollowed = 1")
    suspend fun getFollowedFeedsSync(): List<CustomRssFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(feed: CustomRssFeedEntity)

    @Query("UPDATE custom_rss_feeds SET isFollowed = :isFollowed WHERE url = :url")
    suspend fun updateFollowStatus(url: String, isFollowed: Boolean)

    @Query("DELETE FROM custom_rss_feeds WHERE url = :url")
    suspend fun deleteFeedByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM custom_rss_feeds WHERE url = :url AND isFollowed = 1)")
    suspend fun isFeedFollowed(url: String): Boolean

    @Query("SELECT url FROM custom_rss_feeds WHERE isFollowed = 1")
    suspend fun getFollowedUrls(): List<String>
}
