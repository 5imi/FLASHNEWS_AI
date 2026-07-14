package com.example.baseredy.flashnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.baseredy.flashnews.core.designsystem.theme.FlashNewsTheme
import com.example.baseredy.flashnews.feature.feed.FeedScreen
import com.example.baseredy.flashnews.feature.feed.FeedViewModel
import com.example.baseredy.flashnews.feature.feed.OnboardingScreen
import com.example.baseredy.flashnews.feature.search.SearchScreen
import com.example.baseredy.flashnews.feature.search.SearchViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            FlashNewsTheme {
                val navController = rememberNavController()
                val feedViewModel: FeedViewModel = viewModel()
                val onboardingCompleted by feedViewModel.onboardingCompleted.collectAsState()
                
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
                            onArticleClick = { /* We could open detail here too */ }
                        )
                    }
                }
            }
        }
    }
}
