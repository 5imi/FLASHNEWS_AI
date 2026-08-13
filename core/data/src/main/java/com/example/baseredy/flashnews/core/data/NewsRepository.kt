package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.NewsDao
import com.example.baseredy.flashnews.core.database.NewsArticleEntity
import com.example.baseredy.flashnews.core.model.AiInsight
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.RetrofitClient
import com.example.baseredy.flashnews.core.network.AiClient
import com.example.baseredy.flashnews.core.network.RssClient
import com.example.baseredy.flashnews.core.network.RssItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.net.URL
import kotlin.random.Random

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map

class NewsRepository(
    private val newsDao: NewsDao,
    private val aiClient: AiClient? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getArticles(region: String, category: String): Flow<PagingData<NewsArticle>> {
        val pagingSourceFactory = if (category == "Toate") {
            { newsDao.getArticlesByRegion(region) }
        } else {
            { newsDao.getArticlesByRegionAndCategory(region, category) }
        }

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    fun getFavorites(): Flow<PagingData<NewsArticle>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { newsDao.getFavoriteArticles() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    suspend fun toggleFavorite(url: String, currentStatus: Boolean) {
        newsDao.updateFavoriteStatus(url, !currentStatus)
    }

    suspend fun refreshNews(
        newsApiKeys: List<String>,
        newsDataKey: String? = null,
        mediastackKey: String? = null,
        region: String,
        category: String,
        language: String = "en"
    ) {
        try {
            val entities = mutableListOf<NewsArticleEntity>()
            
            // 1. Fetch from NewsAPI
            newsApiKeys.filter { it.isNotBlank() }.forEach { key ->
                if (region != "RO") {
                    fetchFromNewsApi(key, category, language, entities)
                }
            }

            // 2. Fetch from NewsData.io
            if (!newsDataKey.isNullOrBlank()) {
                fetchFromNewsData(newsDataKey, category, if (region == "RO") "ro" else language, entities)
            }

            // 3. Fetch from Mediastack
            if (!mediastackKey.isNullOrBlank() && region != "RO") {
                fetchFromMediastack(mediastackKey, category, language, entities)
            }

            // 4. Fetch from RSS
            val shouldFetchRss = region == "RO" || category == "General" || category == "Toate" || entities.size < 5
            if (shouldFetchRss) {
                // [OLD] - Motiv înlocuire: Apelul fără parametri descărca 150+ feed-uri RSS indiferent de selecția utilizatorului
                // val rssItems = RssClient.fetchRssNews()
                val rssItems = RssClient.fetchRssNews(targetRegion = region, targetCategory = category)

                val filteredRss = rssItems.filter { item ->
                    val regionMatch = item.region == region
                    val categoryMatch = if (category == "Toate") true else item.category == category
                    regionMatch && categoryMatch
                }

                filteredRss.forEach { rss ->
                    processAndAddEntity(
                        url = rss.link,
                        title = rss.title,
                        description = rss.description,
                        imageUrl = rss.imageUrl,
                        publishedAt = rss.pubDate,
                        sourceName = rss.sourceName,
                        category = rss.category,
                        region = rss.region,
                        entities = entities
                    )
                }
            }

            // Deduplication logic: Title-based grouping
            val deduplicated = deduplicateByTitle(entities)
            
            // Final distinct by URL (Room Primary Key)
            val finalEntities = deduplicated.distinctBy { it.url }
            newsDao.insertArticles(finalEntities)
            
            // Trigger asynchronous non-blocking AI enrichment in background for unanalyzed articles
            triggerBackgroundAiAnalysis(finalEntities.filter { it.aiAnalyzedAt == null || it.aiAnalyzedAt == 0L })

            // Cleanup old articles (older than 14 days)
            val thresholdDate = ZonedDateTime.now().minusDays(14).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            newsDao.deleteOldArticles(thresholdDate)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deduplicateByTitle(articles: List<NewsArticleEntity>): List<NewsArticleEntity> {
        return articles.groupBy { 
            it.title.lowercase()
                .replace(Regex("[^a-z0-9 ]"), "")
                .trim() 
        }.map { (_, group) ->
            // Keep the one with most info (has summary or image) or latest
            group.maxByOrNull { 
                (if (it.aiSummary != null) 10 else 0) + 
                (if (it.urlToImage != null) 5 else 0)
            } ?: group.first()
        }
    }

    suspend fun searchNews(apiKey: String, query: String): List<NewsArticle> {
        return try {
            val response = RetrofitClient.newsApi.searchNews(apiKey = apiKey, query = query)
            response.articles.map { dto ->
                val existing = newsDao.getArticleByUrl(dto.url)
                if (existing?.aiSummary != null) {
                    existing.toDomain()
                } else {
                    val summary = try {
                        aiClient?.summarize(dto.title, dto.description ?: "", dto.source?.name ?: "Unknown", "General", "GLOBAL")
                    } catch (e: Exception) {
                        simulateAiSummary(dto.description ?: dto.title)
                    }
                    
                    val bias = try {
                        aiClient?.analyzeBias(dto.source?.name ?: "Unknown", dto.title, "General", "GLOBAL")
                    } catch (e: Exception) {
                        simulateBias(dto.source?.name ?: "", "General")
                    }
                    
                    NewsArticle(
                        url = dto.url,
                        title = dto.title,
                        description = dto.description,
                        urlToImage = dto.urlToImage,
                        publishedAt = dto.publishedAt,
                        sourceName = dto.source?.name,
                        aiSummary = summary,
                        aiBias = bias,
                        isFavorite = newsDao.isArticleFavorite(dto.url)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun fetchFromNewsApi(apiKey: String, category: String, country: String, entities: MutableList<NewsArticleEntity>) {
        val apiCategory = when(category) {
            "Business & Finanțe" -> "business"
            "Tehnologie", "Auto" -> "technology"
            "Sport" -> "sports"
            "Știință & Mediu" -> "science"
            "Sănătate" -> "health"
            "Lifestyle" -> "entertainment"
            else -> null
        }
        try {
            val response = RetrofitClient.newsApi.getTopHeadlines(apiKey = apiKey, category = apiCategory, country = country)
            response.articles.forEach { dto ->
                processAndAddEntity(
                    url = dto.url,
                    title = dto.title,
                    description = dto.description,
                    imageUrl = dto.urlToImage,
                    publishedAt = dto.publishedAt,
                    sourceName = dto.source?.name,
                    category = category,
                    region = "GLOBAL",
                    entities = entities
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchFromNewsData(apiKey: String, category: String, country: String, entities: MutableList<NewsArticleEntity>) {
        val apiCategory = when(category) {
            "Business & Finanțe" -> "business"
            "Tehnologie", "Auto" -> "technology"
            "Sport" -> "sports"
            "Știință & Mediu" -> "science"
            "Sănătate" -> "health"
            "Lifestyle" -> "entertainment"
            "Politică" -> "politics"
            "Educație" -> "education"
            else -> null
        }
        val apiCountry = if (country == "us") "us" else if (country == "ro") "ro" else null
        try {
            val response = RetrofitClient.newsDataApi.getLatestNews(apiKey = apiKey, category = apiCategory, country = apiCountry)
            response.results.forEach { dto ->
                processAndAddEntity(
                    url = dto.link,
                    title = dto.title,
                    description = dto.description,
                    imageUrl = dto.imageUrl,
                    publishedAt = dto.pubDate ?: ZonedDateTime.now().toString(),
                    sourceName = dto.sourceId,
                    category = category,
                    region = if (country == "ro") "RO" else "GLOBAL",
                    entities = entities
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchFromMediastack(apiKey: String, category: String, country: String, entities: MutableList<NewsArticleEntity>) {
        val apiCategory = when(category) {
            "Business & Finanțe" -> "business"
            "Tehnologie", "Auto" -> "technology"
            "Sport" -> "sports"
            "Știință & Mediu" -> "science"
            "Sănătate" -> "health"
            "Lifestyle" -> "entertainment"
            else -> null
        }
        try {
            val response = RetrofitClient.mediastackApi.getLiveNews(apiKey = apiKey, categories = apiCategory, countries = if (country == "us") "us" else null)
            response.data.forEach { dto ->
                processAndAddEntity(
                    url = dto.url,
                    title = dto.title,
                    description = dto.description,
                    imageUrl = dto.image,
                    publishedAt = dto.publishedAt ?: ZonedDateTime.now().toString(),
                    sourceName = dto.source,
                    category = category,
                    region = "GLOBAL",
                    entities = entities
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // [OLD] - Motiv înlocuire: Apelurile AI sincrone din processAndAddEntity blocau inserarea articolelor în Room DB și încărcarea interfeței
    /*
    private suspend fun processAndAddEntity(
        url: String,
        title: String,
        description: String?,
        imageUrl: String?,
        publishedAt: String,
        sourceName: String?,
        category: String,
        region: String,
        entities: MutableList<NewsArticleEntity>
    ) {
        val existing = newsDao.getArticleByUrl(url)
        if (existing != null) {
            entities.add(existing.copy(category = category))
        } else {
            val normalizedTitle = title.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
            val duplicate = entities.find { it.title.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim() == normalizedTitle }
            if (duplicate != null) return
            delay(100)
            val summary = try { aiClient?.summarize(title, description ?: "", sourceName ?: "Unknown", category, region) } catch (e: Exception) { simulateAiSummary(description ?: title) }
            val bias = try { aiClient?.analyzeBias(sourceName ?: "Unknown", title, category, region) } catch (e: Exception) { simulateBias(sourceName ?: "", category) }
            val impact = if (summary != null && region != "GLOBAL" && !summary.contains("indisponibil", ignoreCase = true)) {
                try { aiClient?.analyzeLocalImpact(title, description ?: "", region) } catch (e: Exception) { null }
            } else null
            entities.add(NewsArticleEntity(
                url = url, title = title, description = description, urlToImage = imageUrl,
                publishedAt = publishedAt, sourceName = sourceName, category = category,
                aiSummary = summary, aiBias = bias, aiLocalImpact = impact,
                aiAnalyzedAt = System.currentTimeMillis(), isFavorite = false,
                region = region, isMultiPerspective = checkMultiPerspective(title)
            ))
        }
    }
    */

    private suspend fun processAndAddEntity(
        url: String,
        title: String,
        description: String?,
        imageUrl: String?,
        publishedAt: String,
        sourceName: String?,
        category: String,
        region: String,
        entities: MutableList<NewsArticleEntity>
    ) {
        val existing = newsDao.getArticleByUrl(url)
        if (existing != null) {
            entities.add(existing.copy(category = category))
        } else {
            // Check if we already have this story by title (even if URL is different)
            val normalizedTitle = title.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
            val duplicate = entities.find { it.title.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim() == normalizedTitle }
            
            if (duplicate != null) return

            // Instant heuristic summary for zero-latency initial UI display
            val summary = simulateAiSummary(description ?: title)
            val bias = simulateBias(sourceName ?: "", category)

            entities.add(NewsArticleEntity(
                url = url,
                title = title,
                description = description,
                urlToImage = imageUrl,
                publishedAt = publishedAt,
                sourceName = sourceName,
                category = category,
                aiSummary = summary,
                aiBias = bias,
                aiLocalImpact = null,
                aiAnalyzedAt = 0L, // Marked as un-enriched for background worker
                isFavorite = false,
                region = region,
                isMultiPerspective = checkMultiPerspective(title)
            ))
        }
    }

    private fun triggerBackgroundAiAnalysis(articlesToEnrich: List<NewsArticleEntity>) {
        if (aiClient == null || articlesToEnrich.isEmpty()) return

        repositoryScope.launch {
            articlesToEnrich.take(10).forEach { article ->
                try {
                    delay(300) // Gentle rate-limit pacing
                    val summary = aiClient.summarize(
                        article.title,
                        article.description ?: "",
                        article.sourceName ?: "Unknown",
                        article.category,
                        article.region
                    )
                    val bias = aiClient.analyzeBias(
                        article.sourceName ?: "Unknown",
                        article.title,
                        article.category,
                        article.region
                    )
                    val impact = if (article.region != "GLOBAL" && !summary.contains("indisponibil", ignoreCase = true)) {
                        try {
                            aiClient.analyzeLocalImpact(article.title, article.description ?: "", article.region)
                        } catch (e: Exception) {
                            null
                        }
                    } else null

                    newsDao.updateAiAnalysis(
                        url = article.url,
                        summary = summary,
                        bias = bias,
                        impact = impact,
                        analyzedAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    // Gracefully skip failed AI background analysis
                }
            }
        }
    }

    private fun simulateAiSummary(text: String): String {
        val cleaned = text.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val sentences = Regex("(?<=[.!?])\\s+").split(cleaned)
            .filter { it.length > 8 }
            .take(2)
            .joinToString("\n• ", prefix = "• ")
        
        return if (sentences.length > 5) sentences else "• Detalii indisponibile momentan.\n• Verifică sursa originală."
    }

    private fun simulateBias(source: String, category: String? = null): String {
        val normalizedSource = source.lowercase()
        return when {
            normalizedSource.contains("cnn") || normalizedSource.contains("bbc") || normalizedSource.contains("guardian") -> "NEUTRU"
            normalizedSource.contains("fox") || normalizedSource.contains("breitbart") -> "DREAPTA"
            normalizedSource.contains("buzzfeed") || normalizedSource.contains("huff") || normalizedSource.contains("politico") -> "STÂNGA"
            else -> "NEUTRU"
        }
    }

    private fun NewsArticleEntity.toDomain() = NewsArticle(
        title = title,
        description = description,
        url = url,
        urlToImage = urlToImage,
        publishedAt = publishedAt,
        sourceName = sourceName,
        aiSummary = aiSummary,
        aiBias = aiBias,
        aiLocalImpact = aiLocalImpact,
        aiAnalyzedAt = aiAnalyzedAt,
        isFavorite = isFavorite,
        relativeTime = formatRelativeTime(publishedAt),
        sourceLogoUrl = getFaviconUrl(url),
        region = region,
        isMultiPerspective = isMultiPerspective
    )

    suspend fun askAi(article: NewsArticle, question: String): String {
        val context = "Titlu: ${article.title}. Descriere: ${article.description?.take(1500) ?: ""}"
        val response = try {
            aiClient?.askQuestion(context, question) ?: "AI indisponibil."
        } catch (e: Exception) {
            "Eroare AI - verifică conexiunea."
        }
        
        // Update DB with this question as a temporary summary or just log it
        // For now we just return it, but ensuring context is 1500 chars was the key.
        return response
    }

    suspend fun getAiInsights(article: NewsArticle): List<AiInsight> {
        // Reuse insights if they exist in DB and are fresh (less than 24h old)
        val existing = newsDao.getArticleByUrl(article.url)
        val aiSummary = existing?.aiSummary
        val aiAnalyzedAt = existing?.aiAnalyzedAt
        
        if (aiSummary != null && aiAnalyzedAt != null) {
            val age = System.currentTimeMillis() - aiAnalyzedAt
            if (age < 24 * 3600 * 1000 && aiSummary.contains("===")) {
                val sections = aiSummary.split("===").map { it.trim() }
                if (sections.size >= 3) {
                    val titles = listOf("Explică simplu", "Impact local", "Ce trebuie să verifici")
                    return titles.mapIndexed { index, title ->
                        AiInsight(title = title, content = sections[index])
                    }
                }
            }
        }

        val combinedPrompt = """
            Ești un analist media senior. Analizează acest articol și oferă 3 secțiuni separate EXACT prin secvența '===' între ele.
            
            Titlu: ${article.title}
            Sursă: ${article.sourceName}
            Context: ${article.description?.take(1500) ?: ""}
            
            Secțiunea 1: Explică simplu (Ce s-a întâmplat defapt? 2-3 propoziții clare)
            ===
            Secțiunea 2: Impact pentru România (Cum ne afectează direct sau indirect? 1-2 propoziții)
            ===
            Secțiunea 3: Fact-check (2-3 puncte critice de verificat pentru a evita dezinformarea)
            
            Răspunde DOAR în limba ROMÂNĂ. Nu adăuga introduceri sau concluzii.
        """.trimIndent()

        return try {
            val fullResponse = aiClient?.askQuestion(article.title, combinedPrompt) ?: ""
            val sections = fullResponse.split("===").map { it.trim() }
            
            val titles = listOf("Explică simplu", "Impact local", "Ce trebuie să verifici")
            val insights = titles.mapIndexed { index, title ->
                AiInsight(
                    title = title,
                    content = if (index < sections.size) sections[index] else "Informație indisponibilă."
                )
            }
            
            // Persist the structured analysis for future use
            if (sections.size >= 3) {
                newsDao.updateAiAnalysis(article.url, fullResponse, article.aiBias, article.aiLocalImpact, System.currentTimeMillis())
            }
            
            insights
        } catch (e: Exception) {
            listOf(
                AiInsight(title = "Explică simplu", content = article.title),
                AiInsight(title = "Impact local", content = "Verifică sursa originală pentru detalii specifice."),
                AiInsight(title = "Ce trebuie să verifici", content = "Compară informația cu alte publicații de încredere.")
            )
        }
    }

    private fun checkMultiPerspective(title: String): Boolean {
        val keywords = listOf("Ucraina", "Rusia", "China", "SUA", "Iran", "NATO", "Israel", "Gaza")
        return keywords.any { title.contains(it, ignoreCase = true) }
    }

    private fun formatRelativeTime(publishedAt: String?): String {
        if (publishedAt == null) return ""
        return try {
            val now = ZonedDateTime.now()
            val articleTime = ZonedDateTime.parse(publishedAt)
            val diff = Duration.between(articleTime, now)
            
            when {
                diff.toMinutes() < 1 -> "chiar acum"
                diff.toMinutes() < 60 -> "acum ${diff.toMinutes()}m"
                diff.toHours() < 24 -> "acum ${diff.toHours()}h"
                else -> "acum ${diff.toDays()}z"
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFaviconUrl(url: String?): String {
        if (url == null) return ""
        return try {
            val domain = URL(url).host
            "https://www.google.com/s2/favicons?sz=64&domain=$domain"
        } catch (e: Exception) {
            ""
        }
    }
}
