package com.example.baseredy.flashnews.feature.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baseredy.flashnews.core.data.NewsRepository
import com.example.baseredy.flashnews.core.data.UserPreferencesRepository
import com.example.baseredy.flashnews.core.database.NewsDatabase
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.GeminiClient

import com.example.baseredy.flashnews.feature.feed.BuildConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.paging.PagingData
import androidx.paging.cachedIn

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiClient = GeminiClient(BuildConfig.GEMINI_API_KEY)
    
    private val repository = NewsRepository(
        NewsDatabase.getDatabase(application).newsDao(),
        geminiClient
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

    val categories = listOf("Toate", "Politică", "Business", "Fiscalitate", "Tehnologie", "Știință", "Sport", "Sănătate", "Divertisment")

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

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading

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

    fun clearChat() {
        _chatResponse.value = null
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

                val apiCategory = when (_selectedCategory.value) {
                    "Toate", "General" -> "General"
                    "Fiscalitate", "Business" -> "Business"
                    "Politică" -> "Politics"
                    "Tehnologie" -> "Technology"
                    "Știință" -> "Science"
                    "Sport" -> "Sports"
                    "Sănătate" -> "Health"
                    "Divertisment" -> "Entertainment"
                    else -> "General"
                }

                if (_selectedRegion.value == "RO") {
                    repository.refreshNews(
                        newsApiKeys = newsApiKeys,
                        newsDataKey = newsDataKey,
                        mediastackKey = mediastackKey,
                        region = "RO",
                        category = apiCategory,
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
                            category = apiCategory,
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
