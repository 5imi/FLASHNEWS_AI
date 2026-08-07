package com.example.baseredy.flashnews.core.network

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import java.security.MessageDigest

class GeminiClient(apiKey: String) : AiClient {
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
    )

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        safetySettings = safetySettings
    )

    // Simple in-memory cache: key -> value
    private val cache = mutableMapOf<String, String>()
    private var lastApiCallTime = 0L
    private val minDelayBetweenCalls = 100L // 100ms to respect rate limits

    private fun hashInput(vararg inputs: String): String {
        val combined = inputs.joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(combined.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    private fun getCacheKey(operation: String, vararg inputs: String): String {
        return "$operation:${hashInput(*inputs)}"
    }

    private suspend fun rateLimitedCall(block: suspend () -> String): String {
        val now = System.currentTimeMillis()
        val timeSinceLastCall = now - lastApiCallTime
        if (timeSinceLastCall < minDelayBetweenCalls) {
            delay(minDelayBetweenCalls - timeSinceLastCall)
        }
        lastApiCallTime = System.currentTimeMillis()
        return block()
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        val cacheKey = getCacheKey("summarize", title, description)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall {
            try {
                val truncDesc = description.take(300) // Limit input to reduce tokens
                val response = model.generateContent(
                    content {
                        text("Rezumă rapid în 2-3 puncte scurte:\nTitlu: $title\nDescriere: $truncDesc\nRăspunde în ROMÂNĂ, cu •, maxim 10 cuvinte/punct.")
                    }
                )
                response.text?.let { result ->
                    cache[cacheKey] = result
                    result
                } ?: "• Sumar disponibil\n• Verifica articolul"
            } catch (e: Exception) {
                "• Sumar disponibil\n• Verifica articolul"
            }
        }
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        val cacheKey = getCacheKey("bias", source, title)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall {
            try {
                val response = model.generateContent(
                    content {
                        text("Sursa: $source\nTitlu: $title\nO SINGURA CUVANT: NEUTRU, STANGA, DREAPTA sau PROPAGANDA")
                    }
                )
                response.text?.trim()?.uppercase()?.let { result ->
                    val valid = setOf("NEUTRU", "STANGA", "DREAPTA", "PROPAGANDA")
                    val bias = if (result in valid) result else "NEUTRU"
                    cache[cacheKey] = bias
                    bias
                } ?: "NEUTRU"
            } catch (e: Exception) {
                "NEUTRU"
            }
        }
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        if (articles.size < 2) return ""
        
        val cacheKey = getCacheKey("compare", *articles.toTypedArray())
        cache[cacheKey]?.let { return it }

        return rateLimitedCall {
            try {
                val truncated = articles.take(2).map { it.take(200) }.joinToString("\n---\n")
                val response = model.generateContent(
                    content {
                        text("Compara rapid aceste 2 articole.\nDiferente majore:\n$truncated\nRezumat 50 cuvinte in ROMANA.")
                    }
                )
                response.text?.let { result ->
                    cache[cacheKey] = result
                    result
                } ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        // Don't cache questions - they're user-specific
        return rateLimitedCall {
            try {
                val truncCtx = articleContext.take(400)
                val response = model.generateContent(
                    content {
                        text("Articol: $truncCtx\n\nIntrebare: $question\n\nRaspunde concis in ROMANA, max 100 cuvinte.")
                    }
                )
                response.text ?: "Nu pot raspunde acum."
            } catch (e: Exception) {
                "Eroare AI - verificati conexiunea."
            }
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        val cacheKey = getCacheKey("impact", title, region)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall {
            try {
                val truncDesc = description.take(250)
                val response = model.generateContent(
                    content {
                        text("Impact asupra ROMANIEI:\nTitlu: $title\nContext: $truncDesc\n1-2 fraze, ROMANA, max 50 cuvinte.")
                    }
                )
                response.text?.let { result ->
                    cache[cacheKey] = result
                    result
                } ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}
