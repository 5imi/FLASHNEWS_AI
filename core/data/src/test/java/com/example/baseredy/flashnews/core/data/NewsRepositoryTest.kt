package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.NewsArticleEntity
import com.example.baseredy.flashnews.core.database.NewsDao
import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.AiClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.paging.PagingSource

class NewsRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Fake in-memory NewsDao implementation for robust unit testing
    private class FakeNewsDao : NewsDao {
        val articles = mutableMapOf<String, NewsArticleEntity>()

        override fun getArticlesByRegionAndCategory(region: String, category: String): PagingSource<Int, NewsArticleEntity> = throw NotImplementedError()
        override fun getArticlesByRegion(region: String): PagingSource<Int, NewsArticleEntity> = throw NotImplementedError()
        override fun getFavoriteArticles(): PagingSource<Int, NewsArticleEntity> = throw NotImplementedError()

        override suspend fun insertArticles(articlesList: List<NewsArticleEntity>) {
            articlesList.forEach { articles[it.url] = it }
        }

        override suspend fun insertArticlesIfAbsent(articlesList: List<NewsArticleEntity>) {
            articlesList.forEach {
                if (!articles.containsKey(it.url)) {
                    articles[it.url] = it
                }
            }
        }

        override suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean) {
            val existing = articles[url]
            if (existing != null) {
                articles[url] = existing.copy(isFavorite = isFavorite)
            }
        }

        override suspend fun isArticleFavorite(url: String): Boolean {
            return articles[url]?.isFavorite ?: false
        }

        override suspend fun getArticleByUrl(url: String): NewsArticleEntity? {
            return articles[url]
        }

        override suspend fun updateAiAnalysis(url: String, summary: String?, bias: String?, impact: String?, analyzedAt: Long?) {
            val existing = articles[url]
            if (existing != null) {
                articles[url] = existing.copy(
                    aiSummary = summary,
                    aiBias = bias,
                    aiLocalImpact = impact,
                    aiAnalyzedAt = analyzedAt
                )
            }
        }

        override suspend fun deleteOldArticles(threshold: String) {
            articles.entries.removeIf { !it.value.isFavorite && it.value.publishedAt < threshold }
        }
    }

    @Test
    fun getDynamicAnalysis_returnsExistingCachedAnalysis_withoutReCallingAi() = runTest {
        val fakeDao = FakeNewsDao()
        var aiCallCount = 0

        val fakeAi = object : AiClient {
            override suspend fun analyzeNewsDynamic(title: String, description: String, source: String, category: String, region: String): DynamicNewsAnalysis {
                aiCallCount++
                return DynamicNewsAnalysis(
                    keyTakeaway = "• Analiza proaspata",
                    editorialBias = "NEUTRU",
                    biasRationale = "Factual",
                    localImpact = null,
                    dynamicQuestions = listOf(DynamicInsight("Intrebare?", "Raspuns."))
                )
            }
            override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String = ""
            override suspend fun analyzeBias(source: String, text: String, category: String, region: String): String = ""
            override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String = ""
            override suspend fun askQuestion(articleContext: String, question: String): String = ""
            override suspend fun comparePerspectives(articles: List<String>): String = ""
        }

        val cachedAnalysis = DynamicNewsAnalysis(
            keyTakeaway = "• Analiza existenta in cache DB",
            editorialBias = "FACTUAL / ECONOMIC",
            biasRationale = "Sursa economica",
            localImpact = "Impact local salvat",
            dynamicQuestions = listOf(DynamicInsight("Cum afecteaza piata?", "Efecte pozitive."))
        )

        val serialized = json.encodeToString(DynamicNewsAnalysis.serializer(), cachedAnalysis)
        fakeDao.insertArticles(listOf(
            NewsArticleEntity(
                url = "https://example.com/articol-1",
                title = "Articol cu analiza existenta",
                description = "Descriere",
                urlToImage = null,
                publishedAt = "2026-08-16T10:00:00Z",
                sourceName = "ZF",
                category = "Business & Finanțe",
                region = "RO",
                aiSummary = serialized,
                aiBias = "FACTUAL / ECONOMIC",
                aiLocalImpact = "Impact local salvat",
                aiAnalyzedAt = System.currentTimeMillis() // Fresh (< 24h)
            )
        ))

        val repository = NewsRepository(fakeDao, fakeAi)
        val article = NewsArticle(
            title = "Articol cu analiza existenta",
            url = "https://example.com/articol-1",
            publishedAt = "2026-08-16T10:00:00Z",
            sourceName = "ZF",
            region = "RO",
            category = "Business & Finanțe"
        )

        val result = repository.getDynamicAnalysis(article)

        assertNotNull(result)
        assertEquals("• Analiza existenta in cache DB", result.keyTakeaway)
        assertEquals("FACTUAL / ECONOMIC", result.editorialBias)
        assertEquals("Impact local salvat", result.localImpact)
        assertEquals(0, aiCallCount) // Verificam ca AI-ul NU a fost apelat inutil datorita cache-ului persistent
    }

    @Test
    fun insertArticlesIfAbsent_preservesExistingAiAnalysisAndFavorite() = runTest {
        val fakeDao = FakeNewsDao()
        
        // 1. Initial article with AI analysis and favorited
        val initial = NewsArticleEntity(
            url = "https://example.com/articol-important",
            title = "Titlu Important",
            description = "Descriere initiala",
            urlToImage = "https://example.com/img.png",
            publishedAt = "2026-08-16T08:00:00Z",
            sourceName = "HotNews",
            category = "General",
            isFavorite = true,
            aiSummary = "• Analiza AI completata",
            aiBias = "CENTRU-DREAPTA",
            aiAnalyzedAt = 1000L
        )
        fakeDao.insertArticles(listOf(initial))

        // 2. Background sync fetches same article with default unanalyzed state
        val syncItem = NewsArticleEntity(
            url = "https://example.com/articol-important",
            title = "Titlu Important",
            description = "Descriere noua",
            urlToImage = "https://example.com/img.png",
            publishedAt = "2026-08-16T08:00:00Z",
            sourceName = "HotNews",
            category = "General",
            isFavorite = false,
            aiSummary = "• Rezumat temporar",
            aiBias = "NEUTRU",
            aiAnalyzedAt = 0L
        )
        fakeDao.insertArticlesIfAbsent(listOf(syncItem))

        // 3. Verify that the existing AI analysis and favorite status are preserved
        val saved = fakeDao.getArticleByUrl("https://example.com/articol-important")
        assertNotNull(saved)
        assertTrue(saved!!.isFavorite)
        assertEquals("• Analiza AI completata", saved.aiSummary)
        assertEquals("CENTRU-DREAPTA", saved.aiBias)
        assertEquals(1000L, saved.aiAnalyzedAt)
    }

    @Test
    fun toggleFavorite_updatesStatusInDao() = runTest {
        val fakeDao = FakeNewsDao()
        fakeDao.insertArticles(listOf(
            NewsArticleEntity(
                url = "https://example.com/fav-test",
                title = "Test Favorite",
                description = null,
                urlToImage = null,
                publishedAt = "2026-08-16T12:00:00Z",
                sourceName = "Digi24",
                category = "General",
                isFavorite = false
            )
        ))

        val repository = NewsRepository(fakeDao, null)
        repository.toggleFavorite("https://example.com/fav-test", false)

        val updated = fakeDao.getArticleByUrl("https://example.com/fav-test")
        assertNotNull(updated)
        assertTrue(updated!!.isFavorite)
    }
}
