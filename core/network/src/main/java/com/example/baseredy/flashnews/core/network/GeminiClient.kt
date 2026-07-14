package com.example.baseredy.flashnews.core.network

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content

class GeminiClient(apiKey: String) : AiClient {
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
    )

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        safetySettings = safetySettings
    )

    override suspend fun summarize(title: String, description: String): String {
        return try {
            val response = model.generateContent(
                content {
                    text("Ești un jurnalist expert. Rezumă această știre în 3 puncte scurte și clare, fiecare de maxim 12 cuvinte. Răspunde EXCLUSIV în LIMBA ROMÂNĂ, indiferent de limba sursei. Format: un punct pe linie, începând cu •. Titlu: $title. Descriere: $description")
                }
            )
            response.text ?: ""
        } catch (e: Exception) {
            "• Sumar indisponibil în română\n• Detalii în articol\n• Verifică sursa originală"
        }
    }

    override suspend fun analyzeBias(source: String, title: String): String {
        return try {
            val response = model.generateContent(
                content {
                    text("Ești un expert în analiza media. Analizează înclinația politică sau editorială a acestei știri din sursa '$source'. Răspunde cu UN SINGUR CUVÂNT în română: NEUTRU, STÂNGA, DREAPTA sau PROPAGANDĂ. Titlu: $title")
                }
            )
            response.text?.trim()?.uppercase() ?: "NEUTRU"
        } catch (e: Exception) {
            "NEUTRU"
        }
    }

    override suspend fun comparePerspectives(articles: List<String>): String {
        if (articles.size < 2) return ""
        return try {
            val combinedText = articles.joinToString("\n---\n")
            val response = model.generateContent(
                content {
                    text("Ai mai multe articole despre același subiect din surse diferite: $combinedText. Analizează diferențele de perspectivă și eventualele contradicții. Oferă un rezumat comparativ scurt (max 100 cuvinte) în limba ROMÂNĂ care să evidențieze punctele comune și diferențele majore între narațiuni.")
                }
            )
            response.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun askQuestion(articleContext: String, question: String): String {
        return try {
            val response = model.generateContent(
                content {
                    text("Context articol: $articleContext\n\nÎntrebare utilizator: $question\n\nEști un asistent inteligent numit FlashNews AI. Răspunde util, concis și obiectiv la întrebarea despre acest articol, EXCLUSIV în limba ROMÂNĂ.")
                }
            )
            response.text ?: "Nu pot răspunde momentan."
        } catch (e: Exception) {
            "Eroare de conexiune AI."
        }
    }

    override suspend fun analyzeLocalImpact(title: String, description: String): String {
        return try {
            val response = model.generateContent(
                content {
                    text("Titlu: $title\nDescriere: $description\n\nEști un analist geopolitic și economic. Explică pe scurt (max 2 fraze) de ce această știre internațională este relevantă pentru un cetățean din ROMÂNIA sau ce impact ar putea avea asupra țării noastre. Răspunde în ROMÂNĂ.")
                }
            )
            response.text ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
