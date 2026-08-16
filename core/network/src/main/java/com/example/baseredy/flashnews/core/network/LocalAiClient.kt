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
        
        val dynamicQuestions = when {
            category.contains("Tehnologie", ignoreCase = true) || category.contains("Auto", ignoreCase = true) || category.contains("Tech", ignoreCase = true) -> listOf(
                DynamicInsight("Care este inovația principală?", "Subiectul aduce noutăți relevante în domeniul tehnologic și al pieței de profil."),
                DynamicInsight("Ce impact are pentru consumatori?", "Utilizatorii ar putea beneficia de funcționalități noi sau schimbări în serviciile utilizate.")
            )
            category.contains("Business", ignoreCase = true) || category.contains("Finan", ignoreCase = true) || category.contains("Economi", ignoreCase = true) -> listOf(
                DynamicInsight("Cum sunt influențate piețele?", "Evoluțiile economice descrise pot genera efecte asupra costurilor și investițiilor."),
                DynamicInsight("Ce trebuie urmărit în continuare?", "Deciziile autorităților financiare și reacțiile companiilor din sector.")
            )
            category.contains("Politic", ignoreCase = true) || category.contains("General", ignoreCase = true) -> listOf(
                DynamicInsight("Care este miza principală?", "Deciziile sau evenimentele menționate influențează direct agenda publică."),
                DynamicInsight("Ce urmează?", "Părțile implicate își vor exprima pozițiile oficiale în perioada următoare.")
            )
            else -> listOf(
                DynamicInsight("De ce este important?", "Informația reflectă o dezvoltare relevantă în categoria $category."),
                DynamicInsight("Ce trebuie verificat?", "Urmărește sursele autorizate pentru confirmări și detalii complete.")
            )
        }

        val rationale = when (bias) {
            "SENZAȚIONALIST" -> "Titlul conține markeri emoționali specifici clickbait-ului sau punctuație accentuată."
            "FACTUAL / ECONOMIC", "FACTUAL / JURIDIC", "FACTUAL / FISCAL" -> "Sursă specializată cu raportare preponderent tehnică și factuală."
            "INDEPENDENT / FACTUAL" -> "Publicație independentă orientată pe jurnalism de investigație și verificare."
            "CENTRU-DREAPTA" -> "Linie editorială pro-piață liberă, cu accent pe transparență administrativă."
            "CENTRU-STÂNGA" -> "Focalizare pe drepturi sociale, protecția muncii și impact comunitar."
            "PARTIZAN / GUVERNAMENTAL", "PARTIZAN" -> "Sursă cu tendință de favorizare a anumitor forțe politice sau agende de grup."
            "PROPAGANDĂ / CONTROLAT DE STAT" -> "Entitate media finanțată sau controlată direct de autorități statale externe."
            "TABLOID" -> "Stil centrat pe spectaculos, viață privată și titluri cu încărcătură emoțională."
            else -> "Raportare standard echilibrată conform profilului de presă generalistă."
        }

        return DynamicNewsAnalysis(
            keyTakeaway = "• $summary\n• Sursa: $source",
            editorialBias = bias,
            biasRationale = rationale,
            localImpact = impact,
            dynamicQuestions = dynamicQuestions
        )
    }

    override suspend fun summarize(title: String, description: String, source: String, category: String, region: String): String {
        val summary = description.take(200).ifBlank { title.take(200) }
        return "• $summary\n• Sursa: $source\n• Verifică articolul pentru detalii suplimentare."
    }

    // [OLD] - Motiv înlocuire: Hartă euristică limitată la doar 10 surse internaționale, fără suport pentru presa din România sau analiză de senzaționalism
    /*
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
    */

    override suspend fun analyzeBias(source: String, title: String, category: String, region: String): String {
        val lowerSource = source.lowercase()
        val lowerTitle = title.lowercase()

        // 1. Verificare indici de senzaționalism / clickbait în titlu
        val sensationalistMarkers = listOf("șoc", "incredibil", "bombă", "nu o să crezi", "cutremurător", "apocalipsă", "dezastru total", "secretul pe care", "atenție români")
        val isSensationalist = sensationalistMarkers.any { lowerTitle.contains(it) } || title.count { it == '!' } >= 2

        if (isSensationalist) {
            return "SENZAȚIONALIST"
        }

        // 2. Profil editorial România
        val roMediaProfiles = mapOf(
            // Agenții de presă și factual / neutru
            "agerpres" to "NEUTRU",
            "mediafax" to "NEUTRU",
            "news.ro" to "NEUTRU",
            "digi24" to "NEUTRU",
            "europa liberă" to "CENTRU",
            "rfi" to "CENTRU",
            "dw românia" to "NEUTRU",
            
            // Presă de investigație și pro-transparență (Centru / Centru-Dreapta)
            "g4media" to "CENTRU-DREAPTA",
            "hotnews" to "CENTRU-DREAPTA",
            "pressone" to "CENTRU",
            "recorder" to "INDEPENDENT / FACTUAL",
            "context.ro" to "INDEPENDENT / FACTUAL",
            "spotmedia" to "CENTRU",
            
            // Presă economică / Factual
            "zf" to "FACTUAL / ECONOMIC",
            "ziarul financiar" to "FACTUAL / ECONOMIC",
            "economica" to "FACTUAL / ECONOMIC",
            "profit.ro" to "FACTUAL / ECONOMIC",
            "wall-street" to "FACTUAL / ECONOMIC",
            "bursa" to "FACTUAL / ECONOMIC",
            "curs de guvernare" to "FACTUAL / ANALITIC",
            "avocatnet" to "FACTUAL / JURIDIC",
            "contzilla" to "FACTUAL / FISCAL",
            "ceccar" to "FACTUAL / FISCAL",
            
            // Social / Centru-Stânga
            "libertatea" to "CENTRU-STÂNGA",
            "snoop" to "INDEPENDENT / SOCIAL",
            
            // Televiziuni / Tabloide cu orientare partizană sau tabloid
            "antena 3" to "PARTIZAN / GUVERNAMENTAL",
            "românia tv" to "SENZAȚIONALIST",
            "realitatea" to "PARTIZAN",
            "cancan" to "TABLOID",
            "click" to "TABLOID"
        )

        // 3. Profil editorial Internațional
        val globalMediaProfiles = mapOf(
            "reuters" to "NEUTRU",
            "ap news" to "NEUTRU",
            "associated press" to "NEUTRU",
            "bbc" to "NEUTRU",
            "bloomberg" to "FACTUAL / ECONOMIC",
            "financial times" to "CENTRU / ECONOMIC",
            "the economist" to "CENTRU / LIBERAL",
            "dw" to "NEUTRU",
            "afp" to "NEUTRU",
            "euronews" to "NEUTRU",
            "the guardian" to "CENTRU-STÂNGA",
            "new york times" to "CENTRU-STÂNGA",
            "cnn" to "CENTRU-STÂNGA",
            "msnbc" to "STÂNGA",
            "politico" to "CENTRU",
            "wall street journal" to "CENTRU-DREAPTA",
            "wsj" to "CENTRU-DREAPTA",
            "forbes" to "FACTUAL / BUSINESS",
            "techcrunch" to "FACTUAL / TECH",
            "the verge" to "FACTUAL / TECH",
            "wired" to "FACTUAL / TECH",
            "fox news" to "DREAPTA",
            "fox" to "DREAPTA",
            "daily mail" to "TABLOID / DREAPTA",
            "breitbart" to "EXTREMA DREAPTĂ",
            "rt" to "PROPAGANDĂ / CONTROLAT DE STAT",
            "tass" to "PROPAGANDĂ / CONTROLAT DE STAT",
            "sputnik" to "PROPAGANDĂ / CONTROLAT DE STAT"
        )

        val allProfiles = roMediaProfiles + globalMediaProfiles
        return allProfiles.entries.firstOrNull { lowerSource.contains(it.key) }?.value ?: "NEUTRU"
    }

    override suspend fun analyzeLocalImpact(title: String, description: String, region: String): String {
        val combinedText = "$title $description".lowercase()

        return when {
            // Fiscalitate, Taxe și Buget
            combinedText.contains("tax") || combinedText.contains("impozit") || combinedText.contains("tva") || 
            combinedText.contains("fiscal") || combinedText.contains("contribu") || combinedText.contains("salariu") || combinedText.contains("pensii") ->
                "Impact fiscal direct: Decizia poate influența taxele, contribuțiile sau puterea de cumpărare a cetățenilor și firmelor din România."

            // Energie, Carburanți & Utilități
            combinedText.contains("energie") || combinedText.contains("gaz") || combinedText.contains("petrol") || 
            combinedText.contains("curent") || combinedText.contains("preț") || combinedText.contains("infla") || combinedText.contains("bcr") || combinedText.contains("bnr") ->
                "Impact economic & costul vieții: Posibile fluctuații ale tarifelor la utilități, carburanți sau rate bancare pe piața locală."

            // Geopolitică, Securitate & Război
            combinedText.contains("nato") || combinedText.contains("ue") || combinedText.contains("uniunea europeană") || combinedText.contains("european") || combinedText.contains("bruxelles") || 
            combinedText.contains("război") || combinedText.contains("ucraina") || combinedText.contains("securitate") || combinedText.contains("armat") || combinedText.contains("marea neagră") ->
                "Relevanță geopolitică & securitate: Evenimentul are implicații directe asupra securității regionale și a poziției strategice a României."

            // Fonduri Europene, PNRR & Infrastructură
            combinedText.contains("pnrr") || combinedText.contains("fonduri") || combinedText.contains("autostrad") || 
            combinedText.contains("transport") || combinedText.contains("infrastructur") || combinedText.contains("investi") ->
                "Impact pe dezvoltare & fonduri: Vizează proiecte majore de infrastructură sau atragerea de finanțări europene pentru România."

            // Sănătate & Mediu
            combinedText.contains("sănătate") || combinedText.contains("medic") || combinedText.contains("spital") || 
            combinedText.contains("clim") || combinedText.contains("mediu") || combinedText.contains("poluare") ->
                "Impact sănătate publică & mediu: Poate determina reglementări noi în domeniul sanitar sau cerințe ecologice la nivel național."

            // Tehnologie & AI
            combinedText.contains("ai") || combinedText.contains("inteligenț") || combinedText.contains("cyber") || 
            combinedText.contains("securitate cibernetic") || combinedText.contains("digital") || combinedText.contains("tech") ->
                "Impact digital & tehnologie: Modificări în tehnologiile utilizate de companiile și consumatorii din România."

            else -> if (region != "RO") "Context internațional: Monitorizează evoluția subiectului pentru potențiale efecte indirecte asupra României." else "Informație de interes public cu relevanță pentru comunitatea locală."
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        val topic = articleContext.take(150).replace(Regex("<[^>]*>"), "")
        return "Am analizat contextul disponibil despre \"$topic\":\n\n" +
               "1. Referitor la întrebarea ta ($question), informațiile curente sugerează monitorizarea sursei pentru actualizări live.\n" +
               "2. Serviciul local euristic oferă asistență de bază, garantând funcționarea fără conexiune la internet.\n" +
               "3. Recomandăm verificarea secțiunii de detalii sau a articolului complet pentru dezvoltări ulterioare."
    }
}

