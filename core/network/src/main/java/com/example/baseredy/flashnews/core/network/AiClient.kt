package com.example.baseredy.flashnews.core.network

import android.util.Log

/**
 * Common interface for all AI providers.
 */
interface AiClient {
    suspend fun summarize(title: String, description: String, source: String = "", category: String = "", region: String = ""): String
    suspend fun analyzeBias(source: String, title: String, category: String = "", region: String = ""): String
    suspend fun analyzeLocalImpact(title: String, description: String, region: String = ""): String
    suspend fun askQuestion(articleContext: String, question: String): String

    // Optional: for future multi-perspective analysis
    suspend fun comparePerspectives(articles: List<String>): String = ""
}

enum class AiProviderType {
    GEMINI,
    GROK,
    GROQ,
    OPENROUTER,
    LOCAL,
    HYBRID
}

/**
 * Orchestrates AI tasks across multiple agents with smart routing and cloud failover.
 */
class AiOrchestrator(
    private val fastAgent: AiClient,           // Gemini 1.5 Flash
    private val analyticalAgent: AiClient,     // Grok
    private val groqAgent: AiClient,           // Groq (Failover)
    private val openRouterAgent: AiClient,     // OpenRouter (Failover)
    private val localAgent: AiClient           // Local heuristics
) : AiClient {

    private val cloudFailoverPool = listOf(fastAgent, groqAgent, openRouterAgent, analyticalAgent)

    private suspend fun <T> runWithFailover(
        primary: AiClient,
        block: suspend (AiClient) -> T,
        fallback: T
    ): T {
        // Try primary first
        try {
            Log.d("AiOrchestrator", "Trying primary: ${primary.javaClass.simpleName}")
            val result = block(primary)
            if (isValid(result)) return result
            Log.w("AiOrchestrator", "Primary returned invalid result, trying failover pool")
        } catch (e: Exception) {
            Log.e("AiOrchestrator", "Primary failed: ${e.message}")
        }

        // Try failover pool
        for (agent in cloudFailoverPool) {
            if (agent == primary) continue
            try {
                Log.d("AiOrchestrator", "Trying failover agent: ${agent.javaClass.simpleName}")
                val result = block(agent)
                if (isValid(result)) {
                    Log.i("AiOrchestrator", "Failover success with ${agent.javaClass.simpleName}")
                    return result
                }
            } catch (e: Exception) {
                Log.e("AiOrchestrator", "Agent ${agent.javaClass.simpleName} failover failed: ${e.message}")
            }
        }

        // Final fallback to local
        Log.w("AiOrchestrator", "All cloud agents failed, falling back to local heuristics")
        return try {
            block(localAgent)
        } catch (e: Exception) {
            fallback
        }
    }

    private fun isValid(result: Any?): Boolean {
        return when (result) {
            is String -> result.isNotBlank() && !result.contains("indisponibil", ignoreCase = true)
            else -> result != null
        }
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        return runWithFailover(
            primary = fastAgent,
            block = { it.summarize(title, description, source, category, region) },
            fallback = "• Sumar indisponibil momentan."
        )
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        return runWithFailover(
            primary = analyticalAgent,
            block = { it.analyzeBias(source, title, category, region) },
            fallback = "NEUTRU"
        )
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        return runWithFailover(
            primary = analyticalAgent,
            block = { it.analyzeLocalImpact(title, description, region) },
            fallback = ""
        )
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return runWithFailover(
            primary = fastAgent,
            block = { it.askQuestion(articleContext, question) },
            fallback = "Eroare AI - serviciile sunt ocupate. Încearcă mai târziu."
        )
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        if (articles.size < 2) return ""
        return runWithFailover(
            primary = analyticalAgent,
            block = { it.comparePerspectives(articles) },
            fallback = ""
        )
    }
}
