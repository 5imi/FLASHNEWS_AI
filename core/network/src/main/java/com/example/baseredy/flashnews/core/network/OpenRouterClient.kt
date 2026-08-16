package com.example.baseredy.flashnews.core.network

import android.util.Log
import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis
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
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
    }
    private val model = "deepseek/deepseek-chat" // DeepSeek Chat

    private val cache = mutableMapOf<String, String>()
    private var lastApiCallTime = 0L
    private val minDelayBetweenCalls = 500L

    private val systemPrompt = "Ești un analist media și jurnalist expert român. Răspunzi strict în format JSON valid, în limba română."

    private fun cleanResponse(raw: String): String {
        return raw.replace(Regex("```[a-z]*\\n?"), "")
            .replace("```", "")
            .trim()
    }

    private fun parseDynamicJson(raw: String, title: String, source: String, region: String): DynamicNewsAnalysis {
        return try {
            val cleaned = raw.replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*", RegexOption.MULTILINE), "")
                .replace("```", "")
                .trim()
            
            val jsonStart = cleaned.indexOf('{')
            val jsonEnd = cleaned.lastIndexOf('}')
            val jsonString = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleaned.substring(jsonStart, jsonEnd + 1)
            } else {
                cleaned
            }
            
            json.decodeFromString<DynamicNewsAnalysis>(jsonString)
        } catch (e: Exception) {
            Log.w("OpenRouterClient", "Failed to parse JSON, returning fallback: ${e.message}")
            DynamicNewsAnalysis(
                keyTakeaway = "• " + title.take(200),
                editorialBias = "NEUTRU",
                biasRationale = "Sursă de știri standard.",
                localImpact = if (region != "RO") "Subiect internațional relevant." else null,
                dynamicQuestions = listOf(
                    DynamicInsight("Ce trebuie să știi?", "Detalii suplimentare sunt disponibile în articolul original.")
                )
            )
        }
    }

    override suspend fun analyzeNewsDynamic(
        title: String,
        description: String,
        source: String,
        category: String,
        region: String
    ): DynamicNewsAnalysis = withContext(Dispatchers.IO) {
        val cacheKey = "dyn_or:${title.hashCode()}"
        cache[cacheKey]?.let { return@withContext parseDynamicJson(it, title, source, region) }

        val prompt = """
            Ești un jurnalist senior de investigație și analist media.
            Analizează această știre și formulează tu însuți 2-3 unghiuri sau întrebări specifice și relevante pentru această știre și răspunde la ele.
            
            Știre:
            - Titlu: $title
            - Sursă: $source
            - Categorie: $category
            - Regiune: $region
            - Conținut: ${description.take(2000)}
            
            Răspunde STRICT în următorul format JSON valid:
            {
              "keyTakeaway": "• 2-3 puncte scurte și clare cu esența știrii",
              "editorialBias": "NEUTRU / STÂNGA / DREAPTA / PROPAGANDĂ",
              "biasRationale": "O frază scurtă care justifică eticheta de bias",
              "localImpact": "1-2 fraze despre relevanța pentru România (sau null)",
              "dynamicQuestions": [
                {
                  "question": "Întrebare specifică formulată de tine (max 8 cuvinte)",
                  "answer": "Răspuns clar și concis (2-3 propoziții)"
                }
              ]
            }
            Răspunde DOAR în limba ROMÂNĂ.
        """.trimIndent()

        val raw = callAi(prompt) ?: return@withContext parseDynamicJson("", title, source, region)
        cache[cacheKey] = raw
        parseDynamicJson(raw, title, source, region)
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

