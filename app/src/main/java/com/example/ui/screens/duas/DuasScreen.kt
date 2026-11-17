package com.example.ui.screens.duas

import com.example.ui.theme.cinemaBackground

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duas.DuaManager
import com.example.duas.model.DuaCategory
import com.example.duas.model.DuaItem

@Composable
fun DuasScreen(
    duaManager: DuaManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duas by duaManager.duas.collectAsState()
    val counts by duaManager.counts.collectAsState()
    val favorites by duaManager.favorites.collectAsState()
    val isSpeaking by duaManager.isSpeaking.collectAsState()
    val currentSpeakingId by duaManager.currentSpeakingId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DuaCategory.ALL) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var isSyncingOnline by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredDuas = remember(duas, selectedCategory, searchQuery, showOnlyFavorites, favorites) {
        duas.filter { item ->
            val matchesCategory = (selectedCategory == DuaCategory.ALL) || (item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.arabicText.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.sourceTakhrij.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.virtue.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFav = !showOnlyFavorites || favorites.contains(item.id)
            matchesCategory && matchesSearch && matchesFav
        }
    }

    val totalCompletedInSelection = remember(filteredDuas, counts) {
        filteredDuas.count { item ->
            val c = counts[item.id] ?: 0
            c >= item.targetCount
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("duas_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "مكتبة الأدعية وحصن المسلم",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${duas.size} دعاء وذكر حقيقي • القرآن والسنة النبوية",
                        color = Color(0xFF22E3FF),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online Quran.com API Sync Button
                IconButton(
                    onClick = {
                        isSyncingOnline = true
                        duaManager.loadOnlineQuranDuas { count ->
                            isSyncingOnline = false
                            Toast.makeText(context, "تم بنجاح جلب $count دعاء موثق من خوادم القرآن الكريم Quran.com API!", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSyncingOnline,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1F4A))
                        .testTag("sync_quran_duas_btn")
                ) {
                    if (isSyncingOnline) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF2EE6A6))
                    } else {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = "تحديث من Quran.com API",
                            tint = Color(0xFF2EE6A6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { showOnlyFavorites = !showOnlyFavorites },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (showOnlyFavorites) Color(0xFFFF6B8B).copy(alpha = 0.2f) else Color(0xFF1A1F4A))
                ) {
                    Icon(
                        if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (showOnlyFavorites) Color(0xFFFF6B8B) else Color(0xFFA9B4D6),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { duaManager.resetAllCounts() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1F4A))
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "إعادة ضبط العدادات",
                        tint = Color(0xFF22E3FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في الأدعية، الأذكار، المصادر، والفضائل...", color = Color(0xFF8B96BD), fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF22E3FF)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF22E3FF),
                unfocusedBorderColor = Color(0xFF272D66),
                focusedContainerColor = Color(0xFF1A1F4A),
                unfocusedContainerColor = Color(0xFF1A1F4A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(50.dp)
                .testTag("dua_search_input")
        )

        // Category Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DuaCategory.values()) { cat ->
                val isSelected = selectedCategory == cat
                val chipBg = if (isSelected) cat.accentColor else Color(0xFF1A1F4A)
                val textColor = if (isSelected) Color(0xFF0E1230) else Color(0xFFE6EAFA)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(chipBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) cat.accentColor else Color(0xFF272D66),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat.titleAr,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Daily Progress Card
        if (filteredDuas.isNotEmpty()) {
            val progress = if (filteredDuas.isNotEmpty()) totalCompletedInSelection.toFloat() / filteredDuas.size else 0f
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55FFD98A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الإنجاز اليومي في هذا القسم",
                                color = Color(0xFFA9B4D6),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$totalCompletedInSelection من ${filteredDuas.size} مكتمل",
                                color = Color(0xFF22E3FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color(0xFF2EE6A6),
                            trackColor = Color(0xFF272D66),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // Duas List
        if (filteredDuas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد أدعية تطابق بحثك الحالي",
                    color = Color(0xFF8B96BD),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredDuas, key = { it.id }) { item ->
                    val currentCount = counts[item.id] ?: 0
                    val isCompleted = currentCount >= item.targetCount
                    val isFav = favorites.contains(item.id)
                    val isCurrentlySpeaking = isSpeaking && currentSpeakingId == item.id

                    DuaItemCard(
                        item = item,
                        currentCount = currentCount,
                        isCompleted = isCompleted,
                        isFavorite = isFav,
                        isSpeaking = isCurrentlySpeaking,
                        onIncrement = { duaManager.incrementCount(item.id, item.targetCount) },
                        onReset = { duaManager.resetCount(item.id) },
                        onToggleFavorite = { duaManager.toggleFavorite(item.id) },
                        onPlayAudio = { duaManager.playAudio(item) },
                        onCopy = { duaManager.copyToClipboard(item) },
                        onShare = { duaManager.shareDua(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaItemCard(
    item: DuaItem,
    currentCount: Int,
    isCompleted: Boolean,
    isFavorite: Boolean,
    isSpeaking: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayAudio: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (isCompleted) Color(0xFF2EE6A6).copy(alpha = 0.6f) else Color(0xFF272D66),
        animationSpec = tween(durationMillis = 300),
        label = "border"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header: Category badge & Actions (TTS, Favorite, Copy, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(item.category.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.category.titleAr,
                            color = item.category.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // TTS Audio Button
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "استماع بالصوت",
                            tint = if (isSpeaking) Color(0xFFFF6B8B) else Color(0xFF22E3FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy Button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = Color(0xFFA9B4D6),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = Color(0xFFA9B4D6),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) Color(0xFFFF6B8B) else Color(0xFFA9B4D6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic Text Box with diacritics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0E2A4A), Color(0xFF1B2358))))
                    .padding(18.dp)
            ) {
                Text(
                    text = item.arabicText,
                    color = Color(0xFFFFF4D6),
                    fontSize = 21.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hadith Source & Takhrij
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📖 المصدر: ${item.sourceTakhrij}",
                    color = Color(0xFF5B8CFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Virtue & Reward Note
            if (item.virtue.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✨ الفضل: ${item.virtue}",
                    color = Color(0xFFFFD98A),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Counter Tap Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Counter Button
                Button(
                    onClick = onIncrement,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Color(0xFF2EE6A6) else Color(0xFF8B5CF6),
                        contentColor = if (isCompleted) Color(0xFF0E1230) else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("dua_count_btn_${item.id}")
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اكتمل الذكر ($currentCount / ${item.targetCount}) ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "تكرار الذكر ($currentCount / ${item.targetCount})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (currentCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF272D66))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "إعادة التكرار",
                            tint = Color(0xFFA9B4D6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
