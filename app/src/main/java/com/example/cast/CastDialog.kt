package com.example.cast

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.model.LocalVideo

@Composable
fun CastDialog(
    castManager: CastManager,
    currentVideo: LocalVideo? = null,
    currentMediaUri: String? = null,
    currentTitle: String? = null,
    currentPositionMs: Long = 0L,
    isLiveStream: Boolean = false,
    onDismissRequest: () -> Unit
) {
    val isCasting by castManager.isCasting.collectAsState()
    val connectedDeviceName by castManager.connectedDeviceName.collectAsState()
    val availableDevices by castManager.availableDevices.collectAsState()
    val isRemotePlaying by castManager.isRemotePlaying.collectAsState()
    val remotePositionMs by castManager.remotePlaybackPositionMs.collectAsState()
    val remoteDurationMs by castManager.remoteDurationMs.collectAsState()
    val currentCastingTitle by castManager.currentMediaTitle.collectAsState()

    var volumeLevel by remember { mutableFloatStateOf(0.8f) }

    DisposableEffect(Unit) {
        castManager.startDeviceScanning()
        onDispose {
            castManager.stopDeviceScanning()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCasting) Color(0xFF22E3FF).copy(alpha = 0.2f)
                                else Color(0xFF1A1F4A)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                            contentDescription = null,
                            tint = if (isCasting) Color(0xFF22E3FF) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isCasting) "متصل بالشاشة (Google Cast)" else "بث إلى التلفاز والشاشات",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCasting) "Chromecast نشط حالياً" else "بحث عن أجهزة على شبكة Wi-Fi المحلية",
                            color = if (isCasting) Color(0xFF22E3FF) else Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF9CA3AF))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (isCasting) {
                    // Connected Remote Control Panel
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(pulseScale)
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF22E3FF).copy(alpha = 0.35f), Color.Transparent)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color(0xFF22E3FF),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = connectedDeviceName ?: "جهاز Chromecast",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = currentCastingTitle.ifEmpty { currentTitle ?: "جاري البث..." },
                                color = Color(0xFFA9B4D6),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Playback controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { castManager.seekRelative(-10000) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF141943))
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = "رجوع 10 ثواني", tint = Color.White)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = { castManager.togglePlayPause() },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22E3FF))
                                        .testTag("cast_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRemotePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isRemotePlaying) "إيقاف مؤقت" else "تشغيل",
                                        tint = Color(0xFF0E1230),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = { castManager.seekRelative(10000) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF141943))
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = "تقديم 10 ثواني", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Volume slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VolumeDown, contentDescription = null, tint = Color(0xFFA9B4D6), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(
                                    value = volumeLevel,
                                    onValueChange = {
                                        volumeLevel = it
                                        castManager.setVolume(it.toDouble())
                                    },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF22E3FF),
                                        activeTrackColor = Color(0xFF22E3FF),
                                        inactiveTrackColor = Color(0xFF363D80)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFA9B4D6), modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    castManager.disconnect()
                                    onDismissRequest()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("cast_disconnect_button")
                            ) {
                                Text("قطع الاتصال بالشاشة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Available devices list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الأجهزة المكتشفة (${availableDevices.size})",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { castManager.refreshAvailableDevices() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (availableDevices.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .scale(pulseScale)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22E3FF).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color(0xFF22E3FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "جاري البحث عن أجهزة Chromecast...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تأكد من أن التلفاز الذكي أو جهاز Chromecast متصل بنفس شبكة الـ Wi-Fi مع هاتفك.",
                                    color = Color(0xFFA9B4D6),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(availableDevices, key = { it.id }) { device ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (device.isSelected) Color(0xFF1E3A5F) else Color(0xFF272D66)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            castManager.selectDevice(device)
                                            // Trigger playback to this device
                                            val targetUri = currentMediaUri ?: currentVideo?.path ?: ""
                                            val targetTitle = currentTitle ?: currentVideo?.title ?: "CinemaX Player Stream"
                                            if (targetUri.isNotEmpty()) {
                                                castManager.castMedia(
                                                    uriString = targetUri,
                                                    title = targetTitle,
                                                    localVideo = currentVideo,
                                                    startPositionMs = currentPositionMs,
                                                    isLiveStream = isLiveStream
                                                )
                                            }
                                        }
                                        .testTag("cast_device_${device.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF141943)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Tv,
                                                    contentDescription = null,
                                                    tint = if (device.isSelected) Color(0xFF22E3FF) else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = device.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (device.description.isNotEmpty()) {
                                                    Text(
                                                        text = device.description,
                                                        color = Color(0xFFA9B4D6),
                                                        fontSize = 10.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                castManager.selectDevice(device)
                                                val targetUri = currentMediaUri ?: currentVideo?.path ?: ""
                                                val targetTitle = currentTitle ?: currentVideo?.title ?: "CinemaX Player Stream"
                                                if (targetUri.isNotEmpty()) {
                                                    castManager.castMedia(
                                                        uriString = targetUri,
                                                        title = targetTitle,
                                                        localVideo = currentVideo,
                                                        startPositionMs = currentPositionMs,
                                                        isLiveStream = isLiveStream
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF22E3FF),
                                                contentColor = Color(0xFF0E1230)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("بث الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1F4A), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إغلاق")
            }
        }
    )
}
