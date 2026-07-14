package com.example.baseredy.flashnews.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val urlToImage: String?,
    val publishedAt: String,
    val sourceName: String?,
    val category: String,
    val aiSummary: List<String> = emptyList(),
    val factCheckStatus: String = "PENDING",
    val factCheckReason: String = "",
    val biasType: String = "NEUTRAL",
    val isFavorite: Boolean = false,
    val region: String = "GLOBAL",
    val isMultiPerspective: Boolean = false,
    val localImpact: String? = null
)
