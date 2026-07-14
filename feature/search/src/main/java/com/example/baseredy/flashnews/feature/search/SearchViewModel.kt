package com.example.baseredy.flashnews.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.baseredy.flashnews.core.data.NewsRepository
import com.example.baseredy.flashnews.core.database.NewsDatabase
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.network.GeminiClient

import com.example.baseredy.flashnews.feature.search.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiClient = GeminiClient(BuildConfig.GEMINI_API_KEY)
    
    private val repository = NewsRepository(
        NewsDatabase.getDatabase(application).newsDao(),
        geminiClient
    )

    private val _searchResults = MutableStateFlow<List<NewsArticle>>(emptyList())
    val searchResults: StateFlow<List<NewsArticle>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val trends = listOf("AI", "SpaceX", "Tesla", "Bitcoin", "Climate", "Healthcare")

    fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = repository.searchNews(BuildConfig.NEWS_API_KEY, query)
            _isLoading.value = false
        }
    }
}
