package com.example.baseredy.flashnews.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.baseredy.flashnews.MainActivity
import com.example.baseredy.flashnews.R
import com.example.baseredy.flashnews.core.database.NewsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlashNewsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val database = NewsDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val recent = try {
                database.newsDao().getRecentArticles(1)
            } catch (e: Exception) {
                emptyList()
            }
            val article = recent.firstOrNull()

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_flashnews)
                if (article != null) {
                    views.setTextViewText(R.id.widget_title, article.title)
                    val summary = article.aiSummary?.takeIf { !it.startsWith("{") } ?: article.description ?: "Deschide aplicația pentru sinteza completă."
                    views.setTextViewText(R.id.widget_summary, summary.take(180))
                    views.setTextViewText(R.id.widget_source, article.sourceName ?: "FlashNews")
                } else {
                    views.setTextViewText(R.id.widget_title, "FlashNews AI")
                    views.setTextViewText(R.id.widget_summary, "Apasă pentru a deschide știrile și a asculta buletinul audio.")
                    views.setTextViewText(R.id.widget_source, "RO & Global")
                }

                // Intent to open MainActivity on click
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
