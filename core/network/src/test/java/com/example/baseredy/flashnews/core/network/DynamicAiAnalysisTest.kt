package com.example.baseredy.flashnews.core.network

import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicAiAnalysisTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun localAiClient_generatesDynamicInsightsForEconomy() = runTest {
        val localClient = LocalAiClient()
        val analysis = localClient.analyzeNewsDynamic(
            title = "Banca Națională crește dobânda de referință cu 0.5%",
            description = "BNR a decis majorarea dobânzii de politică monetară pentru a tempera inflația galopantă.",
            source = "Ziarul Financiar",
            category = "Business",
            region = "RO"
        )

        assertNotNull(analysis)
        assertTrue(analysis.keyTakeaway.isNotBlank())
        assertEquals("FACTUAL / ECONOMIC", analysis.editorialBias)
        assertTrue(analysis.biasRationale.contains("factuală") || analysis.biasRationale.contains("tehnică"))
        assertTrue(analysis.dynamicQuestions.isNotEmpty())
        assertTrue(analysis.dynamicQuestions.any { it.question.contains("piețele") || it.question.contains("economice") || it.question.contains("miza") })
    }

    @Test
    fun localAiClient_detectsSensationalistClickbait() = runTest {
        val localClient = LocalAiClient()
        val bias = localClient.analyzeBias(
            source = "Tabloid News",
            title = "ȘOC TOTAL! Nu o să crezi ce decizie incredibilă a luat Guvernul!!",
            category = "General",
            region = "RO"
        )

        assertEquals("SENZAȚIONALIST", bias)
    }

    @Test
    fun localAiClient_mapsRomanianMediaProfilesCorrectly() = runTest {
        val localClient = LocalAiClient()
        
        assertEquals("CENTRU-DREAPTA", localClient.analyzeBias("HotNews", "Titlu informativ", "General", "RO"))
        assertEquals("INDEPENDENT / FACTUAL", localClient.analyzeBias("Recorder", "Investigație exclusivă", "General", "RO"))
        assertEquals("NEUTRU", localClient.analyzeBias("Digi24", "Stiri de actualitate", "General", "RO"))
        assertEquals("FACTUAL / JURIDIC", localClient.analyzeBias("Avocatnet", "Modificari legislative", "Business", "RO"))
        assertEquals("CENTRU-STÂNGA", localClient.analyzeBias("Libertatea", "Analiza sociala", "General", "RO"))
    }

    @Test
    fun localAiClient_detectsLocalImpactForGlobalNews() = runTest {
        val localClient = LocalAiClient()
        val analysis = localClient.analyzeNewsDynamic(
            title = "Uniunea Europeană impune noi reglementări privind emisiile auto",
            description = "Parlamentul European a votat pachetul legislativ ce va afecta toate statele membre.",
            source = "Reuters",
            category = "General",
            region = "GLOBAL"
        )

        assertNotNull(analysis.localImpact)
        assertTrue(analysis.localImpact!!.contains("Români", ignoreCase = true))
    }

    @Test
    fun localAiClient_detectsFiscalImpact() = runTest {
        val localClient = LocalAiClient()
        val impact = localClient.analyzeLocalImpact(
            title = "Modificări la Codul Fiscal: Noi cote de TVA și impozite pentru companii",
            description = "Ministerul Finanțelor a publicat proiectul de ordonanță de urgență.",
            region = "RO"
        )

        assertTrue(impact.contains("Impact fiscal direct"))
    }

    @Test
    fun dynamicNewsAnalysis_jsonSerialization_roundTrip() {
        val sample = DynamicNewsAnalysis(
            keyTakeaway = "• Punctul 1 important\n• Punctul 2 important",
            editorialBias = "NEUTRU",
            biasRationale = "Ton factual și echilibrat fără judecăți de valoare",
            localImpact = "Impact direct asupra pieței din România",
            dynamicQuestions = listOf(
                DynamicInsight(
                    question = "Care este riscul principal?",
                    answer = "Creșterea costurilor de creditare pentru IMM-uri."
                ),
                DynamicInsight(
                    question = "Ce urmează?",
                    answer = "Decizia va intra în vigoare la începutul lunii viitoare."
                )
            )
        )

        val encoded = json.encodeToString(DynamicNewsAnalysis.serializer(), sample)
        val decoded = json.decodeFromString(DynamicNewsAnalysis.serializer(), encoded)

        assertEquals(sample.keyTakeaway, decoded.keyTakeaway)
        assertEquals(sample.editorialBias, decoded.editorialBias)
        assertEquals(sample.biasRationale, decoded.biasRationale)
        assertEquals(sample.localImpact, decoded.localImpact)
        assertEquals(sample.dynamicQuestions.size, decoded.dynamicQuestions.size)
        assertEquals(sample.dynamicQuestions[0].question, decoded.dynamicQuestions[0].question)
    }

    @Test
    fun aiOrchestrator_fallsBackToSecondary_whenPrimaryFails() = runTest {
        val failingPrimary = object : AiClient {
            override suspend fun analyzeNewsDynamic(title: String, description: String, source: String, category: String, region: String): DynamicNewsAnalysis {
                throw RuntimeException("API Rate Limit 429")
            }
            override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String = throw RuntimeException()
            override suspend fun analyzeBias(source: String, text: String, category: String, region: String): String = throw RuntimeException()
            override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String = throw RuntimeException()
            override suspend fun askQuestion(articleContext: String, question: String): String = throw RuntimeException()
            override suspend fun comparePerspectives(articles: List<String>): String = throw RuntimeException()
        }

        val workingSecondary = object : AiClient {
            override suspend fun analyzeNewsDynamic(title: String, description: String, source: String, category: String, region: String): DynamicNewsAnalysis {
                return DynamicNewsAnalysis(
                    keyTakeaway = "• Rezumat de la Groq Fallback",
                    editorialBias = "NEUTRU",
                    biasRationale = "Evaluat de Groq",
                    localImpact = null,
                    dynamicQuestions = listOf(DynamicInsight("De ce contează?", "Răspuns fallback."))
                )
            }
            override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String = "Groq summary"
            override suspend fun analyzeBias(source: String, text: String, category: String, region: String): String = "NEUTRU"
            override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String = ""
            override suspend fun askQuestion(articleContext: String, question: String): String = "Groq answer"
            override suspend fun comparePerspectives(articles: List<String>): String = "Groq comparison"
        }

        val orchestrator = AiOrchestrator(
            fastAgent = failingPrimary,
            analyticalAgent = failingPrimary,
            groqAgent = workingSecondary,
            openRouterAgent = failingPrimary,
            localAgent = LocalAiClient()
        )

        val result = orchestrator.analyzeNewsDynamic(
            title = "Test Failover",
            description = "Description",
            source = "Media",
            category = "Tech",
            region = "RO"
        )

        assertNotNull(result)
        assertTrue(result.keyTakeaway.contains("Groq Fallback"))
    }

    @Test
    fun liveOrchestrator_withGeminiAndLocal() = runTest {
        val geminiKey = "AIzaSy_DUMMY_KEY_FOR_TESTING"
        val geminiClient = GeminiClient(geminiKey)
        val orchestrator = AiOrchestrator(
            fastAgent = geminiClient,
            analyticalAgent = geminiClient,
            groqAgent = geminiClient,
            openRouterAgent = geminiClient,
            localAgent = LocalAiClient()
        )

        val result = orchestrator.analyzeNewsDynamic(
            title = "Comisia Europeană a aprobat noul plan de tranziție energetică pentru România",
            description = "Finanțarea de 2 miliarde de euro va sprijini modernizarea rețelelor electrice și tranziția către energie verde în următorii 3 ani.",
            source = "Digi24",
            category = "Economie",
            region = "RO"
        )

        println("=== LIVE TEST RESULT ===")
        println("Key Takeaway:\n${result.keyTakeaway}")
        println("Editorial Bias: ${result.editorialBias}")
        println("Bias Rationale: ${result.biasRationale}")
        println("Local Impact: ${result.localImpact}")
        println("Dynamic Questions:")
        result.dynamicQuestions.forEach { q ->
            println(" - Q: ${q.question}")
            println("   A: ${q.answer}")
        }
        println("========================")

        assertNotNull(result)
        assertTrue(result.keyTakeaway.isNotBlank())
    }
}
