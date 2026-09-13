package com.example.baseredy.flashnews.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baseredy.flashnews.core.network.RssSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssCatalogSheet(
    viewModel: FeedViewModel,
    onDismiss: () -> Unit
) {
    val allSources = remember { viewModel.allCatalogSources }
    val followedFeeds by viewModel.followedFeeds.collectAsState()
    val followedUrls = remember(followedFeeds) { followedFeeds.filter { it.isFollowed }.map { it.url }.toSet() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Toate") }
    var customUrlInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    val categories = remember {
        listOf("Toate", "Urmărite", "Politică", "Tehnologie", "Business & Finanțe", "Sport", "Auto", "Presă Internațională", "General")
    }

    val filteredSources = remember(searchQuery, selectedCategoryFilter, followedUrls) {
        allSources.filter { source ->
            val matchesQuery = searchQuery.isBlank() || 
                source.name.contains(searchQuery, ignoreCase = true) || 
                source.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategoryFilter) {
                "Toate" -> true
                "Urmărite" -> followedUrls.contains(source.url)
                "Presă Internațională" -> source.region != "RO"
                else -> source.category.contains(selectedCategoryFilter, ignoreCase = true)
            }
            matchesQuery && matchesCategory
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Catalog Surse RSS & Personalizare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${followedUrls.size} surse urmarite in 'Sursele Mele'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Inchide")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cauta publicatie (ex: Digi24, ZF, Tech...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Sterge")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Custom URL Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddLink, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adauga link RSS propriu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("https://site-ul-tau.ro/rss", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = {
                            clipboardManager.getText()?.let {
                                customUrlInput = it.text
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Lipeste din clipboard")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        validationMessage?.let {
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = if (it.startsWith("OK")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    isValidating = true
                                    validationMessage = "Se valideaza fluxul..."
                                    viewModel.addCustomFeed(customUrlInput.trim(), customNameInput.takeIf { it.isNotBlank() }) { success, message ->
                                        isValidating = false
                                        if (success) {
                                            validationMessage = "OK: Adaugat ($message)"
                                            customUrlInput = ""
                                            customNameInput = ""
                                        } else {
                                            validationMessage = "Eroare: $message"
                                        }
                                    }
                                }
                            },
                            enabled = customUrlInput.isNotBlank() && !isValidating
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Valideaza & Adauga", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Surse disponibile (${filteredSources.size}):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Source list
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSources, key = { it.url }) { source ->
                    val isFollowed = followedUrls.contains(source.url)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = source.category,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (source.region == "RO") "Romania" else "Global",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalIconToggleButton(
                                checked = isFollowed,
                                onCheckedChange = { viewModel.toggleFollowSource(source, it) }
                            ) {
                                Icon(
                                    imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isFollowed) "Urmarit" else "Urmareste",
                                    tint = if (isFollowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
