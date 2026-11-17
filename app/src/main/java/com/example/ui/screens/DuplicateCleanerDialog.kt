package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.MediaScanner
import com.example.media.model.LocalAudio
import com.example.media.model.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DuplicateCleanerDialog(
    onDismiss: () -> Unit,
    onMediaDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaScanner = remember { MediaScanner(context) }

    var isScanning by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }
    var duplicateVideos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var duplicateAudios by remember { mutableStateOf<List<LocalAudio>>(emptyList()) }
    var totalWastedBytes by remember { mutableStateOf(0L) }

    var pendingSingleVideo by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingSingleAudio by remember { mutableStateOf<LocalAudio?>(null) }
    var pendingBatchVideos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var pendingBatchAudios by remember { mutableStateOf<List<LocalAudio>>(emptyList()) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                isDeleting = true
                var deletedCount = 0
                pendingSingleVideo?.let { v ->
                    mediaScanner.finalizeDeletion(v)
                    duplicateVideos = duplicateVideos.filterNot { it.id == v.id }
                    deletedCount = 1
                }
                pendingSingleAudio?.let { a ->
                    mediaScanner.finalizeAudioDeletion(a)
                    duplicateAudios = duplicateAudios.filterNot { it.id == a.id }
                    deletedCount = 1
                }
                if (pendingBatchVideos.isNotEmpty() || pendingBatchAudios.isNotEmpty()) {
                    pendingBatchVideos.forEach { mediaScanner.finalizeDeletion(it) }
                    pendingBatchAudios.forEach { mediaScanner.finalizeAudioDeletion(it) }
                    deletedCount = pendingBatchVideos.size + pendingBatchAudios.size
                    duplicateVideos = emptyList()
                    duplicateAudios = emptyList()
                }
                totalWastedBytes = duplicateVideos.sumOf { it.sizeBytes } + duplicateAudios.sumOf { it.sizeBytes }
                pendingSingleVideo = null
                pendingSingleAudio = null
                pendingBatchVideos = emptyList()
                pendingBatchAudios = emptyList()
                isDeleting = false
                Toast.makeText(context, "✨ تم حذف $deletedCount ملف مكرر بنجاح", Toast.LENGTH_LONG).show()
                onMediaDeleted()
            }
        } else {
            pendingSingleVideo = null
            pendingSingleAudio = null
            pendingBatchVideos = emptyList()
            pendingBatchAudios = emptyList()
            Toast.makeText(context, "تم إلغاء عملية الحذف", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        isScanning = true
        val (videoDups, audioDups) = withContext(Dispatchers.IO) {
            val videos = mediaScanner.scanVideos()
            val audios = mediaScanner.scanAudioFiles()

            // Group by (sizeBytes, durationSec) to detect true duplicates while keeping 1 original copy
            val videoDupsList = mutableListOf<LocalVideo>()
            val seenVideoKeys = mutableSetOf<Pair<Long, Long>>()
            videos.sortedBy { it.dateAdded }.forEach { v ->
                if (v.sizeBytes > 0) {
                    val key = Pair(v.sizeBytes, v.durationMs / 1000L)
                    if (!seenVideoKeys.add(key)) {
                        videoDupsList.add(v)
                    }
                }
            }

            val audioDupsList = mutableListOf<LocalAudio>()
            val seenAudioKeys = mutableSetOf<Pair<Long, Long>>()
            audios.sortedBy { it.dateAddedSec }.forEach { a ->
                if (a.sizeBytes > 0) {
                    val key = Pair(a.sizeBytes, a.durationMs / 1000L)
                    if (!seenAudioKeys.add(key)) {
                        audioDupsList.add(a)
                    }
                }
            }
            videoDupsList to audioDupsList
        }

        duplicateVideos = videoDups
        duplicateAudios = audioDups
        totalWastedBytes = videoDups.sumOf { it.sizeBytes } + audioDups.sumOf { it.sizeBytes }
        isScanning = false
    }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = Color(0xFF1A1F4A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4081))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "منظف الوسائط والملفات المكررة",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
                IconButton(onClick = { if (!isDeleting) onDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                if (isScanning || isDeleting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFFF4081))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isDeleting) "جاري حذف الملفات المكررة..." else "جاري فحص الذاكرة واكتشاف الملفات المكررة...",
                                color = Color(0xFFA9B4D6),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    val totalCount = duplicateVideos.size + duplicateAudios.size
                    Text(
                        text = if (totalCount == 0)
                            "✨ مبروك! لم يتم العثور على أي ملفات أو وسائط مكررة. جهازك نظيف بالكامل."
                        else
                            "تم العثور على $totalCount ملف مكرر تستهلك ${MediaScanner.formatFileSize(totalWastedBytes)} من مساحة الجهاز (مع الاحتفاظ بالنسخة الأصلية).",
                        color = Color(0xFFE6EAFA),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (totalCount > 0) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            items(duplicateVideos, key = { "v_${it.id}" }) { video ->
                                DuplicateItemRow(
                                    title = video.title,
                                    sizeText = MediaScanner.formatFileSize(video.sizeBytes),
                                    type = "فيديو مكرر",
                                    onDelete = {
                                        scope.launch {
                                            when (val res = mediaScanner.requestDeleteVideo(video)) {
                                                is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                                    duplicateVideos = duplicateVideos.filterNot { it.id == video.id }
                                                    totalWastedBytes = duplicateVideos.sumOf { it.sizeBytes } + duplicateAudios.sumOf { it.sizeBytes }
                                                    Toast.makeText(context, "🗑️ تم حذف الفيديو المكرر بنجاح", Toast.LENGTH_SHORT).show()
                                                    onMediaDeleted()
                                                }
                                                is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                                    pendingSingleVideo = video
                                                    deleteConsentLauncher.launch(
                                                        IntentSenderRequest.Builder(res.intentSender).build()
                                                    )
                                                }
                                                is MediaScanner.MediaDeleteResult.Failed -> {
                                                    Toast.makeText(context, res.reason, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            items(duplicateAudios, key = { "a_${it.id}" }) { audio ->
                                DuplicateItemRow(
                                    title = audio.title,
                                    sizeText = MediaScanner.formatFileSize(audio.sizeBytes),
                                    type = "صوت مكرر",
                                    onDelete = {
                                        scope.launch {
                                            when (val res = mediaScanner.requestDeleteAudio(audio)) {
                                                is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                                    duplicateAudios = duplicateAudios.filterNot { it.id == audio.id }
                                                    totalWastedBytes = duplicateVideos.sumOf { it.sizeBytes } + duplicateAudios.sumOf { it.sizeBytes }
                                                    Toast.makeText(context, "🗑️ تم حذف الملف الصوتي المكرر بنجاح", Toast.LENGTH_SHORT).show()
                                                    onMediaDeleted()
                                                }
                                                is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                                    pendingSingleAudio = audio
                                                    deleteConsentLauncher.launch(
                                                        IntentSenderRequest.Builder(res.intentSender).build()
                                                    )
                                                }
                                                is MediaScanner.MediaDeleteResult.Failed -> {
                                                    Toast.makeText(context, res.reason, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isScanning && !isDeleting && (duplicateVideos.isNotEmpty() || duplicateAudios.isNotEmpty())) {
                Button(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val videoUris = duplicateVideos.mapNotNull {
                                if (it.uri.startsWith("content://")) Uri.parse(it.uri) else null
                            }
                            val audioUris = duplicateAudios.map { it.contentUri }
                            val allUris = videoUris + audioUris
                            val fallbackPaths = duplicateVideos.map { it.path } + duplicateAudios.map { it.filePath }

                            when (val res = mediaScanner.requestBatchDeleteUris(allUris, fallbackPaths)) {
                                is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                    val count = duplicateVideos.size + duplicateAudios.size
                                    duplicateVideos.forEach { mediaScanner.finalizeDeletion(it) }
                                    duplicateAudios.forEach { mediaScanner.finalizeAudioDeletion(it) }
                                    duplicateVideos = emptyList()
                                    duplicateAudios = emptyList()
                                    totalWastedBytes = 0L
                                    isDeleting = false
                                    Toast.makeText(context, "✨ تنظيف مكتمل: تم حذف $count ملف مكرر بنجاح", Toast.LENGTH_LONG).show()
                                    onMediaDeleted()
                                }
                                is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                    pendingBatchVideos = duplicateVideos
                                    pendingBatchAudios = duplicateAudios
                                    isDeleting = false
                                    deleteConsentLauncher.launch(
                                        IntentSenderRequest.Builder(res.intentSender).build()
                                    )
                                }
                                is MediaScanner.MediaDeleteResult.Failed -> {
                                    isDeleting = false
                                    Toast.makeText(context, res.reason, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("delete_all_duplicates_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف كافة المكررات (${duplicateVideos.size + duplicateAudios.size})", color = Color.White, fontSize = 12.sp)
                }
            } else {
                TextButton(onClick = onDismiss, enabled = !isDeleting) {
                    Text("إغلاق", color = Color(0xFF22E3FF))
                }
            }
        }
    )
}

@Composable
private fun DuplicateItemRow(
    title: String,
    sizeText: String,
    type: String,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileCopy, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("$type • الحجم: $sizeText", color = Color(0xFFA9B4D6), fontSize = 10.sp)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف الملف المكرر", tint = Color(0xFFEF4444))
            }
        }
    }
}
