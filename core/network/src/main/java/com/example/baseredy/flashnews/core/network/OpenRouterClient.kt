package com.example.baseredy.flashnews.core.network

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

class OpenRouterClient(private val apiKey: String) : AiClient {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val model = "deepseek/deepseek-chat" // Updated to DeepSeek Chat

    private val cache = mutableMapOf<String, String>()
    private var lastApiCallTime = 0L
    private val minDelayBetweenCalls = 500L

    private val systemPrompt = "Ești un asistent jurnalist inteligent. Răspunzi clar în limba română, fără introduceri inutile, respectând strict formatul cerut."

    private fun cleanResponse(raw: String): String {
        return raw.replace(Regex("```[a-z]*\\n?"), "")
            .replace("```", "")
            .trim()
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String = withContext(Dispatchers.IO) {
        val cacheKey = "sum_or:${title.hashCode()}"
        cache[cacheKey]?.let { return@withContext it }

        val prompt = "Rezumă în 3 idei principale sub formă de puncte (•):\nTitlu: $title\nDescriere: $description"
        val res = callAi(prompt) ?: "• Sumar momentan indisponibil"
        cache[cacheKey] = res
        res
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String = withContext(Dispatchers.IO) {
        val prompt = "Bias politic pentru '$source' în titlul '$title'. Răspunde doar cu: NEUTRU, STANGA, DREAPTA sau PROPAGANDA."
        callAi(prompt)?.uppercase()?.filter { it.isLetter() } ?: "NEUTRU"
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String = withContext(Dispatchers.IO) {
        val prompt = "Impact local România (max 2 propoziții):\n$title - $description"
        callAi(prompt) ?: ""
    }

    override suspend fun askQuestion(articleContext: String, question: String): String = withContext(Dispatchers.IO) {
        val prompt = "Articol: $articleContext\n\nÎntrebare: $question"
        callAi(prompt) ?: "Serviciul AI este ocupat."
    }

    private suspend fun callAi(prompt: String): String? {
        if (apiKey.isBlank()) {
            Log.w("OpenRouterClient", "API key is blank, skipping call")
            return null
        }
        
        val now = System.currentTimeMillis()
        val wait = minDelayBetweenCalls - (now - lastApiCallTime)
        if (wait > 0) delay(wait)
        lastApiCallTime = System.currentTimeMillis()

        val requestBody = OpenRouterRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = prompt)
            )
        )
        
        val body = json.encodeToString(OpenRouterRequest.serializer(), requestBody)
            .toRequestBody("application/json".toMediaType())
            
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://flashnews.ai")
            .header("X-Title", "FlashNews AI")
            .post(body)
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string() ?: return null
                val parsed = json.decodeFromString(OpenRouterResponse.serializer(), responseBody)
                parsed.choices.firstOrNull()?.message?.content?.let { cleanResponse(it) }
            }
        } catch (e: Exception) {
            Log.e("OpenRouterClient", "Error: ${e.message}")
            null
        }
    }
}
