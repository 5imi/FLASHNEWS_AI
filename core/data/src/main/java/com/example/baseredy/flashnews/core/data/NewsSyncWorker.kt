package com.example.baseredy.flashnews.core.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.baseredy.flashnews.core.database.NewsDatabase
import com.example.baseredy.flashnews.core.network.RssClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // [OLD] - Motiv înlocuire: Worker-ul descărca 150+ surse globale la fiecare 2h și nu salva articolele în Room DB pentru pre-caching
    /*
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val rssItems = RssClient.fetchRssNews()
            val keywords = listOf("Urgență", "Lege", "Fiscal", "Guvern", "Codul Fiscal", "Impozit", "TVA")
            val importantNews = rssItems.firstOrNull { item ->
                keywords.any { keyword -> 
                    item.title.contains(keyword, ignoreCase = true) 
                }
            }
            if (importantNews != null) {
                val db = NewsDatabase.getDatabase(appContext)
                val existing = db.newsDao().getArticleByUrl(importantNews.link)
                if (existing == null) {
                    showNotification(importantNews.title, importantNews.sourceName, importantNews.link)
                }
            }
            // Auto-cleanup articles older than 14 days that are not bookmarked
            runCatching {
                val thresholdDate = java.time.ZonedDateTime.now().minusDays(14)
                    .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                db.newsDao().deleteOldArticles(thresholdDate)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    */

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = NewsDatabase.getDatabase(appContext)
            val followedFeeds = runCatching { db.customRssFeedDao().getFollowedFeedsSync() }.getOrNull() ?: emptyList()
            val extraSources = followedFeeds.map { 
                com.example.baseredy.flashnews.core.network.RssSource(name = it.name, url = it.url, category = it.category, region = it.region) 
            }
            // Fetch priority RO & General RSS items + any followed/custom feeds for efficient background sync
            val rssItems = RssClient.fetchRssNews(targetRegion = "RO", targetCategory = "General", extraSources = extraSources)

            // Pre-cache new items in Room database
            if (rssItems.isNotEmpty()) {
                val entities = rssItems.map { item ->
                    com.example.baseredy.flashnews.core.database.NewsArticleEntity(
                        url = item.link,
                        title = item.title,
                        description = item.description,
                        urlToImage = item.imageUrl,
                        publishedAt = item.pubDate,
                        sourceName = item.sourceName,
                        category = item.category,
                        region = item.region,
                        aiSummary = "• ${item.description.take(150)}\n• Sursa: ${item.sourceName}",
                        aiBias = "NEUTRU",
                        aiLocalImpact = null,
                        aiAnalyzedAt = 0L,
                        isFavorite = false,
                        isMultiPerspective = false
                    )
                }
                db.newsDao().insertArticlesIfAbsent(entities)
            }
            
            // Check for keywords indicating major breaking news or user-tracked topics
            val prefs = UserPreferencesRepository(appContext)
            val userKeywords = prefs.getTrackedKeywords()
            val systemKeywords = listOf("Urgență", "Lege", "Fiscal", "Guvern", "Codul Fiscal", "Impozit", "TVA", "Alertă")
            val allKeywords = (systemKeywords + userKeywords).distinct()

            var matchedKeyword: String? = null
            val importantNews = rssItems.firstOrNull { item ->
                allKeywords.firstOrNull { keyword ->
                    item.title.contains(keyword, ignoreCase = true) || item.description.contains(keyword, ignoreCase = true)
                }?.also { matchedKeyword = it } != null
            }

            if (importantNews != null) {
                val existing = db.newsDao().getArticleByUrl(importantNews.link)
                if (existing == null || existing.aiAnalyzedAt == 0L) {
                    val notifSource = if (matchedKeyword != null && matchedKeyword in userKeywords) {
                        "Alertă #$matchedKeyword • ${importantNews.sourceName}"
                    } else {
                        importantNews.sourceName
                    }
                    showNotification(importantNews.title, notifSource, importantNews.link)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(title: String, source: String, articleUrl: String) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "flashnews_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Știri Importante",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificări pentru breaking news și fiscalitate"
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.apply {
            putExtra("article_url", articleUrl)
            data = android.net.Uri.parse(articleUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Alerta: $source")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
