package com.example.baseredy.flashnews.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// [OLD] - Motiv înlocuire: Version 5 schema actualizată la Version 6 pentru aplicarea noilor indici compuși
// @Database(entities = [NewsArticleEntity::class], version = 5, exportSchema = false)
@Database(entities = [NewsArticleEntity::class, CustomRssFeedEntity::class], version = 7, exportSchema = false)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun customRssFeedDao(): CustomRssFeedDao

    companion object {
        @Volatile
        private var INSTANCE: NewsDatabase? = null

        fun getDatabase(context: Context): NewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    "flashnews_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
