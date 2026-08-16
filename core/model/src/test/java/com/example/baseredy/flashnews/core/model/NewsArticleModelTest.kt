package com.example.baseredy.flashnews.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsArticleModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun newsArticle_defaultValues_areCorrect() {
        val article = NewsArticle(
            title = "Titlu test",
            url = "https://example.com/test",
            publishedAt = "2026-08-16T22:00:00Z"
        )

        assertEquals("GLOBAL", article.region)
        assertEquals("General", article.category)
        assertEquals("PENDING", article.factCheckStatus)
        assertFalse(article.isFavorite)
        assertFalse(article.isMultiPerspective)
        assertTrue(article.dynamicInsights.isEmpty())
    }

    @Test
    fun newsArticle_serializationRoundTrip() {
        val article = NewsArticle(
            title = "Test Stire Completa",
            description = "Descriere detaliata",
            url = "https://example.com/stire",
            urlToImage = "https://example.com/img.jpg",
            publishedAt = "2026-08-16T22:00:00Z",
            sourceName = "HotNews",
            aiSummary = "• Punct cheie 1\n• Punct cheie 2",
            aiBias = "CENTRU-DREAPTA",
            aiLocalImpact = "Impact direct in Romania",
            aiAnalyzedAt = 1773700000000L,
            biasRationale = "Linie editoriala pro-transparenta",
            dynamicInsights = listOf(
                DynamicInsight("Care este miza?", "Clarificarea legislatiei."),
                DynamicInsight("Ce urmeaza?", "Votul final in Parlament.")
            ),
            isFavorite = true,
            relativeTime = "acum 10m",
            sourceLogoUrl = "https://example.com/favicon.png",
            region = "RO",
            category = "Politica",
            isMultiPerspective = true
        )

        val encoded = json.encodeToString(NewsArticle.serializer(), article)
        val decoded = json.decodeFromString(NewsArticle.serializer(), encoded)

        assertEquals(article.title, decoded.title)
        assertEquals(article.url, decoded.url)
        assertEquals(article.sourceName, decoded.sourceName)
        assertEquals(article.aiBias, decoded.aiBias)
        assertEquals(article.dynamicInsights.size, decoded.dynamicInsights.size)
        assertEquals(article.dynamicInsights[0].question, decoded.dynamicInsights[0].question)
        assertEquals(article.dynamicInsights[0].answer, decoded.dynamicInsights[0].answer)
        assertEquals(article.category, decoded.category)
        assertTrue(decoded.isFavorite)
        assertTrue(decoded.isMultiPerspective)
    }

    @Test
    fun legacyAiInsight_compatibility() {
        val legacy = AiInsight(title = "Intrebare Legacy", content = "Raspuns Legacy")
        val encoded = json.encodeToString(AiInsight.serializer(), legacy)
        val decoded = json.decodeFromString(AiInsight.serializer(), encoded)

        assertEquals(legacy.title, decoded.title)
        assertEquals(legacy.content, decoded.content)
    }
}
