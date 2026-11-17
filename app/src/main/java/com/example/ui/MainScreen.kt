package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import com.example.player.PlaylistItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.util.rememberWindowSizeInfo
import com.example.media.model.LocalVideo
import com.example.player.VideoPlayerScreen
import com.example.ui.components.RadioMiniPlayer
import com.example.ui.screens.CinemaXGatewayScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.LiveStreamScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.VideosScreen
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import com.example.ui.screens.IslamicHubScreen
import com.example.ui.theme.cinemaBackground
import com.example.ui.theme.BrandGradient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

enum class NavTab(val titleAr: String) {
    VIDEOS("فيديوهات"),
    MUSIC("صوتيات"),
    LIVE("تلفاز"),
    RADIO("راديو"),
    ISLAMIC("إسلاميات"),
    FOLDERS("مجلدات"),
    TOOLS("الإعدادات")
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val windowSizeInfo = rememberWindowSizeInfo()

    val videos by viewModel.videos.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val vaultItems by viewModel.vaultItems.collectAsState()
    val nightShield by viewModel.prefsManager.nightShield.collectAsState()

    var currentTab by remember { mutableStateOf(NavTab.VIDEOS) }
    var showGateway by remember { mutableStateOf(true) }
    var showVault by remember { mutableStateOf(false) }
    var showVideoTools by remember { mutableStateOf(false) }
    val isPlayerActive by viewModel.isPlayerActive.collectAsState()
    val isInPipMode by viewModel.isInPipMode.collectAsState()
    val requestedNavTab by viewModel.requestedNavTab.collectAsState()
    val requestedPrayerTab by viewModel.requestedPrayerTab.collectAsState()

    LaunchedEffect(requestedNavTab) {
        requestedNavTab?.let { target ->
            currentTab = target
            showGateway = false
            viewModel.consumeRequestedNavTab()
        }
    }

    // Dialog state for encrypting video to vault
    var videoToEncrypt by remember { mutableStateOf<LocalVideo?>(null) }
    var encryptPin by remember { mutableStateOf("") }

