package com.example.baseredy.flashnews.core.network

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
    LOCAL,
    HYBRID
}

/**
 * Orchestrates AI tasks across multiple agents with smart routing.
 * 
 * Strategy:
 * - Summarize: Use fast agent (Gemini) for speed and large context
 * - Analyze Bias: Use analytical agent (Grok) for precision
 * - Local Impact: Try analytical first, fallback to local/Gemini
 * - Ask Question: Route based on context size and complexity
 * - Compare Perspectives: Use analytical agent (Grok) for nuance
 */
class AiOrchestrator(
    private val fastAgent: AiClient,           // Gemini 1.5 Flash - for summarization
    private val analyticalAgent: AiClient,     // Grok - for bias detection and analysis
    private val localAgent: AiClient           // LocalAiClient - for fallback/offline
) : AiClient {

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        // Route to fast agent for summarization (Gemini excels here)
        return try {
            fastAgent.summarize(title, description, source, category, region)
        } catch (e: Exception) {
            // Failover: if fast agent fails, try analytical, then local
            try {
                analyticalAgent.summarize(title, description, source, category, region)
            } catch (e2: Exception) {
                localAgent.summarize(title, description, source, category, region)
            }
        }
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        // Route to analytical agent for bias detection (Grok's specialty)
        return try {
            analyticalAgent.analyzeBias(source, title, category, region)
        } catch (e: Exception) {
            // Failover to local heuristics
            localAgent.analyzeBias(source, title, category, region)
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        // Try analytical first for nuance, fallback to local
        return try {
            analyticalAgent.analyzeLocalImpact(title, description, region)
        } catch (e: Exception) {
            localAgent.analyzeLocalImpact(title, description, region)
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        // Route to fast agent, fallback to analytical, then local
        return try {
            fastAgent.askQuestion(articleContext, question)
        } catch (e: Exception) {
            try {
                analyticalAgent.askQuestion(articleContext, question)
            } catch (e2: Exception) {
                localAgent.askQuestion(articleContext, question)
            }
        }
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        if (articles.size < 2) return ""
        
        // Use analytical agent for nuanced comparison
        return try {
            analyticalAgent.comparePerspectives(articles)
        } catch (e: Exception) {
            localAgent.comparePerspectives(articles)
        }
    }
}

/**
 * Consensus voting system for critical fact-checking.
 * Queries multiple agents and compares results.
 */
class ConsensusAiClient(
    private val agents: List<AiClient>,
    private val orchestrator: AiOrchestrator
) : AiClient {

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        return orchestrator.summarize(title, description, source, category, region)
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        // For bias: collect votes from multiple agents
        if (agents.size < 2) return orchestrator.analyzeBias(source, title, category, region)
        
        return try {
            val results = agents.take(2).map { agent ->
                try {
                    agent.analyzeBias(source, title, category, region)
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()

            // Consensus: if 2+ agree on same result, use that; else use orchestrator
            if (results.isNotEmpty()) {
                val mostCommon = results.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                mostCommon ?: orchestrator.analyzeBias(source, title, category, region)
            } else {
                orchestrator.analyzeBias(source, title, category, region)
            }
        } catch (e: Exception) {
            orchestrator.analyzeBias(source, title, category, region)
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        return orchestrator.analyzeLocalImpact(title, description, region)
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return orchestrator.askQuestion(articleContext, question)
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        return orchestrator.comparePerspectives(articles)
    }
}

class MultiAgentAiClient(
    private val primary: AiClient,
    private val secondary: List<AiClient> = emptyList()
) : AiClient {
    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        val primaryResult = primary.summarize(title, description, source, category, region)
        if (primaryResult.isBlank() && secondary.isNotEmpty()) {
            return secondary.first().summarize(title, description, source, category, region)
        }
        return primaryResult
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        val primaryResult = primary.analyzeBias(source, title, category, region)
        if (primaryResult.isBlank() && secondary.isNotEmpty()) {
            return secondary.first().analyzeBias(source, title, category, region)
        }
        return primaryResult
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        val primaryResult = primary.analyzeLocalImpact(title, description, region)
        if (primaryResult.isBlank() && secondary.isNotEmpty()) {
            return secondary.first().analyzeLocalImpact(title, description, region)
        }
        return primaryResult
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return primary.askQuestion(articleContext, question)
    }
}
