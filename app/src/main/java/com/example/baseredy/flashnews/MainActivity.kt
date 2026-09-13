package com.example.baseredy.flashnews

import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.baseredy.flashnews.core.data.NewsSyncWorker
import com.example.baseredy.flashnews.core.designsystem.theme.FlashNewsTheme
import com.example.baseredy.flashnews.feature.feed.FeedScreen
import com.example.baseredy.flashnews.feature.feed.FeedViewModel
import com.example.baseredy.flashnews.feature.feed.OnboardingScreen
import com.example.baseredy.flashnews.feature.search.SearchScreen
import com.example.baseredy.flashnews.feature.search.SearchViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupWorkManager()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkNotificationPermissionAndSetupWorkManager()

        enableEdgeToEdge()
        setContent {
            FlashNewsTheme {
                val navController = rememberNavController()
                val feedViewModel: FeedViewModel = viewModel()
                val onboardingCompleted by feedViewModel.onboardingCompleted.collectAsState()
                
                LaunchedEffect(intent) {
                    intent?.getStringExtra("article_url")?.let { url ->
                        feedViewModel.openArticleByUrl(url)
                        navController.navigate("feed")
                    }
                }

                NavHost(
                    navController = navController, 
                    startDestination = if (onboardingCompleted) "feed" else "onboarding"
                ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            categories = feedViewModel.categories,
                            onComplete = { langs, interests ->
                                feedViewModel.completeOnboarding(langs, interests)
                                navController.navigate("feed") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("feed") {
                        FeedScreen(
                            viewModel = feedViewModel, 
                            onSearchClick = { navController.navigate("search") }
                        )
                    }
                    composable("search") {
                        val searchViewModel: SearchViewModel = viewModel()
                        SearchScreen(
                            viewModel = searchViewModel, 
                            onBack = { navController.popBackStack() },
                            onArticleClick = { article ->
                                feedViewModel.selectArticleForDetail(article)
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkNotificationPermissionAndSetupWorkManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    setupWorkManager()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Măcar cerem permisiunea, util ar fi un UI
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            setupWorkManager()
        }
    }

    private fun setupWorkManager() {
        val syncRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(2, TimeUnit.HOURS)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewsSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
