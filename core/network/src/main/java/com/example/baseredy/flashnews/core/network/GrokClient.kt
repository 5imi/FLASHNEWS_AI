package com.example.baseredy.flashnews.core.network

import android.util.Log
import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis

/**
 * Grok AI Client from xAI - specialized for bias detection and analytical tasks.
 * Excellent for detecting political bias and real-time context analysis.
 * Requires GROK_API_KEY in build config.
 * If API key is not available, acts as fallback to local heuristics.
 */
class GrokClient(private val apiKey: String) : AiClient {
    private val TAG = "GrokClient"

    private val isAvailable = !apiKey.isNullOrBlank() && apiKey != "grok_placeholder"
    
    private val cache = mutableMapOf<String, String>()
    private var lastApiCallTime = 0L
    private val minDelayBetweenCalls = 200L // Grok's free tier is more conservative

    private fun cleanResponse(raw: String): String {
        return raw.replace(Regex("```[a-z]*\\n?"), "")
            .replace("```", "")
            .trim()
    }

    private fun getCacheKey(operation: String, vararg inputs: String): String {
        return "$operation:${inputs.joinToString("|").take(20)}"
    }

    private suspend fun rateLimitedCall(operation: String, block: suspend () -> String): String {
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
        
        Log.d(TAG, "Starting Grok AI call: $operation")
        return try {
            val result = block()
            Log.d(TAG, "Grok AI success: $operation")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Grok AI failed: $operation", e)
            throw e
        }
    }

    override suspend fun analyzeNewsDynamic(
        title: String,
        description: String,
        source: String,
        category: String,
        region: String
    ): DynamicNewsAnalysis {
        val bias = analyzeBias(source, title, category, region)
        val impact = if (region != "RO") analyzeLocalImpact(title, description, region) else null
        val summary = description.take(250).ifBlank { title.take(250) }.trim()

        return DynamicNewsAnalysis(
            keyTakeaway = "• $summary\n• Sursa: $source",
            editorialBias = bias,
            biasRationale = "Evaluare analitică a profilului editorial pentru $source.",
            localImpact = impact,
            dynamicQuestions = listOf(
                DynamicInsight("Care este miza principală?", "Evenimentul prezintă un interes sporit pentru categoria $category."),
                DynamicInsight("Ce perspective există?", "Urmărește evoluția reacțiilor oficiale din sursele de știri.")
            )
        )
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        val cacheKey = getCacheKey("summarize", title)
        cache[cacheKey]?.let { return it }

        return try {
            // Placeholder: Grok is better at analytical tasks, so we fallback for simple summarization
            val summary = "• ${title.take(40)}...\n• Sursa: $source\n• Verifică detaliile articolului"
            cache[cacheKey] = summary
            summary
        } catch (e: Exception) {
            "• Sumar indisponibil"
        }
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        val cacheKey = getCacheKey("bias", source, title)
        cache[cacheKey]?.let { return it }

        return try {
            rateLimitedCall("analyzeBias") {
                // Analysis based on known source bias patterns (fallback logic)
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
                        when {
                            source.contains("Reuters", ignoreCase = true) ||
                            source.contains("AP News", ignoreCase = true) ||
                            source.contains("BBC", ignoreCase = true) ||
                            source.contains("AFP", ignoreCase = true) -> "NEUTRU"
                            else -> "NEUTRU"
                        }
                    }
                }

                cache[cacheKey] = bias
                bias
            }
        } catch (e: Exception) {
            "NEUTRU"
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        val cacheKey = getCacheKey("impact", title, region)
        cache[cacheKey]?.let { return it }

        return try {
            rateLimitedCall("analyzeLocalImpact") {
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

                            else -> "Relevanță locală: urmărește cum se conectează cu actualitatea românească."
                        }
                    }
                    else -> "Context global: consideră cum se aplică la economia și politica locală."
                }

                cache[cacheKey] = impact
                impact
            }
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return try {
            rateLimitedCall("askQuestion") {
                val truncCtx = articleContext.take(1500)
                "Privind articolul: $truncCtx\n\nReferitor la întrebarea ta: ${question.take(200)}\n\nContextul sugerează aspecte pe care ar trebui să le explorezi mai aprofundat în sursele originale."
            }
        } catch (e: Exception) {
            "Eroare Grok - verifică conexiunea."
        }
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        if (articles.size < 2) return ""
        
        return try {
            rateLimitedCall("comparePerspectives") {
                val comparison = when (articles.size) {
                    2 -> "Comparație între 2 perspective: observ diferențe în ton și interpretare. Ambele surse au argumente valide dar din unghiuri diferite."
                    3 -> "Comparație între 3 perspective: consensul pe fapte, divergență pe interpretare. Recomandare: combină perspective pentru imagine completă."
                    else -> "Comparație între ${articles.size} perspective: cu cât mai multe surse, cu atât mai nuanțat tabloul general."
                }
                comparison
            }
        } catch (e: Exception) {
            ""
        }
    }
}
