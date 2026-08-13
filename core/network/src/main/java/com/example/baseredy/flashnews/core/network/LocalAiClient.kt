package com.example.baseredy.flashnews.core.network

/**
 * Local AI client - provides heuristic responses in Romanian for offline or failover scenarios.
 */
class LocalAiClient : AiClient {

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
