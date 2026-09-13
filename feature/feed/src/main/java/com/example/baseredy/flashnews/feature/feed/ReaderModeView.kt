package com.example.baseredy.flashnews.feature.feed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baseredy.flashnews.core.model.NewsArticle

data class ReaderTheme(
    val name: String,
    val backgroundColor: Color,
    val cardColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color
)

val ReaderThemes = listOf(
    ReaderTheme(
        name = "AMOLED",
        backgroundColor = Color(0xFF0B0D11),
        cardColor = Color(0xFF161A22),
        textColor = Color(0xFFF1F5F9),
        secondaryTextColor = Color(0xFF94A3B8),
        accentColor = Color(0xFF38BDF8)
    ),
    ReaderTheme(
        name = "Sepia Cald",
        backgroundColor = Color(0xFF1E1A16),
        cardColor = Color(0xFF28231E),
        textColor = Color(0xFFF4EAD4),
        secondaryTextColor = Color(0xFFBFAFA0),
        accentColor = Color(0xFFF59E0B)
    ),
    ReaderTheme(
        name = "Nocturn Indigo",
        backgroundColor = Color(0xFF0F172A),
        cardColor = Color(0xFF1E293B),
        textColor = Color(0xFFE2E8F0),
        secondaryTextColor = Color(0xFF94A3B8),
        accentColor = Color(0xFF818CF8)
    )
)

/**
 * Distraction-free Reader Mode for comfortable, customizable reading of news articles.
 */
@Composable
fun ReaderModeView(
    article: NewsArticle,
    onPlayTts: (String) -> Unit,
    onShare: () -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedThemeIndex by remember { mutableStateOf(0) }
    var fontSizeSp by remember { mutableStateOf(17) }
    val currentTheme = ReaderThemes[selectedThemeIndex]

    val cleanDescription = remember(article.description) {
        cleanHtml(article.description ?: "")
    }
    val cleanSummary = remember(article.aiSummary) {
        cleanHtml(article.aiSummary ?: "")
    }

    val fullReadingText = remember(cleanSummary, cleanDescription) {
        buildString {
            if (cleanSummary.isNotBlank()) {
                append("Rezumat: ")
                append(cleanSummary)
                append("\n\n")
            }
            if (cleanDescription.isNotBlank() && cleanDescription != cleanSummary) {
                append(cleanDescription)
            }
        }.ifBlank { article.title }
    }

    val wordCount = remember(article.title, cleanDescription, cleanSummary) {
        val allText = "${article.title} $cleanDescription $cleanSummary"
        allText.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val readingTimeMinutes = remember(wordCount) {
        (wordCount / 160).coerceAtLeast(1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(currentTheme.backgroundColor)
            .padding(16.dp)
    ) {
        // --- Reading Metadata Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(currentTheme.cardColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = currentTheme.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "~$readingTimeMinutes min lectură",
                    fontSize = 12.sp,
                    color = currentTheme.secondaryTextColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "•",
                    color = currentTheme.secondaryTextColor,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$wordCount cuvinte",
                    fontSize = 12.sp,
                    color = currentTheme.secondaryTextColor
                )
            }

            // Quick Audio Play Button
            IconButton(
                onClick = { onPlayTts(fullReadingText) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Ascultă articolul",
                    tint = currentTheme.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Customization Controls: Font Size & Atmosphere ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Font Size Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(14 to "A-", 17 to "A", 20 to "A+", 23 to "A++").forEach { (size, label) ->
                    val isSelected = fontSizeSp == size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) currentTheme.accentColor.copy(alpha = 0.2f)
                                else currentTheme.cardColor
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) currentTheme.accentColor else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { fontSizeSp = size }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) currentTheme.accentColor else currentTheme.secondaryTextColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Theme Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderThemes.forEachIndexed { index, theme ->
                    val isSelected = selectedThemeIndex == index
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(theme.backgroundColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) currentTheme.accentColor else theme.secondaryTextColor.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { selectedThemeIndex = index }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Article Title ---
        Text(
            text = article.title,
            color = currentTheme.textColor,
            fontSize = (fontSizeSp + 4).sp,
            lineHeight = (fontSizeSp + 10).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Source and Date badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = article.sourceName ?: "Sursă media",
                color = currentTheme.accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "•",
                color = currentTheme.secondaryTextColor,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = article.relativeTime.ifBlank { article.publishedAt },
                color = currentTheme.secondaryTextColor,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- AI Executive Summary Callout (if available) ---
        if (cleanSummary.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = currentTheme.cardColor
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    currentTheme.accentColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = currentTheme.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REZUMAT EXECUTIV (AI)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.accentColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cleanSummary,
                        color = currentTheme.textColor,
                        fontSize = (fontSizeSp - 1).sp,
                        lineHeight = (fontSizeSp * 1.5f).sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // --- Article Body Paragraphs ---
        if (cleanDescription.isNotBlank() && cleanDescription != cleanSummary) {
            val paragraphs = cleanDescription.split("\n\n", "\n").filter { it.isNotBlank() }
            paragraphs.forEachIndexed { idx, p ->
                Text(
                    text = p.trim(),
                    color = currentTheme.textColor.copy(alpha = 0.95f),
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.6f).sp,
                    fontFamily = FontFamily.Default
                )
                if (idx < paragraphs.lastIndex) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        } else if (cleanSummary.isBlank()) {
            Text(
                text = "Niciun conținut suplimentar disponibil în fluxul RSS. Deschideți sursa originală pentru articolul complet.",
                color = currentTheme.secondaryTextColor,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.5f).sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Bottom Actions in Reader Mode ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Articol FlashNews", "${article.title}\n\n$fullReadingText")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Text copiat în clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiază", fontSize = 11.sp, maxLines = 1)
            }

            OutlinedButton(
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Distribuie", fontSize = 11.sp, maxLines = 1)
            }

            Button(
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                onClick = onOpenSource,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sursă", fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

private fun cleanHtml(html: String): String {
    return html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&rsquo;", "'")
        .replace("&lsquo;", "'")
        .replace("&rdquo;", "\"")
        .replace("&ldquo;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
