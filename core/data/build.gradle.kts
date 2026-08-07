plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.baseredy.flashnews.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 31
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
    api(project(":core:model"))
    api(project(":core:network"))
    api(project(":core:database"))
    
    implementation(libs.androidx.core.ktx)
    implementation("androidx.room:room-runtime:2.7.2")
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.work.runtime.ktx)
}
