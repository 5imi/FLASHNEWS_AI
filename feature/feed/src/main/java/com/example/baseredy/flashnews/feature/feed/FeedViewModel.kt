package com.example.baseredy.flashnews.feature.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baseredy.flashnews.core.data.NewsRepository
import com.example.baseredy.flashnews.core.data.UserPreferencesRepository
import com.example.baseredy.flashnews.core.database.NewsDatabase
import com.example.baseredy.flashnews.core.model.AiInsight
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.AiOrchestrator
import com.example.baseredy.flashnews.core.network.GeminiClient
import com.example.baseredy.flashnews.core.network.GrokClient
import com.example.baseredy.flashnews.core.network.GroqClient
import com.example.baseredy.flashnews.core.network.OpenRouterClient
import com.example.baseredy.flashnews.core.network.LocalAiClient

import com.example.baseredy.flashnews.feature.feed.BuildConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.paging.PagingData
import androidx.paging.cachedIn

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    // Initialize multi-agent AI system with orchestrator
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
    private val prefsRepository = UserPreferencesRepository(application)

    private val _selectedRegion = MutableStateFlow("RO")
    val selectedRegion: StateFlow<String> = _selectedRegion

    private val _selectedCategory = MutableStateFlow("Toate")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    private val _onboardingCompleted = MutableStateFlow(prefsRepository.isOnboardingCompleted())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted

    val categories = listOf(
        "Toate", "General", "Business & Finanțe", "Politică", "Tehnologie", 
        "Sport", "Auto", "Știință & Mediu", "Sănătate", "Lifestyle", "Educație"
    )

    fun completeOnboarding(languages: Set<String>, interests: Set<String>) {
        prefsRepository.setLanguages(languages)
        prefsRepository.setInterests(interests)
        prefsRepository.setOnboardingCompleted(true)
        _onboardingCompleted.value = true
        // Set initial category from interests if possible, else General
        _selectedCategory.value = interests.firstOrNull() ?: "Toate"
    }

    val articles: Flow<PagingData<NewsArticle>> = combine(
        _selectedRegion,
        _selectedCategory,
        _showOnlyFavorites
    ) { region, category, favoritesOnly ->
        Triple(region, category, favoritesOnly)
    }.flatMapLatest { (region, category, favoritesOnly) ->
        if (favoritesOnly) {
            repository.getFavorites()
        } else {
            repository.getArticles(region, category)
        }
    }.cachedIn(viewModelScope)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _chatResponse = MutableStateFlow<String?>(null)
    val chatResponse: StateFlow<String?> = _chatResponse

    private val _aiInsights = MutableStateFlow<List<AiInsight>>(emptyList())
    val aiInsights: StateFlow<List<AiInsight>> = _aiInsights

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading

    private val insightsCache = mutableMapOf<String, List<AiInsight>>()

    fun onRegionSelected(region: String) {
        _selectedRegion.value = region
        refreshNews()
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        refreshNews()
    }

    fun toggleFavoritesView() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch {
            repository.toggleFavorite(article.url, article.isFavorite)
        }
    }

    fun askAiAboutArticle(article: NewsArticle, question: String) {
        viewModelScope.launch {
            _isChatLoading.value = true
            _chatResponse.value = repository.askAi(article, question)
            _isChatLoading.value = false
        }
    }

    fun loadAiInsights(article: NewsArticle) {
        viewModelScope.launch {
            // Check cache first
            insightsCache[article.url]?.let {
                _aiInsights.value = it
                return@launch
            }

            _isChatLoading.value = true
            val insights = repository.getAiInsights(article)
            insightsCache[article.url] = insights
            _aiInsights.value = insights
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatResponse.value = null
        _aiInsights.value = emptyList()
    }

    fun refreshNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _isOffline.value = false
            _errorMessage.value = null
            
            try {
                val selectedLangs = prefsRepository.getLanguages()
                
                val newsApiKeys = listOf(BuildConfig.NEWS_API_KEY, BuildConfig.NEWS_API_KEY_ALT)
                val newsDataKey = BuildConfig.NEWSDATA_IO_KEY
                val mediastackKey = BuildConfig.MEDIASTACK_KEY

                val currentCategory = _selectedCategory.value

                if (_selectedRegion.value == "RO") {
                    repository.refreshNews(
                        newsApiKeys = newsApiKeys,
                        newsDataKey = newsDataKey,
                        mediastackKey = mediastackKey,
                        region = "RO",
                        category = currentCategory,
                        language = "ro"
                    )
                } else {
                    // Fetch for all selected languages
                    selectedLangs.forEach { lang ->
                        repository.refreshNews(
                            newsApiKeys = newsApiKeys,
                            newsDataKey = newsDataKey,
                            mediastackKey = mediastackKey,
                            region = "GLOBAL",
                            category = currentCategory,
                            language = lang
                        )
                    }
                }
            } catch (e: Exception) {
                _isOffline.value = true
                _errorMessage.value = "Eroare conexiune: Verifică internetul. Se afișează datele offline."
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