    // Permission handling (READ_MEDIA_VIDEO and READ_MEDIA_AUDIO on Android 13+, READ_EXTERNAL_STORAGE on older)
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it } || requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        hasPermission = granted
        if (granted) {
            viewModel.loadLocalVideos()
            viewModel.loadLocalAudioFiles()
        } else {
            Toast.makeText(context, "إذن الوصول للوسائط مطلوب لعرض الفيديوهات والصوتيات", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadLocalVideos()
            viewModel.loadLocalAudioFiles()
        }
    }

    // Intercept back button when player is full screen
    BackHandler(enabled = isPlayerActive && !isInPipMode) {
        viewModel.setPlayerActive(false)
        viewModel.playerManager.exoPlayer?.pause()
    }

    // In Picture-in-Picture mode, render ONLY the video player frame edge-to-edge
    if (isInPipMode) {
        VideoPlayerScreen(
            playerManager = viewModel.playerManager,
            castManager = viewModel.castManager,
            isInPipMode = true,
            onEnterPiP = { (context as? com.example.MainActivity)?.enterPipMode() },
            onBack = {
                viewModel.setPlayerActive(false)
                viewModel.playerManager.exoPlayer?.pause()
            }
        )
        return
    }

    // CinemaX Entrance Gateway Screen (بوابة الدخول السينمائية)
    if (showGateway && !isPlayerActive && !isInPipMode) {
        CinemaXGatewayScreen(
            onEnterApp = { targetTab ->
                currentTab = targetTab
                showGateway = false
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().cinemaBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (!isPlayerActive) {
                    Column {
                        if (currentTab != NavTab.RADIO) {
                            RadioMiniPlayer(
                                radioManager = viewModel.radioManager,
                                onClick = { currentTab = NavTab.RADIO }
                            )
                        }

                        CinemaBottomBar(
                            currentTab = currentTab,
                            onSelect = { currentTab = it }
                        )
                    }
                }
            }
    ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    NavTab.VIDEOS -> VideosScreen(
                        videos = videos,
                        favorites = favorites,
                        onVideoClick = { video ->
                            val playlist = videos.map { v ->
                                PlaylistItem(
                                    uri = v.uri,
                                    title = v.title,
                                    durationMs = v.durationMs,
                                    isLiveStream = false
                                )
                            }
                            val startIndex = videos.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                            viewModel.radioManager.stop()
                            viewModel.playerManager.setPlaylist(playlist, startIndex)
                            viewModel.playerManager.playMedia(
                                uriString = video.uri,
                                title = video.title,
                                isLiveStream = false
                            )
                            viewModel.setPlayerActive(true)
                        },
                        onToggleFavorite = { video -> viewModel.toggleFavorite(video) },
                        onEncryptToVault = { video ->
                            videoToEncrypt = video
                            encryptPin = ""
                        },
                        onRefresh = { viewModel.loadLocalVideos() },
                        onOpenVideoTools = { showVideoTools = true },
                        hasPermission = hasPermission,
                        onRequestPermission = { permissionLauncher.launch(requiredPermissions) },
                        castManager = viewModel.castManager
                    )

                    NavTab.FOLDERS -> FoldersScreen(
                        folders = folders,
                        allVideos = videos,
                        favorites = favorites,
                        onVideoClick = { video ->
                            val folderVideos = videos.filter { it.folderName == video.folderName }.ifEmpty { videos }
                            val playlist = folderVideos.map { v ->
                                PlaylistItem(
                                    uri = v.uri,
                                    title = v.title,
                                    durationMs = v.durationMs,
                                    isLiveStream = false
                                )
                            }
                            val startIndex = folderVideos.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
                            viewModel.radioManager.stop()
                            viewModel.playerManager.setPlaylist(playlist, startIndex)
                            viewModel.playerManager.playMedia(
                                uriString = video.uri,
                                title = video.title,
                                isLiveStream = false
                            )
                            viewModel.setPlayerActive(true)
                        },
                        onToggleFavorite = { video -> viewModel.toggleFavorite(video) },
                        onEncryptToVault = { video ->
                            videoToEncrypt = video
                            encryptPin = ""
                        },
                        onRefresh = { viewModel.loadLocalVideos() },
                        castManager = viewModel.castManager
                    )

                    NavTab.LIVE -> LiveStreamScreen(
                        onPlayChannel = { channel ->
                            viewModel.radioManager.stop()
                            viewModel.playerManager.playMedia(
                                uriString = channel.url,
                                title = channel.titleAr,
                                isLiveStream = true,
                                backupUrls = channel.backupUrls
                            )
                            viewModel.setPlayerActive(true)
                        },
                        castManager = viewModel.castManager
                    )

                    NavTab.MUSIC -> com.example.ui.screens.music.MusicPlayerScreen(
                        viewModel = viewModel
                    )

                    NavTab.RADIO -> RadioScreen(
                        radioManager = viewModel.radioManager
                    )

                    NavTab.ISLAMIC -> IslamicHubScreen(
                        radioManager = viewModel.radioManager,
                        prayerManager = viewModel.prayerManager,
                        duaManager = viewModel.duaManager,
                        requestedPrayerTab = requestedPrayerTab,
                        onConsumePrayerTab = { viewModel.consumeRequestedPrayerTab() }
                    )

                    NavTab.TOOLS -> ToolsScreen(
                        shareServer = viewModel.shareServer,
                        playerManager = viewModel.playerManager,
                        prefsManager = viewModel.prefsManager,
                        onClearHistory = { viewModel.clearAllHistory() },
                        duaManager = viewModel.duaManager,
                        videos = videos,
                        onMediaChanged = {
                            viewModel.loadLocalVideos()
                            viewModel.loadLocalAudioFiles()
                        },
                        onOpenPrayerAlarms = { viewModel.navigateToPrayerTimes(2) },
                        section = com.example.ui.screens.ToolsSection.SETTINGS,
                        onOpenVault = { showVault = true }
                    )
                }
            }
        }

        // Full Screen Video Player Overlay
        AnimatedVisibility(
            visible = isPlayerActive && !isInPipMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            VideoPlayerScreen(
                playerManager = viewModel.playerManager,
                castManager = viewModel.castManager,
                isInPipMode = false,
                onEnterPiP = { (context as? com.example.MainActivity)?.enterPipMode() },
                onBack = {
                    viewModel.setPlayerActive(false)
                    viewModel.playerManager.exoPlayer?.pause()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Vault overlay (moved here from the bottom bar - reachable from Settings)
        if (showVault && !isPlayerActive) {
            BackHandler { showVault = false }
            Column(modifier = Modifier.fillMaxSize().cinemaBackground()) {
                OverlayTopBar(title = "الخزنة المشفرة", onBack = { showVault = false })
                VaultScreen(
                    vaultItems = vaultItems,
                    prefsManager = viewModel.prefsManager,
                    cryptoManager = viewModel.cryptoManager,
                    onPlayEncryptedVideo = { file, title ->
                        viewModel.radioManager.stop()
                        viewModel.playerManager.playMedia(
                            uriString = file.absolutePath,
                            title = title,
                            isLiveStream = false
                        )
                        viewModel.setPlayerActive(true)
                    },
                    onDeleteItem = { item -> viewModel.deleteVaultItem(item) }
                )
            }
        }

        // Video tools overlay (cutter, compressor, GIF, snapshots ... now live under Videos)
        if (showVideoTools && !isPlayerActive) {
            BackHandler { showVideoTools = false }
            Column(modifier = Modifier.fillMaxSize().cinemaBackground()) {
                OverlayTopBar(title = "أدوات الفيديو", onBack = { showVideoTools = false })
                ToolsScreen(
                    shareServer = viewModel.shareServer,
                    playerManager = viewModel.playerManager,
                    prefsManager = viewModel.prefsManager,
                    onClearHistory = { viewModel.clearAllHistory() },
                    duaManager = viewModel.duaManager,
                    videos = videos,
                    onMediaChanged = {
                        viewModel.loadLocalVideos()
                        viewModel.loadLocalAudioFiles()
                    },
                    section = com.example.ui.screens.ToolsSection.VIDEO
                )
            }
        }

        // Night shield eye protection filter across the entire app if enabled
        if (nightShield && !isPlayerActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x20FFA000))
            )
        }

        // Encrypt Video to Vault Dialog
        videoToEncrypt?.let { video ->
            AlertDialog(
                onDismissRequest = { videoToEncrypt = null },
                containerColor = Color(0xFF1A1F4A),
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF6B8B))
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            "تشفير الفيديو ونقله إلى الخزنة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "أدخل رمز PIN أو كلمة سر لتشفير هذا الفيديو (${video.title}) بواسطة تقنية AES-256 العسكرية:\n(سيكون لهذا الفيديو رمزه السري المستقل تماماً عن رمز دخول الخزنة)",
                            color = Color(0xFFA9B4D6),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = encryptPin,
                            onValueChange = { encryptPin = it },
                            label = { Text("رمز تشفير هذا الفيديو الخاص", color = Color(0xFFA9B4D6)) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF22E3FF),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("encrypt_pin_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (encryptPin.length >= 3) {
                                viewModel.encryptVideoToVault(
                                    video = video,
                                    pin = encryptPin,
                                    onSuccess = {
                                        videoToEncrypt = null
                                        Toast.makeText(context, "تم تشفير الفيديو بالرمز الخاص به ونقله للخزنة بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "فشل التشفير: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "يرجى إدخال رمز تشفير من 3 أحرف/أرقام على الأقل", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B8B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_encrypt_button")
                    ) {
                        Text("تشفير ونقل الآن", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { videoToEncrypt = null }) {
                        Text("إلغاء", color = Color(0xFFA9B4D6))
                    }
                }
            )
        }
    }
}


