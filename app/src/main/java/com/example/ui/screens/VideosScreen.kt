package com.example.ui.screens

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import com.example.cast.CastDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import com.example.ai.AiVideoCategorizer
import com.example.cast.CastButton
import com.example.ui.theme.cinemaBackground
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.cast.CastManager
import com.example.media.MediaScanner
import com.example.media.model.LocalVideo

@Composable
fun VideosScreen(
    videos: List<LocalVideo>,
    favorites: Set<String>,
    onVideoClick: (LocalVideo) -> Unit,
    onToggleFavorite: (LocalVideo) -> Unit,
    onEncryptToVault: (LocalVideo) -> Unit,
    onRefresh: () -> Unit,
    onOpenVideoTools: () -> Unit = {},
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    castManager: CastManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGridView by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("date") } // "date", "name", "duration", "size"
    var showSortMenu by remember { mutableStateOf(false) }
    var videoToCast by remember { mutableStateOf<LocalVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingConsentVideo by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingConsentVideos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedVideoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    fun toggleVideoSelection(videoId: Long) {
        selectedVideoIds = if (videoId in selectedVideoIds) {
            selectedVideoIds - videoId
        } else {
            selectedVideoIds + videoId
        }
    }

    // Launcher for system MediaStore Scoped Storage delete confirmation (Android 10/11+)
    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bulkTargets = pendingConsentVideos
            if (bulkTargets.isNotEmpty()) {
                scope.launch {
                    val scanner = MediaScanner(context)
                    bulkTargets.forEach { scanner.finalizeDeletion(it) }
                    selectedVideoIds = emptySet()
                    isSelectionMode = false
                    Toast.makeText(context, "تم حذف ${bulkTargets.size} فيديوهات من الهاتف", Toast.LENGTH_SHORT).show()
                    onRefresh()
                }
            } else {
                val video = pendingConsentVideo
                if (video != null) {
                    scope.launch {
                        val scanner = MediaScanner(context)
                        scanner.finalizeDeletion(video)
                        Toast.makeText(context, "تم حذف الفيديو نهائياً من ذاكرة الهاتف والمعرض", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    }
                }
            }
        } else {
            Toast.makeText(context, "تم إلغاء عملية حذف الفيديو", Toast.LENGTH_SHORT).show()
        }
        pendingConsentVideos = emptyList()
        pendingConsentVideo = null
    }

    // Speech-to-text voice search launcher
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                Toast.makeText(context, "تم التعرف على الصوت: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categories = listOf("الكل", "شورتس وريلز", "قرآن وتلاوات", "أفلام وسينما", "بودكاست ولقاءات", "شروحات وتعليم", "تصوير الكاميرا", "تنزيلات وتواصل")

    val filteredVideos = remember(videos, searchQuery, selectedCategory, sortBy, favorites) {
        var list = videos

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.folderName.contains(searchQuery, ignoreCase = true) ||
                it.aiCategory.contains(searchQuery, ignoreCase = true)
            }
        }

        if (selectedCategory != null && selectedCategory != "الكل") {
            list = list.filter { it.aiCategory == selectedCategory }
        }

        when (sortBy) {
            "name" -> list.sortedBy { it.title.lowercase() }
            "duration" -> list.sortedByDescending { it.durationMs }
            "size" -> list.sortedByDescending { it.sizeBytes }
            else -> list.sortedByDescending { it.dateAdded }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
    ) {
        // Search & Voice Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في الفيديوهات أو نطق صوتي...", color = Color(0xFF8B96BD), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF22E3FF)) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث للبحث عن مقطع فيديو...")
                                    }
                                    voiceSearchLauncher.launch(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "البحث الصوتي غير مدعوم على جهازك", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("voice_search_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "بحث صوتي", tint = Color(0xFF22E3FF))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF22E3FF),
                    unfocusedBorderColor = Color(0xFF272D66),
                    focusedContainerColor = Color(0xFF1A1F4A),
                    unfocusedContainerColor = Color(0xFF1A1F4A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("video_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Video tools (cutter, compressor, GIF, snapshots...)
            IconButton(
                onClick = onOpenVideoTools,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6))
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "أدوات الفيديو", tint = Color.White)
            }

            // Grid / List toggle
            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1A1F4A))
            ) {
                Icon(
                    if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "عرض شبكي/قائمة",
                    tint = Color.White
                )
            }

            // Google Cast Button
            if (castManager != null) {
                CastButton(
                    castManager = castManager,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1A1F4A))
                )
            }

            // Sort Menu
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1A1F4A))
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "ترتيب", tint = Color(0xFF22E3FF))
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("الأحدث تاريخاً") },
                        onClick = { sortBy = "date"; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("الاسم أبجدياً") },
                        onClick = { sortBy = "name"; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("المدة الأطول") },
                        onClick = { sortBy = "duration"; showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("الحجم الأكبر") },
                        onClick = { sortBy = "size"; showSortMenu = false }
                    )
                }
            }

            IconButton(
                onClick = {
                    isSelectionMode = !isSelectionMode
                    if (!isSelectionMode) selectedVideoIds = emptySet()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelectionMode) Color(0xFF194D4A) else Color(0xFF1A1F4A))
                    .testTag("toggle_video_selection_mode")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isSelectionMode) "إلغاء التحديد" else "تحديد عدة فيديوهات",
                    tint = if (isSelectionMode) Color(0xFF2EE6A6) else Color(0xFFA9B4D6)
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (selectedCategory == cat) || (selectedCategory == null && cat == "الكل")
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = if (cat == "الكل") null else cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF22E3FF),
                        selectedLabelColor = Color(0xFF0E1230),
                        containerColor = Color(0xFF1A1F4A),
                        labelColor = Color(0xFFC3CCE8)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Content State Check
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "الوصول التلقائي لمكتبة الوسائط",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يحتاج المشغل لإذن الوسائط لاكتشاف وتشغيل الفيديوهات المخزنة على هاتفك بسرعة فائقة",
                        color = Color(0xFFA9B4D6),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("grant_media_permission_button")
                    ) {
                        Text("منح إذن الوسائط الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF363D80), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لم يتم العثور على أي مقاطع فيديو", color = Color(0xFF9CA3AF), fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF22E3FF))
                    }
                }
            }
        } else {
            // Library count and bulk-selection actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSelectionMode) "تم تحديد ${selectedVideoIds.size} من ${filteredVideos.size}" else "${filteredVideos.size} مقطع فيديو",
                        color = if (isSelectionMode) Color(0xFF2EE6A6) else Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!isSelectionMode) {
                        Text(
                            text = MediaScanner.formatFileSize(filteredVideos.sumOf { it.sizeBytes }),
                            color = Color(0xFF8B96BD),
                            fontSize = 10.sp
                        )
                    }
                }
                if (isSelectionMode) {
                    TextButton(
                        onClick = { selectedVideoIds = filteredVideos.map { it.id }.toSet() },
                        enabled = filteredVideos.isNotEmpty()
                    ) { Text("تحديد الكل", fontSize = 10.sp) }
                    TextButton(
                        onClick = {
                            selectedVideoIds = emptySet()
                            isSelectionMode = false
                        }
                    ) { Text("إلغاء", fontSize = 10.sp) }
                    Button(
                        onClick = { showBulkDeleteDialog = true },
                        enabled = selectedVideoIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        modifier = Modifier.testTag("delete_selected_videos_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف", fontSize = 10.sp)
                    }
                } else {
                    TextButton(
                        onClick = {
                            isSelectionMode = true
                            selectedVideoIds = emptySet()
                        },
                        modifier = Modifier.testTag("start_video_selection_btn")
                    ) { Text("تحديد عدة فيديوهات", fontSize = 10.sp) }
                }
            }

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoGridCard(
                            video = video,
                            isFavorite = favorites.contains(video.uri),
                            onClick = {
                                if (isSelectionMode) toggleVideoSelection(video.id) else onVideoClick(video)
                            },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onEncrypt = { onEncryptToVault(video) },
                            onCast = { videoToCast = video },
                            onDelete = { videoToDelete = video },
                            isSelectionMode = isSelectionMode,
                            isSelected = video.id in selectedVideoIds,
                            onToggleSelection = { toggleVideoSelection(video.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoListCard(
                            video = video,
                            isFavorite = favorites.contains(video.uri),
                            onClick = {
                                if (isSelectionMode) toggleVideoSelection(video.id) else onVideoClick(video)
                            },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onEncrypt = { onEncryptToVault(video) },
                            onCast = { videoToCast = video },
                            onDelete = { videoToDelete = video },
                            isSelectionMode = isSelectionMode,
                            isSelected = video.id in selectedVideoIds,
                            onToggleSelection = { toggleVideoSelection(video.id) }
                        )
                    }
                }
            }
        }

        // Cast dialog when a specific video is chosen for casting
        videoToCast?.let { v ->
            if (castManager != null) {
                CastDialog(
                    castManager = castManager,
                    currentVideo = v,
                    currentTitle = v.title,
                    onDismissRequest = { videoToCast = null }
                )
            }
        }

        // Real Video Deletion Confirmation Dialog
        videoToDelete?.let { video ->
            AlertDialog(
                onDismissRequest = { videoToDelete = null },
                title = { Text("حذف الفيديو نهائياً", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف فيديو \"${video.title}\" نهائياً من ذاكرة هاتفك؟ سيتم مسح الملف بالكامل من الجهاز ولن يمكنك استرجاعه.", color = Color(0xFFC3CCE8), fontSize = 13.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = video
                            videoToDelete = null
                            scope.launch {
                                val scanner = MediaScanner(context)
                                when (val result = scanner.requestDeleteVideo(target)) {
                                    is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                        Toast.makeText(context, "تم حذف الفيديو نهائياً من الهاتف والمعرض", Toast.LENGTH_SHORT).show()
                                        onRefresh()
                                    }
                                    is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                        pendingConsentVideo = target
                                        val request = androidx.activity.result.IntentSenderRequest.Builder(result.intentSender).build()
                                        deleteConsentLauncher.launch(request)
                                    }
                                    is MediaScanner.MediaDeleteResult.Failed -> {
                                        Toast.makeText(context, "تعذر حذف الفيديو: ${result.reason}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B))
                    ) {
                        Text("حذف الآن", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoToDelete = null }) {
                        Text("إلغاء", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1A1F4A)
            )
        }
        if (showBulkDeleteDialog) {
            val targets = videos.filter { it.id in selectedVideoIds }
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                containerColor = Color(0xFF1A1F4A),
                title = {
                    Text("حذف الفيديوهات المحددة", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "سيتم حذف ${targets.size} فيديو من الجهاز والمعرض. لا يمكن التراجع عن الحذف.",
                        color = Color(0xFFC3CCE8),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        enabled = targets.isNotEmpty(),
                        onClick = {
                            showBulkDeleteDialog = false
                            scope.launch {
                                val scanner = MediaScanner(context)
                                when (
                                    val result = scanner.requestBatchDeleteUris(
                                        uris = targets.map { Uri.parse(it.uri) },
                                        fallbackPaths = targets.map { it.path }
                                    )
                                ) {
                                    is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                        targets.forEach { scanner.finalizeDeletion(it) }
                                        selectedVideoIds = emptySet()
                                        isSelectionMode = false
                                        Toast.makeText(context, "تم حذف ${targets.size} فيديوهات بنجاح", Toast.LENGTH_LONG).show()
                                        onRefresh()
                                    }
                                    is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                        pendingConsentVideos = targets
                                        deleteConsentLauncher.launch(
                                            androidx.activity.result.IntentSenderRequest.Builder(result.intentSender).build()
                                        )
                                    }
                                    is MediaScanner.MediaDeleteResult.Failed -> {
                                        Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B)),
                        modifier = Modifier.testTag("confirm_delete_selected_videos_btn")
                    ) { Text("حذف المحدد", color = Color.White) }
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

@Composable
fun VideoThumbnailImage(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(uriString) { mutableStateOf(true) }

    LaunchedEffect(uriString) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            try {
                if (uriString.startsWith("content://")) {
                    val uri = Uri.parse(uriString)
                    val bm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            context.contentResolver.loadThumbnail(uri, android.util.Size(320, 240), null)
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        try {
                            @Suppress("DEPRECATION")
                            MediaStore.Video.Thumbnails.getThumbnail(
                                context.contentResolver,
                                ContentUris.parseId(uri),
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    bm ?: run {
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } catch (_: Exception) {
                            null
                        } finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                    }
                } else {
                    val path = if (uriString.startsWith("file://")) Uri.parse(uriString).path ?: uriString else uriString
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(path)
                        retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (_: Exception) {
                        null
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
        isLoading = false
    }

    Box(
        modifier = modifier.background(Color(0xFF272D66)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF8B96BD),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun VideoListCard(
    video: LocalVideo,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEncrypt: () -> Unit,
    onCast: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF193B4A) else Color(0xFF1A1F4A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFF2EE6A6) else Color(0xFF272D66)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_card_${video.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Widescreen High-Definition Thumbnail (Balanced 16:9 proportion like professional players)
            Box(
                modifier = Modifier
                    .size(width = 126.dp, height = 76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF272D66))
            ) {
                VideoThumbnailImage(
                    uriString = video.uri,
                    modifier = Modifier.fillMaxSize()
                )

                // Translucent play icon in center
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Duration badge on bottom corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xD9000000))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = MediaScanner.formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (video.aiCategory.isNotBlank() && video.aiCategory != "فيديو عام") {
                    val catColor = AiVideoCategorizer.getCategoryColorHex(video.aiCategory)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(catColor).copy(alpha = 0.85f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.aiCategory,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column (Spacious and formatted to never wrap letters vertically)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${video.resolution} • ${MediaScanner.formatFileSize(video.sizeBytes)}",
                    color = Color(0xFF22E3FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )

                if (video.folderName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.folderName,
                        color = Color(0xFFA9B4D6),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }

            // Compact Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.testTag("select_video_${video.id}")
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "تفضيل",
                        tint = if (isFavorite) Color(0xFFFFC857) else Color(0xFF8B96BD),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "خيارات إضافية",
                            tint = Color(0xFFA9B4D6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = Color(0xFF272D66)
                    ) {
                        DropdownMenuItem(
                            text = { Text("تشفير ونقل للخزنة الآمنة", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFFFF6B8B)) },
                            onClick = {
                                showMenu = false
                                onEncrypt()
                            }
                        )
                        if (onCast != null) {
                            DropdownMenuItem(
                                text = { Text("بث إلى التلفاز (Chromecast)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Cast, null, tint = Color(0xFF22E3FF)) },
                                onClick = {
                                    showMenu = false
                                    onCast()
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("حذف الفيديو نهائياً من الهاتف", color = Color(0xFFFF6B8B)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B8B)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: LocalVideo,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEncrypt: () -> Unit,
    onCast: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF193B4A) else Color(0xFF1A1F4A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFF2EE6A6) else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color(0xFF272D66))
            ) {
                VideoThumbnailImage(
                    uriString = video.uri,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = MediaScanner.formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val catColor = AiVideoCategorizer.getCategoryColorHex(video.aiCategory)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(catColor).copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = video.aiCategory,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .testTag("select_video_grid_${video.id}")
                        )
                    } else if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الفيديو", tint = Color(0xFFFF6B8B), modifier = Modifier.size(15.dp))
                        }
                    }

                    if (!isSelectionMode && onCast != null) {
                        IconButton(
                            onClick = { onCast() },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = "بث للتلفاز", tint = Color(0xFF22E3FF), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = video.resolution, color = Color(0xFF22E3FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = MediaScanner.formatFileSize(video.sizeBytes), color = Color(0xFFA9B4D6), fontSize = 10.sp)
                }
            }
        }
    }
}
