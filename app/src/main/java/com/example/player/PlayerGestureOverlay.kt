package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.MediaScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class KidsBubble(
    val id: Long,
    var x: Float,
    var y: Float,
    val baseRadius: Float,
    val color: Color,
    val birthTime: Long
)

@Composable
fun PlayerGestureOverlay(
    isLocked: Boolean,
    durationMs: Long,
    currentPosMs: Long,
    onSeek: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier,
    zoomScale: Float = 1.0f,
    onZoomChange: (Float) -> Unit = {},
    panOffset: Offset = Offset.Zero,
    onPanChange: (Offset) -> Unit = {},
    onUnlock: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    var currentBrightness by remember {
        val window = activity?.window
        val cur = window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (cur < 0) 0.5f else cur)
    }

    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) } // "left" or "right"

    // Kids Lock interactive bubbles & tap counter
    val kidsBubbles = remember { mutableStateListOf<KidsBubble>() }
    var unlockTaps by remember { mutableIntStateOf(0) }
    var unlockResetJob by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()
    val latestCurrentPosMs by rememberUpdatedState(currentPosMs)
    val latestOnSeek by rememberUpdatedState(onSeek)
    val latestOnSeekRelative by rememberUpdatedState(onSeekRelative)
    val latestOnToggleControls by rememberUpdatedState(onToggleControls)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)
    val latestOnPanChange by rememberUpdatedState(onPanChange)
    val latestOnUnlock by rememberUpdatedState(onUnlock)
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun triggerHideTimer() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(1200)
            showVolumeOverlay = false
            showBrightnessOverlay = false
            showSeekOverlay = false
            doubleTapFeedback = null
        }
    }

    // Bubbles animation frame ticker when locked
    LaunchedEffect(isLocked) {
        if (isLocked) {
            unlockTaps = 0
            while (isActive) {
                delay(30)
                val now = System.currentTimeMillis()
                val iterator = kidsBubbles.iterator()
                while (iterator.hasNext()) {
                    val bubble = iterator.next()
                    bubble.y -= 4f // Float upwards
                    bubble.x += (Random.nextFloat() - 0.5f) * 2f
                    if (now - bubble.birthTime > 2500) {
                        iterator.remove()
                    }
                }
            }
        } else {
            kidsBubbles.clear()
            unlockTaps = 0
        }
    }

    fun handleKidsTap(pos: Offset) {
        val bubbleColors = listOf(
            Color(0xFFFF4081), Color(0xFF22E3FF), Color(0xFFFFC857),
            Color(0xFF5EF0BE), Color(0xFFB388FF), Color(0xFFFFAB40)
        )
        val newBubble = KidsBubble(
            id = System.nanoTime(),
            x = pos.x,
            y = pos.y,
            baseRadius = Random.nextFloat() * 25f + 25f,
            color = bubbleColors.random(),
            birthTime = System.currentTimeMillis()
        )
        kidsBubbles.add(newBubble)

        unlockTaps++
        unlockResetJob?.cancel()
        if (unlockTaps >= 4) {
            unlockTaps = 0
            kidsBubbles.clear()
            latestOnUnlock()
        } else {
            unlockResetJob = scope.launch {
                delay(3000)
                unlockTaps = 0
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Gesture detector is a sibling of the control layer. This ensures
        // controls receive taps before the full-screen gesture surface does.
        Box(
            modifier = Modifier
                .fillMaxSize()
            // 1. Pinch to Zoom & Pan transform gestures (when unlocked)
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1.0f) {
                        val newScale = (zoomScale * zoom).coerceIn(1.0f, 3.5f)
                        latestOnZoomChange(newScale)
                    }
                    if (zoomScale > 1.05f) {
                        val newPan = panOffset + pan
                        latestOnPanChange(newPan)
                    } else {
                        latestOnPanChange(Offset.Zero)
                    }
                }
            }
            // 2. Tap & Double-tap gestures
            .pointerInput(isLocked, zoomScale) {
                if (isLocked) {
                    detectTapGestures(
                        onTap = { offset ->
                            handleKidsTap(offset)
                        }
                    )
                } else {
                    detectTapGestures(
                        onTap = { latestOnToggleControls() },
                        onDoubleTap = { offset ->
                            if (zoomScale > 1.05f) {
                                // Reset zoom on double tap
                                latestOnZoomChange(1.0f)
                                latestOnPanChange(Offset.Zero)
                            } else {
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.35f) {
                                    latestOnSeekRelative(-10_000L)
                                    doubleTapFeedback = "left"
                                    triggerHideTimer()
                                } else if (offset.x > screenWidth * 0.65f) {
                                    latestOnSeekRelative(10_000L)
                                    doubleTapFeedback = "right"
                                    triggerHideTimer()
                                } else {
                                    // Double tap center quick-zooms to 2.0x
                                    latestOnZoomChange(2.0f)
                                }
                            }
                        }
                    )
                }
            }
            // 3. Single-finger drag for volume, brightness, seek (when not locked and not zoomed)
            .pointerInput(isLocked, zoomScale, durationMs) {
                if (isLocked || zoomScale > 1.1f) return@pointerInput

                var dragMode = 0 // 0: undecided, 1: brightness, 2: volume, 3: horizontal seek
                var totalDragX = 0f
                var totalDragY = 0f

                detectDragGestures(
                    onDragStart = {
                        dragMode = 0
                        totalDragX = 0f
                        totalDragY = 0f
                        seekDeltaMs = 0L
                    },
                    onDragEnd = {
                        if (dragMode == 3 && seekDeltaMs != 0L) {
                            latestOnSeek((latestCurrentPosMs + seekDeltaMs).coerceIn(0L, durationMs))
                        }
                        triggerHideTimer()
                    },
                    onDragCancel = {
                        triggerHideTimer()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (dragMode == 0) {
                            if (abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > 20f) {
                                dragMode = 3 // Horizontal seek
                            } else if (abs(totalDragY) > 20f) {
                                val touchX = change.position.x
                                dragMode = if (touchX < size.width * 0.5f) 1 else 2
                            }
                        }

                        when (dragMode) {
                            1 -> { // Brightness (Left side vertical drag)
                                val delta = -dragAmount.y / (size.height * 0.6f)
                                currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
                                activity?.window?.let { window ->
                                    val lp = window.attributes
                                    lp.screenBrightness = currentBrightness
                                    window.attributes = lp
                                }
                                showBrightnessOverlay = true
                                showVolumeOverlay = false
                                showSeekOverlay = false
                            }
                            2 -> { // Volume (Right side vertical drag)
                                val delta = -dragAmount.y / (size.height * 0.6f)
                                currentVolume = (currentVolume + delta).coerceIn(0f, 1f)
                                val targetVol = (currentVolume * maxVolume).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                showVolumeOverlay = true
                                showBrightnessOverlay = false
                                showSeekOverlay = false
                            }
                            3 -> { // Horizontal seek
                                if (durationMs > 0) {
                                    val factor = durationMs.coerceAtMost(300_000L) / size.width
                                    seekDeltaMs = (totalDragX * factor).toLong()
                                    showSeekOverlay = true
                                    showBrightnessOverlay = false
                                    showVolumeOverlay = false
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // Animated Kids Lock Bubbles Canvas
        if (isLocked && kidsBubbles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val now = System.currentTimeMillis()
                for (b in kidsBubbles) {
                    val age = now - b.birthTime
                    val alpha = (1f - (age.toFloat() / 2500f)).coerceIn(0f, 0.85f)
                    drawCircle(
                        color = b.color.copy(alpha = alpha),
                        radius = b.baseRadius,
                        center = Offset(b.x, b.y)
                    )
                    // Inner glowing highlight
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.7f),
                        radius = b.baseRadius * 0.35f,
                        center = Offset(b.x - b.baseRadius * 0.3f, b.y - b.baseRadius * 0.3f)
                    )
                }
            }
        }

        // Kids Lock Indicator & 4-Tap Child-Proof Unlock Bar
        if (isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xD90B0F19))
                    .clickable { handleKidsTap(Offset(200f, 200f)) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "قفل الأطفال",
                        tint = Color(0xFFFF6B8B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (unlockTaps > 0) "قفل الأطفال: انقر ${4 - unlockTaps} مرات إضافية لإلغاء القفل 🎈" else "قفل الأطفال نشط (المس الشاشة لفقاعات ملونة 🎈 | 4 نقرات للفتح)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pinch to Zoom active pill badge
        if (!isLocked && zoomScale > 1.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC000000))
                    .clickable {
                        latestOnZoomChange(1.0f)
                        latestOnPanChange(Offset.Zero)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تكبير ${String.format("%.1f", zoomScale)}x (انقر مرتين لإعادة الضبط 1.0x)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Volume gesture indicator
        AnimatedVisibility(
            visible = showVolumeOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GesturePillIndicator(
                icon = { Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF22E3FF)) },
                text = "${(currentVolume * 100).toInt()}%",
                progress = currentVolume
            )
        }

        // Brightness gesture indicator
        AnimatedVisibility(
            visible = showBrightnessOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GesturePillIndicator(
                icon = { Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color(0xFFFFC857)) },
                text = "${(currentBrightness * 100).toInt()}%",
                progress = currentBrightness
            )
        }

        // Horizontal seek preview indicator
        AnimatedVisibility(
            visible = showSeekOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val targetPos = (currentPosMs + seekDeltaMs).coerceIn(0L, durationMs)
            val prefix = if (seekDeltaMs >= 0) "+ " else "- "
            val formattedDelta = MediaScanner.formatDuration(abs(seekDeltaMs))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0B0F19))
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$prefix$formattedDelta",
                        color = if (seekDeltaMs >= 0) Color(0xFF22E3FF) else Color(0xFFFF6B8B),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${MediaScanner.formatDuration(targetPos)} / ${MediaScanner.formatDuration(durationMs)}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Double-tap visual feedback (Left -10s)
        AnimatedVisibility(
            visible = doubleTapFeedback == "left",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("-10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Double-tap visual feedback (Right +10s)
        AnimatedVisibility(
            visible = doubleTapFeedback == "right",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("+10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    // Controls are a later sibling above the full-screen gesture surface.
    // This keeps taps on the menu, scrubber, and action buttons out of the
    // gesture detector that is only meant for the video viewport.
    content()
    }
}

@Composable
private fun GesturePillIndicator(
    icon: @Composable () -> Unit,
    text: String,
    progress: Float
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC0B0F19))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(110.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(100.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF22E3FF),
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}
