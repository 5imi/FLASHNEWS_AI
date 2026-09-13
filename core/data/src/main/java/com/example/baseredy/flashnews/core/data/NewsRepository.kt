package com.example.baseredy.flashnews.core.data

import com.example.baseredy.flashnews.core.database.NewsDao
import com.example.baseredy.flashnews.core.database.NewsArticleEntity
import com.example.baseredy.flashnews.core.database.CustomRssFeedDao
import com.example.baseredy.flashnews.core.database.CustomRssFeedEntity
import com.example.baseredy.flashnews.core.network.RssSource
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
    private val aiClient: AiClient? = null,
    private val customFeedDao: CustomRssFeedDao? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun getArticles(region: String, category: String): Flow<PagingData<NewsArticle>> {
        val pagingSourceFactory = if (category.contains("Sursele Mele") || category.contains("⭐")) {
            val followed = runCatching { 
                kotlinx.coroutines.runBlocking { customFeedDao?.getFollowedFeedsSync()?.map { it.name } } 
            }.getOrNull() ?: emptyList()
            if (followed.isNotEmpty()) {
                { newsDao.getArticlesBySources(followed) }
            } else {
                { newsDao.getArticlesByRegion(region) }
            }
        } else if (category == "Toate") {
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
                val customFeeds = runCatching { customFeedDao?.getFollowedFeedsSync() }.getOrNull() ?: emptyList()
                val extraSources = customFeeds.map { RssSource(name = it.name, url = it.url, category = it.category, region = it.region) }
                val rssItems = RssClient.fetchRssNews(targetRegion = region, targetCategory = category, extraSources = extraSources)

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
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        // 1. Instant local search from Room Database (Offline first, Romanian & Global RSS)
        val localResults = try {
            newsDao.searchArticles(cleanQuery).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }

        // 2. Supplementary remote search from NewsAPI if online and API key present
        val remoteResults = if (apiKey.isNotBlank() && cleanQuery.length >= 2) {
            try {
                val response = RetrofitClient.newsApi.searchNews(apiKey = apiKey, query = cleanQuery)
                response.articles.map { dto ->
                    val existing = newsDao.getArticleByUrl(dto.url)
                    if (existing != null) {
                        existing.toDomain()
                    } else {
                        NewsArticle(
                            url = dto.url,
                            title = dto.title,
                            description = dto.description,
                            urlToImage = dto.urlToImage,
                            publishedAt = dto.publishedAt,
                            sourceName = dto.source?.name,
                            aiSummary = simulateAiSummary(dto.description ?: dto.title),
                            aiBias = simulateBias(dto.source?.name ?: "", "General"),
                            isFavorite = newsDao.isArticleFavorite(dto.url),
                            region = "GLOBAL",
                            category = "General"
                        )
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Return combined list, local first, deduplicated by URL
        return (localResults + remoteResults).distinctBy { it.url }
    }

    suspend fun getArticleByUrl(url: String): NewsArticle? {
        return newsDao.getArticleByUrl(url)?.toDomain()
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

        val cleanedDescription = description
            ?.replace(Regex("""\[\s*(&#8230;|&hellip;|…|\.\.\.)\s*\]"""), "…")
            ?.replace(Regex("""&#8230;|&hellip;"""), "…")
            ?.trim()

        return NewsArticle(
            title = title,
            description = cleanedDescription ?: description,
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
            category = category,
            isMultiPerspective = checkMultiPerspective(title)
        )
    }

    suspend fun askAi(article: NewsArticle, question: String, tone: String = "EXECUTIV"): String {
        val toneInstruction = when (tone.uppercase()) {
            "SIMPLU" -> "Raspunde intr-un limbaj simplu, prietenos si foarte clar (stil ELI5), fara jargon."
            "CRITIC" -> "Analizeaza critic aceasta perspectiva, investigheaza eventualele omisiuni si verifica factual nuantele."
            else -> "Raspunde concis, structurat si orientat pe fapte cheie si impact (stil executiv / business)."
        }
        val context = "Titlu: " + article.title + ". Descriere: " + (article.description?.take(1500) ?: "")
        val formattedQuestion = "[Ton: " + toneInstruction + "]\nIntrebare: " + question
        val response = try {
            aiClient?.askQuestion(context, formattedQuestion) ?: "AI indisponibil."
        } catch (e: Exception) {
            "Eroare AI - verifica conexiunea."
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
                category = article.category,
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

    // ==========================================
    
    // ==========================================
    // CATALOG RSS & SURSELE MELE
    // ==========================================
    fun getAllCatalogSources(): List<RssSource> = RssClient.getAllCatalogSources()

    fun getFollowedFeeds(): Flow<List<CustomRssFeedEntity>>? = customFeedDao?.getAllFeeds()

    suspend fun toggleFollowSource(name: String, url: String, category: String, region: String, isFollowed: Boolean) {
        if (isFollowed) {
            customFeedDao?.insertOrUpdate(
                CustomRssFeedEntity(
                    name = name,
                    url = url,
                    category = category,
                    region = region,
                    isFollowed = true,
                    isCustomUrl = false
                )
            )
        } else {
            customFeedDao?.updateFollowStatus(url, false)
        }
    }

    suspend fun addCustomFeed(url: String, customName: String? = null, category: String = "Personalizat"): Pair<Boolean, String> {
        val (valid, result) = RssClient.validateRssFeed(url)
        if (!valid) return Pair(false, result)
        val finalName = if (!customName.isNullOrBlank()) customName.trim() else result
        val entity = CustomRssFeedEntity(
            name = finalName,
            url = url.trim(),
            category = category,
            region = "RO",
            isFollowed = true,
            isCustomUrl = true
        )
        customFeedDao?.insertOrUpdate(entity)

        // Proactively fetch initial articles for this newly added feed
        repositoryScope.launch {
            try {
                val items = RssClient.fetchSingleFeed(url.trim(), finalName, "RO", category)
                val articleEntities = items.map { rss ->
                    NewsArticleEntity(
                        url = rss.link,
                        title = rss.title,
                        description = rss.description,
                        urlToImage = rss.imageUrl,
                        publishedAt = rss.pubDate,
                        sourceName = rss.sourceName,
                        category = rss.category,
                        region = rss.region,
                        aiSummary = simulateAiSummary(rss.description ?: rss.title),
                        aiBias = simulateBias(rss.sourceName, rss.category),
                        aiLocalImpact = null,
                        aiAnalyzedAt = 0L,
                        isFavorite = false,
                        isMultiPerspective = false
                    )
                }
                newsDao.insertArticlesIfAbsent(articleEntities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Pair(true, finalName)
    }

    suspend fun deleteCustomFeed(url: String) {
        customFeedDao?.deleteFeedByUrl(url)
    }

    // ==========================================
    // RADIO AI - BULETINUL ZILEI (PODCAST)
    // ==========================================
    suspend fun generateDailyRadioBriefing(): String {
        val recent = newsDao.getRecentArticles(25)
        if (recent.isEmpty()) {
            return "Buna dimineata! Nu avem articole recente disponibile in memorie. Te rugam sa tragi in jos pentru reimprospatare."
        }
        val selected = mutableListOf<NewsArticleEntity>()
        val seenCategories = mutableSetOf<String>()
        for (item in recent) {
            if (item.category !in seenCategories && selected.size < 5) {
                seenCategories.add(item.category)
                selected.add(item)
            }
        }
        if (selected.size < 3) {
            selected.clear()
            selected.addAll(recent.take(5))
        }

        val headlinesList = selected.mapIndexed { idx, it ->
            val takeaway = it.aiSummary?.takeIf { s -> !s.startsWith("{") } ?: it.description ?: ""
            "${idx + 1}. [${it.category} - ${it.sourceName}] ${it.title}. ${takeaway.take(200)}"
        }.joinToString("\n")

        val prompt = "Esti un prezentator de radio profesionist, cald, alert si concis pentru FlashNews AI. Creeaza un buletin radio de 2 minute in limba ROMANA pe baza acestor evenimente:\n" + headlinesList + "\n\nIncepe cu 'Buna dimineata! Iata sinteza celor mai importante evenimente ale momentului in FlashNews:'. Conecteaza stirile fluid cu tranzitii naturale. Incheie cu 'Aceasta a fost sinteza FlashNews AI. Ramai informat!' Fii direct, fara cuvinte de umplutura."
        
        return try {
            val response = aiClient?.askQuestion("Evenimente:\n" + headlinesList, prompt)
            if (!response.isNullOrBlank() && !response.contains("indisponibil") && !response.contains("Eroare")) {
                response
            } else {
                buildFallbackRadioScript(selected)
            }
        } catch (e: Exception) {
            buildFallbackRadioScript(selected)
        }
    }

    private fun buildFallbackRadioScript(articles: List<NewsArticleEntity>): String {
        return buildString {
            append("Buna dimineata! Iata sinteza celor mai importante evenimente ale momentului in FlashNews. ")
            articles.forEachIndexed { i, a ->
                append("Stirea ")
                append(i + 1)
                append(": ")
                append(a.title)
                append(". Din categoria ")
                append(a.category)
                append(", transmis de ")
                append(a.sourceName)
                append(". ")
                val sum = a.aiSummary?.takeIf { !it.startsWith("{") } ?: a.description
                if (!sum.isNullOrBlank()) {
                    append(sum.take(160).replace("\n", " "))
                    append(". ")
                }
            }
            append("Aceasta a fost sinteza rapida a zilei in FlashNews AI. O zi productiva!")
        }
    }

    // ==========================================
    // PERSPECTIVA 360 (COMPARATIE ZIARE)
    // ==========================================
    suspend fun get360Perspective(article: NewsArticle): String {
        val keywords = article.title.split(" ")
            .filter { it.length > 4 }
            .map { it.lowercase().trim(',', '.', ':', '"') }
        
        val recent = newsDao.getRecentArticles(40).map { it.toDomain() }
        val related = recent.filter { other ->
            other.url != article.url && other.sourceName != article.sourceName &&
            keywords.any { kw -> other.title.lowercase().contains(kw) }
        }.take(3)

        if (related.isEmpty()) {
            return "Perspectiva unica: Acest eveniment a fost relatat exclusiv de " + (article.sourceName ?: "aceasta sursa") + ". Nu s-au detectat inca variatii divergente de la alte redactii in ultimele 24 de ore."
        }

        val articlesText = buildString {
            append("1. [")
            append(article.sourceName)
            append("]: ")
            append(article.title)
            append("\n")
            related.forEachIndexed { i, r ->
                append(i + 2)
                append(". [")
                append(r.sourceName)
                append("]: ")
                append(r.title)
                append("\n")
            }
        }

        val prompt = "Analizeaza comparativ tratarea acestui subiect de catre publicatii diferite:\n" + articlesText + "\n\nStructureaza raspunsul in ROMANA:\n- Fapte confirmate unanim de toate sursele\n- Nuante si diferente de accent intre publicatii\n- Concluzie privind neutralitatea relatarii."
        return try {
            aiClient?.askQuestion(articlesText, prompt) ?: "Comparatie indisponibila."
        } catch (e: Exception) {
            "S-au identificat " + (related.size + 1) + " surse diferite raportand acest subiect. Faptele de baza coincid in privinta desfasurarii evenimentului."
        }
    }

    suspend fun findRelatedSourcesCount(article: NewsArticle): Int {
        val keywords = article.title.split(" ")
            .filter { it.length > 4 }
            .map { it.lowercase().trim(',', '.', ':', '"') }
        if (keywords.isEmpty()) return 1
        val recent = newsDao.getRecentArticles(30)
        val count = recent.count { other ->
            other.url != article.url && other.sourceName != article.sourceName &&
            keywords.any { kw -> other.title.lowercase().contains(kw) }
        }
        return count + 1
    }

    suspend fun exportOpml(): String {
        val followed = customFeedDao?.getFollowedFeedsSync() ?: emptyList()
        return OpmlManager.exportToOpml(followed)
    }

    suspend fun importOpml(xmlContent: String): Int {
        val dao = customFeedDao ?: return 0
        val parsed = OpmlManager.parseOpml(xmlContent)
        for (item in parsed) {
            dao.insertOrUpdate(
                com.example.baseredy.flashnews.core.database.CustomRssFeedEntity(
                    name = item.title,
                    url = item.xmlUrl,
                    category = item.category,
                    region = "RO",
                    isFollowed = true,
                    isCustomUrl = true
                )
            )
        }
        return parsed.size
    }

}
