package com.example.baseredy.flashnews.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticle(
    val title: String,
    val description: String? = null,
    val url: String,
    val urlToImage: String? = null,
    val publishedAt: String,
    val sourceName: String? = null,
    val aiSummary: List<String> = emptyList(),
    val factCheckStatus: String = "PENDING",
    val factCheckReason: String = "",
    val biasType: String = "NEUTRAL", // NEUTRAL, LEFT, RIGHT, CLICKBAIT
    val isFavorite: Boolean = false,
    val relativeTime: String = "",
    val sourceLogoUrl: String = "",
    val region: String = "GLOBAL",
    val isMultiPerspective: Boolean = false,
    val localImpact: String? = null
)

@Serializable
data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsArticleDto>
)

@Serializable
data class NewsArticleDto(
    val source: SourceDto? = null,
    val title: String,
    val description: String? = null,
    val url: String,
    val urlToImage: String? = null,
    val publishedAt: String
)

@Serializable
data class SourceDto(
    val id: String? = null,
    val name: String? = null
)
