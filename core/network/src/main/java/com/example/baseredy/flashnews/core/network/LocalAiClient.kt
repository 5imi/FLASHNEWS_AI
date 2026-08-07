package com.example.baseredy.flashnews.core.network

/**
 * Local mock AI client - uses templates for offline/fallback scenarios.
 * Zero cost, no API calls, good for testing and failover.
 */
class LocalAiClient : AiClient {

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        // Template-based summaries
        val truncDesc = description.take(150)
        return """
            • ${title.take(40)}...
            • Sursă: $source, Categorie: $category
            • Citește articolul complet pentru context
        """.trimIndent()
    }

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        // Simple heuristics for bias detection
        val biasIndicators = mapOf(
            "CNN" to "STÂNGA",
            "FOX" to "DREAPTA",
            "BBC" to "NEUTRU",
            "Reuters" to "NEUTRU",
            "AP" to "NEUTRU",
            "RT" to "PROPAGANDA",
            "TASS" to "PROPAGANDA"
        )
        
        return biasIndicators[source] ?: "NEUTRU"
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        return when {
            title.contains("Fiscal", ignoreCase = true) || 
            title.contains("Impozit", ignoreCase = true) ||
            title.contains("Económie", ignoreCase = true) -> 
                "Poate afecta bugetul personal și impozitele. Verifică ultimele anunțuri oficiale."
            
            title.contains("Brexit", ignoreCase = true) ||
            title.contains("UE", ignoreCase = true) ||
            title.contains("NATO", ignoreCase = true) ->
                "Are implicații pentru România prin relații comerciale și securitate NATO."
            
            else -> "Pune atenție la cum acesta se conectează cu politica locală română."
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return when {
            question.contains("ce", ignoreCase = true) ->
                "Articolul spune: ${articleContext.take(100)}... Citește sursa originală pentru răspuns complet."
            
            question.contains("cum", ignoreCase = true) ->
                "Documentația oficială sau sursele citate în articol ar trebui să clarifice acest aspect."
            
            question.contains("de ce", ignoreCase = true) ->
                "Contextul în articol sugerează motivele. Consultă analiști sau comentatori pentru perspective mai profunde."
            
            else -> "Aceasta este o întrebare interesantă. Articolul oferă unele indicii - cere mai mult context din alte surse."
        }
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        return when (articles.size) {
            0, 1 -> ""
            else -> """
                Comparație între ${articles.size} perspective:
                • Toate articolele discută același subiect
                • Diferențele pot fi în interpretare sau context
                • Recomandare: Citește ambele surse pentru imagine completă
            """.trimIndent()
        }
    }
}
