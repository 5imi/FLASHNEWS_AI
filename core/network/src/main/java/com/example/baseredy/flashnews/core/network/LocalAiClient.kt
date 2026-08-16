package com.example.baseredy.flashnews.core.network

import com.example.baseredy.flashnews.core.model.DynamicInsight
import com.example.baseredy.flashnews.core.model.DynamicNewsAnalysis

/**
 * Local AI client - provides heuristic responses in Romanian for offline or failover scenarios.
 */
class LocalAiClient : AiClient {

    override suspend fun analyzeNewsDynamic(
        title: String,
        description: String,
        source: String,
        category: String,
        region: String
    ): DynamicNewsAnalysis {
        val summary = description.take(250).ifBlank { title.take(250) }.replace(Regex("<[^>]*>"), "").trim()
        val bias = analyzeBias(source, title, category, region)
        val impact = if (region != "RO") analyzeLocalImpact(title, description, region) else null
        
        val dynamicQuestions = when (category) {
            "Tehnologie", "Auto" -> listOf(
                DynamicInsight("Care este inovația principală?", "Subiectul aduce noutăți relevante în domeniul tehnologic și al pieței de profil."),
                DynamicInsight("Ce impact are pentru consumatori?", "Utilizatorii ar putea beneficia de funcționalități noi sau schimbări în serviciile utilizate.")
            )
            "Business & Finanțe" -> listOf(
                DynamicInsight("Cum sunt influențate piețele?", "Evoluțiile economice descrise pot genera efecte asupra costurilor și investițiilor."),
                DynamicInsight("Ce trebuie urmărit în continuare?", "Deciziile autorităților financiare și reacțiile companiilor din sector.")
            )
            "Politică", "General" -> listOf(
                DynamicInsight("Care este miza principală?", "Deciziile sau evenimentele menționate influențează direct agenda publică."),
                DynamicInsight("Ce urmează?", "Părțile implicate își vor exprima pozițiile oficiale în perioada următoare.")
            )
            else -> listOf(
                DynamicInsight("De ce este important?", "Informația reflectă o dezvoltare relevantă în categoria $category."),
                DynamicInsight("Ce trebuie verificat?", "Urmărește sursele autorizate pentru confirmări și detalii complete.")
            )
        }

        return DynamicNewsAnalysis(
            keyTakeaway = "• $summary\n• Sursa: $source",
            editorialBias = bias,
            biasRationale = "Evaluare euristică pe baza profilului editorial al sursei.",
            localImpact = impact,
            dynamicQuestions = dynamicQuestions
        )
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        val summary = description.take(200).ifBlank { title.take(200) }
        return "• $summary\n• Sursa: $source\n• Verifică articolul pentru detalii suplimentare."
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        val biasIndicators = mapOf(
            "CNN" to "STÂNGA",
            "MSNBC" to "STÂNGA",
            "FOX" to "DREAPTA",
            "Breitbart" to "DREAPTA",
            "BBC" to "NEUTRU",
            "Reuters" to "NEUTRU",
            "AP" to "NEUTRU",
            "RT" to "PROPAGANDĂ",
            "TASS" to "PROPAGANDĂ",
            "Sputnik" to "PROPAGANDĂ"
        )
        
        return biasIndicators.entries.find { source.contains(it.key, ignoreCase = true) }?.value ?: "NEUTRU"
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        return when {
            title.contains("Fiscal", ignoreCase = true) || title.contains("Impozit", ignoreCase = true) -> 
                "Impact fiscal probabil: Poate afecta taxe și impozite în România."
            
            title.contains("Preț", ignoreCase = true) || title.contains("Gaz", ignoreCase = true) || title.contains("Energie", ignoreCase = true) ->
                "Impact economic: Posibile fluctuații ale prețurilor la nivel local."
            
            title.contains("UE", ignoreCase = true) || title.contains("NATO", ignoreCase = true) || title.contains("Război", ignoreCase = true) ->
                "Relevanță geopolitică: Importanță strategică pentru securitatea României."
            
            else -> "Monitorizează contextul: Subiectul poate influența agenda publică românească."
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        val topic = articleContext.take(150).replace(Regex("<[^>]*>"), "")
        return "Am analizat contextul disponibil despre \"$topic\":\n\n" +
               "1. Referitor la întrebarea ta ($question), informațiile curente sugerează monitorizarea sursei pentru actualizări live.\n" +
               "2. Momentan, serviciile cloud sunt ocupate, dar acest răspuns euristic îți confirmă că subiectul este în atenția noastră.\n" +
               "3. Recomandăm verificarea secțiunii de detalii pentru contextul complet al știrii."
    }
}

