package com.example.ui.screens

import android.widget.Toast
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
import com.example.ui.theme.cinemaBackground
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.live.WorldChannelsDownloader
import com.example.live.StreamHealthService
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cast.CastButton
import com.example.cast.CastManager
import com.example.data.model.LiveStreamChannel
import com.example.live.LiveStreamRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamScreen(
    onPlayChannel: (LiveStreamChannel) -> Unit,
    modifier: Modifier = Modifier,
    castManager: CastManager? = null
) {
    val context = LocalContext.current
    val channels by LiveStreamRepository.channels.collectAsState()
    val favoriteIds by LiveStreamRepository.favoriteIds.collectAsState()
    val importScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        LiveStreamRepository.refreshCatalog()
    }

    val isScanning by LiveStreamRepository.isScanning.collectAsState()
    val scanProgress by LiveStreamRepository.scanProgress.collectAsState()
    val scannedCount by LiveStreamRepository.scannedChannelsCount.collectAsState()
    val totalToScan by LiveStreamRepository.totalToScan.collectAsState()
    val deletedBrokenCount by LiveStreamRepository.deletedBrokenChannelsCount.collectAsState()
    val lastScanReport by LiveStreamRepository.lastScanReport.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: By Country, 1: By Category, 2: Favorites
    var selectedCountryCode by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showM3uDialog by remember { mutableStateOf(false) }
    var showCountryImportDialog by remember { mutableStateOf(false) }
    var isImportingCountry by remember { mutableStateOf(false) }
    var importingCountryName by remember { mutableStateOf("") }

    var customTitle by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("بث مخصص") }
    var m3uText by remember { mutableStateOf("") }
    var isImportingM3u by remember { mutableStateOf(false) }
    var channelToDelete by remember { mutableStateOf<LiveStreamChannel?>(null) }

    val m3uFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importScope.launch {
                isImportingM3u = true
                try {
                    val loadedText = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            ?: throw IllegalStateException("تعذر قراءة ملف القائمة المحدد.")
                    }
                    m3uText = loadedText
                    Toast.makeText(context, "تم فتح ملف القائمة. اضغط استيراد لإضافة جميع القنوات.", Toast.LENGTH_LONG).show()
                } catch (error: Exception) {
                    Toast.makeText(context, error.localizedMessage ?: "تعذر فتح ملف M3U.", Toast.LENGTH_LONG).show()
                } finally {
                    isImportingM3u = false
                }
            }
        }
    }

    val countries = com.example.live.WorldChannelsDownloader.supportedCountries

    val baseCategories = listOf(
        "الكل",
        "المفضلة ❤️",
        "⚽ رياضة وكورة وبث مباشر",
        "🎬 أفلام وسينما ومسلسلات",
        "إسلاميات وقرآن",
        "أخبار وتقارير",
        "قنوات عربية وفضائيات",
        "قنوات فرنسية (France TV 🇫🇷)",
        "قنوات تركية (Turkey TV 🇹🇷)",
        "قنوات أوروبية (Europe TV 🇪🇺)",
        "قنوات آسيوية (Asia TV)",
        "قنوات أمريكية ولاتينية 🇺🇸",
        "قنوات أفريقية (Africa TV)",
        "أطفال وعائلة",
        "وثائقيات ومعرفة",
        "بث مخصص"
    )
    val categories = remember(channels) {
        (baseCategories + channels.map { it.category }.filter { it.isNotBlank() }).distinct()
    }

    // Dynamic filtering by Tab (Country vs Category vs Favorites)
    val filtered = remember(channels, favoriteIds, activeTab, selectedCountryCode, selectedCategory, searchQuery) {
        channels.filter { channel ->
            val matchesTab = when (activeTab) {
                0 -> { // By Country
                    if (selectedCountryCode == null) true
                    else {
                        val preset = countries.firstOrNull { it.code == selectedCountryCode }
                        val countryName = preset?.nameAr ?: ""
                        val flag = preset?.flag ?: ""
                        channel.id.contains("imp_${selectedCountryCode}_") ||
                                channel.id.startsWith("${selectedCountryCode}_") ||
                                channel.category.contains(countryName) ||
                                channel.titleAr.contains(flag) ||
                                channel.description.contains(countryName)
                    }
                }
                1 -> { // By Category
                    when (selectedCategory) {
                        null, "الكل" -> true
                        "المفضلة ❤️" -> favoriteIds.contains(channel.id) || channel.isFavorite
                        "⚽ رياضة وكورة وبث مباشر", "رياضة وبث مباشر" -> channel.category.contains("رياضة") || channel.category.contains("كورة") || channel.category.contains("Sports")
                        "🎬 أفلام وسينما ومسلسلات", "أفلام وسينما" -> channel.category.contains("أفلام") || channel.category.contains("سينما") || channel.category.contains("Movies")
                        "قنوات فرنسية (France TV 🇫🇷)" -> channel.category.contains("فرنسية") || channel.category.contains("France")
                        "قنوات تركية (Turkey TV 🇹🇷)" -> channel.category.contains("تركية") || channel.category.contains("Turkey")
                        "قنوات أوروبية (Europe TV 🇪🇺)" -> channel.category.contains("أوروبية") || channel.category.contains("Europe")
                        "قنوات أمريكية ولاتينية 🇺🇸" -> channel.category.contains("أمريكية") || channel.category.contains("لاتينية")
                        else -> channel.category == selectedCategory
                    }
                }
                2 -> { // Favorites
                    favoriteIds.contains(channel.id) || channel.isFavorite
                }
                else -> true
            }

            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim()
                channel.titleAr.contains(q, ignoreCase = true) ||
                        channel.title.contains(q, ignoreCase = true) ||
                        channel.category.contains(q, ignoreCase = true) ||
                        channel.description.contains(q, ignoreCase = true)
            }

            matchesTab && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Header (All action icons strictly fit without clipping on mobile widths)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(com.example.ui.theme.AmberGradient)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "التلفاز المباشر",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF4D6D)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${channels.size} قناة تعمل الآن",
                                color = Color(0xDDFFFFFF),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (castManager != null) {
                            CastButton(
                                castManager = castManager,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FFFFFF))
                            )
                        }

                        // Import World Country Channels Button
                        IconButton(
                            onClick = { showCountryImportDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .testTag("import_world_channels_btn")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = "استيراد دول", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Refresh Catalog Button
                        IconButton(
                            onClick = {
                                LiveStreamRepository.refreshCatalog()
                                Toast.makeText(context, "تم تحديث القنوات!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .testTag("refresh_catalog_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Import M3U button
                        IconButton(
                            onClick = { showM3uDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .testTag("import_m3u_button")
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "M3U", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Add single channel button
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .testTag("add_custom_stream_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            }
        }

        // Clean Centered Search Bar (never clips text)
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
                            text = "بحث فوري في القنوات (سبيستون، الجزيرة، ناسا، قرآن)...",
                            fontSize = 11.sp,
                            color = Color(0xFF8B96BD),
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
                            .testTag("live_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color(0xFFA9B4D6), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Primary Navigation Tabs (By Country vs By Category vs Favorites)
        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF1A1F4A),
                contentColor = Color(0xFF22E3FF),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFF22E3FF),
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حسب الدول 🌍", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حسب النوع 📺", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المفضلة ❤️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // Sub-filters depending on activeTab
        item {
            if (activeTab == 0) {
                // Country Filter Chips
                val countryChips = listOf(null to "🌐 كل دول العالم") + countries.map { it.code to "${it.flag} ${it.nameAr}" }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(countryChips) { (code, label) ->
                        val isSelected = selectedCountryCode == code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCountryCode = code
                                // If selected country has no channels yet, auto-prompt import
                                if (code != null) {
                                    val preset = countries.firstOrNull { it.code == code }
                                    if (preset != null) {
                                        val hasChannels = channels.any { it.id.contains("imp_${code}_") || it.category.contains(preset.nameAr) || it.titleAr.contains(preset.flag) }
                                        if (!hasChannels) {
                                            isImportingCountry = true
                                            importingCountryName = preset.nameAr
                                            LiveStreamRepository.importCountryChannels(preset) { count ->
                                                isImportingCountry = false
                                                Toast.makeText(context, "تم بنجاح جلب $count قناة حقيقية من ${preset.nameAr}!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF22E3FF),
                                selectedLabelColor = Color(0xFF0E1230),
                                containerColor = Color(0xFF1A1F4A),
                                labelColor = Color(0xFFC3CCE8)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            } else if (activeTab == 1) {
                // Category Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = (selectedCategory == cat) || (selectedCategory == null && cat == "الكل")
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (cat == "الكل") null else cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF22E3FF),
                                selectedLabelColor = Color(0xFF0E1230),
                                containerColor = Color(0xFF1A1F4A),
                                labelColor = Color(0xFFC3CCE8)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Slim Link Scanner Status Card
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isScanning) Color(0xFF272D66) else Color(0xFF161B42)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.Sync else Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = if (isScanning) Color(0xFF22E3FF) else Color(0xFF2EE6A6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isScanning) "فاحص الروابط: ($scannedCount / $totalToScan)" else "فاحص الروابط التلقائي (HLS / DASH)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isScanning) "جاري الفحص وحذف الروابط التالفة بالخلفية..." else (lastScanReport ?: "فحص دوري تلقائي لصلاحية الروابط في قاعدة البيانات"),
                                    color = if (isScanning) Color(0xFF22E3FF) else Color(0xFFA9B4D6),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                if (!isScanning) {
                                    LiveStreamRepository.startUserHealthScan()
                                    StreamHealthService.startScan(context)
                                    Toast.makeText(context, "بدأ فحص الروابط الفعلي...", Toast.LENGTH_SHORT).show()
                                } else {
                                    LiveStreamRepository.stopHealthScan()
                                    StreamHealthService.stopScan(context)
                                    Toast.makeText(context, "تم إيقاف الفحص", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) Color(0xFFFF6B8B) else Color(0xFF22E3FF),
                                contentColor = Color(0xFF0E1230)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isScanning) "إيقاف الفحص" else "فحص وتنظيف",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isScanning) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF22E3FF),
                            trackColor = Color(0xFF0E1230)
                        )
                    }
                }
            }
        }

        // Channels List
        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "لم يتم العثور على أي قنوات تطابق البحث",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "جرّب البحث باسم قارئ، فيلم، كرتون، أو اختر فئة «الكل»",
                            color = Color(0xFFA9B4D6),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { channel ->
                LiveChannelCard(
                    channel = channel,
                    isFavorite = favoriteIds.contains(channel.id) || channel.isFavorite,
                    onClick = { onPlayChannel(channel) },
                    onToggleFavorite = { LiveStreamRepository.toggleFavorite(channel.id) },
                    onDelete = { channelToDelete = channel }
                )
            }
        }
    }

    // Channel Deletion Confirmation Dialog (supports deleting ANY broken or unwanted channel permanently)
    channelToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { channelToDelete = null },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Text(text = "حذف القناة", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "هل تريد حذف قناة «${target.titleAr}» من القائمة؟ لن تظهر مجدداً عند تحديث القنوات.",
                    color = Color(0xFFC3CCE8),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        LiveStreamRepository.deleteChannel(target.id)
                        channelToDelete = null
                        Toast.makeText(context, "تم حذف القناة نهائياً من القائمة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White)
                ) {
                    Text("حذف الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToDelete = null }) {
                    Text("إلغاء", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // Add Custom Stream Dialog
    if (showAddDialog) {
        val addCategories = listOf(
            "بث مخصص",
            "⚽ رياضة وكورة وبث مباشر",
            "أفلام وسينما",
            "أخبار وتقارير",
            "قنوات عربية وفضائيات",
            "إسلاميات وقرآن",
            "أطفال وعائلة"
        )
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Text(text = "إضافة قناة بث مباشرة", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "أدخل اسم القناة ورابط البث المباشر (يدعم HLS / M3U8 / MP4 / RTSP):",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("اسم القناة", color = Color(0xFFA9B4D6)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22E3FF),
                            unfocusedBorderColor = Color(0xFF272D66)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("رابط البث (URL)", color = Color(0xFFA9B4D6)) },
                        placeholder = { Text("https://example.com/live.m3u8", color = Color(0xFF8B96BD)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22E3FF),
                            unfocusedBorderColor = Color(0xFF272D66)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("التصنيف:", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(addCategories) { cat ->
                            FilterChip(
                                selected = customCategory == cat,
                                onClick = { customCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF22E3FF),
                                    selectedLabelColor = Color(0xFF0E1230),
                                    containerColor = Color(0xFF272D66),
                                    labelColor = Color(0xFFC3CCE8)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedTitle = customTitle.trim()
                        val trimmedUrl = customUrl.trim()
                        val isValidScheme = trimmedUrl.startsWith("https://", ignoreCase = true) ||
                            trimmedUrl.startsWith("http://", ignoreCase = true) ||
                            trimmedUrl.startsWith("rtsp://", ignoreCase = true)
                        if (trimmedTitle.isNotBlank() && isValidScheme) {
                            LiveStreamRepository.addCustomStream(trimmedTitle, trimmedUrl, customCategory)
                            showAddDialog = false
                            customTitle = ""
                            customUrl = ""
                            Toast.makeText(context, "تمت إضافة القناة في ($customCategory) بنجاح", Toast.LENGTH_SHORT).show()
                        } else if (trimmedTitle.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال اسم القناة", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى إدخال رابط بث صحيح يبدأ بـ https:// أو http://", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
                ) {
                    Text("حفظ وتشغيل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // Import M3U Playlist Dialog
    if (showM3uDialog) {
        AlertDialog(
            onDismissRequest = { if (!isImportingM3u) showM3uDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Text(text = "استيراد قائمة M3U (IPTV)", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "الصق رابط ملف M3U من الإنترنت، أو محتوى القائمة، أو اختر ملفاً من هاتفك. سيقرأ CinemaX كل القنوات ويضيفها دفعة واحدة:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = m3uText,
                        onValueChange = { m3uText = it },
                        placeholder = {
                            Text(
                                "https://iptv-org.github.io/iptv/index.category.m3u\nأو الصق محتوى يبدأ بـ #EXTM3U",
                                color = Color(0xFF8B96BD),
                                fontSize = 11.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22E3FF),
                            unfocusedBorderColor = Color(0xFF272D66)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { m3uFilePicker.launch(arrayOf("*/*")) },
                        enabled = !isImportingM3u,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("اختيار ملف M3U من الهاتف")
                    }
                    if (isImportingM3u) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF22E3FF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري قراءة أو استيراد القائمة...", color = Color(0xFF22E3FF), fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (m3uText.isBlank()) {
                            Toast.makeText(context, "أدخل رابط ملف M3U أو محتوى القائمة أولاً.", Toast.LENGTH_SHORT).show()
                        } else {
                            isImportingM3u = true
                            importScope.launch {
                                try {
                                    val report = LiveStreamRepository.importM3uPlaylist(m3uText)
                                    showM3uDialog = false
                                    m3uText = ""
                                    Toast.makeText(
                                        context,
                                        "تمت قراءة ${report.parsedChannels} قناة وإضافة ${report.importedChannels} جديدة. المكرر الموجود: ${report.alreadyPresent}.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (error: Exception) {
                                    Toast.makeText(
                                        context,
                                        error.localizedMessage ?: "تعذر استيراد قائمة M3U.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isImportingM3u = false
                                }
                            }
                        }
                    },
                    enabled = !isImportingM3u,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
                ) {
                    if (isImportingM3u) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0E1230)
                        )
                    } else {
                        Text("استيراد كل القنوات", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showM3uDialog = false }) {
                    Text("إلغاء", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // World Countries Live TV Channels Downloader Dialog
    if (showCountryImportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isImportingCountry) showCountryImportDialog = false
            },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "استيراد قنوات العالم المباشرة 🌍", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "اختر أي دولة لتنزيل مئات القنوات الحقيقية المباشرة المفتوحة فورياً وإضافتها لتطبيقك:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isImportingCountry) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF22E3FF))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "جاري تنزيل واستيراد قنوات $importingCountryName الحية...",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(WorldChannelsDownloader.supportedCountries) { country ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isImportingCountry = true
                                            importingCountryName = country.nameAr
                                            LiveStreamRepository.importCountryChannels(country) { count ->
                                                isImportingCountry = false
                                                showCountryImportDialog = false
                                                Toast.makeText(
                                                    context,
                                                    "تم بنجاح استيراد $count قناة حية لدولة ${country.nameAr} ${country.flag}!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = country.flag, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = country.nameAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(text = country.categoryName, color = Color(0xFF22E3FF), fontSize = 10.sp)
                                            }
                                        }
                                        Icon(Icons.Default.Add, contentDescription = "استيراد", tint = Color(0xFF22E3FF), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCountryImportDialog = false },
                    enabled = !isImportingCountry
                ) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }
}

@Composable
fun LiveChannelCard(
    channel: LiveStreamChannel,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }
            .testTag("channel_${channel.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Category Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.example.ui.theme.BrandGradient),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (channel.iconName) {
                    "mosque" -> Icons.Default.AutoAwesome
                    "feed" -> Icons.Default.Feed
                    "trending_up" -> Icons.Default.TrendingUp
                    "tv" -> Icons.Default.Tv
                    "child_care" -> Icons.Default.ChildCare
                    "movie" -> Icons.Default.Movie
                    "radio" -> Icons.Default.Radio
                    "forest" -> Icons.Default.Forest
                    else -> Icons.Default.LiveTv
                }
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.titleAr,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.category,
                        color = Color(0xFF22E3FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Live status pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FF1744))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF1744))
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "مباشر",
                                color = Color(0xFFFF1744),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (channel.backupUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${channel.backupUrls.size + 1} مصادر",
                            color = Color(0xFF2EE6A6),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Favorite heart button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "المفضلة",
                    tint = if (isFavorite) Color(0xFFFF1744) else Color(0xFF8B96BD),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف القناة", tint = Color(0xFFFF6B8B), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
