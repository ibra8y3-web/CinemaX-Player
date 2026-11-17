package com.example.ui.screens.music

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.MediaScanner
import com.example.media.RingtoneMakerStudio
import com.example.media.model.LocalAudio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Universal Audio Share Helper
 */
fun shareAudioFile(context: Context, audio: LocalAudio) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, audio.contentUri)
            putExtra(Intent.EXTRA_SUBJECT, audio.title)
            putExtra(Intent.EXTRA_TEXT, "استمع إلى: ${audio.title} - ${audio.artist}\nعبر CinemaX Player 🎵")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الأغنية عبر"))
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر مشاركة المقطع: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Audio Cutter & Ringtone Maker Dialog
 */
@Composable
fun AudioCutterDialog(
    audio: LocalAudio,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalDurSec = (audio.durationMs / 1000f).coerceAtLeast(3f)
    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf((30f).coerceAtMost(totalDurSec)) }
    var ringtoneTitle by remember {
        mutableStateOf("نغمة_${audio.title.take(18).trim()}")
    }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    var isCutting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var cutResult by remember { mutableStateOf<RingtoneMakerStudio.RingtoneResult?>(null) }

    val waveform = remember(audio.id, audio.title) {
        RingtoneMakerStudio.generateWaveformPoints(32, audio.title)
    }

    // Stop and release preview player on dispose
    DisposableEffect(Unit) {
        onDispose {
            previewJob?.cancel()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPreviewPlaying = false
    }

    fun togglePreview() {
        if (isPreviewPlaying) {
            stopPreview()
        } else {
            try {
                stopPreview()
                val mp = MediaPlayer().apply {
                    setDataSource(context, audio.contentUri)
                    prepare()
                    seekTo((startSec * 1000L).toInt())
                    start()
                }
                mediaPlayer = mp
                isPreviewPlaying = true

                previewJob = scope.launch {
                    val durationToPlayMs = ((endSec - startSec) * 1000L).toLong().coerceAtLeast(500L)
                    val startTime = System.currentTimeMillis()
                    while (isActive && isPreviewPlaying) {
                        delay(100)
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed >= durationToPlayMs) {
                            stopPreview()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                stopPreview()
                Toast.makeText(context, "تعذر تشغيل المعاينة: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isCutting) {
                stopPreview()
                onDismiss()
            }
        },
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFD54F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = Color(0xFFFFC857),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "استوديو قص الموسيقى والنغمات",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        audio.title,
                        color = Color(0xFFA9B4D6),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Interactive Waveform Visualization
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            waveform.forEachIndexed { idx, amp ->
                                val pointTimeRatio = idx.toFloat() / waveform.size.toFloat()
                                val inRange = pointTimeRatio >= (startSec / totalDurSec) &&
                                        pointTimeRatio <= (endSec / totalDurSec)
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(((42 * amp).dp).coerceAtLeast(6.dp))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (inRange) Color(0xFFFFC857) else Color(0xFF475569)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "البداية: ${MediaScanner.formatDuration((startSec * 1000L).toLong())}",
                                color = Color(0xFFFFC857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "المحدد: ${String.format("%.1f", endSec - startSec)} ثانية",
                                color = Color(0xFF22E3FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "النهاية: ${MediaScanner.formatDuration((endSec * 1000L).toLong())}",
                                color = Color(0xFFFFC857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Start Slider
                Text(
                    "نقطة البداية: ${MediaScanner.formatDuration((startSec * 1000L).toLong())}",
                    color = Color(0xFFA9B4D6),
                    fontSize = 11.sp
                )
                Slider(
                    value = startSec,
                    onValueChange = {
                        startSec = it.coerceAtMost(endSec - 1f)
                    },
                    valueRange = 0f..totalDurSec,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFC857),
                        activeTrackColor = Color(0xFFFFC857),
                        inactiveTrackColor = Color(0xFF363D80)
                    )
                )

                // End Slider
                Text(
                    "نقطة النهاية: ${MediaScanner.formatDuration((endSec * 1000L).toLong())}",
                    color = Color(0xFFA9B4D6),
                    fontSize = 11.sp
                )
                Slider(
                    value = endSec,
                    onValueChange = {
                        endSec = it.coerceAtLeast(startSec + 1f)
                    },
                    valueRange = 0f..totalDurSec,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF22E3FF),
                        activeTrackColor = Color(0xFF22E3FF),
                        inactiveTrackColor = Color(0xFF363D80)
                    )
                )

                // Output Title input
                OutlinedTextField(
                    value = ringtoneTitle,
                    onValueChange = { ringtoneTitle = it },
                    label = { Text("اسم النغمة / المقطع المقتطع", color = Color(0xFFA9B4D6), fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFC857),
                        unfocusedBorderColor = Color(0xFF363D80),
                        focusedContainerColor = Color(0xFF161B42),
                        unfocusedContainerColor = Color(0xFF161B42)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Audition Preview Button
                OutlinedButton(
                    onClick = { togglePreview() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPreviewPlaying) Color(0xFFFF6B8B) else Color(0xFFFFC857)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isPreviewPlaying) "إيقاف المعاينة" else "استماع ومعاينة المقطع المحدد 🎧",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Cutting Progress
                if (isCutting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFFFFC857),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(statusText, color = Color(0xFF22E3FF), fontSize = 11.sp)
                }

                // Result Card
                cutResult?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    if (res.success) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF143026)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2EE6A6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "تم قص وحفظ النغمة بنجاح!",
                                        color = Color(0xFF2EE6A6),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text("المدة: ${res.durationFormatted} | الحجم: ${res.fileSizeFormatted}", color = Color.White, fontSize = 11.sp)
                                Text("المسار: Ringtones & Music / CinemaX", color = Color(0xFF80CBC4), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        res.uri?.let { uri ->
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/*"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, ringtoneTitle)
                                                putExtra(Intent.EXTRA_TEXT, "نغمة مقتطعة: $ringtoneTitle عبر CinemaX Player ✂️")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة النغمة المقتطعة عبر"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة المقطع المقتطع فوراً 📤", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    stopPreview()
                    isCutting = true
                    cutResult = null
                    scope.launch {
                        val startMs = (startSec * 1000L).toLong()
                        val endMs = (endSec * 1000L).toLong()
                        val res = RingtoneMakerStudio.cutRingtone(
                            context = context,
                            sourceUri = audio.contentUri,
                            ringtoneTitle = ringtoneTitle,
                            startMs = startMs,
                            endMs = endMs,
                            onProgress = { p, st ->
                                progress = p
                                statusText = st
                            }
                        )
                        cutResult = res
                        isCutting = false
                    }
                },
                enabled = !isCutting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC857),
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("confirm_audio_cut_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isCutting) "جاري القص..." else "قص وحفظ النغمة ✂️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    stopPreview()
                    onDismiss()
                },
                enabled = !isCutting
            ) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}
