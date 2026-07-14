package com.example.baseredy.flashnews.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    categories: List<String>,
    onComplete: (Set<String>, Set<String>) -> Unit
) {
    var selectedLanguages by remember { mutableStateOf(setOf("ro", "us")) }
    var selectedInterests by remember { mutableStateOf(setOf("General")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Personalizează-ți experiența",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "În ce limbi vrei știrile?",
            color = Color.LightGray,
            fontSize = 16.sp
        )
        
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LanguageChip(
                label = "Română",
                selected = selectedLanguages.contains("ro"),
                onClick = {
                    selectedLanguages = if (selectedLanguages.contains("ro")) {
                        if (selectedLanguages.size > 1) selectedLanguages - "ro" else selectedLanguages
                    } else selectedLanguages + "ro"
                }
            )
            LanguageChip(
                label = "English",
                selected = selectedLanguages.contains("us"),
                onClick = {
                    selectedLanguages = if (selectedLanguages.contains("us")) {
                        if (selectedLanguages.size > 1) selectedLanguages - "us" else selectedLanguages
                    } else selectedLanguages + "us"
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Ce domenii te interesează?",
            color = Color.LightGray,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedInterests.contains(category),
                    onClick = {
                        selectedInterests = if (selectedInterests.contains(category)) {
                            if (selectedInterests.size > 1) selectedInterests - category else selectedInterests
                        } else selectedInterests + category
                    },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        containerColor = Color.DarkGray,
                        labelColor = Color.White,
                        selectedLabelColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = null
                )
            }
        }
        
        Button(
            onClick = { onComplete(selectedLanguages, selectedInterests) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Să începem!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            containerColor = Color.DarkGray,
            labelColor = Color.White,
            selectedLabelColor = Color.Black
        ),
        shape = CircleShape,
        border = null
    )
}
