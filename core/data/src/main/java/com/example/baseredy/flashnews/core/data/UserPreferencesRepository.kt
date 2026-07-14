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
}
