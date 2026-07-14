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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Fetch latest RSS items
            val rssItems = RssClient.fetchRssNews()
            
            // Check for keywords indicating major news
            val keywords = listOf("Urgență", "Lege", "Fiscal", "Guvern", "Codul Fiscal", "Impozit", "TVA")
            
            val importantNews = rssItems.firstOrNull { item ->
                keywords.any { keyword -> 
                    item.title.contains(keyword, ignoreCase = true) 
                }
            }

            if (importantNews != null) {
                // If we found something important, show notification
                // Check if it's already in DB to avoid duplicate notifications
                val db = NewsDatabase.getDatabase(appContext)
                val existing = db.newsDao().getArticleByUrl(importantNews.link)
                
                if (existing == null) {
                    showNotification(importantNews.title, importantNews.sourceName)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(title: String, source: String) {
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

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
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
