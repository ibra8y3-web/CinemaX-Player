package com.example.ui.screens

import com.example.ui.theme.cinemaBackground

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cast.CastButton
import com.example.cast.CastDialog
import com.example.cast.CastManager
import com.example.media.MediaScanner
import com.example.media.model.LocalVideo
import com.example.media.model.VideoFolder
import kotlinx.coroutines.launch

@Composable
fun FoldersScreen(
    folders: List<VideoFolder>,
    allVideos: List<LocalVideo>,
    favorites: Set<String>,
    onVideoClick: (LocalVideo) -> Unit,
    onToggleFavorite: (LocalVideo) -> Unit,
    onEncryptToVault: (LocalVideo) -> Unit,
    modifier: Modifier = Modifier,
    castManager: CastManager? = null,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var videoToCast by remember { mutableStateOf<LocalVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingConsentVideo by remember { mutableStateOf<LocalVideo?>(null) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingConsentVideo?.let { v ->
                scope.launch {
                    MediaScanner(context).finalizeDeletion(v)
                    Toast.makeText(context, "🗑️ تم حذف الفيديو نهائياً من ذاكرة الهاتف", Toast.LENGTH_SHORT).show()
                    onRefresh()
                }
            }
        } else {
            Toast.makeText(context, "تم إلغاء عملية حذف الفيديو", Toast.LENGTH_SHORT).show()
        }
        pendingConsentVideo = null
    }

    BackHandler(enabled = selectedFolder != null) {
        selectedFolder = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
    ) {
        if (selectedFolder != null) {
            // Header with back button to folders
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = { selectedFolder = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع للمجلدات", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFolder!!,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val folderVideos = allVideos.filter { it.folderName == selectedFolder }
                    Text(
                        text = "${folderVideos.size} مقطع فيديو",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp
                    )
                }
                if (castManager != null) {
                    CastButton(
                        castManager = castManager,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFF1A1F4A))
                    )
                }
            }

            val folderVideos = allVideos.filter { it.folderName == selectedFolder }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folderVideos, key = { it.id }) { video ->
                    VideoListCard(
                        video = video,
                        isFavorite = favorites.contains(video.uri),
                        onClick = { onVideoClick(video) },
                        onToggleFavorite = { onToggleFavorite(video) },
                        onEncrypt = { onEncryptToVault(video) },
                        onCast = { videoToCast = video },
                        onDelete = { videoToDelete = video }
                    )
                }
            }
        } else {
            // Folders list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجلدات الوسائط",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${folders.size} مجلد",
                        color = Color(0xFFA9B4D6),
                        fontSize = 13.sp
                    )
                    if (castManager != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        CastButton(
                            castManager = castManager,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFF1A1F4A))
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folders) { folder ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFolder = folder.name }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(com.example.ui.theme.BrandGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                if (folder.firstVideoUri != null) {
                                    AsyncImage(
                                        model = folder.firstVideoUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${folder.videoCount} مقطع • ${MediaScanner.formatFileSize(folder.totalSizeBytes)}",
                                    color = Color(0xFFA9B4D6),
                                    fontSize = 12.sp
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF22E3FF)
                            )
                        }
                    }
                }
            }
        }
    }

    if (videoToCast != null && castManager != null) {
        CastDialog(
            castManager = castManager,
            currentVideo = videoToCast,
            currentTitle = videoToCast?.title,
            onDismissRequest = { videoToCast = null }
        )
    }

    videoToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            containerColor = Color(0xFF1A1F4A),
            title = { Text("حذف الفيديو نهائياً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    text = "هل تريد حذف \"${target.title}\" نهائياً من ذاكرة الهاتف؟",
                    color = Color(0xFFD5DCF2),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = target
                        videoToDelete = null
                        scope.launch {
                            when (val res = MediaScanner(context).requestDeleteVideo(item)) {
                                is MediaScanner.MediaDeleteResult.DirectSuccess -> {
                                    Toast.makeText(context, "🗑️ تم حذف الفيديو بنجاح", Toast.LENGTH_SHORT).show()
                                    onRefresh()
                                }
                                is MediaScanner.MediaDeleteResult.RequiresConsent -> {
                                    pendingConsentVideo = item
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("حذف نهائياً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("إلغاء", color = Color(0xFFA9B4D6))
                }
            }
        )
    }
}
