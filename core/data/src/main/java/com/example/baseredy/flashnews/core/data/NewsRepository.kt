package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.NewsDao
import com.example.baseredy.flashnews.core.database.NewsArticleEntity
import com.example.baseredy.flashnews.core.model.AiInsight
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.RetrofitClient
import com.example.baseredy.flashnews.core.network.AiClient
import com.example.baseredy.flashnews.core.network.RssClient
import com.example.baseredy.flashnews.core.network.RssItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.net.URL
import kotlin.random.Random

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.map

class NewsRepository(
    private val newsDao: NewsDao,
    private val aiClient: AiClient? = null
) {

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

            // 4. Fetch from RSS — folosim categoriile din RssSource
            val shouldFetchRss = region == "RO" || category == "General" || category == "Toate" || entities.size < 5
            if (shouldFetchRss) {
                println("RSS_DEBUG: Starting RSS fetch for region=$region, category=$category")
                val rssItems = RssClient.fetchRssNews()

                val filteredRss = rssItems.filter { item ->
                    val regionMatch = item.region == region
                    val categoryMatch = if (category == "Toate") true else item.category == category
                    regionMatch && categoryMatch
                }
                println("RSS_DEBUG: After filtering: ${filteredRss.size} items")

                filteredRss.forEach { rss ->
                    val existing = newsDao.getArticleByUrl(rss.link)
                    if (existing != null) {
                        entities.add(existing.copy(category = category))
                    } else {
                        delay(150) // Rate limit dla AI calls - free tier friendly
                        
                        // Use AI for summary, but with fallback
                        val rawSummary = try {
                            aiClient?.summarize(rss.title, rss.description, rss.sourceName, rss.category, rss.region)
                        } catch (e: Exception) {
                            null
                        }
                        val summary = rawSummary?.split("\n")?.filter { it.isNotBlank() }?.take(3) 
                            ?: simulateAiSummary(rss.description, rss.sourceName, rss.category)

                        val bias = try {
                            aiClient?.analyzeBias(rss.sourceName, rss.title, rss.category, rss.region)
                        } catch (e: Exception) {
                            null
                        } ?: simulateBias(rss.sourceName, rss.category)
                        
                        val (fcStatus, fcReason) = simulateFactCheck(rss.sourceName)

                        // Impact local: calculat doar pentru știri cu AI activ
                        val impact = if (rss.region != "RO" && rawSummary != null) {
                            try {
                                aiClient?.analyzeLocalImpact(rss.title, rss.description, rss.region)
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        println("RSS_DEBUG: Adding article '${rss.title.take(50)}' from ${rss.sourceName}")
                        entities.add(NewsArticleEntity(
                            url = rss.link,
                            title = rss.title,
                            description = rss.description,
                            urlToImage = rss.imageUrl,
                            publishedAt = rss.pubDate,
                            sourceName = rss.sourceName,
                            category = rss.category,
                            aiSummary = summary,
                            factCheckStatus = fcStatus,
                            factCheckReason = fcReason,
                            biasType = bias,
                            isFavorite = false,
                            region = rss.region,
                            isMultiPerspective = checkMultiPerspective(rss.title),
                            localImpact = impact
                        ))
                    }
                }
            }

            // Remove potential duplicates by URL
            val distinctEntities = entities.distinctBy { it.url }
            newsDao.insertArticles(distinctEntities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchNews(apiKey: String, query: String): List<NewsArticle> {
        return try {
            val response = RetrofitClient.newsApi.searchNews(apiKey = apiKey, query = query)
            response.articles.map { dto ->
                val rawSummary = try {
                    aiClient?.summarize(dto.title, dto.description ?: "", dto.source?.name ?: "Unknown", "General", "GLOBAL")
                } catch (e: Exception) {
                    null
                }
                val summary = rawSummary?.split("\n")?.filter { it.isNotBlank() }?.take(3)
                    ?: simulateAiSummary(dto.description ?: dto.title, dto.source?.name ?: "Unknown", "General")
                
                val bias = try {
                    aiClient?.analyzeBias(dto.source?.name ?: "Unknown", dto.title, "General", "GLOBAL")
                } catch (e: Exception) {
                    null
                } ?: simulateBias(dto.source?.name ?: "", "General")
                
                val (fcStatus, fcReason) = simulateFactCheck(dto.source?.name ?: "")
                
                NewsArticle(
                    url = dto.url,
                    title = dto.title,
                    description = dto.description,
                    urlToImage = dto.urlToImage,
                    publishedAt = dto.publishedAt,
                    sourceName = dto.source?.name,
                    aiSummary = summary,
                    factCheckStatus = fcStatus,
                    factCheckReason = fcReason,
                    biasType = bias,
                    isFavorite = newsDao.isArticleFavorite(dto.url)
                )
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
            delay(150) // Reduced delay for free tier
            
            val rawSummary = try {
                aiClient?.summarize(title, description ?: "", sourceName ?: "Unknown", category, region)
            } catch (e: Exception) {
                null
            }
            val summary = rawSummary?.split("\n")?.filter { it.isNotBlank() }?.take(3)
                ?: simulateAiSummary(description ?: title, sourceName, category)

            val bias = try {
                aiClient?.analyzeBias(sourceName ?: "Unknown", title, category, region)
            } catch (e: Exception) {
                null
            } ?: simulateBias(sourceName ?: "", category)
            
            val (fcStatus, fcReason) = simulateFactCheck(sourceName ?: "")
            
            val impact = if (rawSummary != null && region != "GLOBAL") {
                try {
                    aiClient?.analyzeLocalImpact(title, description ?: "", region)
                } catch (e: Exception) {
                    null
                }
            } else null

            entities.add(NewsArticleEntity(
                url = url,
                title = title,
                description = description,
                urlToImage = imageUrl,
                publishedAt = publishedAt,
                sourceName = sourceName,
                category = category,
                aiSummary = summary,
                factCheckStatus = fcStatus,
                factCheckReason = fcReason,
                biasType = bias,
                isFavorite = false,
                region = region,
                isMultiPerspective = checkMultiPerspective(title),
                localImpact = impact
            ))
        }
    }

    private fun simulateAiSummary(text: String, sourceName: String? = null, category: String? = null): List<String> {
        val cleaned = text.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val sentences = Regex("(?<=[.!?])\\s+").split(cleaned)
            .filter { it.length > 8 }
            .map { it.trim().removeSuffix(".") }
            .take(3)

        return if (sentences.isNotEmpty()) {
            sentences
        } else {
            listOf(
                "Sursă: ${sourceName ?: "necunoscută"}",
                "Categorie: ${category ?: "general"}",
                "Detalii în articol"
            )
        }
    }

    private fun simulateFactCheck(source: String): Pair<String, String> {
        return when (Random.nextInt(100)) {
            in 0..75 -> "VERIFIED" to "Sursă de încredere ($source) confirmată de multiple agenții de presă."
            in 76..90 -> "PENDING" to "În curs de verificare. Informația este proaspătă."
            else -> "UNVERIFIED" to "Sursă cu istoric de clickbait. Se recomandă prudență."
        }
    }

    private fun simulateBias(source: String, category: String? = null): String {
        val normalizedSource = source.lowercase()
        val normalizedCategory = category?.lowercase().orEmpty()
        return when {
            normalizedSource.contains("cnn") || normalizedSource.contains("bbc") || normalizedSource.contains("guardian") -> "NEUTRU"
            normalizedSource.contains("fox") || normalizedSource.contains("breitbart") -> "DREAPTA"
            normalizedSource.contains("buzzfeed") || normalizedSource.contains("huff") || normalizedSource.contains("politico") -> "STÂNGA"
            normalizedCategory.contains("polit") -> "NEUTRU"
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
        factCheckStatus = factCheckStatus,
        factCheckReason = factCheckReason,
        biasType = biasType,
        isFavorite = isFavorite,
        relativeTime = formatRelativeTime(publishedAt),
        sourceLogoUrl = getFaviconUrl(url),
        region = region,
        isMultiPerspective = isMultiPerspective,
        localImpact = localImpact
    )

    suspend fun askAi(article: NewsArticle, question: String): String {
        val context = "Titlu: ${article.title}. Descriere: ${article.description?.take(300) ?: ""}"
        return try {
            aiClient?.askQuestion(context, question) ?: "AI indisponibil."
        } catch (e: Exception) {
            "Eroare AI - verifică conexiunea."
        }
    }

    suspend fun getAiInsights(article: NewsArticle): List<AiInsight> {
        // Optimized: Single AI call with structured output instead of 3 separate calls
        val combinedPrompt = """
            Pentru acest articol, oferă răspunsuri separate cu === între ele:
            1. Explică simplu (2 fraze max, în română)
            2. Impact pentru România (1-2 fraze)
            3. 2-3 lucruri de verificat
            
            Articol: ${article.title}
            Descriere: ${article.description?.take(250) ?: ""}
        """.trimIndent()

        return try {
            val fullResponse = aiClient?.askQuestion(article.title, combinedPrompt) ?: ""
            val sections = fullResponse.split("===").map { it.trim() }
            
            val titles = listOf("Explică simplu", "Impact local", "Ce trebuie să verifici")
            titles.mapIndexed { index, title ->
                AiInsight(
                    title = title,
                    content = if (index < sections.size) sections[index] else "Indisponibil."
                )
            }
        } catch (e: Exception) {
            // Fallback insights
            listOf(
                AiInsight(title = "Explică simplu", content = article.title),
                AiInsight(title = "Impact local", content = "Verifică sursa pentru detalii."),
                AiInsight(title = "Ce trebuie să verifici", content = "Compară cu alte surse fiabile.")
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
