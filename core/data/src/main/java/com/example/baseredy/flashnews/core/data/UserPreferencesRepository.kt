package com.example.baseredy.flashnews.core.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LANGUAGES = "languages"
        private const val KEY_INTERESTS = "interests"
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getLanguages(): Set<String> {
        return sharedPreferences.getStringSet(KEY_LANGUAGES, setOf("ro", "us")) ?: setOf("ro", "us")
    }

    fun setLanguages(languages: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_LANGUAGES, languages).apply()
    }

    fun getInterests(): Set<String> {
        return sharedPreferences.getStringSet(KEY_INTERESTS, setOf("General")) ?: setOf("General")
    }

    fun setInterests(interests: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_INTERESTS, interests).apply()
    }

    fun getReadArticleUrls(): Set<String> {
        return sharedPreferences.getStringSet("read_article_urls", emptySet()) ?: emptySet()
    }

    fun markArticleAsRead(url: String) {
        val current = getReadArticleUrls().toMutableSet()
        if (current.size > 300) {
            val oldest = current.firstOrNull()
            if (oldest != null) current.remove(oldest)
        }
        current.add(url)
        sharedPreferences.edit().putStringSet("read_article_urls", current).apply()
    }

    fun getTrackedKeywords(): Set<String> {
        return sharedPreferences.getStringSet("tracked_keywords", setOf("Fiscal", "TVA", "AI", "România")) ?: setOf("Fiscal", "TVA", "AI", "România")
    }

    fun addTrackedKeyword(keyword: String) {
        val current = getTrackedKeywords().toMutableSet()
        current.add(keyword.trim())
        sharedPreferences.edit().putStringSet("tracked_keywords", current).apply()
    }

    fun removeTrackedKeyword(keyword: String) {
        val current = getTrackedKeywords().toMutableSet()
        current.remove(keyword.trim())
        sharedPreferences.edit().putStringSet("tracked_keywords", current).apply()
    }

    fun isCommuteModeEnabled(): Boolean {
        return sharedPreferences.getBoolean("commute_mode_enabled", false)
    }

    fun setCommuteModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("commute_mode_enabled", enabled).apply()
    }
}
