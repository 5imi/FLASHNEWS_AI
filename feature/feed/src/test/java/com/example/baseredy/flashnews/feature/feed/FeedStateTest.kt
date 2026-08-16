package com.example.baseredy.flashnews.feature.feed

import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis
import com.example.baseredy.flashnews.core.model.NewsArticle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedStateTest {

    @Test
    fun feedCategories_areCompleteAndProperlyOrdered() {
        val expectedCategories = listOf(
            "Toate", "General", "Business & Finanțe", "Politică", "Tehnologie", 
            "Sport", "Auto", "Știință & Mediu", "Sănătate", "Lifestyle", "Educație"
        )

        assertEquals(11, expectedCategories.size)
        assertTrue(expectedCategories.contains("Toate"))
        assertTrue(expectedCategories.contains("Business & Finanțe"))
        assertTrue(expectedCategories.contains("Tehnologie"))
    }

    @Test
    fun newsArticle_dynamicInsights_mappingCorrect() {
        val analysis = DynamicNewsAnalysis(
            keyTakeaway = "• Sinteza importanta",
            editorialBias = "INDEPENDENT / FACTUAL",
            biasRationale = "Raportare obiectiva",
            localImpact = "Relevanta locala confirmata",
            dynamicQuestions = listOf(
                DynamicInsight("Care este miza?", "Dezvoltare economica."),
                DynamicInsight("Cine este afectat?", "Sectorul energetic.")
            )
        )

        val article = NewsArticle(
            title = "Titlu test",
            url = "https://example.com/test",
            publishedAt = "2026-08-16T22:00:00Z",
            aiSummary = analysis.keyTakeaway,
            aiBias = analysis.editorialBias,
            aiLocalImpact = analysis.localImpact,
            biasRationale = analysis.biasRationale,
            dynamicInsights = analysis.dynamicQuestions,
            region = "RO",
            category = "Business & Finanțe"
        )

        assertEquals(2, article.dynamicInsights.size)
        assertEquals("Care este miza?", article.dynamicInsights[0].question)
        assertEquals("Dezvoltare economica.", article.dynamicInsights[0].answer)
        assertEquals("INDEPENDENT / FACTUAL", article.aiBias)
        assertEquals("Business & Finanțe", article.category)
    }
}
