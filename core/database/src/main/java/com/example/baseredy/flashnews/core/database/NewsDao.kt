package com.example.baseredy.flashnews.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

import androidx.paging.PagingSource

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles WHERE region = :region AND category = :category ORDER BY publishedAt DESC")
    fun getArticlesByRegionAndCategory(region: String, category: String): PagingSource<Int, NewsArticleEntity>

    @Query("SELECT * FROM news_articles WHERE region = :region ORDER BY publishedAt DESC")
    fun getArticlesByRegion(region: String): PagingSource<Int, NewsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("SELECT * FROM news_articles WHERE isFavorite = 1 ORDER BY publishedAt DESC")
    fun getFavoriteArticles(): PagingSource<Int, NewsArticleEntity>

    @Query("UPDATE news_articles SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean)

    @Query("SELECT isFavorite FROM news_articles WHERE url = :url")
    suspend fun isArticleFavorite(url: String): Boolean

    @Query("SELECT * FROM news_articles WHERE url = :url LIMIT 1")
    suspend fun getArticleByUrl(url: String): NewsArticleEntity?

    @Query("UPDATE news_articles SET aiSummary = :summary, aiBias = :bias, aiLocalImpact = :impact, aiAnalyzedAt = :analyzedAt WHERE url = :url")
    suspend fun updateAiAnalysis(url: String, summary: String?, bias: String?, impact: String?, analyzedAt: Long?)

    @Query("DELETE FROM news_articles WHERE isFavorite = 0 AND publishedAt < :threshold")
    suspend fun deleteOldArticles(threshold: String)
}
