package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.NewsDao
import com.example.baseredy.flashnews.core.database.NewsArticleEntity
import com.example.baseredy.flashnews.core.model.AiInsight
import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis
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
import kotlinx.serialization.json.Json
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
    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

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
                    // [OLD] - Motiv înlocuire: Apelurile separate (summarize, bias, impact) cauzau fragmentare, latență și rate-limiting
                    /*
                    val summary = aiClient.summarize(article.title, article.description ?: "", article.sourceName ?: "Unknown", article.category, article.region)
                    val bias = aiClient.analyzeBias(article.sourceName ?: "Unknown", article.title, article.category, article.region)
                    val impact = if (article.region != "GLOBAL" && !summary.contains("indisponibil", ignoreCase = true)) {
                        try { aiClient.analyzeLocalImpact(article.title, article.description ?: "", article.region) } catch (e: Exception) { null }
                    } else null
                    newsDao.updateAiAnalysis(article.url, summary, bias, impact, System.currentTimeMillis())
                    */

                    val dynamicAnalysis = aiClient.analyzeNewsDynamic(
                        title = article.title,
                        description = article.description ?: "",
                        source = article.sourceName ?: "Unknown",
                        category = article.category,
                        region = article.region
                    )

                    val serializedJson = jsonSerializer.encodeToString(DynamicNewsAnalysis.serializer(), dynamicAnalysis)

                    newsDao.updateAiAnalysis(
                        url = article.url,
                        summary = serializedJson,
                        bias = dynamicAnalysis.editorialBias,
                        impact = dynamicAnalysis.localImpact,
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

    private fun NewsArticleEntity.toDomain(): NewsArticle {
        val rawSummary = aiSummary
        var displaySummary = rawSummary
        var rationale = ""
        var dynamicList = emptyList<DynamicInsight>()

        if (rawSummary?.startsWith("{") == true) {
            try {
                val parsed = jsonSerializer.decodeFromString(DynamicNewsAnalysis.serializer(), rawSummary)
                displaySummary = parsed.keyTakeaway.ifBlank { displaySummary }
                rationale = parsed.biasRationale
                dynamicList = parsed.dynamicQuestions
            } catch (e: Exception) {
                // Keep raw aiSummary on parse exception
            }
        }

        return NewsArticle(
            title = title,
            description = description,
            url = url,
            urlToImage = urlToImage,
            publishedAt = publishedAt,
            sourceName = sourceName,
            aiSummary = displaySummary,
            aiBias = aiBias,
            aiLocalImpact = aiLocalImpact,
            aiAnalyzedAt = aiAnalyzedAt,
            biasRationale = rationale,
            dynamicInsights = dynamicList,
            isFavorite = isFavorite,
            relativeTime = formatRelativeTime(publishedAt),
            sourceLogoUrl = getFaviconUrl(url),
            region = region,
            isMultiPerspective = checkMultiPerspective(title)
        )
    }

    suspend fun askAi(article: NewsArticle, question: String): String {
        val context = "Titlu: ${article.title}. Descriere: ${article.description?.take(1500) ?: ""}"
        val response = try {
            aiClient?.askQuestion(context, question) ?: "AI indisponibil."
        } catch (e: Exception) {
            "Eroare AI - verifică conexiunea."
        }
        return response
    }

    suspend fun getDynamicAnalysis(article: NewsArticle): DynamicNewsAnalysis {
        val existing = newsDao.getArticleByUrl(article.url)
        val aiSummary = existing?.aiSummary
        val aiAnalyzedAt = existing?.aiAnalyzedAt
        
        // Return fresh cached analysis if available (< 24h)
        if (!aiSummary.isNullOrBlank() && aiAnalyzedAt != null) {
            val age = System.currentTimeMillis() - aiAnalyzedAt
            if (age < 24 * 3600 * 1000 && aiSummary.startsWith("{")) {
                try {
                    return jsonSerializer.decodeFromString(DynamicNewsAnalysis.serializer(), aiSummary)
                } catch (e: Exception) {
                    // Fall through to re-generate
                }
            }
        }

        return try {
            val analysis = aiClient?.analyzeNewsDynamic(
                title = article.title,
                description = article.description ?: "",
                source = article.sourceName ?: "Unknown",
                category = article.region,
                region = article.region
            ) ?: DynamicNewsAnalysis(
                keyTakeaway = "• ${article.title}",
                editorialBias = article.aiBias ?: "NEUTRU",
                biasRationale = "Sursă de știri standard.",
                localImpact = article.aiLocalImpact,
                dynamicQuestions = listOf(
                    DynamicInsight("Ce trebuie să știi?", article.description ?: article.title)
                )
            )

            val serializedJson = jsonSerializer.encodeToString(DynamicNewsAnalysis.serializer(), analysis)
            newsDao.updateAiAnalysis(
                url = article.url,
                summary = serializedJson,
                bias = analysis.editorialBias,
                impact = analysis.localImpact,
                analyzedAt = System.currentTimeMillis()
            )

            analysis
        } catch (e: Exception) {
            DynamicNewsAnalysis(
                keyTakeaway = "• ${article.title}",
                editorialBias = article.aiBias ?: "NEUTRU",
                biasRationale = "Sursă de știri standard.",
                localImpact = article.aiLocalImpact,
                dynamicQuestions = listOf(
                    DynamicInsight("Ce trebuie să știi?", article.description ?: article.title)
                )
            )
        }
    }

    suspend fun getAiInsights(article: NewsArticle): List<AiInsight> {
        val analysis = getDynamicAnalysis(article)
        return analysis.dynamicQuestions.map {
            AiInsight(title = it.question, content = it.answer)
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
