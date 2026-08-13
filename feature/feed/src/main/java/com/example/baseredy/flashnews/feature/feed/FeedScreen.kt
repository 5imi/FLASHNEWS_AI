package com.example.baseredy.flashnews.feature.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.baseredy.flashnews.core.model.AiInsight
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.designsystem.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: FeedViewModel, onSearchClick: () -> Unit) {
    val articles = viewModel.articles.collectAsLazyPagingItems()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsState()
    val chatResponse by viewModel.chatResponse.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    
    var selectedArticleForDetail by remember { mutableStateOf<NewsArticle?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        if (articles.itemCount == 0 && !showOnlyFavorites) {
            viewModel.refreshNews()
        }
    }

    LaunchedEffect(selectedArticleForDetail) {
        selectedArticleForDetail?.let { article ->
            viewModel.loadAiInsights(article)
        }
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshNews() },
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        if (articles.itemCount > 0) {
            val pagerState = rememberPagerState(pageCount = { articles.itemCount })
            // [OLD] - Motiv înlocuire: VerticalPager fără `key` cauza recompoziții redundante și resetarea stării imaginilor la scroll
            /*
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val article = articles[page]
                if (article != null) {
                    NewsCard(
                        article = article,
                        onBookmark = { viewModel.toggleBookmark(article) },
                        onClick = { selectedArticleForDetail = article }
                    )
                }
            }
            */
            VerticalPager(
                state = pagerState,
                key = { page -> articles.peek(page)?.url ?: page },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val article = articles[page]
                if (article != null) {
                    NewsCard(
                        article = article,
                        onBookmark = { viewModel.toggleBookmark(article) },
                        onClick = { selectedArticleForDetail = article }
                    )
                }
            }
        } else if (!isLoading) {
            EmptyState(
                title = if (showOnlyFavorites) "Nicio știre salvată" else "Nicio știre găsită",
                description = if (showOnlyFavorites) "Începe să salvezi știri din flux pentru a le vedea aici." else "Nu am putut găsi știri. Verifică conexiunea sau schimbă filtrele.",
                icon = if (showOnlyFavorites) Icons.Default.Bookmarks else Icons.Default.Newspaper,
                onRetry = { viewModel.refreshNews() }
            )
        }
        
        // Error Banner
        if (isOffline) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = errorMessage ?: "Ești offline. Se afișează datele salvate local.",
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Top Nav overlayed
    TopBar(
        viewModel = viewModel,
        selectedRegion = selectedRegion,
        selectedCategory = selectedCategory,
        showOnlyFavorites = showOnlyFavorites,
        onSearchClick = onSearchClick
    )

    // Detail Bottom Sheet
    if (selectedArticleForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                selectedArticleForDetail = null
                viewModel.clearChat()
            },
            sheetState = sheetState,
            containerColor = Color(0xFF1C1C1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            ArticleDetailContent(
                article = selectedArticleForDetail!!,
                chatResponse = chatResponse,
                aiInsights = aiInsights,
                isChatLoading = isChatLoading,
                onAskQuestion = { q -> viewModel.askAiAboutArticle(selectedArticleForDetail!!, q) }
            )
        }
    }
}

