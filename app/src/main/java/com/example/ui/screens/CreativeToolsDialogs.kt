package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.MediaScanner
import com.example.media.RingtoneMakerStudio
import com.example.media.VideoCompressorStudio
import com.example.media.VideoMuteAudioRemover
import com.example.media.VideoToGifConverter
import com.example.media.model.LocalVideo
import kotlinx.coroutines.launch

/**
 * 1. Fast Video to GIF Maker Dialog
 */
@Composable
fun VideoToGifDialog(
    videos: List<LocalVideo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideo by remember { mutableStateOf(videos.firstOrNull()) }
    var startSeconds by remember { mutableFloatStateOf(0f) }
    var endSeconds by remember { mutableFloatStateOf(3f) }
    var fps by remember { mutableIntStateOf(8) }
    var isConverting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<VideoToGifConverter.ConversionResult?>(null) }

    val maxDur = ((selectedVideo?.durationMs ?: 30_000L) / 1000f).coerceAtLeast(3f)

    AlertDialog(
        onDismissRequest = { if (!isConverting) onDismiss() },
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x3322E3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("محول الفيديو إلى صورة GIF متحركة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("اختر الفيديو لتحويل جزء منه إلى GIF عالي الجودة:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videos.take(15)) { v ->
                        val isSel = v.id == selectedVideo?.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF22E3FF) else Color(0xFF272D66)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = !isConverting) {
                                selectedVideo = v
                                startSeconds = 0f
                                endSeconds = 3f.coerceAtMost(maxDur)
                                result = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VideoThumbnailImage(uriString = v.uri, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = v.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSel) Color(0xFF0E1230) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "نطاق الاقتطاع: من ${String.format("%.1f", startSeconds)} ثانية إلى ${String.format("%.1f", endSeconds)} ثانية (المدة: ${String.format("%.1f", (endSeconds - startSeconds).coerceAtLeast(0.5f))} ث)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = startSeconds,
                    onValueChange = {
                        startSeconds = it
                        if (endSeconds <= it + 0.5f) endSeconds = (it + 2.5f).coerceAtMost(maxDur)
                    },
                    valueRange = 0f..(maxDur - 1f).coerceAtLeast(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF22E3FF), activeTrackColor = Color(0xFF22E3FF)),
                    enabled = !isConverting
                )

                Slider(
                    value = endSeconds,
                    onValueChange = {
                        endSeconds = it
                        if (startSeconds >= it - 0.5f) startSeconds = (it - 2.5f).coerceAtLeast(0f)
                    },
                    valueRange = 1f..maxDur,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF4081), activeTrackColor = Color(0xFFFF4081)),
                    enabled = !isConverting
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سلاسة العرض (FPS):", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                    listOf(6 to "6 (خفيف)", 8 to "8 (متوازن)", 12 to "12 (فائق)").forEach { (f, label) ->
                        val sel = fps == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) Color(0xFF22E3FF) else Color(0xFF272D66))
                                .clickable(enabled = !isConverting) { fps = f }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = if (sel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isConverting) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF22E3FF),
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(statusText, color = Color(0xFF22E3FF), fontSize = 11.sp)
                }

                result?.let { res ->
                    if (res.success) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("✅ تم إنشاء صورة GIF بنجاح!", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("عدد الإطارات: ${res.frameCount} إطار | الحجم: ${res.fileSizeFormatted}", color = Color.White, fontSize = 11.sp)
                                Text("المسار: Pictures/CinemaX_GIFs", color = Color(0xFF80CBC4), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        res.uri?.let { uri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/gif"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة صورة GIF عبر"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة صورة GIF الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (res.errorMessage != null) {
                        Text(res.errorMessage, color = Color(0xFFFF6B8B), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val vid = selectedVideo ?: return@Button
                    isConverting = true
                    result = null
                    scope.launch {
                        val startMs = (startSeconds * 1000L).toLong()
                        val endMs = (endSeconds * 1000L).toLong()
                        val res = VideoToGifConverter.convertVideoToGif(
                            context = context,
                            videoUri = Uri.parse(vid.uri),
                            startMs = startMs,
                            endMs = endMs,
                            fps = fps,
                            onProgress = { p, st ->
                                progress = p
                                statusText = st
                            }
                        )
                        result = res
                        isConverting = false
                    }
                },
                enabled = !isConverting && selectedVideo != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
            ) {
                Text(if (isConverting) "جاري التحويل..." else "تحويل إلى GIF الآن 🚀", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isConverting) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}

/**
 * 2. Ringtone Maker Studio & Waveform Cutter Dialog
 */
@Composable
fun RingtoneMakerDialog(
    videos: List<LocalVideo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideo by remember { mutableStateOf(videos.firstOrNull()) }
    var startSeconds by remember { mutableFloatStateOf(0f) }
    var endSeconds by remember { mutableFloatStateOf(30f) }
    var ringtoneTitle by remember { mutableStateOf(selectedVideo?.title?.take(20) ?: "نغمة رنين مخصصة") }
    var isCutting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<RingtoneMakerStudio.RingtoneResult?>(null) }

    val maxDur = ((selectedVideo?.durationMs ?: 60_000L) / 1000f).coerceAtLeast(10f)
    val waveform = remember(selectedVideo?.title) {
        RingtoneMakerStudio.generateWaveformPoints(36, selectedVideo?.title ?: "")
    }

    AlertDialog(
        onDismissRequest = { if (!isCutting) onDismiss() },
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FFD54F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("صانع نغمات الرنين وقص الصوتيات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("اختر ملف الفيديو أو الصوت لاقتطاع النغمة:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videos.take(15)) { v ->
                        val isSel = v.id == selectedVideo?.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFFFFC857) else Color(0xFF272D66)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = !isCutting) {
                                selectedVideo = v
                                ringtoneTitle = v.title.take(20)
                                startSeconds = 0f
                                endSeconds = 30f.coerceAtMost(maxDur)
                                result = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VideoThumbnailImage(uriString = v.uri, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = v.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("شكل الموجة الصوتية (Audio Waveform):", color = Color(0xFF81D4FA), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0E1230))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waveform.forEachIndexed { idx, amp ->
                            val normPos = idx.toFloat() / waveform.size.toFloat()
                            val inRange = normPos >= (startSeconds / maxDur) && normPos <= (endSeconds / maxDur)
                            val barHeight = (amp * 40).dp.coerceAtLeast(6.dp)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (inRange) Color(0xFFFFC857) else Color(0xFF363D80))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "نطاق النغمة: ${MediaScanner.formatDuration((startSeconds * 1000).toLong())} إلى ${MediaScanner.formatDuration((endSeconds * 1000).toLong())} (المدة: ${((endSeconds - startSeconds).toInt())} ثانية)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = startSeconds,
                    onValueChange = {
                        startSeconds = it
                        if (endSeconds <= it + 5f) endSeconds = (it + 15f).coerceAtMost(maxDur)
                    },
                    valueRange = 0f..(maxDur - 5f).coerceAtLeast(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFC857), activeTrackColor = Color(0xFFFFC857)),
                    enabled = !isCutting
                )

                Slider(
                    value = endSeconds,
                    onValueChange = {
                        endSeconds = it
                        if (startSeconds >= it - 5f) startSeconds = (it - 15f).coerceAtLeast(0f)
                    },
                    valueRange = 5f..maxDur,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF4081), activeTrackColor = Color(0xFFFF4081)),
                    enabled = !isCutting
                )

                if (isCutting) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFFC857),
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(statusText, color = Color(0xFFFFC857), fontSize = 11.sp)
                }

                result?.let { res ->
                    if (res.success) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🔔 تم حفظ النغمة بنجاح في مجلد النغمات!", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("المدة: ${res.durationFormatted} | الحجم: ${res.fileSizeFormatted}", color = Color.White, fontSize = 11.sp)
                                Text("المسار: Ringtones/CinemaX_Ringtones", color = Color(0xFF80CBC4), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        res.uri?.let { uri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/*"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة النغمة"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة النغمة عبر واتساب / التطبيقات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (res.errorMessage != null) {
                        Text(res.errorMessage, color = Color(0xFFFF6B8B), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val vid = selectedVideo ?: return@Button
                    isCutting = true
                    result = null
                    scope.launch {
                        val startMs = (startSeconds * 1000L).toLong()
                        val endMs = (endSeconds * 1000L).toLong()
                        val res = RingtoneMakerStudio.cutRingtone(
                            context = context,
                            sourceUri = Uri.parse(vid.uri),
                            ringtoneTitle = ringtoneTitle,
                            startMs = startMs,
                            endMs = endMs,
                            onProgress = { p, st ->
                                progress = p
                                statusText = st
                            }
                        )
                        result = res
                        isCutting = false
                    }
                },
                enabled = !isCutting && selectedVideo != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color.Black)
            ) {
                Text(if (isCutting) "جاري الاقتطاع..." else "قص وحفظ النغمة 🔔", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCutting) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}

/**
 * 3. Smart Offline Video Compressor Dialog
 */
@Composable
fun VideoCompressorDialog(
    videos: List<LocalVideo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideo by remember { mutableStateOf(videos.firstOrNull()) }
    var selectedProfile by remember { mutableStateOf(VideoCompressorStudio.CompressionProfile.BALANCED) }
    var isCompressing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<VideoCompressorStudio.CompressionResult?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isCompressing) onDismiss() },
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x3369F0AE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF5EF0BE), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("ضاغط الفيديوهات وموفر المساحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("اختر الفيديو المطلوب ضغطه بدون إنترنت لتوفير المساحة:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videos.take(15)) { v ->
                        val isSel = v.id == selectedVideo?.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF5EF0BE) else Color(0xFF272D66)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = !isCompressing) {
                                selectedVideo = v
                                result = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VideoThumbnailImage(uriString = v.uri, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = v.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSel) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                    Text(
                                        text = MediaScanner.formatFileSize(v.sizeBytes),
                                        color = if (isSel) Color(0xFF1B5E20) else Color(0xFF81C784),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("اختر نمط الضغط وتوفير المساحة:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                VideoCompressorStudio.CompressionProfile.values().forEach { profile ->
                    val isSel = selectedProfile == profile
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF272D66) else Color(0xFF161F30)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isCompressing) { selectedProfile = profile }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) Color(0xFF5EF0BE) else Color(0xFF363D80)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(profile.titleAr, color = if (isSel) Color(0xFF5EF0BE) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(profile.descriptionAr, color = Color(0xFFA9B4D6), fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isCompressing) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF5EF0BE),
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(statusText, color = Color(0xFF5EF0BE), fontSize = 11.sp)
                }

                result?.let { res ->
                    if (res.success) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("💾 تم ضغط الفيديو وتوفير مساحة حقيقية!", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("الحجم الأصلي: ${res.originalSizeFormatted} ⬅ الحجم الجديد: ${res.compressedSizeFormatted}", color = Color.White, fontSize = 11.sp)
                                Text("المساحة الموفرة: ${res.savedSpaceFormatted} (${res.savedPercentage}% توفير)", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("المسار: Movies/CinemaX_Compressed", color = Color(0xFF80CBC4), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        res.uri?.let { uri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو المضغوط عبر واتساب"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة الفيديو المضغوط الخفيف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (res.errorMessage != null) {
                        Text(res.errorMessage, color = Color(0xFFFF6B8B), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val vid = selectedVideo ?: return@Button
                    isCompressing = true
                    result = null
                    scope.launch {
                        val res = VideoCompressorStudio.compressVideo(
                            context = context,
                            sourceUri = Uri.parse(vid.uri),
                            videoTitle = vid.title,
                            originalSizeBytes = vid.sizeBytes,
                            profile = selectedProfile,
                            onProgress = { p, st ->
                                progress = p
                                statusText = st
                            }
                        )
                        result = res
                        isCompressing = false
                    }
                },
                enabled = !isCompressing && selectedVideo != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5EF0BE), contentColor = Color.Black)
            ) {
                Text(if (isCompressing) "جاري الضغط..." else "بدء الضغط وتوفير المساحة 💾", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCompressing) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}

/**
 * 4. Lossless Video Audio & Music Remover Dialog (Silent Video Maker)
 */
@Composable
fun VideoAudioRemoverDialog(
    videos: List<LocalVideo>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideo by remember { mutableStateOf(videos.firstOrNull()) }
    var isRemoving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<VideoMuteAudioRemover.MuteResult?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isRemoving) onDismiss() },
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = null, tint = Color(0xFFFF6B8B), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("إزالة الموسيقى وكتم الصوت من الفيديو", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("اختر الفيديو المطلوب إزالة كافة مسارات الصوت والموسيقى منه لإنشاء فيديو صامت نقي 100% بدون فقدان للجودة وبسرعة فائقة:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videos.take(15)) { v ->
                        val isSel = v.id == selectedVideo?.id
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFFFF6B8B) else Color(0xFF272D66)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(enabled = !isRemoving) {
                                selectedVideo = v
                                result = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VideoThumbnailImage(uriString = v.uri, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = v.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isRemoving) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF6B8B),
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(statusText, color = Color(0xFFFF6B8B), fontSize = 11.sp)
                }

                result?.let { res ->
                    if (res.success) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🔇 تم كتم الصوت بنجاح بدون إعادة ترميز!", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("المدة: ${res.durationFormatted} | الحجم: ${res.fileSizeFormatted}", color = Color.White, fontSize = 11.sp)
                                Text("المسار: Movies/CinemaX_Silent", color = Color(0xFF80CBC4), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        res.uri?.let { uri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو الصامت"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة الفيديو الصامت الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (res.errorMessage != null) {
                        Text(res.errorMessage, color = Color(0xFFFF6B8B), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val vid = selectedVideo ?: return@Button
                    isRemoving = true
                    result = null
                    scope.launch {
                        val res = VideoMuteAudioRemover.removeAudioFromVideo(
                            context = context,
                            sourceUri = Uri.parse(vid.uri),
                            videoTitle = vid.title,
                            onProgress = { p, st ->
                                progress = p
                                statusText = st
                            }
                        )
                        result = res
                        isRemoving = false
                    }
                },
                enabled = !isRemoving && selectedVideo != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White)
            ) {
                Text(if (isRemoving) "جاري الكتم..." else "كتم الصوت وحفظ الفيديو 🔇", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRemoving) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}
