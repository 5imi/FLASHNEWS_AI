package com.example.baseredy.flashnews.core.network

/**
 * Common interface for all AI providers.
 */
interface AiClient {
    suspend fun summarize(title: String, description: String): String
    suspend fun analyzeBias(source: String, title: String): String
    suspend fun analyzeLocalImpact(title: String, description: String): String
    suspend fun askQuestion(articleContext: String, question: String): String
    
    // Optional: for future multi-perspective analysis
    suspend fun comparePerspectives(articles: List<String>): String = ""
}

enum class AiProviderType {
    GEMINI,
    GROQ_LLAMA,
    MISTRAL
}
