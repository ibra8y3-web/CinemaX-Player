package com.example.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import com.example.cast.CastButton
import com.example.cast.CastDialog
import com.example.cast.CastManager
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import com.example.player.sheets.VideoQualitySheet
import com.example.player.sheets.SubtitlesLiveSheet
import com.example.player.sheets.AudioStudioSheet
import com.example.player.components.LiveSubtitleOverlay
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ai.AiEnhanceMode
import com.example.media.MediaScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    playerManager: NovaPlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    castManager: CastManager? = null,
    isInPipMode: Boolean = false,
    onEnterPiP: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPos by playerManager.currentPositionMs.collectAsState()
    val duration by playerManager.durationMs.collectAsState()
    val speed by playerManager.playbackSpeed.collectAsState()
    val isLocked by playerManager.isScreenLocked.collectAsState()
    val resizeMode by playerManager.resizeMode.collectAsState()
    val isLooping by playerManager.isLooping.collectAsState()
    val videoRepeatMode by playerManager.videoRepeatMode.collectAsState()
    val pointA by playerManager.repeatPointA.collectAsState()
    val pointB by playerManager.repeatPointB.collectAsState()
    val aiFilter by playerManager.aiFilterMode.collectAsState()
    val isAudioOnly by playerManager.isAudioOnly.collectAsState()
    val isYouTubeStream by playerManager.isYouTubeStream.collectAsState()
    val youTubeVideoId by playerManager.youTubeVideoId.collectAsState()
    val currentMediaUri by playerManager.videoUri.collectAsState()
    val videoTitle by playerManager.videoTitle.collectAsState()
    val isLive by playerManager.isLive.collectAsState()
    val audioBoost by playerManager.audioBoostLevel.collectAsState()
    val playbackError by playerManager.playbackError.collectAsState()
    val networkStreamInfo by playerManager.networkStreamInfo.collectAsState()
    val streamProtocol by playerManager.currentStreamProtocol.collectAsState()
    val sleepTimerMinutes by playerManager.sleepTimerMinutes.collectAsState()
    val sleepTimerRemainingSeconds by playerManager.sleepTimerRemainingSeconds.collectAsState()
    val sleepTimerWarning by playerManager.sleepTimerWarning.collectAsState()
    val autoPlayState by playerManager.autoPlayState.collectAsState()
    val isAutoPlayEnabled by playerManager.isAutoPlayEnabled.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentPlaylistIndex by playerManager.currentPlaylistIndex.collectAsState()

    val availableQualities by playerManager.availableQualities.collectAsState()
    val selectedQuality by playerManager.selectedQuality.collectAsState()
    val videoTechStats by playerManager.videoTechStats.collectAsState()
    val availableSubtitleTracks by playerManager.availableSubtitleTracks.collectAsState()
    val selectedSubtitleTrack by playerManager.selectedSubtitleTrack.collectAsState()
    val activeSubtitleCueText by playerManager.activeSubtitleCueText.collectAsState()
    val translatedSubtitleCueText by playerManager.translatedSubtitleCueText.collectAsState()
    val subtitleStyle by playerManager.subtitleStyle.collectAsState()
    val audioStudioState by playerManager.audioStudio.state.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var hideControlsJob by remember { mutableStateOf<Job?>(null) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAudioBoostSheet by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAbRepeatBar by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitlesSheet by remember { mutableStateOf(false) }
    var showAudioStudioSheet by remember { mutableStateOf(false) }
    var showCinemaFiltersSheet by remember { mutableStateOf(false) }
    var showCastDialog by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BackHandler(enabled = !isInPipMode) {
        onBack()
    }

    fun scheduleHideControls() {
        hideControlsJob?.cancel()
        if (isPlaying && !isLocked) {
            hideControlsJob = scope.launch {
                delay(4000)
                showControls = false
            }
        }
    }

    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            scheduleHideControls()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Keep player state or pause if desired
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Player Surface or Audio-only placeholder
        if (isAudioOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF272D66), Color(0xFF0E1230))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0x3322E3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            tint = Color(0xFF22E3FF),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "وضع الصوت في الخلفية (Audio Only)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = videoTitle,
                        color = Color(0xFFC3CCE8),
                        fontSize = 14.sp
                    )
                }
            }
        } else if (isYouTubeStream && youTubeVideoId != null) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = android.webkit.WebChromeClient()
                        webViewClient = android.webkit.WebViewClient()
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    * { margin:0; padding:0; box-sizing:border-box; }
                                    html, body { width:100%; height:100%; background:#000; overflow:hidden; display:flex; align-items:center; justify-content:center; }
                                    iframe { width:100%; height:100%; border:none; }
                                </style>
                            </head>
                            <body>
                                <iframe 
                                    src="https://www.youtube-nocookie.com/embed/${youTubeVideoId}?autoplay=1&playsinline=1&controls=1&rel=0&modestbranding=1" 
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                    allowfullscreen>
                                </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.exoPlayer
                        useController = false
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerViewRef = this
                    }
                },
                update = { view ->
                    view.player = playerManager.exoPlayer
                    view.resizeMode = resizeMode
                    playerViewRef = view
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = panOffset.x,
                        translationY = panOffset.y
                    )
            )
        }

        // 🎬 Live Cinema HDR & Video Filter Overlays (Real Visible Color Grading & Visual Atmosphere)
        when (aiFilter) {
            AiEnhanceMode.AMOLED_PURE_BLACK -> {
                // AMOLED Pure Black: Deep edge vignette & ultra-contrast shadow deepening
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color(0x40FFFFFF), Color(0x990E1230))
                            )
                        )
                )
            }
            AiEnhanceMode.CINEMA_WARM -> {
                // Hollywood warm golden cinematic color grading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x38FF8F00))
                )
            }
            AiEnhanceMode.NIGHT_SHIELD -> {
                // Night comfort blue-light blocker amber shield
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x42FFA000))
                )
            }
            AiEnhanceMode.VIVID_SUPER -> {
                // Vivid super-clarity cyan contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x2822E3FF))
                )
            }
            AiEnhanceMode.NOIR_DRAMATIC -> {
                // Cinematic Noir classic B&W atmospheric tint with dark letterbox
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xAA000000), Color.Transparent, Color.Transparent, Color(0xAA000000))
                            )
                        )
                )
            }
            AiEnhanceMode.CYBERPUNK_NEON -> {
                // Cyberpunk Neon dual tint (cyan & magenta)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0x3522E3FF), Color(0x30E040FB))
                            )
                        )
                )
            }
            AiEnhanceMode.VINTAGE_SEPIA -> {
                // Vintage 70s warm sepia film tone
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x45704214))
                )
            }
            AiEnhanceMode.COLD_SCIFI -> {
                // Cold Sci-Fi space electric blue
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x3500B0FF))
                )
            }
            AiEnhanceMode.EMERALD_NATURE -> {
                // Vibrant Emerald nature green
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x3000E676))
                )
            }
            AiEnhanceMode.SUNSET_GOLD -> {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x35FF6D00), Color(0x25FFD600)))))
            }
            AiEnhanceMode.MATTE_CINEMA -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x33475569)))
            }
            AiEnhanceMode.DEEP_BLUES -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x351E3A8A)))
            }
            AiEnhanceMode.ROSE_GOLD -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x35F43F5E)))
            }
            AiEnhanceMode.MATRIX_GREEN -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x3510B981)))
            }
            AiEnhanceMode.AUTUMN_ORANGE -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x35D97706)))
            }
            AiEnhanceMode.ARCTIC_FROST -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x3593C5FD)))
            }
            AiEnhanceMode.CHOCOLATE_BROWN -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x3578350F)))
            }
            AiEnhanceMode.NEON_PURPLE -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x359333EA)))
            }
            AiEnhanceMode.VINTAGE_8MM -> {
                Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.Transparent, Color(0x55000000)))))
            }
            AiEnhanceMode.HDR_ULTRA_BOOST -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x25FFFFFF)))
            }
            AiEnhanceMode.TEAL_AND_ORANGE -> {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x300EA5E9), Color(0x30F97316)))))
            }
            AiEnhanceMode.CYAN_DREAM -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x3006B6D4)))
            }
            AiEnhanceMode.MOONLIGHT_SILVER -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x3064748B)))
            }
            AiEnhanceMode.VAMPIRE_BLOOD_RED -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x40990000)))
            }
            AiEnhanceMode.STANDARD -> { /* No filter */ }
        }

        // Touch Gestures Overlay (Volume, Brightness, Seek, Pinch-to-Zoom 350%, Kids Lock)
        PlayerGestureOverlay(
            isLocked = isLocked,
            durationMs = duration,
            currentPosMs = currentPos,
            zoomScale = zoomScale,
            onZoomChange = { zoomScale = it },
            panOffset = panOffset,
            onPanChange = { panOffset = it },
            onUnlock = { playerManager.toggleScreenLock() },
            onSeek = { target -> playerManager.seekTo(target) },
            onSeekRelative = { delta ->
                if (delta > 0) playerManager.seekForward(delta)
                else playerManager.seekBackward(-delta)
            },
            onToggleControls = {
                showControls = !showControls
                if (showControls) scheduleHideControls()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // Interactive UI Controls Overlay
            AnimatedVisibility(
                visible = showControls && !isInPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // Top Bar Overlay - Firmly docked to top edge with statusBarsPadding
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xF0000000), Color(0x990E1230), Color.Transparent)
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(42.dp).testTag("player_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = videoTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLive) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF1744))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "مباشر ($streamProtocol)", color = Color(0xFFFF1744), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                        if (networkStreamInfo.currentResolution.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${networkStreamInfo.currentResolution}",
                                                color = Color(0xFF22E3FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (networkStreamInfo.status == NetworkHealthStatus.POOR) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.SignalCellularAlt,
                                                contentDescription = "شبكة ضعيفة",
                                                tint = Color(0xFFFFC857),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isLocked) {
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Video Quality quick button
                                        IconButton(
                                            onClick = { showQualitySheet = true },
                                            modifier = Modifier.size(36.dp).testTag("video_quality_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HighQuality,
                                                contentDescription = "جودة الفيديو",
                                                tint = if (!selectedQuality.isAuto) Color(0xFF22E3FF) else Color.White,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }

                                        // Subtitles quick button
                                        IconButton(
                                            onClick = { showSubtitlesSheet = true },
                                            modifier = Modifier.size(36.dp).testTag("subtitles_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Subtitles,
                                                contentDescription = "الترجمة",
                                                tint = if (selectedSubtitleTrack != null || subtitleStyle.isLiveTranslationEnabled) Color(0xFF22E3FF) else Color.White,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }

                                        // Cast button
                                        if (castManager != null) {
                                            CastButton(
                                                castManager = castManager,
                                                currentMediaUri = currentMediaUri,
                                                currentTitle = videoTitle,
                                                currentPositionMs = currentPos,
                                                isLiveStream = isLive
                                            )
                                        }

                                        // Overflow Menu
                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.size(36.dp).testTag("player_more_menu_btn")
                                            ) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color.White, modifier = Modifier.size(21.dp))
                                            }

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("جودة ودقة الفيديو (${selectedQuality.resolutionLabel})") },
                                                leadingIcon = { Icon(Icons.Default.HighQuality, null, tint = Color(0xFF22E3FF)) },
                                                onClick = {
                                                    showMenu = false
                                                    showQualitySheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("الترجمة والبحث عبر الإنترنت") },
                                                leadingIcon = { Icon(Icons.Default.Subtitles, null, tint = Color(0xFF22E3FF)) },
                                                onClick = {
                                                    showMenu = false
                                                    showSubtitlesSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("استوديو الصوت والموازن Equalizer") },
                                                leadingIcon = { Icon(Icons.Default.Equalizer, null, tint = Color(0xFF22E3FF)) },
                                                onClick = {
                                                    showMenu = false
                                                    showAudioStudioSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("فلاتر السينما والألوان الحية (${aiFilter.titleAr})") },
                                                leadingIcon = { Icon(Icons.Default.Palette, null, tint = Color(0xFFFFC857)) },
                                                onClick = {
                                                    showMenu = false
                                                    showCinemaFiltersSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("سرعة التشغيل ($speed x)") },
                                                leadingIcon = { Icon(Icons.Default.Speed, null) },
                                                onClick = {
                                                    showMenu = false
                                                    showSpeedSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("وضع الصوت فقط (Audio Only)") },
                                                leadingIcon = { Icon(Icons.Default.Headphones, null, tint = if (isAudioOnly) Color(0xFF22E3FF) else Color.White) },
                                                onClick = {
                                                    showMenu = false
                                                    playerManager.toggleAudioOnly()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("تكرار مقطع A-B") },
                                                leadingIcon = { Icon(Icons.Default.RepeatOne, null) },
                                                onClick = {
                                                    showMenu = false
                                                    showAbRepeatBar = !showAbRepeatBar
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        if (sleepTimerMinutes > 0)
                                                            "مؤقت النوم (${sleepTimerRemainingSeconds / 60} دقيقة متبقية)"
                                                        else
                                                            "مؤقت النوم (Sleep Timer)"
                                                    )
                                                },
                                                leadingIcon = { Icon(Icons.Default.Timer, null, tint = if (sleepTimerMinutes > 0) Color(0xFFFFD600) else Color.White) },
                                                onClick = {
                                                    showMenu = false
                                                    showSleepTimerSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        if (isAutoPlayEnabled) "التشغيل التلقائي للتالي (مفعّل)"
                                                        else "التشغيل التلقائي للتالي (معطّل)"
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Autorenew,
                                                        null,
                                                        tint = if (isAutoPlayEnabled) Color(0xFF22E3FF) else Color(0xFF8B96BD)
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    playerManager.toggleAutoPlay()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("معلومات ودقة الفيديو") },
                                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                                onClick = {
                                                    showMenu = false
                                                    showInfoSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("صورة داخل صورة (PiP)") },
                                                leadingIcon = { Icon(Icons.Default.PictureInPictureAlt, null, tint = Color(0xFF22E3FF)) },
                                                onClick = {
                                                    showMenu = false
                                                    onEnterPiP?.invoke() ?: run {
                                                        val mainAct = context as? com.example.MainActivity
                                                        mainAct?.enterPipMode()
                                                    }
                                                }
                                            )
                                            if (castManager != null) {
                                                DropdownMenuItem(
                                                    text = { Text("بث إلى التلفاز (Google Cast)") },
                                                    leadingIcon = { Icon(Icons.Default.Cast, null, tint = Color(0xFF22E3FF)) },
                                                    onClick = {
                                                        showMenu = false
                                                        showCastDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }

                    // A-B Repeat Bar (Optional tool)
                    if (showAbRepeatBar && !isLocked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 70.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xCC0B0F19))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تكرار A-B: ${if (pointA != null) MediaScanner.formatDuration(pointA!!) else "--"} إلى ${if (pointB != null) MediaScanner.formatDuration(pointB!!) else "--"}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { playerManager.setPointA() }) {
                                    Text("A", color = Color(0xFF22E3FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                IconButton(onClick = { playerManager.setPointB() }) {
                                    Text("B", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                IconButton(onClick = { playerManager.clearAbRepeat() }) {
                                    Text("مسح", color = Color(0xFFFF6B8B), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Center Play / Rewind / Forward Controls
                    if (!isLocked) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isLive) 16.dp else 10.dp)
                        ) {
                            // Previous item (channel or playlist video)
                            if (playlist.size > 1) {
                                IconButton(
                                    onClick = { playerManager.playPrevious() },
                                    enabled = currentPlaylistIndex > 0,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (currentPlaylistIndex > 0) Color(0x40FFFFFF) else Color(0x18FFFFFF))
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "المقطع السابق",
                                        tint = if (currentPlaylistIndex > 0) Color.White else Color(0x55FFFFFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // -10s rewind (only for recorded videos)
                            if (!isLive) {
                                IconButton(
                                    onClick = { playerManager.seekBackward(10_000L) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x40FFFFFF))
                                ) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }

                            // Main Play / Pause Button
                            IconButton(
                                onClick = { playerManager.togglePlayPause() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.BrandGradient)
                                    .testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل/إيقاف",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            // +10s forward (only for recorded videos)
                            if (!isLive) {
                                IconButton(
                                    onClick = { playerManager.seekForward(10_000L) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x40FFFFFF))
                                ) {
                                    Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }

                            // Next item (channel or playlist video)
                            if (playlist.size > 1) {
                                IconButton(
                                    onClick = { playerManager.playNext() },
                                    enabled = currentPlaylistIndex < playlist.size - 1,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (currentPlaylistIndex < playlist.size - 1) Color(0x40FFFFFF) else Color(0x18FFFFFF))
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "المقطع التالي",
                                        tint = if (currentPlaylistIndex < playlist.size - 1) Color.White else Color(0x55FFFFFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Screen Lock Toggle Button (always visible when controls show)
                    IconButton(
                        onClick = {
                            playerManager.toggleScreenLock()
                            scheduleHideControls()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x552B2F73))
                            .testTag("player_lock_button")
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "قفل الشاشة",
                            tint = if (isLocked) Color(0xFFFF6B8B) else Color.White
                        )
                    }

                    // Bottom Bar Controls Overlay
                    if (!isLocked) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xE60E1230))
                                    )
                                )
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Progress bar / Slider
                            if (!isLive && duration > 0) {
                                var sliderPos by remember { mutableFloatStateOf(0f) }
                                var isDraggingSlider by remember { mutableStateOf(false) }

                                val displayPos = if (isDraggingSlider) sliderPos else currentPos.toFloat()

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = MediaScanner.formatDuration(displayPos.toLong()),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Slider(
                                        value = displayPos.coerceIn(0f, duration.toFloat()),
                                        onValueChange = {
                                            isDraggingSlider = true
                                            sliderPos = it
                                        },
                                        onValueChangeFinished = {
                                            playerManager.seekTo(sliderPos.toLong())
                                            isDraggingSlider = false
                                        },
                                        valueRange = 0f..duration.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF8B5CF6),
                                            inactiveTrackColor = Color(0x44FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                            .testTag("player_seek_slider")
                                    )

                                    Text(
                                        text = MediaScanner.formatDuration(duration),
                                        color = Color(0xFFC3CCE8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Secondary Bottom Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Aspect ratio toggle
                                IconButton(onClick = { playerManager.cycleResizeMode() }) {
                                    Icon(Icons.Default.AspectRatio, contentDescription = "أبعاد الشاشة", tint = Color.White)
                                }

                                // Screenshot grabber
                                IconButton(onClick = {
                                    scope.launch {
                                        playerViewRef?.videoSurfaceView?.let { surfaceView ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                val bitmap = android.graphics.Bitmap.createBitmap(
                                                    surfaceView.width.coerceAtLeast(100),
                                                    surfaceView.height.coerceAtLeast(100),
                                                    android.graphics.Bitmap.Config.ARGB_8888
                                                )
                                                android.view.PixelCopy.request(
                                                    surfaceView as android.view.SurfaceView,
                                                    bitmap,
                                                    { result ->
                                                        if (result == android.view.PixelCopy.SUCCESS) {
                                                            scope.launch {
                                                                val saved = playerManager.saveScreenshot(bitmap)
                                                                if (saved != null) {
                                                                    Toast.makeText(context, "تم حفظ لقطة الشاشة في الصور", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    android.os.Handler(android.os.Looper.getMainLooper())
                                                )
                                            }
                                        } ?: Toast.makeText(context, "جاري التقاط الإطار...", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "لقطة شاشة", tint = Color.White)
                                }

                                // Loop / Repeat toggle (OFF -> ONE -> ALL)
                                IconButton(
                                    onClick = {
                                        playerManager.toggleLoop()
                                        val msg = when (playerManager.videoRepeatMode.value) {
                                            NovaPlayerManager.VideoRepeatMode.ONE -> "🔂 تكرار الفيديو الحالي"
                                            NovaPlayerManager.VideoRepeatMode.ALL -> "🔁 تكرار كافة فيديوهات القائمة"
                                            NovaPlayerManager.VideoRepeatMode.OFF -> "⏹️ إيقاف التكرار"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("player_repeat_button")
                                ) {
                                    Icon(
                                        imageVector = if (videoRepeatMode == NovaPlayerManager.VideoRepeatMode.ONE)
                                            Icons.Default.RepeatOne else Icons.Default.Repeat,
                                        contentDescription = "تكرار الفيديو",
                                        tint = when (videoRepeatMode) {
                                            NovaPlayerManager.VideoRepeatMode.OFF -> if (isLooping) Color(0xFF22E3FF) else Color.White
                                            NovaPlayerManager.VideoRepeatMode.ONE -> Color(0xFFFFC857)
                                            NovaPlayerManager.VideoRepeatMode.ALL -> Color(0xFF22E3FF)
                                        }
                                    )
                                }

                                // Video Live Effects Studio Toggle Button
                                IconButton(
                                    onClick = { showCinemaFiltersSheet = true },
                                    modifier = Modifier.testTag("player_filters_toggle_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "تأثيرات وفلاتر الفيديو",
                                        tint = if (aiFilter != AiEnhanceMode.STANDARD) Color(0xFFFFC857) else Color.White
                                    )
                                }

                                // Speed button
                                IconButton(onClick = { showSpeedSheet = true }) {
                                    Text(
                                        text = "${speed}x",
                                        color = Color(0xFF22E3FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Stream Network Quality Alert Banner
        if (isLive && networkStreamInfo.alertMessage != null && playbackError == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xE61F1B24))
                    .border(
                        1.dp,
                        if (networkStreamInfo.status == NetworkHealthStatus.DISCONNECTED) Color(0xFFFF6B8B) else Color(0xFFFFC857),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (networkStreamInfo.status == NetworkHealthStatus.DISCONNECTED) Icons.Default.CloudOff else Icons.Default.WarningAmber,
                            contentDescription = "تنبيه الشبكة",
                            tint = if (networkStreamInfo.status == NetworkHealthStatus.DISCONNECTED) Color(0xFFFF6B8B) else Color(0xFFFFC857),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = networkStreamInfo.alertMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (networkStreamInfo.status == NetworkHealthStatus.POOR && !networkStreamInfo.isAutoQualityDowngraded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { playerManager.dismissNetworkAlert() }) {
                                Text("تجاهل", color = Color(0xFFC3CCE8), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { playerManager.downgradeQualityToLowest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("خفض الجودة (360p)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (networkStreamInfo.isAutoQualityDowngraded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { playerManager.resetQualityToAuto() }) {
                                Text("استعادة الجودة التلقائية", color = Color(0xFF22E3FF), fontSize = 12.sp)
                            }
                            TextButton(onClick = { playerManager.dismissNetworkAlert() }) {
                                Text("إغلاق", color = Color(0xFFC3CCE8), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timer 1-Minute Pre-Stop Warning Banner
        if (sleepTimerWarning != null && playbackError == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (isLive && networkStreamInfo.alertMessage != null) 140.dp else 70.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF21C1824))
                    .border(1.5.dp, Color(0xFFFFD600), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFD600)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "مؤقت النوم",
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه مؤقت النوم",
                                color = Color(0xFFFFD600),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "متبقي $sleepTimerRemainingSeconds ثانية قبل إيقاف التشغيل تلقائياً",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { playerManager.setSleepTimer(0) }) {
                            Text("إلغاء المؤقت", color = Color(0xFFFF6B8B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { playerManager.extendSleepTimer(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("تمديد 10 دقائق", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = { playerManager.dismissSleepTimerWarning() }) {
                            Text("حسناً", color = Color(0xFFC3CCE8), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // BackHandler for canceling AutoPlay countdown if user taps back
        BackHandler(enabled = autoPlayState.isCountingDown) {
            playerManager.cancelAutoPlay()
        }

        // Auto-play Next Video Countdown Window (5-second cancelable popup)
        AnimatedVisibility(
            visible = autoPlayState.isCountingDown && autoPlayState.nextVideo != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2151C2C)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF22E3FF)),
                elevation = CardDefaults.cardElevation(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("autoplay_countdown_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header: Tag + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = Color(0xFF22E3FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "التشغيل التلقائي للفيديو التالي",
                                color = Color(0xFF22E3FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { playerManager.cancelAutoPlay() },
                            modifier = Modifier.size(28.dp).testTag("close_autoplay_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إلغاء التشغيل التلقائي",
                                tint = Color(0xFFA9B4D6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Next Video Title
                    Text(
                        text = autoPlayState.nextVideo?.title ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Countdown with circular timer and Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Circular Countdown Timer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(44.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { autoPlayState.remainingSeconds / 5f },
                                    color = Color(0xFF22E3FF),
                                    trackColor = Color(0x3322E3FF),
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "${autoPlayState.remainingSeconds}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "يبدأ خلال ${autoPlayState.remainingSeconds} ثوانٍ",
                                color = Color(0xFFD5DCF2),
                                fontSize = 13.sp
                            )
                        }

                        // Action Buttons: Cancel and Play Now
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { playerManager.cancelAutoPlay() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B8B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF5252)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("cancel_autoplay_button")
                            ) {
                                Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { playerManager.playNextVideoImmediately() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22E3FF),
                                    contentColor = Color(0xFF0E1230)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("play_next_now_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل الآن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Subtitle Overlay
        LiveSubtitleOverlay(
            activeCueText = activeSubtitleCueText,
            translatedCueText = translatedSubtitleCueText,
            style = subtitleStyle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showControls) 115.dp else 40.dp)
        )

        // Playback / Network Error Card Overlay
        playbackError?.let { errorMsg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0B0F19))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                    elevation = CardDefaults.cardElevation(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("playback_error_card")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FF5252)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = "خطأ شبكة",
                                tint = Color(0xFFFF6B8B),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "تعذر تشغيل الفيديو أو البث",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMsg,
                            color = Color(0xFFA9B4D6),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    playerManager.clearError()
                                    onBack()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA9B4D6)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("الرجوع")
                            }

                            Button(
                                onClick = { playerManager.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                                modifier = Modifier.weight(1.2f).testTag("retry_playback_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Playback Speed Bottom Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF1A1F4A)
        ) {
            Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                Text(
                    text = "سرعة التشغيل",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
                speeds.chunked(4).forEach { rowSpeeds ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSpeeds.forEach { s ->
                            val isSelected = speed == s
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF22E3FF) else Color(0xFF272D66))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.text.BasicText(
                                    text = "${s}x",
                                    style = androidx.compose.ui.text.TextStyle(
                                        color = if (isSelected) Color(0xFF0E1230) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.testTag("speed_${s}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Audio Boost Bottom Sheet
    if (showAudioBoostSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioBoostSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF1A1F4A)
        ) {
            Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                Text(
                    text = "مضخم الصوت وموازن الصوت (Audio Booster)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "تضخيم الصوت حتى 200% لتجاوز مكبرات الصوت الضعيفة بأمان",
                    fontSize = 13.sp,
                    color = Color(0xFFA9B4D6),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF22E3FF))
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = audioBoost.toFloat(),
                        onValueChange = { playerManager.setAudioBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF22E3FF),
                            activeTrackColor = Color(0xFF22E3FF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "+${(audioBoost / 10)}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Info Bottom Sheet
    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF1A1F4A)
        ) {
            Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                Text(
                    text = "تفاصيل الفيديو والوسائط",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InfoRow("العنوان", videoTitle)
                InfoRow("المدة", MediaScanner.formatDuration(duration))
                InfoRow("حالة البث", if (isLive) "بث حي مباشر (Live Stream)" else "ملف محلي")
                if (isLive) {
                    InfoRow("بروتوكول البث", streamProtocol)
                    if (networkStreamInfo.currentResolution.isNotEmpty()) {
                        InfoRow("دقة الفيديو", networkStreamInfo.currentResolution)
                    }
                    if (networkStreamInfo.bitrateEstimateKbps > 0) {
                        InfoRow("سرعة التحميل التقديرية", "${networkStreamInfo.bitrateEstimateKbps} Kbps")
                    }
                    InfoRow("استقرار الشبكة", when (networkStreamInfo.status) {
                        NetworkHealthStatus.EXCELLENT -> "ممتاز وسلس"
                        NetworkHealthStatus.GOOD -> "جيد"
                        NetworkHealthStatus.POOR -> "ضعيف (جاري التكيف)"
                        NetworkHealthStatus.DISCONNECTED -> "منقطع"
                    })
                }
                InfoRow("المحرك", "Media3 ExoPlayer v1.5.1 (HLS/DASH/RTSP)")
                InfoRow("المرشح الذكي", aiFilter.titleAr)
            }
        }
    }

    // Sleep Timer Bottom Sheet
    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF1A1F4A)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFD600)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "مؤقت النوم (Sleep Timer)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (sleepTimerMinutes > 0) {
                        Text(
                            text = "${sleepTimerRemainingSeconds / 60}:${String.format("%02d", sleepTimerRemainingSeconds % 60)} متبقي",
                            color = Color(0xFFFFD600),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "سيتم إيقاف تشغيل الفيديو تلقائياً وإيقاف البث بعد المدة المحددة، مع تنبيه قبل الإيقاف بدقيقة.",
                    fontSize = 12.sp,
                    color = Color(0xFFA9B4D6),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Presets Grid
                val timerPresets = listOf(
                    15 to "15 دقيقة",
                    30 to "30 دقيقة",
                    45 to "45 دقيقة",
                    60 to "ساعة واحدة",
                    90 to "ساعة ونصف",
                    120 to "ساعتان"
                )

                timerPresets.chunked(3).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { (mins, label) ->
                            val isSelected = sleepTimerMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFFFD600) else Color(0xFF272D66))
                                    .clickable {
                                        playerManager.setSleepTimer(mins)
                                        showSleepTimerSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sleepTimerMinutes > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                playerManager.setSleepTimer(0)
                                showSleepTimerSheet = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B8B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TimerOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إلغاء المؤقت")
                        }

                        Button(
                            onClick = {
                                playerManager.extendSleepTimer(10)
                                showSleepTimerSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600), contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تمديد +10 دقائق", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showSleepTimerSheet = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA9B4D6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }

    if (showQualitySheet) {
        VideoQualitySheet(
            qualities = availableQualities,
            selectedQuality = selectedQuality,
            techStats = videoTechStats,
            onSelectQuality = { playerManager.selectVideoQuality(it) },
            onDismiss = { showQualitySheet = false }
        )
    }

    if (showSubtitlesSheet) {
        SubtitlesLiveSheet(
            videoTitle = videoTitle,
            availableTracks = availableSubtitleTracks,
            selectedTrack = selectedSubtitleTrack,
            subtitleStyle = subtitleStyle,
            subtitleService = playerManager.subtitleService,
            onSelectTrack = { playerManager.selectSubtitleTrack(it) },
            onAttachSubtitle = { uri, lang, label -> playerManager.attachExternalSubtitle(uri, lang, label) },
            onUpdateStyle = { playerManager.setSubtitleStyle(it) },
            onToggleLiveTranslation = { playerManager.toggleLiveTranslation() },
            onDismiss = { showSubtitlesSheet = false }
        )
    }

    if (showAudioStudioSheet) {
        AudioStudioSheet(
            audioStudioManager = playerManager.audioStudio,
            audioState = audioStudioState,
            onDismiss = { showAudioStudioSheet = false }
        )
    }

    if (showCastDialog && castManager != null) {
        CastDialog(
            castManager = castManager,
            currentMediaUri = currentMediaUri,
            currentTitle = videoTitle,
            currentPositionMs = currentPos,
            isLiveStream = isLive,
            onDismissRequest = { showCastDialog = false }
        )
    }

    if (showCinemaFiltersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCinemaFiltersSheet = false },
            containerColor = Color(0xFF141943),
            contentColor = Color.White
        ) {
            val filterModes = remember {
                listOf(
                    Triple(AiEnhanceMode.STANDARD, "وضع العرض الطبيعي بدون فلاتر إضافية", Color(0xFF8B96BD)),
                    Triple(AiEnhanceMode.AMOLED_PURE_BLACK, "عمق فائق للون الأسود وتوفير البطارية لشاشات OLED", Color(0xFF272D66)),
                    Triple(AiEnhanceMode.CINEMA_WARM, "طابع هوليوودي دافئ مع ألوان سينمائية ذهبية ناعمة", Color(0xFFFFB74D)),
                    Triple(AiEnhanceMode.NIGHT_SHIELD, "تصفية الضوء الأزرق لراحة العين أثناء المشاهدة في الظلام", Color(0xFFFFA000)),
                    Triple(AiEnhanceMode.VIVID_SUPER, "تعزيز التباين والحدة وإبراز تفاصيل الوجوه والمشاهد", Color(0xFF22E3FF)),
                    Triple(AiEnhanceMode.NOIR_DRAMATIC, "أبيض وأسود درامي كلاسيكي مع تباين سينمائي عميق", Color(0xFFE6EAFA)),
                    Triple(AiEnhanceMode.CYBERPUNK_NEON, "نيون سايبربانك مستقبلي أزرق كهربائي وماجنتا متوهج", Color(0xFFE040FB)),
                    Triple(AiEnhanceMode.VINTAGE_SEPIA, "سيبيا كلاسيكي قديم بطابع أفلام السبعينات الدافئة", Color(0xFF8D6E63)),
                    Triple(AiEnhanceMode.COLD_SCIFI, "خيال علمي بارد وإضاءة فضائية سينمائية ثلجية", Color(0xFF5B8CFF)),
                    Triple(AiEnhanceMode.EMERALD_NATURE, "طبيعة زمردية خضراء فائقة الحيوية للأفلام الوثائقية", Color(0xFF2EE6A6)),
                    Triple(AiEnhanceMode.SUNSET_GOLD, "أجواء شروق وغروب ذهبية متدرجة دافئة", Color(0xFFFF9100)),
                    Triple(AiEnhanceMode.MATTE_CINEMA, "مات سينمائي هادئ وأنيق للأفلام والهواة", Color(0xFF475569)),
                    Triple(AiEnhanceMode.DEEP_BLUES, "أزرق عميق سينمائي لأفلام الغموض والتشويق", Color(0xFF1E3A8A)),
                    Triple(AiEnhanceMode.ROSE_GOLD, "روز جولد راقي ودافئ للمشاهد الرومانسية", Color(0xFFF43F5E)),
                    Triple(AiEnhanceMode.MATRIX_GREEN, "أخضر ماتركس رقمي مستقبلي", Color(0xFF10B981)),
                    Triple(AiEnhanceMode.AUTUMN_ORANGE, "خريفي دافئ برتقالي للمناظر الطبيعية", Color(0xFFD97706)),
                    Triple(AiEnhanceMode.ARCTIC_FROST, "صقيع قطبي ناصع لمشاهد الثلوج", Color(0xFF93C5FD)),
                    Triple(AiEnhanceMode.CHOCOLATE_BROWN, "شوكولاتة دافئة كلاسيكية دافئة", Color(0xFF78350F)),
                    Triple(AiEnhanceMode.NEON_PURPLE, "بنفسجي أرجواني ساطع للوك إلكتروني فخم", Color(0xFF9333EA)),
                    Triple(AiEnhanceMode.VINTAGE_8MM, "كاميرا 8 مم قديمة مع حواف مظلمة ذكريات الزمن الجميل", Color(0xFF333333)),
                    Triple(AiEnhanceMode.HDR_ULTRA_BOOST, "تعزيز HDR فائق القوة والنقاء اللوني", Color(0xFFFFFFFF)),
                    Triple(AiEnhanceMode.TEAL_AND_ORANGE, "تيل وأورانج هوليوودي الشهير (مفضل لدى المخرجين)", Color(0xFF0EA5E9)),
                    Triple(AiEnhanceMode.CYAN_DREAM, "حلم السيان السماوي المنعش", Color(0xFF06B6D4)),
                    Triple(AiEnhanceMode.MOONLIGHT_SILVER, "ضوء القمر الفضي الهادئ للمشاهد الليلية", Color(0xFF8B96BD)),
                    Triple(AiEnhanceMode.VAMPIRE_BLOOD_RED, "أحمر درامي سينمائي داكن لأفلام الإثارة", Color(0xFF990000))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "استوديو تحسين الألوان والفلاتر السينمائية (${filterModes.size} فلتر)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text("Cinema HDR & Live Video Grading", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .testTag("cinema_filters_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(filterModes, key = { it.first.name }) { (mode, desc, accentColor) ->
                        val isSelected = aiFilter == mode
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF272D66) else Color(0xFF161F30)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22E3FF)) else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.setAiFilter(mode)
                                    showCinemaFiltersSheet = false
                                    Toast.makeText(context, "🎬 تم تفعيل: ${mode.titleAr}", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("filter_item_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.titleAr,
                                        color = if (isSelected) Color(0xFF22E3FF) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = Color(0xFFA9B4D6),
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "مفعل",
                                        tint = Color(0xFF22E3FF),
                                        modifier = Modifier.size(20.dp)
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFA9B4D6), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
