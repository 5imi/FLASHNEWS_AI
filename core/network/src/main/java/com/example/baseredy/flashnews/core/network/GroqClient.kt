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
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)

class GroqClient(private val apiKey: String) : AiClient {
    private val client = OkHttpClient()
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        coerceInputValues = true
    }
    private val model = "llama-3.3-70b-versatile"
    
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
            Log.w("GroqClient", "Failed to parse JSON, returning fallback: ${e.message}")
            DynamicNewsAnalysis(
                keyTakeaway = "• " + title.take(200),
                editorialBias = "NEUTRU",
                biasRationale = "Sursă de știri standard.",
                localImpact = if (region != "RO") "Subiect internațional de interes pentru România." else null,
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
        val cacheKey = "dyn_groq:${title.hashCode()}"
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
        val cacheKey = "sum:${title.hashCode()}"
        cache[cacheKey]?.let { return@withContext it }

        val prompt = "Rezumă jurnalistic în 3 puncte (•) scurte:\nTitlu: $title\nContext: $description"
        val res = callAi(prompt) ?: "• Sumar indisponibil"
        cache[cacheKey] = res
        res
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String = withContext(Dispatchers.IO) {
        val cacheKey = "bias:${source.hashCode()}:${title.hashCode()}"
        cache[cacheKey]?.let { return@withContext it }

        val prompt = "Analizează bias-ul pentru '$source' și titlul '$title'. Răspunde doar cu un cuvânt: NEUTRU, STANGA, DREAPTA sau PROPAGANDA."
        val res = callAi(prompt)?.uppercase()?.filter { it.isLetter() } ?: "NEUTRU"
        cache[cacheKey] = res
        res
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String = withContext(Dispatchers.IO) {
        val prompt = "Explică impactul asupra României (max 2 propoziții):\nTitlu: $title\nContext: $description"
        callAi(prompt) ?: ""
    }

    override suspend fun askQuestion(articleContext: String, question: String): String = withContext(Dispatchers.IO) {
        val prompt = "Context: $articleContext\n\nÎntrebare: $question\n\nRăspunde scurt, inteligent, în română."
        callAi(prompt) ?: "Nu pot răspunde acum."
    }

    private suspend fun callAi(prompt: String): String? {
        if (apiKey.isBlank()) {
            Log.w("GroqClient", "API key is blank, skipping call")
            return null
        }
        
        val now = System.currentTimeMillis()
        val wait = minDelayBetweenCalls - (now - lastApiCallTime)
        if (wait > 0) delay(wait)
        lastApiCallTime = System.currentTimeMillis()

        val requestBody = GroqRequest(
            model = model,
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = prompt)
            )
        )
        
        val body = json.encodeToString(GroqRequest.serializer(), requestBody)
            .toRequestBody("application/json".toMediaType())
            
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string() ?: return null
                val parsed = json.decodeFromString(GroqResponse.serializer(), responseBody)
                parsed.choices.firstOrNull()?.message?.content?.let { cleanResponse(it) }
            }
        } catch (e: Exception) {
            Log.e("GroqClient", "Error: ${e.message}")
            null
        }
    }
}

