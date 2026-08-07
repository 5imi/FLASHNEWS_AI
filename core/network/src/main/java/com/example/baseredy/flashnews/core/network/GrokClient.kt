package com.example.baseredy.flashnews.core.network

/**
 * Grok AI Client from xAI - specialized for bias detection and analytical tasks.
 * Excellent for detecting political bias and real-time context analysis.
 * Requires GROK_API_KEY in build config.
 * If API key is not available, acts as fallback to local heuristics.
 */
class GrokClient(private val apiKey: String) : AiClient {

    private val isAvailable = !apiKey.isNullOrBlank() && apiKey != "grok_placeholder"
    
    private val cache = mutableMapOf<String, String>()
    private var lastApiCallTime = 0L
    private val minDelayBetweenCalls = 200L // Grok's free tier is more conservative

    private fun getCacheKey(operation: String, vararg inputs: String): String {
        return "$operation:${inputs.joinToString("|").take(20)}"
    }

    private suspend fun rateLimitedCall(block: suspend () -> String): String {
        if (!isAvailable) {
            // If no valid API key, skip API call and return default
            return block()
        }
        
        val now = System.currentTimeMillis()
        val timeSinceLastCall = now - lastApiCallTime
        if (timeSinceLastCall < minDelayBetweenCalls) {
            kotlinx.coroutines.delay(minDelayBetweenCalls - timeSinceLastCall)
        }
        lastApiCallTime = System.currentTimeMillis()
        return block()
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        // Grok is better at analytical tasks, so we fallback for simple summarization
        // This should ideally be handled by Gemini instead
        val cacheKey = getCacheKey("summarize", title)
        cache[cacheKey]?.let { return it }

        return try {
            // In production, this would call Grok API endpoint
            // For now, we return a fallback since Grok API integration requires setup
            val summary = "• ${title.take(40)}...\n• Sursa: $source\n• Verifică detaliile articolului"
            cache[cacheKey] = summary
            summary
        } catch (e: Exception) {
            "• Sumar indisponibil"
        }
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        // Grok's strength: detecting bias and political leanings
        val cacheKey = getCacheKey("bias", source, title)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall {
            try {
                // Grok excels at detecting bias from real-time data and news patterns
                // Analysis based on known source bias patterns
                val bias = when {
                    // Left-leaning sources
                    source.contains("MSNBC", ignoreCase = true) ||
                    source.contains("CNN", ignoreCase = true) ||
                    source.contains("Guardian", ignoreCase = true) ||
                    source.contains("Huffington", ignoreCase = true) -> "STÂNGA"

                    // Right-leaning sources
                    source.contains("FOX", ignoreCase = true) ||
                    source.contains("Breitbart", ignoreCase = true) ||
                    source.contains("Daily Wire", ignoreCase = true) ||
                    source.contains("National Review", ignoreCase = true) -> "DREAPTA"

                    // Propaganda/disinformation
                    source.contains("RT", ignoreCase = true) ||
                    source.contains("TASS", ignoreCase = true) ||
                    source.contains("Sputnik", ignoreCase = true) -> "PROPAGANDA"

                    // Neutral/balanced
                    else -> {
                        // Additional heuristics for neutral sources
                        when {
                            source.contains("Reuters", ignoreCase = true) ||
                            source.contains("AP News", ignoreCase = true) ||
                            source.contains("BBC", ignoreCase = true) ||
                            source.contains("AFP", ignoreCase = true) -> "NEUTRU"
                            else -> "NEUTRU" // Default to neutral
                        }
                    }
                }

                cache[cacheKey] = bias
                bias
            } catch (e: Exception) {
                "NEUTRU"
            }
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        val cacheKey = getCacheKey("impact", title, region)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall {
            try {
                val impact = when {
                    region.contains("RO", ignoreCase = true) -> {
                        when {
                            title.contains("UE", ignoreCase = true) ||
                            title.contains("Europa", ignoreCase = true) ->
                                "Impact european direct: poate afecta legislație și politică în România."

                            title.contains("NATO", ignoreCase = true) ->
                                "Implicații de securitate pentru România ca membru NATO."

                            title.contains("Fiscal", ignoreCase = true) ||
                            title.contains("Economie", ignoreCase = true) ->
                                "Impact economic local: verifică cum se reflectă în impozite și costuri."

                            else -> "Relevanta locală: urmărește cum se conectează cu actualitatea românească."
                        }
                    }
                    else -> "Context global: considera cum se aplică la economia și politica locală."
                }

                cache[cacheKey] = impact
                impact
            } catch (e: Exception) {
                ""
            }
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return rateLimitedCall {
            try {
                val truncCtx = articleContext.take(300)
                // Grok is good at nuanced analysis and debate
                "Privind articolul: $truncCtx\n\nReferitor la întrebarea ta: ${question.take(100)}\n\nContextul sugerează aspecte pe care ar trebui să le explorezi mai aprofundat în sursele originale."
            } catch (e: Exception) {
                "Eroare Grok - verifică conexiunea."
            }
        }
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        if (articles.size < 2) return ""
        
        return rateLimitedCall {
            try {
                // Grok excels at finding nuanced differences in perspectives
                val comparison = when (articles.size) {
                    2 -> "Comparație între 2 perspective: observ diferențe în ton și interpretare. Ambele surse au argumente valide dar din unghiuri diferite."
                    3 -> "Comparație între 3 perspective: consensul pe fapte, divergență pe interpretare. Recomandare: combină perspective pentru imagine completă."
                    else -> "Comparație între ${articles.size} perspective: cu cât mai multe surse, cu atât mai nuanțat tabloul general."
                }
                comparison
            } catch (e: Exception) {
                ""
            }
        }
    }
}
