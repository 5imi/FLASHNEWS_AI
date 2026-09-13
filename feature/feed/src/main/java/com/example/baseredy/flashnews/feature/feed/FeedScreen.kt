package com.example.baseredy.flashnews.feature.feed

import android.content.Intent
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.baseredy.flashnews.core.model.AiInsight
import com.example.baseredy.flashnews.core.model.NewsArticle
import com.example.baseredy.flashnews.core.designsystem.component.EmptyState


fun shareArticle(context: Context, article: NewsArticle) {
    val shareText = buildString {
        append("📰 *${article.title}*\n\n")
        val summary = article.aiSummary
        if (!summary.isNullOrBlank()) {
            append("🤖 *Sinteză AI:*\n$summary\n\n")
        } else if (!article.description.isNullOrBlank()) {
            append("${article.description}\n\n")
        }
        append("🔗 Sursă: ${article.sourceName ?: "FlashNews AI"}\n")
        append("${article.url}\n\n")
        append("Trimis prin FlashNews AI ⚡")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Distribuie știrea"))
}

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
    val readArticleUrls by viewModel.readArticleUrls.collectAsState()
    
    val selectedArticleForDetail by viewModel.selectedArticleForDetail.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var playingArticleUrl by remember { mutableStateOf<String?>(null) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRate by remember { mutableFloatStateOf(1.35f) }
    var isPlayingRadio by remember { mutableStateOf(false) }
    var showRadioSheet by remember { mutableStateOf(false) }
    var showCatalogSheet by remember { mutableStateOf(false) }

    val perspective360 by viewModel.perspective360.collectAsState()
    val isPerspectiveLoading by viewModel.isPerspectiveLoading.collectAsState()

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ro = Locale("ro", "RO")
                val result = tts?.setLanguage(ro)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(speechRate)
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                playingArticleUrl = null
                isPlayingRadio = false
            }
            override fun onError(utteranceId: String?) {
                playingArticleUrl = null
                isPlayingRadio = false
            }
        })
        ttsEngine = tts

        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val onPlayText: (String) -> Unit = { text ->
        val tts = ttsEngine
        if (tts != null) {
            tts.stop()
            tts.setSpeechRate(speechRate)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FlashNews_Text_Utterance")
        }
    }

    val onTogglePlayRadio: (String) -> Unit = { script ->
        val tts = ttsEngine
        if (tts != null) {
            if (isPlayingRadio) {
                tts.stop()
                isPlayingRadio = false
            } else {
                tts.stop()
                playingArticleUrl = null
                isPlayingRadio = true
                tts.setSpeechRate(speechRate)
                tts.speak(script, TextToSpeech.QUEUE_FLUSH, null, "FlashNews_Radio_Briefing")
            }
        }
    }

    val onToggleAudio: (NewsArticle) -> Unit = { article ->
        val tts = ttsEngine
        if (tts != null) {
            if (playingArticleUrl == article.url) {
                tts.stop()
                playingArticleUrl = null
            } else {
                tts.stop()
                playingArticleUrl = article.url
                val textToRead = buildString {
                    append(article.title)
                    append(". ")
                    val summary = article.aiSummary
                    if (!summary.isNullOrBlank()) {
                        append("Sinteză AI: ")
                        val clean = summary.replace("\n", ". ").replace("•", "").replace("*", "")
                        append(clean)
                    } else if (!article.description.isNullOrBlank()) {
                        append(article.description)
                    }
                }
                tts.setSpeechRate(speechRate)
                tts.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "FlashNews_Utterance")
            }
        }
    }


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

    val isPagingLoading = articles.loadState.refresh is androidx.paging.LoadState.Loading
    val isInitialLoading = (isLoading || isPagingLoading) && articles.itemCount == 0

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshNews() },
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        if (isInitialLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (articles.itemCount > 0) {
            val pagerState = rememberPagerState(pageCount = { articles.itemCount })
            VerticalPager(
                state = pagerState,
                key = { page -> articles.peek(page)?.url ?: page },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val article = articles[page]
                if (article != null) {
                    NewsCard(
                        article = article,
                        isRead = article.url in readArticleUrls,
                        isPlayingAudio = playingArticleUrl == article.url,
                        onPlayAudio = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleAudio(article) 
                        },
                        onShare = { shareArticle(context, article) },
                        onBookmark = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleBookmark(article) 
                        },
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectArticleForDetail(article) 
                        }
                    )
                }
            }
        } else {
            EmptyState(
                title = if (showOnlyFavorites) "Nicio știre salvată" else "Nicio știre găsită",
                description = if (showOnlyFavorites) "Începe să salvezi știri din flux pentru a le vedea aici." else "Nu am putut găsi știri. Verifică conexiunea sau schimbă filtrele.",
                icon = if (showOnlyFavorites) Icons.Default.Bookmarks else Icons.Default.Newspaper,
                onRetry = { viewModel.refreshNews() }
            )
        }
        
        // Modern Interactive Offline Banner
        AnimatedVisibility(
            visible = isOffline,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 115.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2C1515).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.4f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "Mod Offline • Se afișează știrile din memorie",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                    TextButton(
                        onClick = { viewModel.refreshNews() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Reîncearcă", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Top Nav overlayed
    TopBar(
        viewModel = viewModel,
        selectedRegion = selectedRegion,
        selectedCategory = selectedCategory,
        showOnlyFavorites = showOnlyFavorites,
        onSearchClick = onSearchClick,
        onRadioClick = { showRadioSheet = true },
        onCatalogClick = { showCatalogSheet = true }
    )

    // Detail Bottom Sheet
    if (selectedArticleForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                if (playingArticleUrl != null) {
                    ttsEngine?.stop()
                    playingArticleUrl = null
                }
                viewModel.selectArticleForDetail(null)
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
                perspective360 = perspective360,
                isPerspectiveLoading = isPerspectiveLoading,
                onLoad360Perspective = { viewModel.load360Perspective(selectedArticleForDetail!!) },
                onPlayTts = onPlayText,
                onAskQuestion = { q -> viewModel.askAiAboutArticle(selectedArticleForDetail!!, q) },
                onClose = {
                    if (playingArticleUrl != null) {
                        ttsEngine?.stop()
                        playingArticleUrl = null
                    }
                    viewModel.selectArticleForDetail(null)
                }
            )
        }
    }

    if (showRadioSheet) {
        RadioBriefingSheet(
            viewModel = viewModel,
            ttsEngine = ttsEngine,
            isPlayingRadio = isPlayingRadio,
            onTogglePlayRadio = onTogglePlayRadio,
            speechRate = speechRate,
            onSpeechRateChange = { newRate ->
                speechRate = newRate
                ttsEngine?.setSpeechRate(newRate)
            },
            onDismiss = { 
                showRadioSheet = false
                if (isPlayingRadio) {
                    ttsEngine?.stop()
                    isPlayingRadio = false
                }
            }
        )
    }

    if (showCatalogSheet) {
        RssCatalogSheet(
            viewModel = viewModel,
            onDismiss = { showCatalogSheet = false }
        )
    }
}