@Composable
fun TopBar(viewModel: FeedViewModel, selectedRegion: String, selectedCategory: String, showOnlyFavorites: Boolean, onSearchClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
        
        // HUB SELECTOR (ROMÂNIA / INTERNAȚIONAL)
        if (!showOnlyFavorites) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        FilterChip(
                            selected = selectedRegion == "RO",
                            onClick = { viewModel.onRegionSelected("RO") },
                            label = { Text("🇷🇴 ROMÂNIA", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.Transparent,
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = selectedRegion == "GLOBAL",
                            onClick = { viewModel.onRegionSelected("GLOBAL") },
                            label = { Text("🌍 INTERNAȚIONAL", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.Transparent,
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Search, null, tint = Color.White)
            }

            LazyRow(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!showOnlyFavorites) {
                    items(viewModel.categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black
                            ),
                            border = null,
                            shape = CircleShape
                        )
                    }
                } else {
                    item { Text("Știri Salvate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                }
            }

            IconButton(
                onClick = { viewModel.toggleFavoritesView() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (showOnlyFavorites) Icons.Default.Newspaper else Icons.Default.Bookmarks,
                    contentDescription = null,
                    tint = if (showOnlyFavorites) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle, onBookmark: () -> Unit, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().clickable { onClick() }) {
        AsyncImage(
            model = article.urlToImage,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)), startY = 400f)
        ))

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).padding(bottom = 40.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = article.sourceLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        article.sourceName ?: "NEWS", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                    if (article.relativeTime.isNotEmpty()) {
                        Text(
                            " • ${article.relativeTime}", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FactCheckBadge(article.factCheckStatus)
                    if (article.isMultiPerspective) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFFFF9800).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("PERSPECTIVE MULTIPLE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        }
                    }
                }
                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (article.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (article.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(article.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 34.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // AI Digest
            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI DIGEST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)
                    }
                    val points = article.aiSummary?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                    points.forEach { point ->
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            val displayPoint = point.trim().removePrefix("•").trim()
                            Text("•", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(displayPoint, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleDetailContent(
    article: NewsArticle,
    chatResponse: String?,
    aiInsights: List<AiInsight>,
    isChatLoading: Boolean,
    onAskQuestion: (String) -> Unit
) {
    val context = LocalContext.current
    var questionText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = article.sourceLogoUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(article.sourceName ?: "Sursă Necunoscută", fontWeight = FontWeight.Bold, color = Color.White)
                Text(article.publishedAt, fontSize = 12.sp, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(article.description ?: "Nicio descriere disponibilă.", color = Color.LightGray, fontSize = 16.sp, lineHeight = 24.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Impact for Romania
        article.aiLocalImpact?.let { impact ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("IMPACT ASUPRA ROMÂNIEI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(impact, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // AI Transparency Note
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val timeLabel = article.aiAnalyzedAt?.let { " (analizat acum ${formatTimestamp(it)})" } ?: ""
                Text(
                    "Rezumat generat și tradus automat de AI$timeLabel. Verifică sursa originală pentru context complet.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Insight Section
        val biasColor = when (article.aiBias) {
            "NEUTRU" -> Color(0xFF4CAF50)
            "DREAPTA" -> Color(0xFFF44336)
            "STÂNGA" -> Color(0xFF2196F3)
            "PROPAGANDĂ", "PROPAGANDA" -> Color(0xFFFFEB3B)
            else -> Color.White
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ANALIZĂ AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Text("Înclinație Editorială: ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(
                        text = article.aiBias ?: "NEUTRU", 
                        color = biasColor, 
                        fontSize = 14.sp, 
                        fontWeight = if (article.aiBias == "PROPAGANDĂ" || article.aiBias == "PROPAGANDA") FontWeight.ExtraBold else FontWeight.Bold
                    )
                }
                Text(article.factCheckReason, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }


        if (article.isMultiPerspective) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.05f)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CompareArrows, null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("ANALIZĂ COMPARATIVĂ", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Acest subiect este intens dezbătut la nivel global. AI-ul analizează acum narațiunile din ${article.region} și alte regiuni pentru a-ți oferi o imagine de ansamblu echilibrată.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // AI ASSISTANT SECTION
        Spacer(modifier = Modifier.height(32.dp))
        Text("ASISTENTUL AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AssistChip(
                    onClick = { onAskQuestion("Explică simplu această știre în 2 propoziții.") },
                    label = { Text("Explică simplu") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { onAskQuestion("Spune-mi de ce contează această știre pentru România.") },
                    label = { Text("Impact local") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { onAskQuestion("Ce trebuie să verific înainte să cred această știre?") },
                    label = { Text("Ce să verific") },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (aiInsights.isNotEmpty()) {
            aiInsights.forEach { insight ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(insight.title, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(insight.content, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        TextField(
            value = questionText,
            onValueChange = { questionText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex: Cum ne afectează asta economia?", color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            trailingIcon = {
                IconButton(
                    onClick = { 
                        if (questionText.isNotBlank()) {
                            onAskQuestion(questionText)
                            questionText = ""
                        }
                    },
                    enabled = !isChatLoading
                ) {
                    if (isChatLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        if (chatResponse != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = chatResponse,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { 
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Citește Articolul Complet")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    return when {
        minutes < 1 -> "câteva secunde"
        minutes < 60 -> "$minutes minute"
        else -> "${minutes / 60} ore"
    }
}

@Composable
fun FactCheckBadge(status: String) {
    val (color, text) = when (status) {
        "VERIFIED" -> Color(0xFF4CAF50) to "VERIFICAT"
        "UNVERIFIED" -> Color(0xFFF44336) to "NEVERIFICAT"
        else -> Color(0xFFFFD60A) to "VERIFICARE"
    }
    Surface(color = color.copy(alpha = 0.15f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
