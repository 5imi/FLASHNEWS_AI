# ==============================================================================
# FlashNews AI - ProGuard & R8 Optimization Rules
# ==============================================================================

# --- Kotlin Coroutines & Flow ---
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}
-keep,allowobfuscation,allowshrinking class *$$serializer { *; }
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keepclassmembers class * {
    @androidx.room.Dao <methods>;
    @androidx.room.Entity <fields>;
}

# --- Ktor & OkHttp Networking ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Domain & Network Models ---
-keep class com.example.baseredy.flashnews.core.model.** { *; }
-keepclassmembers class com.example.baseredy.flashnews.core.model.** { *; }
-keep class com.example.baseredy.flashnews.core.database.NewsEntity { *; }

# --- Jetpack Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Google Generative AI SDK ---
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# --- Retrofit & Gson/JSON ---
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class com.jakewharton.retrofit2.** { *; }

# --- Coil Image Loading ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- WorkManager ---
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Keep BuildConfig ---
-keep class com.example.baseredy.flashnews.BuildConfig { *; }

# --- Keep Application class ---
-keep class com.example.baseredy.flashnews.FlashNewsApplication { *; }
-keep class com.example.baseredy.flashnews.MainActivity { *; }