@Composable
fun TopBar(
    viewModel: FeedViewModel, 
    selectedRegion: String, 
    selectedCategory: String, 
    showOnlyFavorites: Boolean, 
    onSearchClick: () -> Unit,
    onRadioClick: () -> Unit = {},
    onCatalogClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp)
    ) {
        // ROW 1: Hub Selector on Left/Center, Action Icons on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!showOnlyFavorites) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        FilterChip(
                            selected = selectedRegion == "RO",
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.onRegionSelected("RO") },
                            label = { Text("🇷🇴 RO", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.Transparent,
                                labelColor = Color.White,
                                selectedLabelColor = Color.Black
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        FilterChip(
                            selected = selectedRegion == "GLOBAL",
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.onRegionSelected("GLOBAL") },
                            label = { Text("🌍 GLOBAL", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
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
            } else {
                Text(
                    text = "⭐ Știri Salvate",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Quick Actions: Search, Radio AI, Catalog RSS, Bookmarks
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(38.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Caută știri", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onRadioClick,
                    modifier = Modifier.size(38.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Radio, contentDescription = "Radio AI Buletin", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onCatalogClick,
                    modifier = Modifier.size(38.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Catalog Surse", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { viewModel.toggleFavoritesView() },
                    modifier = Modifier.size(38.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.Newspaper else Icons.Default.Bookmarks,
                        contentDescription = if (showOnlyFavorites) "Comută la fluxul principal de știri" else "Comută la știrile salvate la favorite",
                        tint = if (showOnlyFavorites) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ROW 2: Category Chips Row (Full width horizontal scroll)
        if (!showOnlyFavorites) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(viewModel.categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            labelColor = Color.White,
                            selectedLabelColor = Color.Black
                        ),
                        border = null,
                        shape = CircleShape
                    )
                }
            }
        }
    }
}

@Composable
fun NewsCard(
    article: NewsArticle,
    isRead: Boolean = false,
    isPlayingAudio: Boolean = false,
    onPlayAudio: () -> Unit = {},
    onShare: () -> Unit = {},
    onBookmark: () -> Unit,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().clickable(onClickLabel = "Deschide analiza detaliată a știrii") { onClick() }) {
        AsyncImage(
            model = article.urlToImage,
            contentDescription = "Imagine ilustrativă pentru știrea: ${article.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)), startY = 400f)
        ))

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).padding(bottom = 40.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
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
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (article.relativeTime.isNotEmpty()) {
                        Text(
                            " • ${article.relativeTime}", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    if (isRead) {
                        Text(
                            " • Citit",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    val bias = article.aiBias
                    if (!bias.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeColor = getBiasBadgeColor(bias)
                        Surface(
                            color = badgeColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = bias,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingAudio) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isPlayingAudio) "Oprește audio" else "Ascultă rezumatul audio",
                            tint = if (isPlayingAudio) MaterialTheme.colorScheme.secondary else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Distribuie știrea",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = onBookmark,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (article.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (article.isFavorite) "Elimină din favorite" else "Salvează la favorite",
                            tint = if (article.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(article.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 34.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // AI Digest
            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Sinteză inteligentă AI", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
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
    perspective360: String? = null,
    isPerspectiveLoading: Boolean = false,
    onLoad360Perspective: () -> Unit = {},
    onPlayTts: (String) -> Unit = {},
    onAskQuestion: (String) -> Unit,
    onClose: () -> Unit = {}
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = article.sourceLogoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(article.sourceName ?: "Sursă Necunoscută", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(article.relativeTime.ifBlank { article.publishedAt }, fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Închide detalii", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(article.description ?: "Nicio descriere disponibilă.", color = Color.LightGray, fontSize = 16.sp, lineHeight = 24.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Impact for Romania (Shown when available and relevant)
        val localImpact = article.aiLocalImpact
        if (!localImpact.isNullOrBlank()) {
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
                    Text(localImpact, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
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
                val analyzedAt = article.aiAnalyzedAt
                val timeLabel = if (analyzedAt != null && analyzedAt > 0L) {
                    val formatted = formatTimestamp(analyzedAt)
                    if (formatted.isNotBlank()) " (analizat acum $formatted)" else ""
                } else ""
                Text(
                    "Rezumat și analiză generate de AI News Analyst$timeLabel. Verifică sursa originală pentru context complet.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Editorial Bias Section
        val biasColor = getBiasBadgeColor(article.aiBias)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ANALIZĂ EDITORIALĂ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Înclinație: ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(
                        text = article.aiBias ?: "NEUTRU", 
                        color = biasColor, 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
                if (article.biasRationale.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.biasRationale,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // AI NEWS ANALYST DYNAMIC INSIGHTS
        Spacer(modifier = Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("UNGHIURI CRITICE DE ANALIZĂ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic chips generated for this specific article
        if (aiInsights.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(aiInsights) { insight ->
                    AssistChip(
                        onClick = { onAskQuestion(insight.title) },
                        label = { Text(insight.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.QuestionAnswer, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            aiInsights.forEach { insight ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(insight.title, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(insight.content, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else if (isChatLoading) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI News Analyst generează analiza dinamică...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        }

        // 🌐 PERSPECTIVĂ 360° & ANTI-MANIPULARE (COMPARAȚIE ZIARE)
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PERSPECTIVĂ 360° & ANTI-MANIPULARE",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                    if (perspective360 == null && !isPerspectiveLoading) {
                        TextButton(onClick = onLoad360Perspective) {
                            Text("Compară ziarele", fontSize = 12.sp)
                        }
                    }
                }

                if (isPerspectiveLoading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AI-ul compară relatarea subiectului între mai multe redacții...", fontSize = 12.sp, color = Color.LightGray)
                    }
                } else if (perspective360 != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = perspective360!!,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Verifică automat dacă faptele relatate coincid cu alte publicații și dacă există nuanțe sau omisiuni.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 💬 ÎNTREABĂ ȘTIREA - CIPURI RAPIDE
        Spacer(modifier = Modifier.height(24.dp))
        Text("Întreabă AI despre acest subiect:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val quickQuestions = listOf(
            "Cum mă afectează direct?",
            "Context istoric pe scurt",
            "Explică ca unui copil de 10 ani",
            "Ce spun vocile critice?"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickQuestions) { q ->
                AssistChip(
                    onClick = { onAskQuestion(q) },
                    label = { Text(q, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = questionText,
            onValueChange = { questionText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Întreabă AI despre acest subiect...", color = Color.Gray) },
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
                    enabled = !isChatLoading,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isChatLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Trimite întrebarea către AI News Analyst", tint = MaterialTheme.colorScheme.primary)
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Răspuns AI:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                        IconButton(
                            onClick = { onPlayTts(chatResponse) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ascultă răspunsul la viteză rapidă",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = chatResponse,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
                Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { shareArticle(context, article) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Distribuie")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Distribuie")
            }

            Button(
                onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Deschide sursa originală în browser extern")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Citește tot")
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60000
    return when {
        minutes < 1 -> "câteva secunde"
        minutes < 60 -> "$minutes minute"
        minutes < 1440 -> "${minutes / 60} ore"
        else -> "${minutes / 1440} zile"
    }
}

private fun getBiasBadgeColor(bias: String?): Color {
    val normalized = bias?.uppercase() ?: return Color(0xFF78909C)
    return when {
        normalized.contains("SENZAȚIONALIST") || normalized.contains("SENZATIONALIST") || normalized.contains("TABLOID") -> Color(0xFFFB8C00)
        normalized.contains("NEUTRU") || normalized.contains("FACTUAL") || normalized.contains("INDEPENDENT") -> Color(0xFF4CAF50)
        normalized.contains("DREAPTA") -> Color(0xFF1E88E5)
        normalized.contains("STÂNGA") || normalized.contains("STANGA") || normalized.contains("SOCIAL") -> Color(0xFF8E24AA)
        normalized.contains("PROPAGANDĂ") || normalized.contains("PROPAGANDA") || normalized.contains("EXTREMA") -> Color(0xFFE53935)
        normalized.contains("PARTIZAN") -> Color(0xFFFF7043)
        else -> Color(0xFF78909C)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NewsCardPreview() {
    MaterialTheme {
        NewsCard(
            article = NewsArticle(
                title = "Comisia Europeană aprobă tranziția energetică pentru România",
                description = "Planul de investiții de 2 miliarde de euro a primit acordul oficial la Bruxelles.",
                url = "https://example.com/news/1",
                urlToImage = null,
                publishedAt = "2026-08-16T12:00:00Z",
                sourceName = "Digi24",
                aiSummary = "• Investiție de 2 miliarde de euro pentru rețele electrice.\n• Tranziție accelerată către surse regenerabile.",
                aiBias = "NEUTRU",
                aiLocalImpact = "Impact direct asupra tarifelor energetice locale.",
                biasRationale = "Raportare tehnică factuală.",
                relativeTime = "acum 15m",
                region = "RO",
                category = "Economie"
            ),
            onBookmark = {},
            onClick = {}
        )
    }
}
