package com.example.ui.screens.music

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.media.MediaScanner
import com.example.media.model.LocalAudio
import com.example.music.MusicPlayerManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MusicPlayerScreen(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaScanner = remember { MediaScanner(context) }
    val audioFiles by viewModel.audioFiles.collectAsState()
    val musicManager = viewModel.musicPlayerManager

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentPlayingAudio by musicManager.currentTrack.collectAsState()
    val isPlaying by musicManager.isPlaying.collectAsState()
    val currentPositionMs by musicManager.currentPositionMs.collectAsState()
    val durationMs by musicManager.durationMs.collectAsState()
    val repeatMode by musicManager.repeatMode.collectAsState()
    val isShuffleEnabled by musicManager.isShuffleEnabled.collectAsState()
    val playbackError by musicManager.playbackError.collectAsState()
    var audioToCut by remember { mutableStateOf<LocalAudio?>(null) }
    var audioToDelete by remember { mutableStateOf<LocalAudio?>(null) }
    var pendingDeleteConsentAudio by remember { mutableStateOf<LocalAudio?>(null) }
    var pendingBulkConsentAudios by remember { mutableStateOf<List<LocalAudio>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAudioIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    fun toggleAudioSelection(audioId: Long) {
        selectedAudioIds = if (audioId in selectedAudioIds) {
            selectedAudioIds - audioId
        } else {
            selectedAudioIds + audioId
        }
    }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bulkTargets = pendingBulkConsentAudios
            if (bulkTargets.isNotEmpty()) {
                scope.launch {
                    bulkTargets.forEach { audio ->
                        mediaScanner.finalizeAudioDeletion(audio)
                        musicManager.removeTrackFromPlaylist(audio.id)
                    }
                    selectedAudioIds = emptySet()
                    isSelectionMode = false
                    Toast.makeText(context, "تم حذف ${bulkTargets.size} ملفات صوتية", Toast.LENGTH_SHORT).show()
                    viewModel.loadLocalAudioFiles()
                }
            } else {
                pendingDeleteConsentAudio?.let { deletedAudio ->
                    scope.launch {
                        mediaScanner.finalizeAudioDeletion(deletedAudio)
                        musicManager.removeTrackFromPlaylist(deletedAudio.id)
                        Toast.makeText(context, "🗑️ تم حذف المقطع الصوتي بنجاح", Toast.LENGTH_SHORT).show()
                        viewModel.loadLocalAudioFiles()
                    }
                }
            }
        } else {
            Toast.makeText(context, "تم إلغاء حذف الملف الصوتي", Toast.LENGTH_SHORT).show()
        }
        pendingBulkConsentAudios = emptyList()
        pendingDeleteConsentAudio = null
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadLocalAudioFiles()
    }

    LaunchedEffect(Unit) {
        viewModel.loadLocalAudioFiles()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 0) {
            isSelectionMode = false
            selectedAudioIds = emptySet()
        }
    }

    // Filter audio tracks based on search
    val filteredAudio = remember(audioFiles, searchQuery) {
        if (searchQuery.isBlank()) audioFiles
        else audioFiles.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true) ||
                    it.folderName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E1230), Color(0xFF111827), Color(0xFF0A0E17))
                )
            )
    ) {
        // Top Header: Slim, sleek, and compact (takes minimal vertical height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = Color(0xFF9D7BFF),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "مشغل الموسيقى",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "(${audioFiles.size})",
                    color = Color(0xFF9D7BFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Compact Scan button
            Button(
                onClick = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF272D66),
                    contentColor = Color(0xFF9D7BFF)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "اكتشاف الملفات",
                    tint = Color(0xFF9D7BFF),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("مسح", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar (Clean centered BasicTextField - never clips text)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1F4A))
                .border(
                    width = 1.dp,
                    color = if (searchQuery.isNotEmpty()) Color(0xFF9D7BFF) else Color(0xFF272D66),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "بحث",
                tint = Color(0xFF9D7BFF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "بحث عن أغنية، فنان، ألبوم...",
                        color = Color(0xFF8B96BD),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color(0xFF9D7BFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_search_input")
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "مسح البحث",
                        tint = Color(0xFFA9B4D6),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Permission Card (only shown when empty, and compact)
        if (audioFiles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33818CF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x22818CF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = Color(0xFF9D7BFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اكتشاف الملفات الصوتية على الهاتف",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط للمسح واستيراد MP3 و WAV و FLAC",
                            color = Color(0xFFA9B4D6),
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissions)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D7BFF), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("مسح الآن", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tabs Header
        val tabs = listOf("جميع المقاطع", "الفنانون الألبومات", "المجلدات")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0E1230),
            contentColor = Color(0xFF9D7BFF),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF9D7BFF),
                        height = 3.dp
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color(0xFF9D7BFF) else Color(0xFF8B96BD),
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSelectionMode) "تم تحديد ${selectedAudioIds.size} من ${filteredAudio.size}" else "يمكنك تحديد أكثر من مقطع للحذف",
                    color = if (isSelectionMode) Color(0xFF2EE6A6) else Color(0xFFA9B4D6),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isSelectionMode) {
                    TextButton(
                        onClick = { selectedAudioIds = filteredAudio.map { it.id }.toSet() },
                        enabled = filteredAudio.isNotEmpty()
                    ) { Text("الكل", fontSize = 10.sp) }
                    TextButton(
                        onClick = {
                            selectedAudioIds = emptySet()
                            isSelectionMode = false
                        }
                    ) { Text("إلغاء", fontSize = 10.sp) }
                    Button(
                        onClick = { showBulkDeleteDialog = true },
                        enabled = selectedAudioIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B)),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("delete_selected_audio_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("حذف", fontSize = 10.sp)
                    }
                } else {
                    TextButton(
                        onClick = {
                            isSelectionMode = true
                            selectedAudioIds = emptySet()
                        },
                        modifier = Modifier.testTag("start_audio_selection_btn")
                    ) { Text("تحديد عدة مقاطع", fontSize = 10.sp) }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AudioListContent(
                    audioList = filteredAudio,
                    currentPlaying = currentPlayingAudio,
                    onPlayAudio = { audio ->
                        val index = filteredAudio.indexOf(audio).coerceAtLeast(0)
                        musicManager.setPlaylist(filteredAudio, index)
                    },
                    onCutAudio = { audio -> audioToCut = audio },
                    onShareAudio = { audio -> shareAudioFile(context, audio) },
                    onDeleteAudio = { audio -> audioToDelete = audio },
                    isSelectionMode = isSelectionMode,
                    selectedAudioIds = selectedAudioIds,
                    onToggleSelection = { audioId -> toggleAudioSelection(audioId) }
                )
                1 -> GroupedArtistsContent(
                    audioList = filteredAudio,
                    onPlayAudio = { audio ->
                        val index = filteredAudio.indexOf(audio).coerceAtLeast(0)
                        musicManager.setPlaylist(filteredAudio, index)
                    }
                )
                2 -> GroupedFoldersContent(
                    audioList = filteredAudio,
                    onPlayAudio = { audio ->
                        val index = filteredAudio.indexOf(audio).coerceAtLeast(0)
                        musicManager.setPlaylist(filteredAudio, index)
                    }
                )
            }
        }

        playbackError?.let { err ->
            Text(
                text = err,
                color = Color(0xFFFF6B8B),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Dynamic Interactive Visualizer & Audio Player Controls
        currentPlayingAudio?.let { audio ->
            DynamicMusicVisualizer(
                audio = audio,
                isPlaying = isPlaying,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )

            AudioPlayerControlBar(
                audio = audio,
                isPlaying = isPlaying,
                currentPosMs = currentPositionMs,
                durationMs = durationMs,
                repeatMode = repeatMode,
                isShuffleEnabled = isShuffleEnabled,
                onSeek = { pos -> musicManager.seekTo(pos) },
                onTogglePlay = { musicManager.togglePlayPause() },
                onNext = { musicManager.playNext() },
                onPrev = { musicManager.playPrevious() },
                onToggleRepeat = {
                    musicManager.cycleRepeatMode()
                    val msg = when (musicManager.repeatMode.value) {
                        MusicPlayerManager.AudioRepeatMode.ONE -> "🔂 تكرار المقطع الحالي فقط"
                        MusicPlayerManager.AudioRepeatMode.ALL -> "🔁 تكرار كافة المقاطع تلقائياً"
                        MusicPlayerManager.AudioRepeatMode.OFF -> "⏹️ إيقاف التكرار التلقائي"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onToggleShuffle = {
                    musicManager.toggleShuffle()
                    val msg = if (musicManager.isShuffleEnabled.value) "🔀 تفعيل التشغيل العشوائي" else "⬇️ التشغيل بالترتيب العادي"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onClose = { musicManager.stop() },
                onCutAudio = { audioToCut = audio },
                onShareAudio = { shareAudioFile(context, audio) }
            )
        }

        // Audio Cutter & Ringtone Maker Dialog
        audioToCut?.let { audio ->
            AudioCutterDialog(
                audio = audio,
                onDismiss = { audioToCut = null }
            )
        }

        // Delete Audio Confirmation Dialog
        audioToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { audioToDelete = null },
                containerColor = Color(0xFF1A1F4A),
                title = {
                    Text(
                        text = "حذف المقطع الصوتي نهائياً",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "هل تريد حذف \"${target.title}\" نهائياً من ذاكرة الهاتف؟ لا يمكن التراجع عن هذا الإجراء.",
                        color = Color(0xFFD5DCF2),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val item = target
                            audioToDelete = null
                            scope.launch {
                                when (val res = mediaScanner.requestDeleteAudio(item)) {
                                    is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                        musicManager.removeTrackFromPlaylist(item.id)
                                        Toast.makeText(context, "🗑️ تم حذف المقطع الصوتي بنجاح", Toast.LENGTH_SHORT).show()
                                        viewModel.loadLocalAudioFiles()
                                    }
                                    is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                        pendingDeleteConsentAudio = item
                                        deleteConsentLauncher.launch(
                                            IntentSenderRequest.Builder(res.intentSender).build()
                                        )
                                    }
                                    is MediaScanner.MediaDeleteResult.Failed -> {
                                        Toast.makeText(context, res.reason, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                        modifier = Modifier.testTag("confirm_delete_audio_btn")
                    ) {
                        Text("حذف نهائياً", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { audioToDelete = null }) {
                        Text("إلغاء", color = Color(0xFFA9B4D6))
                    }
                }
            )
        }
        if (showBulkDeleteDialog) {
            val targets = audioFiles.filter { it.id in selectedAudioIds }
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                containerColor = Color(0xFF1A1F4A),
                title = { Text("حذف المقاطع الصوتية المحددة", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "سيتم حذف ${targets.size} ملفات صوتية من ذاكرة الهاتف. لا يمكن التراجع عن الحذف.",
                        color = Color(0xFFD5DCF2),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        enabled = targets.isNotEmpty(),
                        onClick = {
                            showBulkDeleteDialog = false
                            scope.launch {
                                when (
                                    val result = mediaScanner.requestBatchDeleteUris(
                                        uris = targets.map { it.contentUri },
                                        fallbackPaths = targets.map { it.filePath }
                                    )
                                ) {
                                    is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                        targets.forEach { audio ->
                                            mediaScanner.finalizeAudioDeletion(audio)
                                            musicManager.removeTrackFromPlaylist(audio.id)
                                        }
                                        selectedAudioIds = emptySet()
                                        isSelectionMode = false
                                        Toast.makeText(context, "تم حذف ${targets.size} ملفات صوتية", Toast.LENGTH_LONG).show()
                                        viewModel.loadLocalAudioFiles()
                                    }
                                    is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                        pendingBulkConsentAudios = targets
                                        deleteConsentLauncher.launch(
                                            IntentSenderRequest.Builder(result.intentSender).build()
                                        )
                                    }
                                    is MediaScanner.MediaDeleteResult.Failed -> {
                                        Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B)),
                        modifier = Modifier.testTag("confirm_delete_selected_audio_btn")
                    ) { Text("حذف المحدد", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDeleteDialog = false }) {
                        Text("إلغاء", color = Color(0xFFA9B4D6))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioListContent(
    audioList: List<LocalAudio>,
    currentPlaying: LocalAudio?,
    onPlayAudio: (LocalAudio) -> Unit,
    onCutAudio: (LocalAudio) -> Unit,
    onShareAudio: (LocalAudio) -> Unit,
    onDeleteAudio: (LocalAudio) -> Unit,
    isSelectionMode: Boolean,
    selectedAudioIds: Set<Long>,
    onToggleSelection: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(audioList, key = { it.id }) { audio ->
            val isSelected = currentPlaying?.id == audio.id
            val isMarkedForDeletion = audio.id in selectedAudioIds
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isMarkedForDeletion -> Color(0xFF193B4A)
                        isSelected -> Color(0xFF2A2470)
                        else -> Color(0xFF1A1F4A)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isMarkedForDeletion) 2.dp else 1.dp,
                    when {
                        isMarkedForDeletion -> Color(0xFF2EE6A6)
                        isSelected -> Color(0xFF9D7BFF)
                        else -> Color(0xFF272D66)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isSelectionMode) onToggleSelection(audio.id) else onPlayAudio(audio)
                    }
                    .testTag("audio_card_${audio.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF9D7BFF) else Color(0xFF272D66)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF9D7BFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = audio.artist,
                                color = Color(0xFFA9B4D6),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF363D80))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(audio.format, color = Color(0xFF22E3FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = MediaScanner.formatDuration(audio.durationMs),
                            color = Color(0xFF8B96BD),
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isSelectionMode) {
                            Checkbox(
                                checked = isMarkedForDeletion,
                                onCheckedChange = { onToggleSelection(audio.id) },
                                modifier = Modifier.testTag("select_audio_${audio.id}")
                            )
                        } else {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { onCutAudio(audio) },
                                        modifier = Modifier.size(28.dp).testTag("cut_audio_btn_${audio.id}")
                                    ) {
                                        Icon(Icons.Default.ContentCut, contentDescription = "قص الأغنية ونغمة", tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onShareAudio(audio) },
                                        modifier = Modifier.size(28.dp).testTag("share_audio_btn_${audio.id}")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة الأغنية", tint = Color(0xFF22E3FF), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteAudio(audio) },
                                        modifier = Modifier.size(28.dp).testTag("delete_audio_btn_${audio.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف المقطع الصوتي", tint = Color(0xFFFF6B8B), modifier = Modifier.size(16.dp))
                                    }
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
private fun GroupedArtistsContent(
    audioList: List<LocalAudio>,
    onPlayAudio: (LocalAudio) -> Unit
) {
    val grouped = remember(audioList) { audioList.groupBy { it.artist } }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(grouped.keys.toList()) { artistName ->
            val list = grouped[artistName] ?: emptyList()
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { list.firstOrNull()?.let(onPlayAudio) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF9D7BFF), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${list.size} أغنية ومقطع صوتي", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupedFoldersContent(
    audioList: List<LocalAudio>,
    onPlayAudio: (LocalAudio) -> Unit
) {
    val grouped = remember(audioList) { audioList.groupBy { it.folderName } }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(grouped.keys.toList()) { folderName ->
            val list = grouped[folderName] ?: emptyList()
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { list.firstOrNull()?.let(onPlayAudio) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${list.size} ملف صوتي", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioPlayerControlBar(
    audio: LocalAudio,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    repeatMode: MusicPlayerManager.AudioRepeatMode,
    isShuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onClose: () -> Unit = {},
    onCutAudio: () -> Unit = {},
    onShareAudio: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141943)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272D66)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Row 1: Track Identity + Quick Studio Tools (Cut, Share, Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF272D66))
                        .border(1.5.dp, Color(0xFF9D7BFF), CircleShape)
                        .rotate(if (isPlaying) rotation else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = Color(0xFF9D7BFF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audio.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = audio.artist,
                        color = Color(0xFFA9B4D6),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onCutAudio,
                            modifier = Modifier.size(32.dp).testTag("mini_player_cut_btn")
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = "قص الأغنية", tint = Color(0xFFFFC857), modifier = Modifier.size(17.dp))
                        }
                        IconButton(
                            onClick = onShareAudio,
                            modifier = Modifier.size(32.dp).testTag("mini_player_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة الأغنية", tint = Color(0xFF22E3FF), modifier = Modifier.size(17.dp))
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp).testTag("mini_player_close_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق المشغل", tint = Color(0xFFFF6B8B), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Row 2: Seek Slider + Time
            val totalDur = if (durationMs > 0) durationMs else (if (audio.durationMs > 0) audio.durationMs else 1L)
            val progressFraction = (currentPosMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

            Slider(
                value = progressFraction,
                onValueChange = { frac ->
                    onSeek((frac * totalDur).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF9D7BFF),
                    activeTrackColor = Color(0xFF9D7BFF),
                    inactiveTrackColor = Color(0xFF363D80)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = MediaScanner.formatDuration(currentPosMs),
                    color = Color(0xFFA9B4D6),
                    fontSize = 9.sp
                )
                Text(
                    text = MediaScanner.formatDuration(totalDur),
                    color = Color(0xFFA9B4D6),
                    fontSize = 9.sp
                )
            }

            // Row 3: Full Centered Transport Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(36.dp).testTag("audio_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "تشغيل عشوائي",
                            tint = if (isShuffleEnabled) Color(0xFF22E3FF) else Color(0xFF8B96BD),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(36.dp).testTag("audio_prev_btn")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "السابق", tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    // Play / Pause
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9D7BFF))
                            .testTag("audio_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(36.dp).testTag("audio_next_btn")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "التالي", tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    // Repeat Mode (OFF / ALL / ONE)
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(36.dp).testTag("audio_repeat_btn")
                    ) {
                        Icon(
                            imageVector = if (repeatMode == MusicPlayerManager.AudioRepeatMode.ONE)
                                Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "تكرار الصوتيات",
                            tint = when (repeatMode) {
                                MusicPlayerManager.AudioRepeatMode.OFF -> Color(0xFF8B96BD)
                                MusicPlayerManager.AudioRepeatMode.ALL -> Color(0xFF22E3FF)
                                MusicPlayerManager.AudioRepeatMode.ONE -> Color(0xFFFFC857)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
