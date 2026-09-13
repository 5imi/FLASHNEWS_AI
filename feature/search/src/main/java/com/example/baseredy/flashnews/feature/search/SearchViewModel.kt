package com.example.baseredy.flashnews.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baseredy.flashnews.core.data.NewsRepository
import com.example.baseredy.flashnews.core.database.NewsDatabase
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.AiOrchestrator
import com.example.baseredy.flashnews.core.network.GeminiClient
import com.example.baseredy.flashnews.core.network.GrokClient
import com.example.baseredy.flashnews.core.network.GroqClient
import com.example.baseredy.flashnews.core.network.OpenRouterClient
import com.example.baseredy.flashnews.core.network.LocalAiClient

import com.example.baseredy.flashnews.feature.search.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiClient = GeminiClient(BuildConfig.GEMINI_API_KEY)
    private val grokClient = GrokClient(BuildConfig.GROK_API_KEY ?: "")
    private val groqClient = GroqClient(BuildConfig.GROQ_API_KEY)
    private val openRouterClient = OpenRouterClient(BuildConfig.OPENROUTER_API_KEY)
    private val localClient = LocalAiClient()
    
    private val aiOrchestrator = AiOrchestrator(
        fastAgent = geminiClient,
        analyticalAgent = grokClient,
        groqAgent = groqClient,
        openRouterAgent = openRouterClient,
        localAgent = localClient
    )
    
    private val repository = NewsRepository(
        NewsDatabase.getDatabase(application).newsDao(),
        aiOrchestrator
    )

    private val _searchResults = MutableStateFlow<List<NewsArticle>>(emptyList())
    val searchResults: StateFlow<List<NewsArticle>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val prefsRepository = com.example.baseredy.flashnews.core.data.UserPreferencesRepository(application)
    private val _trackedKeywords = MutableStateFlow<Set<String>>(prefsRepository.getTrackedKeywords())
    val trackedKeywords: StateFlow<Set<String>> = _trackedKeywords

    fun toggleTrackKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        val current = prefsRepository.getTrackedKeywords()
        val existing = current.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            prefsRepository.removeTrackedKeyword(existing)
        } else {
            prefsRepository.addTrackedKeyword(trimmed)
        }
        _trackedKeywords.value = prefsRepository.getTrackedKeywords()
    }

    val trends = listOf("AI", "SpaceX", "Tesla", "Bitcoin", "Climate", "Healthcare")

    fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = repository.searchNews(BuildConfig.NEWS_API_KEY, query)
            _isLoading.value = false
        }
    }
}
