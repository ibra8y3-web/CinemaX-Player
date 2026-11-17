package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.radio.RadioBrowserApiClient
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.data.model.RadioCategory
import com.example.data.model.RadioStation
import com.example.radio.RadioCatalog
import com.example.radio.RadioPlayerManager

import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mic
import com.example.prayer.PrayerTimesManager
import com.example.ui.screens.prayer.PrayerTimesScreen
import com.example.ui.screens.quran.QuranReaderScreen
import com.example.ui.theme.cinemaBackground
import com.example.ui.theme.BrandGradient

@OptIn(UnstableApi::class)
@Composable
fun RadioScreen(
    radioManager: RadioPlayerManager,
    prayerManager: PrayerTimesManager? = null,
    duaManager: com.example.duas.DuaManager? = null,
    requestedPrayerTab: Int? = null,
    onConsumePrayerTab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentStation by radioManager.currentStation.collectAsState()
    val isPlaying by radioManager.isPlaying.collectAsState()
    val isLoading by radioManager.isLoading.collectAsState()
    val playbackError by radioManager.playbackError.collectAsState()
    val favorites by radioManager.favorites.collectAsState()
    val sleepMinutes by radioManager.sleepTimerMinutes.collectAsState()
    val sleepSecondsRemaining by radioManager.sleepTimerRemainingSeconds.collectAsState()
    val dataConsumedBytes by radioManager.dataConsumedBytes.collectAsState()
    val activeBundleMode by radioManager.networkBundleMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<RadioCategory?>(null) }
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQuranReader by remember { mutableStateOf(false) }
    var showPrayerTimes by remember { mutableStateOf(false) }
    var prayerInitialTab by remember { mutableStateOf(0) }
    var showDuasScreen by remember { mutableStateOf(false) }

    LaunchedEffect(requestedPrayerTab) {
        requestedPrayerTab?.let { tabIdx ->
            prayerInitialTab = tabIdx
            showPrayerTimes = true
            onConsumePrayerTab()
        }
    }

    var onlineStations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var currentOffset by remember { mutableStateOf(0) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val allStations = RadioCatalog.stations

    // Search and dynamic country stations from online Radio-Browser API (45,000+ real stations)
    LaunchedEffect(searchQuery, selectedCategory, selectedCountry) {
        currentOffset = 0
        val q = searchQuery.trim()
        if (q.length >= 2) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.searchStations(q, limit = 150, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCountry != null) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByCountry(selectedCountry!!, limit = 200, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.QURAN) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByTag("quran", limit = 200, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.ARABIC_RADIO) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByTag("arabic", limit = 200, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.FRANCE_GLOBAL) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByCountry("France", limit = 200, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.TURKISH_RADIO) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByCountry("Turkey", limit = 200, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.UK_USA) {
            isSearchingOnline = true
            val resultsUk = RadioBrowserApiClient.getStationsByCountry("United Kingdom", limit = 100, offset = 0)
            val resultsUs = RadioBrowserApiClient.getStationsByCountry("United States", limit = 100, offset = 0)
            onlineStations = (resultsUk + resultsUs).distinctBy { it.streamUrl }
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.TARAB) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByTag("classical", limit = 150, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.MUSIC_RELAX) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getStationsByTag("chillout", limit = 150, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else if (selectedCategory == RadioCategory.WORLD_GLOBAL) {
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getTopVotedStations(limit = 200, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        } else {
            // Default: Load top global clicked stations so the screen is immediately populated with hundreds of real streams
            isSearchingOnline = true
            val results = RadioBrowserApiClient.getTopClickStations(limit = 250, offset = 0)
            onlineStations = results
            isSearchingOnline = false
        }
    }

    if (showDuasScreen) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val dm = duaManager ?: remember { com.example.duas.DuaManager(context) }
        com.example.ui.screens.duas.DuasScreen(
            duaManager = dm,
            onBack = { showDuasScreen = false },
            modifier = modifier
        )
        return
    }

    if (showQuranReader) {
        QuranReaderScreen(
            radioManager = radioManager,
            onBack = { showQuranReader = false },
            modifier = modifier
        )
        return
    }

    if (showPrayerTimes) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val pm = prayerManager ?: remember { PrayerTimesManager(context) }
        PrayerTimesScreen(
            prayerManager = pm,
            initialTab = prayerInitialTab,
            onBack = { showPrayerTimes = false },
            modifier = modifier
        )
        return
    }

    val combinedStations = remember(allStations, onlineStations) {
        (allStations + onlineStations).distinctBy { it.streamUrl }
    }

    val filteredStations = remember(searchQuery, selectedCategory, showOnlyFavorites, favorites, combinedStations) {
        combinedStations.filter { station ->
            val matchesCategory = selectedCategory == null || station.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    station.titleAr.contains(searchQuery.trim(), ignoreCase = true) ||
                    station.reciterOrDesc.contains(searchQuery.trim(), ignoreCase = true) ||
                    station.country.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFavorite = !showOnlyFavorites || favorites.contains(station.id)
            matchesCategory && matchesSearch && matchesFavorite
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Header (hero card)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandGradient)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("الراديو", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("أكثر من 45 ألف محطة من كل العالم", color = Color(0xDDFFFFFF), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = { showSleepTimerDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (sleepMinutes > 0) Color.White else Color(0x33FFFFFF))
                        .testTag("radio_sleep_timer_btn")
                ) {
                    Icon(
                        imageVector = if (sleepMinutes > 0) Icons.Default.Timer else Icons.Default.TimerOff,
                        contentDescription = "مؤقت النوم",
                        tint = if (sleepMinutes > 0) Color(0xFF0E1230) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Clean Centered Search Bar (never clips placeholder or typed query)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141943))
                    .border(
                        width = 1.dp,
                        color = if (searchQuery.isNotEmpty()) Color(0xFF22E3FF) else Color(0xFF272D66),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = Color(0xFF22E3FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "بحث في آلاف الإذاعات العالمية (فرنسا، مصر، قرآن)...",
                            color = Color(0xFF8B96BD),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(Color(0xFF22E3FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("radio_search_input")
                    )
                }
                if (isSearchingOnline) {
                    CircularProgressIndicator(color = Color(0xFF22E3FF), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFFA9B4D6), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Smart Network & Telecom Bundle Compatibility Selector (collapsed by default)
        item {
            var bundleExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161B42))
                    .clickable { bundleExpanded = !bundleExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡ باقة الإنترنت:",
                            color = Color(0xFF22E3FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${activeBundleMode.icon} ${activeBundleMode.shortLabel}",
                            color = Color(0xFFFFC857),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "يعمل بدون نت عام",
                        color = Color(0xFF5EF0BE),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (bundleExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(com.example.radio.NetworkBundleMode.values()) { mode ->
                        val isSelected = activeBundleMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF22E3FF) else Color(0xFF272D66))
                                .clickable {
                                    radioManager.setNetworkBundleMode(mode)
                                    android.widget.Toast.makeText(context, "تم تفعيل نمط: ${mode.nameAr}\n${mode.description}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = mode.icon, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.shortLabel,
                                    color = if (isSelected) Color(0xFF0E1230) else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                }
            }
        }

        // Live Countries Quick Filter Row
        item {
            val countries = listOf(
                null to "🌐 الكل",
                "France" to "🇫🇷 فرنسا",
                "Egypt" to "🇪🇬 مصر",
                "Saudi Arabia" to "🇸🇦 السعودية",
                "Turkey" to "🇹🇷 تركيا",
                "Morocco" to "🇲🇦 المغرب",
                "Algeria" to "🇩🇿 الجزائر",
                "United Kingdom" to "🇬🇧 بريطانيا",
                "United States" to "🇺🇸 أمريكا",
                "Germany" to "🇩🇪 ألمانيا",
                "Spain" to "🇪🇸 إسبانيا",
                "Italy" to "🇮🇹 إيطاليا"
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(countries) { (code, label) ->
                    val isSelected = selectedCountry == code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF22E3FF) else Color(0xFF1A1F4A))
                            .border(1.dp, if (isSelected) Color(0xFF22E3FF) else Color(0xFF272D66), RoundedCornerShape(10.dp))
                            .clickable {
                                selectedCountry = if (isSelected) null else code
                                if (selectedCountry != null) selectedCategory = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0E1230) else Color(0xFFE6EAFA),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAllSelected = selectedCategory == null && !showOnlyFavorites
                    FilterChip(
                        selected = isAllSelected,
                        onClick = {
                            selectedCategory = null
                            showOnlyFavorites = false
                        },
                        label = { Text("الكل (${combinedStations.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22E3FF),
                            selectedLabelColor = Color(0xFF0E1230),
                            containerColor = Color(0xFF1A1F4A),
                            labelColor = Color(0xFFC3CCE8)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(RadioCategory.values()) { cat ->
                    val isSelected = selectedCategory == cat && !showOnlyFavorites
                    val count = combinedStations.count { it.category == cat }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = cat
                            showOnlyFavorites = false
                        },
                        label = { Text("${cat.titleAr} ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(cat.badgeColor),
                            selectedLabelColor = Color(0xFF0E1230),
                            containerColor = Color(0xFF1A1F4A),
                            labelColor = Color(0xFFC3CCE8)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    val favCount = combinedStations.count { favorites.contains(it.id) }
                    FilterChip(
                        selected = showOnlyFavorites,
                        onClick = {
                            showOnlyFavorites = !showOnlyFavorites
                            if (showOnlyFavorites) selectedCategory = null
                        },
                        label = { Text("المفضلة ($favCount)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFC857),
                            selectedLabelColor = Color(0xFF0E1230),
                            containerColor = Color(0xFF1A1F4A),
                            labelColor = Color(0xFFC3CCE8)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Active Player Hero Card
        if (currentStation != null) {
            item {
                val station = currentStation!!
                val mbUsed = (dataConsumedBytes / (1024f * 1024f))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22E3FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_hero_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color(0xFF2EE6A6) else Color(0xFFFFC857))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "بث مباشر نشط" else "متوقف مؤقتاً",
                                    color = if (isPlaying) Color(0xFF2EE6A6) else Color(0xFFFFC857),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "نت مستهلك: %.1f MB".format(mbUsed),
                                color = Color(0xFF22E3FF),
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioSoundwaveVisualizer(isPlaying = isPlaying)

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.titleAr,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = station.reciterOrDesc,
                                    color = Color(0xFFA9B4D6),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val isFav = favorites.contains(station.id)
                            IconButton(
                                onClick = { radioManager.toggleFavorite(station.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "تفضيل",
                                    tint = if (isFav) Color(0xFFFFC857) else Color(0xFFA9B4D6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Playback Controls Row (compact)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { radioManager.playPrevious() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "السابق", tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp).padding(3.dp),
                                    color = Color(0xFF22E3FF),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Button(
                                    onClick = { radioManager.togglePlayPause() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22E3FF),
                                        contentColor = Color(0xFF0E1230)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { radioManager.playNext() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "التالي", tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { radioManager.stop() }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إيقاف", tint = Color(0xFFFF6B8B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Stations List
        items(filteredStations, key = { it.id }) { station ->
            val isCurrent = currentStation?.id == station.id
            val isFav = favorites.contains(station.id)

            RadioStationCard(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                isFavorite = isFav,
                onPlay = { radioManager.playStation(station) },
                onToggleFavorite = { radioManager.toggleFavorite(station.id) }
            )
        }

        // Load More Real Stations (Pagination from +45,000 real global stations)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "المعروض حالياً: ${combinedStations.size} محطة من أصل +45,000 إذاعة حقيقية عالمية",
                        color = Color(0xFFA9B4D6),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (!isLoadingMore) {
                                isLoadingMore = true
                                scope.launch {
                                    val nextOffset = currentOffset + 100
                                    val more: List<RadioStation> = when {
                                        searchQuery.isNotBlank() -> RadioBrowserApiClient.searchStations(searchQuery.trim(), limit = 100, offset = nextOffset)
                                        selectedCountry != null -> RadioBrowserApiClient.getStationsByCountry(selectedCountry!!, limit = 100, offset = nextOffset)
                                        selectedCategory == RadioCategory.QURAN -> RadioBrowserApiClient.getStationsByTag("quran", limit = 100, offset = nextOffset)
                                        selectedCategory == RadioCategory.ARABIC_RADIO -> RadioBrowserApiClient.getStationsByTag("arabic", limit = 100, offset = nextOffset)
                                        selectedCategory == RadioCategory.FRANCE_GLOBAL -> RadioBrowserApiClient.getStationsByCountry("France", limit = 100, offset = nextOffset)
                                        selectedCategory == RadioCategory.TURKISH_RADIO -> RadioBrowserApiClient.getStationsByCountry("Turkey", limit = 100, offset = nextOffset)
                                        selectedCategory == RadioCategory.UK_USA -> RadioBrowserApiClient.getStationsByCountry("United Kingdom", limit = 100, offset = nextOffset)
                                        else -> RadioBrowserApiClient.getTopClickStations(limit = 100, offset = nextOffset)
                                    }
                                    if (more.isNotEmpty()) {
                                        onlineStations = (onlineStations + more).distinctBy { it.streamUrl }
                                        currentOffset = nextOffset
                                    }
                                    isLoadingMore = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22E3FF),
                            contentColor = Color(0xFF0E1230)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF0E1230),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ جلب المزيد من الإذاعات...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استكشاف المزيد (+100 محطة إذاعية حقيقية)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF22E3FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "مؤقت النوم التلقائي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "اختر مدة الاستماع قبل إيقاف الراديو تلقائياً للنوم والراحة:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val timerOptions = listOf(15, 30, 45, 60, 90)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timerOptions.take(3).forEach { mins ->
                            Button(
                                onClick = {
                                    radioManager.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sleepMinutes == mins) Color(0xFF22E3FF) else Color(0xFF272D66),
                                    contentColor = if (sleepMinutes == mins) Color(0xFF0E1230) else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$mins د", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timerOptions.drop(3).forEach { mins ->
                            Button(
                                onClick = {
                                    radioManager.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sleepMinutes == mins) Color(0xFF22E3FF) else Color(0xFF272D66),
                                    contentColor = if (sleepMinutes == mins) Color(0xFF0E1230) else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$mins د", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (sleepMinutes > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                radioManager.setSleepTimer(0)
                                showSleepTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252), contentColor = Color(0xFFFF6B8B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إلغاء المؤقت", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }
}

@Composable
fun RadioSoundwaveVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vis")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h4"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF272D66)),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(16.dp)
            ) {
                Box(modifier = Modifier.width(2.5.dp).height((16 * h1).dp).clip(RoundedCornerShape(1.dp)).background(Color(0xFF22E3FF)))
                Box(modifier = Modifier.width(2.5.dp).height((16 * h2).dp).clip(RoundedCornerShape(1.dp)).background(Color(0xFF2EE6A6)))
                Box(modifier = Modifier.width(2.5.dp).height((16 * h3).dp).clip(RoundedCornerShape(1.dp)).background(Color(0xFFFFC857)))
                Box(modifier = Modifier.width(2.5.dp).height((16 * h4).dp).clip(RoundedCornerShape(1.dp)).background(Color(0xFF22E3FF)))
            }
        } else {
            Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun RadioStationCard(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF272D66) else Color(0xFF1A1F4A)
        ),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22E3FF)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onPlay() }
            .testTag("station_${station.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(station.category.badgeColor).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getStationIcon(station.iconName),
                    contentDescription = null,
                    tint = Color(station.category.badgeColor),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.titleAr,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Bitrate pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (station.bitrateKbps <= 32) Color(0x332EE6A6)
                                else Color(0x3322E3FF)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${station.bitrateKbps}k",
                            color = if (station.bitrateKbps <= 32) Color(0xFF2EE6A6) else Color(0xFF22E3FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val subText = if (station.country.isNotBlank() && station.country != "العالم") {
                    "${station.country} • ${station.reciterOrDesc}"
                } else {
                    station.reciterOrDesc
                }
                Text(
                    text = subText,
                    color = Color(0xFFA9B4D6),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "المفضلة",
                    tint = if (isFavorite) Color(0xFFFFC857) else Color(0xFF8B96BD),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Play / Indicator button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) com.example.ui.theme.BrandGradient else androidx.compose.ui.graphics.SolidColor(Color(0xFF272D66)))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun getStationIcon(iconName: String): ImageVector {
    return when (iconName) {
        "mosque", "kaaba" -> Icons.Default.Shield
        "person" -> Icons.Default.Person
        "wb_sunny" -> Icons.Default.WbSunny
        "health_and_safety" -> Icons.Default.HealthAndSafety
        "shield" -> Icons.Default.Shield
        "celebration" -> Icons.Default.Celebration
        "history_edu" -> Icons.Default.HistoryEdu
        "nights_stay" -> Icons.Default.NightsStay
        "menu_book", "school" -> Icons.Default.MenuBook
        "spa" -> Icons.Default.Spa
        "headphones" -> Icons.Default.Headphones
        "self_improvement" -> Icons.Default.SelfImprovement
        "piano" -> Icons.Default.Piano
        "music_note" -> Icons.Default.MusicNote
        else -> Icons.Default.Radio
    }
}
