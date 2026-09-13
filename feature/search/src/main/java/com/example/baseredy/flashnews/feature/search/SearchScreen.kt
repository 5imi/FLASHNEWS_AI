package com.example.baseredy.flashnews.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.designsystem.component.EmptyState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel, 
    onBack: () -> Unit, 
    onArticleClick: (NewsArticle) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val trackedKeywords by viewModel.trackedKeywords.collectAsState()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    // Debounced real-time search
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            delay(350)
            viewModel.search(trimmed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Caută subiecte, politicieni, companii...", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { 
                                focusManager.clearFocus()
                                if (query.isNotBlank()) viewModel.search(query.trim()) 
                            }
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (query.isNotBlank()) {
                                    val isTracked = trackedKeywords.any { it.equals(query.trim(), ignoreCase = true) }
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleTrackKeyword(query.trim())
                                    }) {
                                        Icon(
                                            imageVector = if (isTracked) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                            contentDescription = if (isTracked) "Dezactivează alerta" else "Activează alerta",
                                            tint = if (isTracked) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        query = "" 
                                        viewModel.search("")
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Șterge căutarea", tint = Color.Gray)
                                    }
                                }
                                IconButton(onClick = { 
                                    focusManager.clearFocus()
                                    if (query.isNotBlank()) viewModel.search(query.trim()) 
                                }) {
                                    Icon(Icons.Default.Search, contentDescription = "Caută", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Trend-uri fierbinți", 
                style = MaterialTheme.typography.labelLarge, 
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.trends) { trend ->
                    FilterChip(
                        selected = query.equals(trend, ignoreCase = true),
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            query = trend
                            viewModel.search(trend)
                        },
                        label = { Text("#$trend", fontSize = 12.sp) },
                        shape = CircleShape
                    )
                }
            }

            if (trackedKeywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alerte de știri active", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trackedKeywords.toList(), key = { it }) { kw ->
                        InputChip(
                            selected = query.equals(kw, ignoreCase = true),
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                query = kw
                                viewModel.search(kw)
                            },
                            label = { Text("#$kw", fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleTrackKeyword(kw) 
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Șterge alertă", modifier = Modifier.size(12.dp))
                                }
                            },
                            shape = CircleShape
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (results.isEmpty() && query.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "Niciun rezultat",
                        description = "Nu am găsit știri pentru \"$query\". Încearcă alți termeni sau verifică conexiunea.",
                        icon = Icons.Default.Search,
                        onRetry = { viewModel.search(query) }
                    )
                }
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tastează un cuvânt sau selectează un #trend pentru a căuta în știrile salvate și globale.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "${results.size} rezultate găsite",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.url }) { article ->
                        SearchResultItem(
                            article = article, 
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onArticleClick(article) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(article: NewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = article.urlToImage,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.sourceName ?: "FlashNews", 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold
                    )
                    if (article.relativeTime.isNotBlank()) {
                        Text(
                            text = article.relativeTime,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = article.title, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    maxLines = 2,
                    lineHeight = 18.sp
                )
                if (!article.aiSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = article.aiSummary!!.replace(Regex("^•\\s*"), "").take(100),
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
