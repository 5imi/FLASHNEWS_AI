package com.example.baseredy.flashnews.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// [OLD] - Motiv înlocuire: Lipsa indicilor compuși cauza scanare completă (Full Table Scan) la sortările după publishedAt și filtrarea pe regiune/categorie
// @Entity(tableName = "news_articles")

@Entity(
    tableName = "news_articles",
    indices = [
        Index(value = ["region", "category", "publishedAt"]),
        Index(value = ["region", "publishedAt"]),
        Index(value = ["isFavorite", "publishedAt"])
    ]
)
data class NewsArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val urlToImage: String?,
    val publishedAt: String,
    val sourceName: String?,
    val category: String,
    val isFavorite: Boolean = false,
    val region: String = "GLOBAL",
    val isMultiPerspective: Boolean = false,
    // AI Persistence fields
    val aiSummary: String? = null,
    val aiBias: String? = null,
    val aiLocalImpact: String? = null,
    val aiAnalyzedAt: Long? = null
)
