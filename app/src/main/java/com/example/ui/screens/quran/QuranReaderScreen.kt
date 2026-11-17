package com.example.ui.screens.quran

import com.example.ui.theme.cinemaBackground

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.data.model.QuranSurah
import com.example.quran.QuranRepository
import com.example.radio.RadioPlayerManager
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun QuranReaderScreen(
    radioManager: RadioPlayerManager? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmark by QuranRepository.bookmark.collectAsState()
    val currentRadioStation by radioManager?.currentStation?.collectAsState() ?: remember { mutableStateOf(null) }
    val isRadioPlaying by radioManager?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    var selectedSurah by remember { mutableStateOf<QuranSurah?>(null) }
    var verses by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingVerses by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableFloatStateOf(24f) }
    var showFontSizeSlider by remember { mutableStateOf(false) }

    val allSurahs = QuranRepository.surahList
    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) allSurahs
        else allSurahs.filter {
            it.nameAr.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.englishName.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.number.toString() == searchQuery.trim()
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedSurah) {
        selectedSurah?.let { s ->
            isLoadingVerses = true
            verses = QuranRepository.getSurahVerses(context, s.number)
            isLoadingVerses = false
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
    ) {
        if (selectedSurah != null) {
            val surah = selectedSurah!!
            // Reader Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedSurah = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع للفهرس", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "سُورَةُ ${surah.nameAr}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${surah.revelationType} • ${surah.numberOfAyahs} آية • الجزء ${surah.juzNumber}",
                            color = Color(0xFF22E3FF),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Font Size Adjuster
                    IconButton(
                        onClick = { showFontSizeSlider = !showFontSizeSlider },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (showFontSizeSlider) Color(0x3322E3FF) else Color(0xFF1A1F4A))
                    ) {
                        Icon(Icons.Default.FormatSize, contentDescription = "حجم الخط", tint = Color.White)
                    }

                    // Bookmark
                    IconButton(
                        onClick = {
                            QuranRepository.saveBookmark(context, surah.number, surah.nameAr, 1)
                        }
                    ) {
                        val isBookmarked = bookmark?.surahNumber == surah.number
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "حفظ العلامة",
                            tint = if (isBookmarked) Color(0xFFFFC857) else Color(0xFFA9B4D6)
                        )
                    }
                }
            }

            // Font Size Slider Popup
            AnimatedVisibility(visible = showFontSizeSlider) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("أ", color = Color(0xFFA9B4D6), fontSize = 14.sp)
                        Slider(
                            value = fontSizeSp,
                            onValueChange = { fontSizeSp = it },
                            valueRange = 16f..34f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF22E3FF),
                                activeTrackColor = Color(0xFF22E3FF),
                                inactiveTrackColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("أ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sync with Quran Radio Live Banner
            if (currentRadioStation != null && isRadioPlaying) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B42)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3322E3FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مستمع الآن إلى: ${currentRadioStation!!.titleAr} (اقرأ بالتزامن مع التلاوة)",
                            color = Color(0xFFD5DCF2),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isLoadingVerses) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF22E3FF))
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Surah Frame Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0FA67E), Color(0xFF1D6F8F), Color(0xFF3B2A8C))
                                    )
                                )
                                .border(1.5.dp, Color(0xFFFFD98A).copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                                .padding(vertical = 24.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "﴿ سُورَةُ ${surah.nameAr} ﴾",
                                    color = Color(0xFFFFD98A),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "آياتها ${surah.numberOfAyahs} • نزلت بـ ${surah.revelationType}",
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 13.sp
                                )

                                if (surah.number != 9 && surah.number != 1) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Verses with Ayah Badges
                    itemsIndexed(verses) { index, verseText ->
                        val ayahNumber = index + 1
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF161B42))
                                .border(0.5.dp, Color(0x33FFD98A), RoundedCornerShape(20.dp))
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x22FFD98A))
                                            .border(1.dp, Color(0xFFFFD98A).copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$ayahNumber",
                                            color = Color(0xFFFFD98A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "الآية $ayahNumber",
                                        color = Color(0xFFA9B4D6),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = verseText,
                                    color = Color(0xFFFFFFFF),
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 2.0f).sp,
                                    textAlign = TextAlign.Right,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Index & Surah List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3322E3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "المصحف الشريف الإلكتروني",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "١١٤ سورة كاملة مع إمكانية القراءة أثناء الاستماع للإذاعة",
                            color = Color(0xFF22E3FF),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Bookmark Quick Jump Card
            if (bookmark != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC857)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            val s = allSurahs.find { it.number == bookmark!!.surahNumber }
                            if (s != null) selectedSurah = s
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "متابعة القراءة من العلامة المحفوظة",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سورة ${bookmark!!.surahNameAr}",
                                color = Color(0xFFFFC857),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "بحث في أسماء السور أو رقم السورة (الفاتحة، الكهف، يس، ٦٧)...",
                        color = Color(0xFF8B96BD),
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF22E3FF),
                    unfocusedBorderColor = Color(0xFF272D66),
                    focusedContainerColor = Color(0xFF141943),
                    unfocusedContainerColor = Color(0xFF141943)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("quran_search_input")
            )

            // Surahs List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSurahs, key = { it.number }) { surah ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSurah = surah }
                            .testTag("surah_${surah.number}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Surah Number Badge
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFF0FA67E), Color(0xFF1D6F8F))))
                                    .border(1.5.dp, Color(0xFFFFD98A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${surah.number}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "سُورَةُ ${surah.nameAr}",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${surah.englishName} • ${surah.revelationType} • ${surah.numberOfAyahs} آية",
                                    color = Color(0xFFA9B4D6),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FFD98A))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الجزء ${surah.juzNumber}",
                                    color = Color(0xFFFFD98A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
