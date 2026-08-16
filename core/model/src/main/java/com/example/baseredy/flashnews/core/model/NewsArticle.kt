package com.example.baseredy.flashnews.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DynamicInsight(
    val question: String,
    val answer: String
)

@Serializable
data class DynamicNewsAnalysis(
    val keyTakeaway: String = "",
    val editorialBias: String = "NEUTRU",
    val biasRationale: String = "",
    val localImpact: String? = null,
    val dynamicQuestions: List<DynamicInsight> = emptyList()
)

@Serializable
data class NewsArticle(
    val title: String,
    val description: String? = null,
    val url: String,
    val urlToImage: String? = null,
    val publishedAt: String,
    val sourceName: String? = null,
    val aiSummary: String? = null,
    val aiBias: String? = null,
    val aiLocalImpact: String? = null,
    val aiAnalyzedAt: Long? = null,
    val biasRationale: String = "",
    val dynamicInsights: List<DynamicInsight> = emptyList(),
    val factCheckStatus: String = "PENDING",
    val factCheckReason: String = "",
    val isFavorite: Boolean = false,
    val relativeTime: String = "",
    val sourceLogoUrl: String = "",
    val region: String = "GLOBAL",
    val isMultiPerspective: Boolean = false
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

// [LEGACY] - Reținut pentru compatibilitate inversă în straturile UI existente
@Serializable
data class AiInsight(
    val title: String,
    val content: String
)

