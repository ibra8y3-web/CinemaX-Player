package com.example.player.sheets

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.SubtitleTrackOption
import com.example.subtitles.OnlineSubtitleItem
import com.example.subtitles.OnlineSubtitleService
import com.example.subtitles.SubtitleStyleConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitlesLiveSheet(
    videoTitle: String,
    availableTracks: List<SubtitleTrackOption>,
    selectedTrack: SubtitleTrackOption?,
    subtitleStyle: SubtitleStyleConfig,
    subtitleService: OnlineSubtitleService,
    onSelectTrack: (SubtitleTrackOption?) -> Unit,
    onAttachSubtitle: (Uri, String, String) -> Unit,
    onUpdateStyle: (SubtitleStyleConfig) -> Unit,
    onToggleLiveTranslation: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("المسارات المدمجة", "بحث بالإنترنت", "ترجمة فورية وتزامن")

    // Online search state
    var searchQuery by remember {
        mutableStateOf(
            videoTitle
                .replace(Regex("\\.(mp4|mkv|avi|mov|flv|webm|ts|m3u8)$", RegexOption.IGNORE_CASE), "")
                .trim()
        )
    }
    var targetLangCode by remember { mutableStateOf("ar") }
    var searchResults by remember { mutableStateOf<List<OnlineSubtitleItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var downloadingId by remember { mutableStateOf<String?>(null) }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        isSearching = true
        scope.launch {
            try {
                val results = subtitleService.searchOnlineSubtitles(searchQuery, targetLangCode)
                searchResults = results
            } catch (e: Exception) {
                Toast.makeText(context, "حدث خطأ أثناء البحث: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && searchResults.isEmpty()) {
            performSearch()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F141C),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Drag indicator
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363D80))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22E3FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = Color(0xFF22E3FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الترجمة والبحث عبر الإنترنت",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تحميل مباشر، مزامنة دقيقة، وترجمة حية بالذكاء الاصطناعي",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab bar
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF161B42),
                contentColor = Color(0xFF22E3FF),
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) Color(0xFF22E3FF) else Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTabIndex) {
                // Tab 0: Embedded tracks
                0 -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        // Option: Turn Off Subtitles
                        item {
                            val isOff = selectedTrack == null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isOff) Color(0xFF22E3FF).copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { onSelectTrack(null) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "إيقاف الترجمة (Off)",
                                    color = if (isOff) Color(0xFF22E3FF) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isOff) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isOff) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (availableTracks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("لا توجد مسارات ترجمة مدمجة داخل هذا الملف", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("يمكنك استخدام تبويب «بحث بالإنترنت» لتحميل ترجمة فوراً", color = Color(0xFF22E3FF), fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            items(availableTracks) { track ->
                                val isSelected = selectedTrack?.id == track.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF22E3FF).copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { onSelectTrack(track) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = track.label,
                                            color = if (isSelected) Color(0xFF22E3FF) else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = "اللغة: ${track.language} ${if (track.isExternal) "• (ملف خارجي محمل)" else "• (مدمج بالملف)"}",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 1: Online Search & Direct Download
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth().height(290.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("اسم الفيلم أو الفيديو...", fontSize = 12.sp, color = Color(0xFF8B96BD)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF22E3FF),
                                    unfocusedBorderColor = Color(0xFF363D80),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF161B42),
                                    unfocusedContainerColor = Color(0xFF161B42)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ElevatedButton(
                                onClick = { performSearch() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFF22E3FF),
                                    contentColor = Color.Black
                                )
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Language pills
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ar" to "العربية", "en" to "English", "fr" to "Français", "es" to "Español").forEach { (code, name) ->
                                val isSel = targetLangCode == code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSel) Color(0xFF22E3FF) else Color(0xFF272D66))
                                        .clickable {
                                            targetLangCode = code
                                            performSearch()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = name,
                                        color = if (isSel) Color.Black else Color(0xFFD1D5DB),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (isSearching) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF22E3FF))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                items(searchResults) { item ->
                                    val isDownloading = downloadingId == item.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF161B42))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = item.releaseName,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFD600), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(text = "${item.rating}", color = Color(0xFFFFD600), fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = "• ${item.language}", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = "• ${item.downloadsCount} تنزيل", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                            }
                                        }

                                        ElevatedButton(
                                            onClick = {
                                                downloadingId = item.id
                                                scope.launch {
                                                    try {
                                                        val savedUri = subtitleService.downloadAndSaveSubtitle(item)
                                                        onAttachSubtitle(savedUri, item.languageCode, item.releaseName)
                                                        Toast.makeText(context, "تم تحميل وتفعيل الترجمة بنجاح!", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "فشل التحميل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        downloadingId = null
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.elevatedButtonColors(
                                                containerColor = Color(0xFF22E3FF).copy(alpha = 0.2f),
                                                contentColor = Color(0xFF22E3FF)
                                            ),
                                            enabled = !isDownloading
                                        ) {
                                            if (isDownloading) {
                                                CircularProgressIndicator(color = Color(0xFF22E3FF), modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                                            } else {
                                                Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تطبيق", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 2: Live AI Translation & Sync Controls
                2 -> {
                    Column(modifier = Modifier.fillMaxWidth().height(290.dp)) {
                        // Live Translation Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161B42))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22E3FF).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Translate, null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("ترجمة فورية بالذكاء الاصطناعي أونلاين", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("يترجم السطور الأجنبية مباشرة للغة العربية بالإنترنت", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = subtitleStyle.isLiveTranslationEnabled,
                                onCheckedChange = { onToggleLiveTranslation() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF22E3FF)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sync Offset
                        Text("ضبط تزامن الترجمة مع الصوت:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OffsetPill("-500ms") { onUpdateStyle(subtitleStyle.copy(syncOffsetMs = subtitleStyle.syncOffsetMs - 500)) }
                            OffsetPill("-100ms") { onUpdateStyle(subtitleStyle.copy(syncOffsetMs = subtitleStyle.syncOffsetMs - 100)) }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF22E3FF).copy(alpha = 0.2f))
                                    .clickable { onUpdateStyle(subtitleStyle.copy(syncOffsetMs = 0L)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (subtitleStyle.syncOffsetMs == 0L) "0.0s (مطابق)" else "${subtitleStyle.syncOffsetMs} ms",
                                    color = Color(0xFF22E3FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OffsetPill("+100ms") { onUpdateStyle(subtitleStyle.copy(syncOffsetMs = subtitleStyle.syncOffsetMs + 100)) }
                            OffsetPill("+500ms") { onUpdateStyle(subtitleStyle.copy(syncOffsetMs = subtitleStyle.syncOffsetMs + 500)) }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Text Size
                        Text("حجم الخط:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(14 to "صغير", 18 to "متوسط", 22 to "كبير", 26 to "ضخم").forEach { (sz, label) ->
                                val isSel = subtitleStyle.fontSizeSp == sz
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color(0xFF22E3FF) else Color(0xFF272D66))
                                        .clickable { onUpdateStyle(subtitleStyle.copy(fontSizeSp = sz)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OffsetPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF272D66))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
