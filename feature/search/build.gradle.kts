plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.baseredy.flashnews.feature.search"
    compileSdk = 35

    defaultConfig {
        minSdk = 31
        
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "GROK_API_KEY", "\"${project.findProperty("GROK_API_KEY") ?: "grok_placeholder"}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${project.findProperty("GROQ_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${project.findProperty("OPENROUTER_API_KEY") ?: ""}\"")
        buildConfigField("String", "NEWS_API_KEY", "\"${project.findProperty("NEWS_API_KEY") ?: ""}\"")
        buildConfigField("String", "NEWSDATA_IO_KEY", "\"${project.findProperty("NEWSDATA_IO_KEY") ?: ""}\"")
        buildConfigField("String", "MEDIASTACK_KEY", "\"${project.findProperty("MEDIASTACK_KEY") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
}