@Composable
private fun OverlayTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
        }
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun navIcon(tab: NavTab): ImageVector = when (tab) {
    NavTab.VIDEOS -> Icons.Default.PlayCircle
    NavTab.FOLDERS -> Icons.Default.Folder
    NavTab.MUSIC -> Icons.Default.Headphones
    NavTab.LIVE -> Icons.Default.LiveTv
    NavTab.RADIO -> Icons.Default.Radio
    NavTab.ISLAMIC -> Icons.Default.Mosque
    NavTab.TOOLS -> Icons.Default.Settings
}

/** Floating glass-style bottom bar: selected item gets a brand gradient pill + label. */
@Composable
private fun CinemaBottomBar(currentTab: NavTab, onSelect: (NavTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xF01A1F4A))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.values().forEach { tab ->
                val selected = tab == currentTab
                Column(
                    modifier = Modifier
                        .weight(if (selected) 1.5f else 1f)
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (selected) Modifier.background(BrandGradient) else Modifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(tab) }
                        .padding(vertical = 7.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        navIcon(tab),
                        contentDescription = tab.titleAr,
                        tint = if (selected) Color(0xFF0E1230) else Color(0xFFA9B4D6),
                        modifier = Modifier.size(22.dp)
                    )
                    if (selected) {
                        Text(
                            tab.titleAr,
                            color = Color(0xFF0E1230),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
