package com.example.baseredy.flashnews.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// NewsData.io Models
@Serializable
data class NewsDataResponse(
    val status: String,
    val totalResults: Int? = null,
    val results: List<NewsDataArticleDto> = emptyList()
)

@Serializable
data class NewsDataArticleDto(
    val title: String,
    val link: String,
    val description: String? = null,
    val pubDate: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("source_id") val sourceId: String? = null
)

interface NewsDataApiService {
    @GET("news")
    suspend fun getLatestNews(
        @Query("apikey") apiKey: String,
        @Query("language") language: String? = null,
        @Query("category") category: String? = null,
        @Query("country") country: String? = null
    ): NewsDataResponse
}

// Mediastack Models
@Serializable
data class MediastackResponse(
    val data: List<MediastackArticleDto> = emptyList()
)

@Serializable
data class MediastackArticleDto(
    val title: String,
    val description: String? = null,
    val url: String,
    val source: String? = null,
    val image: String? = null,
    @SerialName("published_at") val publishedAt: String? = null
)

interface MediastackApiService {
    @GET("news")
    suspend fun getLiveNews(
        @Query("access_key") apiKey: String,
        @Query("languages") languages: String? = null,
        @Query("categories") categories: String? = null,
        @Query("countries") countries: String? = null
    ): MediastackResponse
}
