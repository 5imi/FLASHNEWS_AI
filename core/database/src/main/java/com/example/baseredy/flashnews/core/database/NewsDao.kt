package com.example.baseredy.flashnews.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles WHERE region = :region AND category = :category ORDER BY publishedAt DESC")
    fun getArticlesByRegionAndCategory(region: String, category: String): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles WHERE region = :region ORDER BY publishedAt DESC")
    fun getArticlesByRegion(region: String): Flow<List<NewsArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("SELECT * FROM news_articles WHERE isFavorite = 1 ORDER BY publishedAt DESC")
    fun getFavoriteArticles(): Flow<List<NewsArticleEntity>>

    @Query("UPDATE news_articles SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean)

    @Query("SELECT isFavorite FROM news_articles WHERE url = :url")
    suspend fun isArticleFavorite(url: String): Boolean?

    @Query("SELECT * FROM news_articles WHERE url = :url LIMIT 1")
    suspend fun getArticleByUrl(url: String): NewsArticleEntity?
}
