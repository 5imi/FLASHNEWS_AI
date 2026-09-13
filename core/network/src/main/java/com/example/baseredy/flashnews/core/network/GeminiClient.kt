package com.example.baseredy.flashnews.core.network

import android.util.Log
import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class GeminiClient(apiKey: String) : AiClient {
    private val TAG = "GeminiClient"
    
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
    )

    private val model = GenerativeModel(
        modelName = "gemini-3.6-flash",
        apiKey = apiKey,
        safetySettings = safetySettings,
        systemInstruction = content { text("Ești un analist media și jurnalist expert român. Analizezi obiectiv, identifici unghiurile critice și răspunzi strict în formatul cerut, în limba română.") }
    )

    private val cache: LinkedHashMap<String, String> = object : LinkedHashMap<String, String>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > 100
    }
    private var lastApiCallTime = 0L
    private val minDelayBetweenCalls = 100L

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

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
            
            jsonParser.decodeFromString<DynamicNewsAnalysis>(jsonString)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse dynamic analysis JSON, using fallback: ${e.message}")
            DynamicNewsAnalysis(
                keyTakeaway = "• " + title.take(200),
                editorialBias = "NEUTRU",
                biasRationale = "Sursă de știri standard.",
                localImpact = if (region != "RO") "Subiectul poate avea relevanță indirectă pentru România." else null,
                dynamicQuestions = listOf(
                    DynamicInsight("Ce trebuie să știi?", "Detalii suplimentare sunt disponibile în articolul original.")
                )
            )
        }
    }

    private fun getCacheKey(operation: String, vararg inputs: String): String {
        val combined = inputs.joinToString("|")
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(combined.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return "$operation:$hash"
    }

    private suspend fun rateLimitedCall(operation: String, block: suspend () -> String): String {
        val now = System.currentTimeMillis()
        val timeSinceLastCall = now - lastApiCallTime
        if (timeSinceLastCall < minDelayBetweenCalls) {
            delay(minDelayBetweenCalls - timeSinceLastCall)
        }
        lastApiCallTime = System.currentTimeMillis()
        
        Log.d(TAG, "Starting Gemini call: $operation")
        return block()
    }

    override suspend fun analyzeNewsDynamic(
        title: String,
        description: String,
        source: String,
        category: String,
        region: String
    ): DynamicNewsAnalysis {
        val cacheKey = getCacheKey("dynamic_analysis", title, description)
        cache[cacheKey]?.let { cachedJson ->
            return parseDynamicJson(cachedJson, title, source, region)
        }

        val rawResult = rateLimitedCall("analyzeNewsDynamic") {
            val truncDesc = description.take(2000)
            val prompt = """
                Ești un jurnalist senior de investigație și analist media.
                Analizează în profunzime această știre și generează un raport structurat, fără șabloane prestabilite.
                Formulează tu însuți 2-3 unghiuri sau întrebări specifice și relevante exclusiv pentru această știre și răspunde la ele.
                
                Știre:
                - Titlu: $title
                - Sursă: $source
                - Categorie: $category
                - Regiune: $region
                - Conținut: $truncDesc
                
                Răspunde STRICT în următorul format JSON valid (fără alt text în afara JSON-ului):
                {
                  "keyTakeaway": "• 2-3 puncte scurte și clare cu esența știrii",
                  "editorialBias": "NEUTRU / STÂNGA / DREAPTA / PROPAGANDĂ",
                  "biasRationale": "O frază scurtă care justifică eticheta de bias",
                  "localImpact": "1-2 fraze despre relevanța pentru România/cetățeanul român (sau null dacă e complet irelevant)",
                  "dynamicQuestions": [
                    {
                      "question": "Întrebare specifică formulată de tine (max 8 cuvinte)",
                      "answer": "Răspuns clar și concis (2-3 propoziții)"
                    }
                  ]
                }
                
                IMPORTANT: Răspunde DOAR în limba ROMÂNĂ.
            """.trimIndent()

            val response = model.generateContent(content { text(prompt) })
            val raw = response.text ?: throw Exception("Empty dynamic analysis response")
            cache[cacheKey] = raw
            raw
        }

        return parseDynamicJson(rawResult, title, source, region)
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        val cacheKey = getCacheKey("summarize", title, description)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall("summarize") {
            val truncDesc = description.take(1500)
            val response = model.generateContent(
                content {
                    text("Rezumă rapid în 2-3 puncte scurte:\nTitlu: $title\nDescriere: $truncDesc\nRăspunde în ROMÂNĂ, cu •, maxim 10 cuvinte/punct.")
                }
            )
            val result = response.text?.let { cleanResponse(it) } ?: throw Exception("Empty response")
            cache[cacheKey] = result
            result
        }
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        val cacheKey = getCacheKey("bias", source, title)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall("analyzeBias") {
            val response = model.generateContent(
                content {
                    text("Sursa: $source\nTitlu: $title\nO SINGURA CUVANT: NEUTRU, STANGA, DREAPTA sau PROPAGANDA")
                }
            )
            val result = response.text?.trim()?.uppercase()?.let { cleanResponse(it) } ?: "NEUTRU"
            val valid = setOf("NEUTRU", "STANGA", "DREAPTA", "PROPAGANDA")
            val bias = if (result in valid) result else "NEUTRU"
            cache[cacheKey] = bias
            bias
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return rateLimitedCall("askQuestion") {
            val truncCtx = articleContext.take(1500)
            val response = model.generateContent(
                content {
                    text("Articol: $truncCtx\n\nIntrebare: $question\n\nRaspunde concis in ROMANA, max 100 cuvinte.")
                }
            )
            response.text?.let { cleanResponse(it) } ?: throw Exception("Empty response")
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        val cacheKey = getCacheKey("impact", title, region)
        cache[cacheKey]?.let { return it }

        return rateLimitedCall("analyzeLocalImpact") {
            val truncDesc = description.take(250)
            val response = model.generateContent(
                content {
                    text("Impact asupra ROMANIEI:\nTitlu: $title\nContext: $truncDesc\n1-2 fraze, ROMANA, max 50 cuvinte.")
                }
            )
            val result = response.text?.let { cleanResponse(it) } ?: ""
            cache[cacheKey] = result
            result
        }
    }
}

