package com.example.ui.screens

import com.example.ui.theme.cinemaBackground
import com.example.IncomingShareRequest
import com.example.media.DownloadLinkAnalyzer

import com.example.sync.CinemaXSyncDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Speed
import com.example.media.VideoCutterTrimmer
import com.example.media.VideoSnapshotStudio
import com.example.media.DataSaverCalculator
import com.example.media.WebVideoSnifferDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.player.NovaPlayerManager
import com.example.sharing.LocalHttpShareServer
import com.example.util.ApkShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

enum class ToolsSection { ALL, VIDEO, SETTINGS }

@Composable
fun ToolsScreen(
    shareServer: LocalHttpShareServer,
    playerManager: NovaPlayerManager,
    prefsManager: PreferencesManager,
    onClearHistory: () -> Unit,
    duaManager: com.example.duas.DuaManager? = null,
    videos: List<com.example.media.model.LocalVideo> = emptyList(),
    onMediaChanged: () -> Unit = {},
    onOpenPrayerAlarms: () -> Unit = {},
    section: ToolsSection = ToolsSection.ALL,
    onOpenVault: () -> Unit = {},
    incomingShareRequest: IncomingShareRequest? = null,
    onConsumeIncomingShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isServerRunning by shareServer.isRunning.collectAsState()
    val serverUrl by shareServer.serverUrl.collectAsState()
    val nightShield by prefsManager.nightShield.collectAsState()
    val soundPreset by prefsManager.soundPreset.collectAsState()
    val language by prefsManager.language.collectAsState()
    val isAutoPlayEnabled by playerManager.isAutoPlayEnabled.collectAsState()
    val downloadPreferences = remember(context) {
        context.getSharedPreferences("cinemax_download_preferences", Context.MODE_PRIVATE)
    }
    var wifiOnlyDownloads by remember {
        mutableStateOf(downloadPreferences.getBoolean("wifi_only_downloads", false))
    }

    var showDuasScreen by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf("") }
    var downloadRequestHeaders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var downloadExpectedMimeType by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var showSnifferDialog by remember { mutableStateOf(false) }
    var showDuplicateCleanerDialog by remember { mutableStateOf(false) }

    // Universal website extractor states (yt-dlp + FFmpeg + aria2c).
    var universalInfo by remember { mutableStateOf<com.example.media.UniversalMediaEngine.MediaInfo?>(null) }
    var universalInfoUrl by remember { mutableStateOf("") }
    var selectedUniversalFormat by remember { mutableStateOf<com.example.media.UniversalMediaEngine.FormatOption?>(null) }
    var isUniversalInspecting by remember { mutableStateOf(false) }
    var isUniversalDownloading by remember { mutableStateOf(false) }
    var universalDownloadPercent by remember { mutableIntStateOf(0) }
    var universalDownloadMessage by remember { mutableStateOf("") }
    var universalProcessId by remember { mutableStateOf<String?>(null) }
    var isUpdatingUniversalEngine by remember { mutableStateOf(false) }
    var universalEngineVersionMessage by remember { mutableStateOf("") }

    fun inspectWithUniversalEngine(
        rawUrl: String,
        requestHeaders: Map<String, String> = emptyMap()
    ) {
        val url = com.example.media.RealMediaDownloader.normalizeUrl(rawUrl)
        if (url.isBlank()) {
            Toast.makeText(context, "أدخل رابط صفحة أو ملف أولاً.", Toast.LENGTH_SHORT).show()
            return
        }
        downloadUrl = url
        universalInfoUrl = url
        universalInfo = null
        selectedUniversalFormat = null
        isUniversalInspecting = true
        universalEngineVersionMessage = ""
        scope.launch {
            try {
                val info = com.example.media.UniversalMediaEngine.inspect(context, url, requestHeaders)
                universalInfo = info
                selectedUniversalFormat = info.formats.firstOrNull { it.hasVideo } ?: info.formats.firstOrNull()
                if (info.formats.isEmpty()) {
                    universalEngineVersionMessage = "الموقع لم يعرض صيغ تنزيل قابلة للاختيار."
                }
            } catch (error: Exception) {
                universalInfo = null
                universalEngineVersionMessage = error.message ?: "تعذر تحليل الوسائط من هذا الموقع."
                Toast.makeText(context, universalEngineVersionMessage, Toast.LENGTH_LONG).show()
            } finally {
                isUniversalInspecting = false
            }
        }
    }

    fun downloadWithUniversalEngine(
        urls: List<String>,
        audioOnly: Boolean = false,
        selectedFormat: com.example.media.UniversalMediaEngine.FormatOption? = null,
        requestHeaders: Map<String, String> = emptyMap()
    ) {
        if (isUniversalDownloading) return
        if (urls.isEmpty()) {
            Toast.makeText(context, "لم يتم العثور على روابط صالحة للتنزيل.", Toast.LENGTH_SHORT).show()
            return
        }
        val processId = UUID.randomUUID().toString()
        universalProcessId = processId
        universalDownloadPercent = 0
        universalDownloadMessage = "تهيئة محركات استخراج المواقع..."
        isUniversalDownloading = true

        scope.launch {
            try {
                val files = com.example.media.UniversalMediaEngine.download(
                    context = context,
                    urls = urls,
                    audioOnly = audioOnly,
                    selectedFormat = selectedFormat,
                    wifiOnly = wifiOnlyDownloads,
                    processId = processId,
                    requestHeaders = requestHeaders
                ) { progress ->
                    scope.launch(Dispatchers.Main) {
                        universalDownloadPercent = progress.percent
                        universalDownloadMessage = progress.message.ifBlank {
                            if (progress.etaSeconds > 0) "المتبقي نحو ${progress.etaSeconds} ثانية" else "جاري التنزيل..."
                        }
                    }
                }
                onMediaChanged()
                val totalBytes = files.sumOf { it.sizeBytes }
                val megabytes = String.format("%.1f", totalBytes / (1024.0 * 1024.0))
                Toast.makeText(
                    context,
                    "اكتمل تنزيل ${files.size} ملفاً عبر محرك المواقع — ${megabytes} MB",
                    Toast.LENGTH_LONG
                ).show()
                universalDownloadPercent = 100
                universalDownloadMessage = "اكتمل التنزيل وحُفظت الملفات في Downloads/CinemaX."
            } catch (error: Exception) {
                universalDownloadMessage = error.message ?: "تعذر تنزيل الوسائط من هذا المصدر."
                Toast.makeText(context, universalDownloadMessage, Toast.LENGTH_LONG).show()
            } finally {
                isUniversalDownloading = false
                universalProcessId = null
            }
        }
    }

    fun stopUniversalDownload() {
        universalProcessId?.let { processId ->
            val stopped = com.example.media.UniversalMediaEngine.stop(processId)
            if (stopped) {
                universalDownloadMessage = "تم إرسال أمر إيقاف محرك التنزيل."
            }
        }
    }

    // Audio Extractor Tool States
    var showAudioExtractDialog by remember { mutableStateOf(false) }
    var selectedVideoForAudio by remember { mutableStateOf<com.example.media.model.LocalVideo?>(null) }
    var isExtractingAudio by remember { mutableStateOf(false) }

    // Technical Codec Inspector Tool States
    var showInspectorDialog by remember { mutableStateOf(false) }
    var selectedVideoForInspect by remember { mutableStateOf<com.example.media.model.LocalVideo?>(null) }
    var inspectedSpecs by remember { mutableStateOf<com.example.media.VideoMetadataInspector.VideoTechnicalSpecs?>(null) }
    var isInspecting by remember { mutableStateOf(false) }

    // Video Trimmer & Scene Cutter Tool States
    var showVideoCutterDialog by remember { mutableStateOf(false) }
    var selectedVideoForCut by remember { mutableStateOf<com.example.media.model.LocalVideo?>(null) }
    var cutStartSeconds by remember { mutableFloatStateOf(0f) }
    var cutEndSeconds by remember { mutableFloatStateOf(30f) }
    var isTrimmingVideo by remember { mutableStateOf(false) }
    var trimProgressState by remember { mutableIntStateOf(0) }
    var trimStatusState by remember { mutableStateOf("") }

    // HD Video Frame Grabber & Snapshot Studio States
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var selectedVideoForSnapshot by remember { mutableStateOf<com.example.media.model.LocalVideo?>(null) }
    var snapshotTimeSeconds by remember { mutableFloatStateOf(5f) }
    var snapshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var snapshotFilter by remember { mutableStateOf(VideoSnapshotStudio.SnapshotFilter.ORIGINAL) }
    var isCapturingSnapshot by remember { mutableStateOf(false) }

    // Data Saver & Network Ping Tester States
    var showDataSaverDialog by remember { mutableStateOf(false) }
    var selectedProfileIdx by remember { mutableIntStateOf(1) } // 1080p FHD default
    var watchTimeMinutes by remember { mutableIntStateOf(60) }
    var pingResults by remember { mutableStateOf<List<DataSaverCalculator.PingResult>>(emptyList()) }
    var isTestingPing by remember { mutableStateOf(false) }

    // Creative Media Tools States
    var showGifDialog by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var showCompressorDialog by remember { mutableStateOf(false) }
    var showAudioRemoverDialog by remember { mutableStateOf(false) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJson by remember { mutableStateOf("") }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepMinutes by remember { mutableIntStateOf(30) }

    var showLicenseDialog by remember { mutableStateOf(false) }

    var isSharingApk by remember { mutableStateOf(false) }
    val apkSize = remember { ApkShareManager.getApkSizeFormatted(context) }
    val appVersion = remember { ApkShareManager.getAppVersion(context) }

    if (showDuasScreen) {
        val dm = duaManager ?: remember { com.example.duas.DuaManager(context) }
        com.example.ui.screens.duas.DuasScreen(
            duaManager = dm,
            onBack = { showDuasScreen = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section: Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (section == ToolsSection.VIDEO) Icons.Default.AutoAwesome else Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFF22E3FF),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = when (section) {
                    ToolsSection.VIDEO -> "أدوات الفيديو والوسائط"
                    ToolsSection.SETTINGS -> "الإعدادات العامة"
                    ToolsSection.ALL -> "الأدوات والإعدادات"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Lock,
                iconTint = Color(0xFFFF6B8B),
                title = "الخزنة المشفرة",
                subtitle = "خزنة محلية للمقاطع الخاصة مع قفل برمز PIN",
                action = {
                    Button(
                        onClick = onOpenVault,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White)
                    ) {
                        Text("فتح الخزنة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ToolCard(
                icon = Icons.Default.Wifi,
                iconTint = Color(0xFF22E3FF),
                title = "حماية التنزيلات",
                subtitle = if (wifiOnlyDownloads) "التنزيل عبر Wi-Fi فقط — بيانات الهاتف متوقفة"
                    else "السماح بالتنزيل عبر Wi-Fi وبيانات الهاتف",
                action = {
                    Switch(
                        checked = wifiOnlyDownloads,
                        onCheckedChange = { enabled ->
                            wifiOnlyDownloads = enabled
                            downloadPreferences.edit().putBoolean("wifi_only_downloads", enabled).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2EE6A6),
                            checkedTrackColor = Color(0xFF245E54)
                        ),
                        modifier = Modifier.testTag("download_wifi_only_switch")
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ToolCard(
                icon = Icons.Default.Settings,
                iconTint = Color(0xFF9D7BFF),
                title = "محركات استخراج المواقع",
                subtitle = "yt-dlp لتحليل المواقع المدعومة، FFmpeg لاستخراج الصوت ودمج المسارات، وaria2c للتنزيل المتوازي. يتطلب المحتوى أن يكون متاحاً للتنزيل ولا يتجاوز DRM أو صلاحيات الدخول.",
                action = {
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = {
                                if (!isUpdatingUniversalEngine) {
                                    isUpdatingUniversalEngine = true
                                    scope.launch {
                                        try {
                                            val status = com.example.media.UniversalMediaEngine.updateExtractor(context)
                                            universalEngineVersionMessage = "حالة تحديث المحرك: $status"
                                            Toast.makeText(context, universalEngineVersionMessage, Toast.LENGTH_LONG).show()
                                        } catch (error: Exception) {
                                            universalEngineVersionMessage = error.message ?: "تعذر تحديث محرك المواقع."
                                            Toast.makeText(context, universalEngineVersionMessage, Toast.LENGTH_LONG).show()
                                        } finally {
                                            isUpdatingUniversalEngine = false
                                        }
                                    }
                                }
                            },
                            enabled = !isUpdatingUniversalEngine,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9D7BFF),
                                contentColor = Color.White
                            )
                        ) {
                            if (isUpdatingUniversalEngine) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("تحديث مستخرجات المواقع", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        if (universalEngineVersionMessage.isNotBlank()) {
                            Text(universalEngineVersionMessage, color = Color(0xFFA9B4D6), fontSize = 9.sp)
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ToolCard(
                icon = Icons.Default.CloudDownload,
                iconTint = Color(0xFF2EE6A6),
                title = "مركز التنزيل",
                subtitle = "تنزيل الملفات المباشرة، متابعة السرعة والحجم، واستئناف التنزيلات المتقطعة",
                action = {
                    Button(
                        onClick = { showDownloadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_downloader_button")
                    ) {
                        Text("فتح التنزيلات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ToolCard(
                icon = Icons.Default.Language,
                iconTint = Color(0xFF22E3FF),
                title = "مستكشف الفيديو",
                subtitle = "افتح صفحة الويب، شغّل الفيديو، واختر المصدر المباشر المتاح مع معاينة أكبر",
                action = {
                    Button(
                        onClick = {
                            downloadUrl = ""
                            downloadRequestHeaders = emptyMap()
                            downloadExpectedMimeType = null
                            showSnifferDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_sniffer_card_btn")
                    ) {
                        Text("فتح المستكشف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tool: Full APK Share directly inside the app
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Share,
                iconTint = Color(0xFF22E3FF),
                title = "مشاركة التطبيق APK بالكامل",
                subtitle = "إرسال ملف تثبيت CinemaX Player (APK) مباشرة لأصدقائك عبر البلوتوث، واتساب، أو تيليجرام (الحجم: $apkSize | الإصدار: v$appVersion)",
                action = {
                    Button(
                        onClick = {
                            if (!isSharingApk) {
                                scope.launch {
                                    ApkShareManager.shareFullApk(context) { isSharingApk = it }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("share_full_apk_button")
                    ) {
                        Text(
                            text = if (isSharingApk) "جاري التجهيز..." else "مشاركة APK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 1: Local Wi-Fi Sharing Server
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Wifi,
                iconTint = Color(0xFF22E3FF),
                title = "مشاركة الوسائط عبر الشبكة المحلية (Wi-Fi Hub)",
                subtitle = if (isServerRunning) "الخادم نشط: $serverUrl" else "بث وتحميل فيديوهات هاتفك مباشرة على الكمبيوتر أو الشاشة الذكية بدون إنترنت",
                action = {
                    Switch(
                        checked = isServerRunning,
                        onCheckedChange = { start ->
                            if (start) {
                                shareServer.start()
                                Toast.makeText(context, "تم بدء خادم المشاركة المحلي", Toast.LENGTH_SHORT).show()
                            } else {
                                shareServer.stop()
                                Toast.makeText(context, "تم إيقاف الخادم", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF22E3FF), checkedTrackColor = Color(0xFF272D66)),
                        modifier = Modifier.testTag("wifi_server_switch")
                    )
                }
            ) {
                if (isServerRunning && serverUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0E1230))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = serverUrl!!, color = Color(0xFF22E3FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Share URL", serverUrl))
                                Toast.makeText(context, "تم نسخ الرابط للحافظة", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
    
            Spacer(modifier = Modifier.height(10.dp))
    
            // Tool: Authentic Duas Library & Hisn Al-Muslim
            ToolCard(
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFF22E3FF),
                title = "الأدعية المأثورة وحصن المسلم (مكتبة موثقة)",
                subtitle = "أدعية القرآن الكريم، أذكار الصباح والمساء، الرقية، تفريج الكرب، مع عداد تسبيح ونطق صوتي",
                action = {
                    Button(
                        onClick = { showDuasScreen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فتح الأدعية 🤲", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: Extract Audio Track (M4A/AAC) from Video
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Audiotrack,
                iconTint = Color(0xFFFF4081),
                title = "استخراج الصوت النقي (M4A / AAC) من أي فيديو",
                subtitle = "فصل النغمة الصوتية بنقاء 100% بدون إعادة ترميز وحفظها في مجلد الموسيقى بالهاتف",
                action = {
                    Button(
                        onClick = { showAudioExtractDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_audio_extractor_btn")
                    ) {
                        Text("استخراج صوت", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: Video Technical Codec Inspector
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Analytics,
                iconTint = Color(0xFF22E3FF),
                title = "فاحص كودك ومواصفات الفيديو التقنية (Codec Inspector)",
                subtitle = "تحليل كودك الفيديو والصوت (H.264/HEVC/VP9)، الدقة الحقيقية، معدل البت، ومعدل الإطارات",
                action = {
                    Button(
                        onClick = { showInspectorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_codec_inspector_btn")
                    ) {
                        Text("فحص المواصفات", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: Lossless Video Trimmer & Scene Cutter
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Movie,
                iconTint = Color(0xFFFF6B8B),
                title = "مقص واقتطاع مشاهد الفيديو (Fast Lossless Video Trimmer)",
                subtitle = "قص أي جزء زمني بدقة متناهية وبدون إعادة ترميز أو فقدان للجودة وحفظه فوراً في الفيديوهات",
                action = {
                    Button(
                        onClick = {
                            if (videos.isNotEmpty() && selectedVideoForCut == null) {
                                selectedVideoForCut = videos.first()
                                cutStartSeconds = 0f
                                val durSec = (selectedVideoForCut!!.durationMs / 1000f).coerceAtLeast(5f)
                                cutEndSeconds = (durSec * 0.5f).coerceAtLeast(3f).coerceAtMost(durSec)
                            }
                            showVideoCutterDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_video_cutter_btn")
                    ) {
                        Text("قص الفيديو", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: HD Video Frame Grabber & Snapshot Studio
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.CameraAlt,
                iconTint = Color(0xFFFFC857),
                title = "استوديو التقاط اللقطات والصور فائقة الدقة (HD Snapshot Studio)",
                subtitle = "استخراج صور وفريمات عالية النقاء من أي فيديو بالملي ثانية مع فلاتر سينمائية وحفظها في المعرض ومشاركتها",
                action = {
                    Button(
                        onClick = {
                            if (videos.isNotEmpty() && selectedVideoForSnapshot == null) {
                                selectedVideoForSnapshot = videos.first()
                                snapshotTimeSeconds = 5f
                                snapshotBitmap = null
                            }
                            showSnapshotDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_snapshot_studio_btn")
                    ) {
                        Text("استخراج صور", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: Smart Data Saver & Streaming Latency Ping Tester
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Speed,
                iconTint = Color(0xFF5EF0BE),
                title = "حاسبة استهلاك باقة الإنترنت وفاحص سرعة البث (Bandwidth & Ping Advisor)",
                subtitle = "حساب دقيق لاستهلاك الميغابايت لكل دقة (4K/1080p/720p/Audio) مع فحص حقيقي لسرعة استجابة سيرفرات البث",
                action = {
                    Button(
                        onClick = { showDataSaverDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5EF0BE), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_data_saver_btn")
                    ) {
                        Text("حاسبة البيانات", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 8: Fast Video to GIF Maker
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Movie,
                iconTint = Color(0xFF22E3FF),
                title = "محول الفيديو إلى صور متحركة (Fast Video to GIF Maker)",
                subtitle = "تحديد أي ثوانٍ من أي فيديو وتحويلها فوراً إلى صورة GIF خفيفة لمشاركتها مباشرة على واتساب بدون إنترنت",
                action = {
                    Button(
                        onClick = { showGifDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_gif_maker_btn")
                    ) {
                        Text("صنع GIF", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 9: Ringtone Maker & Waveform Cutter
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFFFFC857),
                title = "استوديو صنع نغمات الرنين وقص الصوتيات (Ringtone Maker)",
                subtitle = "مخطط موجات صوتية تفاعلي لاقتطاع أي مقطع موسيقي بدقة وحفظه مباشرة في نغمات رنين الهاتف الرسمية",
                action = {
                    Button(
                        onClick = { showRingtoneDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_ringtone_maker_btn")
                    ) {
                        Text("صنع نغمة", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 10: Smart Offline Video Compressor
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Speed,
                iconTint = Color(0xFF5EF0BE),
                title = "ضاغط الفيديوهات وموفر المساحة بدون إنترنت (Smart Compressor)",
                subtitle = "ضغط الفيديوهات الكبيرة وتوفير حتى 70% من ذاكرة الهاتف مع الحفاظ على وضوح الفيديو لتسهيل الإرسال",
                action = {
                    Button(
                        onClick = { showCompressorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5EF0BE), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_compressor_btn")
                    ) {
                        Text("ضغط الفيديو", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 11: Lossless Audio & Music Remover (Silent Video Maker)
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Headphones,
                iconTint = Color(0xFFFF6B8B),
                title = "إزالة الموسيقى وكتم الصوت من الفيديو (Audio Remover Studio)",
                subtitle = "استخراج فيديو صامت نقي 100% بدون فقدان للجودة في ثوانٍ معدودة لإضافة تعليقك الصوتي أو مشاركته صامتاً",
                action = {
                    Button(
                        onClick = { showAudioRemoverDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_audio_remover_btn")
                    ) {
                        Text("كتم الصوت", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: Smart Cloud Sync & Universal Decentralized Backup
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.CloudSync,
                iconTint = Color(0xFFFFC857),
                title = "المزامنة السحابية الذكية والنسخ اللامركزي الفوري",
                subtitle = "تصدير واستيراد سجل المشاهدة بالملي ثانية، المفضلة، محطات الراديو والتلفاز، والإعدادات عبر رمز سحابي موحد PIN أو كود مشفر",
                action = {
                    Button(
                        onClick = {
                            showBackupDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_cloud_sync_btn")
                    ) {
                        Text("مزامنة واستيراد 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool: Duplicate Media & File Cleaner
        if (section == ToolsSection.ALL || section == ToolsSection.VIDEO) {
            ToolCard(
                icon = Icons.Default.Delete,
                iconTint = Color(0xFFFF4081),
                title = "مكتشف ومنظف الملفات والوسائط المكررة",
                subtitle = "فحص ذاكرة الهاتف واكتشاف مقاطع الفيديو والصوتيات والملفات المكررة لحذفها بضغطة واحدة وتوفير مساحة التخزين",
                action = {
                    Button(
                        onClick = {
                            showDuplicateCleanerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_duplicate_cleaner_btn")
                    ) {
                        Text("فحص المكررات 🧹", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 15: Night Reading Mode
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Nightlight,
                iconTint = Color(0xFFFFC857),
                title = "وضع القراءة والمشاهدة الليلية (Eye Comfort Shield)",
                subtitle = "تصفية الضوء الأزرق لراحة العين أثناء المشاهدة في الظلام",
                action = {
                    Switch(
                        checked = nightShield,
                        onCheckedChange = { enabled ->
                            prefsManager.setNightShield(enabled)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFC857), checkedTrackColor = Color(0xFF272D66))
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 16: Sleep Timer
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Timer,
                iconTint = Color(0xFFE040FB),
                title = "مؤقت النوم الذكي (Sleep Timer)",
                subtitle = "إيقاف التشغيل وإراحة الجهاز تلقائياً بعد فترة محددة",
                action = {
                    Button(
                        onClick = { showSleepTimerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ضبط", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 17: Auto-Play Next Video
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Autorenew,
                iconTint = Color(0xFF22E3FF),
                title = "التشغيل التلقائي للفيديو التالي (Auto-play)",
                subtitle = if (isAutoPlayEnabled) "مفعّل: يبدأ الفيديو التالي بعد 5 ثوانٍ مع نافذة للإلغاء" else "معطّل: يتوقف التشغيل عند نهاية الفيديو",
                action = {
                    Switch(
                        checked = isAutoPlayEnabled,
                        onCheckedChange = { playerManager.setAutoPlayEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF22E3FF),
                            checkedTrackColor = Color(0x6622E3FF)
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 18: Sound Equalizer & Boost
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Equalizer,
                iconTint = Color(0xFF22E3FF),
                title = "موازن الصوت وتضخيم النبرات (Equalizer)",
                subtitle = "أوضاع صوتية: عادي، نبرات عميقة (Bass)، سينما، وضوح أصوات",
                action = {
                    Button(
                        onClick = {
                            val next = when (soundPreset) {
                                "normal" -> "bass"
                                "bass" -> "vocal"
                                "vocal" -> "cinema"
                                else -> "normal"
                            }
                            prefsManager.setSoundPreset(next)
                            Toast.makeText(context, "الوضع الصوتي: $next", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFF22E3FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(soundPreset.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 19: Multi-Language Selector
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Language,
                iconTint = Color(0xFF40C4FF),
                title = "لغة التطبيق (Language Support)",
                subtitle = if (language == "ar") "اللغة الحالية: العربية (الواجهة الافتراضية)" else "Current: English",
                action = {
                    Button(
                        onClick = {
                            val newLang = if (language == "ar") "en" else "ar"
                            prefsManager.setLanguage(newLang)
                            Toast.makeText(context, "تم تبديل اللغة إلى: $newLang", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFF40C4FF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (language == "ar") "العربية" else "English", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 20: Real Adhan, Pre/Post Alarms & Tasbih Center
        // (moved to the Islamic tab) 20. منبهات الأذان (قبل وبعد الصلاة) والتسبيح

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 21: Clear Watch History
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.History,
                iconTint = Color(0xFFFF6B8B),
                title = "إدارة ومسح سجل المشاهدة (Watch History)",
                subtitle = "تصفير مواضع المشاهدة السابقة للحفاظ على الخصوصية التامة",
                action = {
                    Button(
                        onClick = {
                            onClearHistory()
                            Toast.makeText(context, "تم مسح سجل المشاهدة بالكامل", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5252), contentColor = Color(0xFFFF6B8B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("مسح السجل", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool 22: Open Source Info & Code License
        if (section == ToolsSection.ALL || section == ToolsSection.SETTINGS) {
            ToolCard(
                icon = Icons.Default.Info,
                iconTint = Color(0xFFA9B4D6),
                title = "رخصة المصدر المفتوح وتفاصيل المشروع",
                subtitle = "مشغل CinemaX Player الاحترافي مفتوح المصدر ومجاني بالكامل وفق رخصة MIT",
                action = {
                    Button(
                        onClick = { showLicenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFFA9B4D6)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("عرض", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }
    }

    // Direct Video Downloader Dialog with Pre-Verification
    var downloadProgressText by remember { mutableStateOf("") }
    var downloadPercent by remember { mutableIntStateOf(0) }
    var downloadSpeedText by remember { mutableStateOf("") }
    var downloadEtaText by remember { mutableStateOf("") }
    var isDownloadPaused by remember { mutableStateOf(false) }
    var isVerifyingUrl by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<com.example.media.RealMediaDownloader.VideoVerificationResult?>(null) }
    var handledShareRequestId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(incomingShareRequest?.id) {
        val request = incomingShareRequest ?: return@LaunchedEffect
        if (handledShareRequestId == request.id) return@LaunchedEffect
        handledShareRequestId = request.id

        val sharedUrls = DownloadLinkAnalyzer.extractUrls(request.text.orEmpty())
        if (sharedUrls.isNotEmpty()) {
            downloadUrl = sharedUrls.joinToString("\n")
            downloadRequestHeaders = emptyMap()
            verificationResult = null
            showDownloadDialog = true
        }

        if (request.uris.isNotEmpty()) {
            scope.launch {
                var imported = 0
                val failures = mutableListOf<String>()
                request.uris.forEach { sharedUri ->
                    runCatching {
                        com.example.media.RealMediaDownloader.importSharedContent(context, sharedUri)
                    }.onSuccess { imported++ }
                        .onFailure { failures += (it.localizedMessage ?: "تعذر استيراد ملف") }
                }
                val message = when {
                    imported > 0 && failures.isEmpty() -> "تم استيراد $imported ملف إلى مجلدات CinemaX العامة."
                    imported > 0 -> "تم استيراد $imported ملف، وتعذر استيراد ${failures.size} ملف."
                    else -> failures.firstOrNull() ?: "تعذر استيراد الملفات المرسلة."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                if (imported > 0) onMediaChanged()
            }
        } else if (sharedUrls.isEmpty()) {
            Toast.makeText(context, "المشاركة لا تحتوي على رابط HTTP أو ملف يمكن استيراده.", Toast.LENGTH_LONG).show()
        }

        onConsumeIncomingShare()
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading && !isDownloadPaused) {
                    showDownloadDialog = false
                    downloadProgressText = ""
                    downloadPercent = 0
                    downloadSpeedText = ""
                    downloadEtaText = ""
                    isDownloadPaused = false
                    verificationResult = null
                }
            },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF2EE6A6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مركز التنزيل العالمي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "الصق رابط الفيديو أو صفحة الموقع مباشرة. CinemaX سيكتشف المصدر ويختار طريقة التنزيل تلقائياً.",
                        color = Color(0xFFA9B4D6),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = downloadUrl,
                            onValueChange = {
                                downloadUrl = it
                                downloadRequestHeaders = emptyMap()
                                downloadExpectedMimeType = null
                                verificationResult = null
                                universalInfo = null
                                universalInfoUrl = ""
                                selectedUniversalFormat = null
                            },
                            placeholder = { Text("الصق رابط Facebook أو TikTok أو Instagram أو أي صفحة فيديو أو رابط ملف...", color = Color(0xFF8B96BD), fontSize = 11.sp) },
                            minLines = 2,
                            maxLines = 5,
                            enabled = !isDownloading && !isDownloadPaused,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF2EE6A6),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.weight(1f).testTag("download_url_input")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        downloadUrl = clip.trim()
                                        downloadRequestHeaders = emptyMap()
                                        downloadExpectedMimeType = null
                                        verificationResult = null
                                        Toast.makeText(context, "تم لصق الرابط بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الحافظة فارغة! انسخ رابط ملف أو صفحة أولاً", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "تعذر الوصول للحافظة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isDownloading && !isDownloadPaused,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFF2EE6A6)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("paste_download_url_btn")
                        ) {
                            Text("لصق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "لا تحتاج لمعرفة نوع الرابط: الصق رابط صفحة الفيديو أو رابط الملف واضغط تنزيل. يدعم تنزيل الصفحات التي يستطيع محرك المواقع تحليلها، والملفات المباشرة، مع بقاء المواقع المحمية أو غير المدعومة خارج نطاق الاستخراج.",
                        color = Color(0xFF9EABD0),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 5.dp, bottom = 4.dp)
                    )

                    // Verification Check Button & Status
                    if (downloadUrl.isNotBlank() && !isDownloading && !isDownloadPaused) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تحليل الرابط والتحقق من الملف:", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                            OutlinedButton(
                                onClick = {
                                    val links = DownloadLinkAnalyzer.extractUrls(downloadUrl)
                                    when {
                                        links.size > 1 -> Toast.makeText(
                                            context,
                                            "تم إدخال ${links.size} روابط. استخدم زر الإضافة للخلفية لفحصها وإضافتها دفعة واحدة.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        links.size == 1 -> {
                                            isVerifyingUrl = true
                                            verificationResult = null
                                            scope.launch {
                                                try {
                                                    verificationResult = com.example.media.RealMediaDownloader.verifyVideoBeforeDownload(
                                                        links.first(),
                                                        downloadRequestHeaders,
                                                        downloadExpectedMimeType
                                                    )
                                                } catch (error: Exception) {
                                                    Toast.makeText(context, error.localizedMessage ?: "تعذر فحص الرابط", Toast.LENGTH_LONG).show()
                                                } finally {
                                                    isVerifyingUrl = false
                                                }
                                            }
                                        }
                                        else -> Toast.makeText(context, "أدخل رابط HTTP أو HTTPS صالحاً أولاً.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isVerifyingUrl,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (isVerifyingUrl) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF22E3FF))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("جاري الفحص...", fontSize = 10.sp, color = Color(0xFF22E3FF))
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حلّل الرابط 🔍", fontSize = 11.sp, color = Color(0xFF22E3FF), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Verification Result Badge
                    verificationResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        when (res) {
                            is com.example.media.RealMediaDownloader.VideoVerificationResult.Valid -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF063520)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تم التحقق من ملف قابل للتنزيل", color = Color(0xFF2EE6A6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("النوع: ${res.category} · ${res.format}", color = Color.White, fontSize = 11.sp)
                                        Text("اسم الملف: ${res.fileName}", color = Color(0xFFB7C5F5), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("الحجم: ${res.formattedSize}", color = Color(0xFF81C784), fontSize = 11.sp)
                                        if (res.isResumable) {
                                            Text("استئناف التحميل: مدعوم من السيرفر (Range Request)", color = Color(0xFF80CBC4), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            is com.example.media.RealMediaDownloader.VideoVerificationResult.SocialWebPage -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2634)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تم التعرف: ${res.platform}", color = Color(0xFF22E3FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(res.advice, color = Color(0xFFE6EAFA), fontSize = 11.sp)
                                        Text(
                                            "محرك المواقع سيحاول تحليل الصفحة وعرض الصيغ العامة المتاحة. إذا تطلب الموقع حساباً أو منع الاستخراج أو استخدم DRM، سيعرض سبب عدم الإتاحة بدلاً من تنزيل ملف غير صحيح.",
                                            color = Color(0xFFB8C5EB),
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { inspectWithUniversalEngine(res.pageUrl) },
                                                enabled = !isUniversalInspecting && !isUniversalDownloading,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("فحص بمحرك المواقع", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(res.pageUrl)))
                                                    }.onFailure {
                                                        Toast.makeText(context, "تعذر فتح المتصفح الخارجي.", Toast.LENGTH_LONG).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("المتصفح الخارجي", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            is com.example.media.RealMediaDownloader.VideoVerificationResult.Invalid -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF381214)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF6B8B), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تعذر اعتماد الرابط كملف قابل للتنزيل", color = Color(0xFFFF6B8B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(res.reason, color = Color(0xFFFFCDD2), fontSize = 11.sp)
                                        res.detectedType?.let {
                                            Text("نوع المحتوى المكتشف: $it", color = Color(0xFFEF9A9A), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (universalInfoUrl.isNotBlank() && universalInfoUrl == downloadUrl.trim()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B36)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("محرك المواقع", color = Color(0xFF22E3FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isUniversalInspecting) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF22E3FF))
                                    }
                                }
                                universalInfo?.let { info ->
                                    Text(info.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${info.extractor} · ${info.formats.size} صيغة متاحة",
                                        color = Color(0xFFA9B4D6),
                                        fontSize = 10.sp
                                    )
                                    if (info.durationSeconds > 0) {
                                        Text(
                                            "المدة: ${info.durationSeconds / 60}:${(info.durationSeconds % 60).toString().padStart(2, '0')}",
                                            color = Color(0xFFA9B4D6),
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (info.formats.isNotEmpty()) {
                                        Text("اختر الجودة:", color = Color(0xFFA9B4D6), fontSize = 10.sp)
                                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            info.formats.take(6).forEach { format ->
                                                val isSelected = selectedUniversalFormat?.formatId == format.formatId
                                                OutlinedButton(
                                                    onClick = { selectedUniversalFormat = format },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isSelected) Color(0xFF194E5D) else Color.Transparent,
                                                        contentColor = if (isSelected) Color(0xFF2EE6A6) else Color(0xFFE6EAFA)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(format.qualityLabel, fontSize = 10.sp)
                                                        Text(
                                                            when {
                                                                format.hasVideo && format.hasAudio -> "صورة وصوت"
                                                                format.hasVideo -> "فيديو"
                                                                format.hasAudio -> "صوت"
                                                                else -> "ملف"
                                                            },
                                                            color = Color(0xFFA9B4D6),
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                downloadWithUniversalEngine(
                                                    listOf(info.webpageUrl),
                                                    audioOnly = false,
                                                    selectedFormat = selectedUniversalFormat,
                                                    requestHeaders = downloadRequestHeaders
                                                )
                                            },
                                            enabled = !isUniversalDownloading,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2EE6A6),
                                                contentColor = Color(0xFF0E1230)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("تنزيل الفيديو", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                downloadWithUniversalEngine(
                                                    listOf(info.webpageUrl),
                                                    audioOnly = true,
                                                    requestHeaders = downloadRequestHeaders
                                                )
                                            },
                                            enabled = !isUniversalDownloading,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("استخراج MP3", fontSize = 10.sp)
                                        }
                                    }
                                }
                                if (universalEngineVersionMessage.isNotBlank() && universalInfo == null) {
                                    Text(universalEngineVersionMessage, color = Color(0xFFFFC857), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (isUniversalDownloading) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1322)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("محرك المواقع يعمل...", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("${universalDownloadPercent}%", color = Color(0xFF22E3FF), fontSize = 11.sp)
                                }
                                LinearProgressIndicator(
                                    progress = { (universalDownloadPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    color = Color(0xFF2EE6A6),
                                    trackColor = Color(0xFF272D66)
                                )
                                Text(universalDownloadMessage, color = Color(0xFFA9B4D6), fontSize = 9.sp, maxLines = 2)
                                TextButton(onClick = { stopUniversalDownload() }) {
                                    Text("إيقاف محرك المواقع", color = Color(0xFFFF6B8B), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (isDownloading || isDownloadPaused) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1322)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isDownloadPaused) "متوقف مؤقتاً ⏸" else "جاري التنزيل والتسجيل...",
                                        color = if (isDownloadPaused) Color(0xFFFFC857) else Color(0xFF2EE6A6),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (downloadPercent >= 0) {
                                        Text("$downloadPercent%", color = Color(0xFF22E3FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (downloadPercent >= 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (downloadPercent / 100f).coerceIn(0f, 1f) },
                                        color = if (isDownloadPaused) Color(0xFFFFC857) else Color(0xFF2EE6A6),
                                        trackColor = Color(0xFF272D66),
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                    )
                                }

                                if (downloadProgressText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(downloadProgressText, color = Color(0xFFE6EAFA), fontSize = 11.sp)
                                }

                                if (downloadSpeedText.isNotBlank() || downloadEtaText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (downloadSpeedText.isNotBlank()) {
                                            Text("السرعة: $downloadSpeedText", color = Color(0xFF9D7BFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                        if (downloadEtaText.isNotBlank()) {
                                            Text("المتبقي: $downloadEtaText", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Pause / Resume Action Control Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isDownloading && !isDownloadPaused) {
                                        Button(
                                            onClick = {
                                                com.example.media.RealMediaDownloader.requestPause()
                                                isDownloading = false
                                                isDownloadPaused = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("إيقاف مؤقت ⏸", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (isDownloadPaused) {
                                        Button(
                                            onClick = {
                                                isDownloadPaused = false
                                                isDownloading = true
                                                scope.launch {
                                                    val savedMetadata = verificationResult as? com.example.media.RealMediaDownloader.VideoVerificationResult.Valid
                                                    com.example.media.RealMediaDownloader.downloadWithHttpEngine(
                                                        context = context,
                                                        urlStr = savedMetadata?.directUrl ?: downloadUrl.trim(),
                                                        requestHeaders = downloadRequestHeaders,
                                                        customFileName = savedMetadata?.fileName,
                                                        mimeTypeHint = savedMetadata?.mimeType,
                                                        wifiOnly = wifiOnlyDownloads
                                                    ) { progress ->
                                                        scope.launch(Dispatchers.Main) {
                                                            downloadPercent = progress.percent.coerceAtLeast(0)
                                                            downloadSpeedText = com.example.media.RealMediaDownloader.formatSpeed(progress.speedBytesPerSec)
                                                            downloadEtaText = com.example.media.RealMediaDownloader.formatEta(progress.etaSeconds)

                                                            if (progress.totalBytes > 0) {
                                                                val downloadedMb = String.format("%.1f", progress.bytesDownloaded / (1024f * 1024f))
                                                                val totalMb = String.format("%.1f", progress.totalBytes / (1024f * 1024f))
                                                                downloadProgressText = "$downloadedMb ميجابايت من أصل $totalMb ميجابايت"
                                                            } else {
                                                                downloadProgressText = "تم تنزيل ${progress.bytesDownloaded / 1024} كيلوبايت..."
                                                            }

                                                            if (progress.isCompleted) {
                                                                isDownloading = false
                                                                isDownloadPaused = false
                                                                onMediaChanged()
                                                                showDownloadDialog = false
                                                                downloadUrl = ""
                                                                downloadRequestHeaders = emptyMap()
                                                                downloadExpectedMimeType = null
                                                                downloadProgressText = ""
                                                                downloadPercent = 0
                                                                downloadSpeedText = ""
                                                                downloadEtaText = ""
                                                                Toast.makeText(context, "تم استكمال تنزيل الملف وحفظه في المجلد المناسب.", Toast.LENGTH_LONG).show()
                                                            } else if (progress.errorMsg != null) {
                                                                isDownloading = false
                                                                isDownloadPaused = false
                                                                Toast.makeText(context, progress.errorMsg, Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("استئناف التنزيل ▶ (Range Request)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column {
                    if (!isDownloading && !isDownloadPaused) {
                        Button(
                            onClick = {
                                val pastedUrls = DownloadLinkAnalyzer.extractUrls(downloadUrl)
                                val urlToDownload = pastedUrls.firstOrNull()
                                    ?: com.example.media.RealMediaDownloader.normalizeUrl(downloadUrl)
                                if (pastedUrls.size > 1) {
                                    Toast.makeText(
                                        context,
                                        "لديك عدة روابط. استخدم زر إضافة إلى قائمة التنزيلات لتنزيلها بالخلفية.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (urlToDownload.isNotBlank()) {
                                    scope.launch {
                                        try {
                                        // Route website links directly to the universal extractor.
                                        // Users paste the original page URL; the app does not require
                                        // them to discover a separate MP4 URL or inspect the page first.
                                        val linkRoute = DownloadLinkAnalyzer.analyze(urlToDownload).route
                                        val useWebsiteExtractor = downloadExpectedMimeType.isNullOrBlank() &&
                                            linkRoute in setOf(
                                                DownloadLinkAnalyzer.Route.SOCIAL_PAGE,
                                                DownloadLinkAnalyzer.Route.WEB_PAGE,
                                                DownloadLinkAnalyzer.Route.HLS_MANIFEST,
                                                DownloadLinkAnalyzer.Route.DASH_MANIFEST
                                            )
                                        if (useWebsiteExtractor) {
                                            isVerifyingUrl = false
                                            verificationResult = null
                                            universalInfo = null
                                            universalInfoUrl = ""
                                            selectedUniversalFormat = null
                                            Toast.makeText(
                                                context,
                                                "تم اكتشاف رابط صفحة — جاري استخراج الفيديو وتنزيل أفضل جودة متاحة تلقائياً...",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            downloadWithUniversalEngine(
                                                urls = listOf(urlToDownload),
                                                requestHeaders = downloadRequestHeaders
                                            )
                                            return@launch
                                        }

                                        // Direct files and captured CDN media use the direct downloader.
                                        var currentVerification = verificationResult
                                        if (currentVerification == null) {
                                            isVerifyingUrl = true
                                            currentVerification = com.example.media.RealMediaDownloader.verifyVideoBeforeDownload(
                                                urlToDownload,
                                                downloadRequestHeaders,
                                                downloadExpectedMimeType
                                            )
                                            verificationResult = currentVerification
                                            isVerifyingUrl = false
                                        }

                                        if (currentVerification is com.example.media.RealMediaDownloader.VideoVerificationResult.SocialWebPage) {
                                            val socialPage = currentVerification
                                            isDownloading = false
                                            isVerifyingUrl = false
                                            verificationResult = null
                                            universalInfo = null
                                            universalInfoUrl = ""
                                            selectedUniversalFormat = null
                                            Toast.makeText(
                                                context,
                                                "تم اكتشاف صفحة فيديو — جاري استخراج الوسائط وبدء التنزيل تلقائياً...",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            downloadWithUniversalEngine(
                                                urls = listOf(socialPage.pageUrl),
                                                requestHeaders = downloadRequestHeaders
                                            )
                                            return@launch
                                        }

                                        if (currentVerification is com.example.media.RealMediaDownloader.VideoVerificationResult.Invalid) {
                                            val route = DownloadLinkAnalyzer.analyze(urlToDownload).route
                                            if (route == DownloadLinkAnalyzer.Route.WEB_PAGE &&
                                                downloadExpectedMimeType.isNullOrBlank()
                                            ) {
                                                // A normal website URL is not a bad link merely because
                                                // it does not respond like a direct file. Let yt-dlp resolve
                                                // the page and download the best available media itself.
                                                isVerifyingUrl = false
                                                verificationResult = null
                                                universalInfo = null
                                                universalInfoUrl = ""
                                                selectedUniversalFormat = null
                                                Toast.makeText(
                                                    context,
                                                    "الرابط صفحة موقع — جاري تحليلها وتنزيل الفيديو تلقائياً...",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                downloadWithUniversalEngine(
                                                    urls = listOf(urlToDownload),
                                                    requestHeaders = downloadRequestHeaders
                                                )
                                                return@launch
                                            }
                                            Toast.makeText(context, currentVerification.reason, Toast.LENGTH_LONG).show()
                                            return@launch
                                        }

                                        val verified = currentVerification as? com.example.media.RealMediaDownloader.VideoVerificationResult.Valid
                                        if (verified == null) {
                                            Toast.makeText(context, "تعذر تأكيد ملف الفيديو. لم يبدأ التنزيل.", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }
                                        val verifiedDirectUrl = verified.directUrl

                                        isDownloading = true
                                        isDownloadPaused = false
                                        downloadProgressText = "جاري الاتصال بالسيرفر والتحميل المباشر..."
                                        downloadPercent = 0
                                        downloadSpeedText = ""
                                        downloadEtaText = ""

                                        com.example.media.RealMediaDownloader.downloadWithHttpEngine(
                                            context = context,
                                            urlStr = verifiedDirectUrl,
                                            requestHeaders = downloadRequestHeaders,
                                            customFileName = verified.fileName,
                                            mimeTypeHint = verified.mimeType,
                                            wifiOnly = wifiOnlyDownloads
                                        ) { progress ->
                                            scope.launch(Dispatchers.Main) {
                                                downloadPercent = progress.percent.coerceAtLeast(0)
                                                downloadSpeedText = com.example.media.RealMediaDownloader.formatSpeed(progress.speedBytesPerSec)
                                                downloadEtaText = com.example.media.RealMediaDownloader.formatEta(progress.etaSeconds)

                                                if (progress.totalBytes > 0) {
                                                    val downloadedMb = String.format("%.1f", progress.bytesDownloaded / (1024f * 1024f))
                                                    val totalMb = String.format("%.1f", progress.totalBytes / (1024f * 1024f))
                                                    downloadProgressText = "$downloadedMb ميجابايت من أصل $totalMb ميجابايت"
                                                } else {
                                                    downloadProgressText = "تم تنزيل ${progress.bytesDownloaded / 1024} كيلوبايت..."
                                                }

                                                if (progress.isCompleted) {
                                                    isDownloading = false
                                                    isDownloadPaused = false
                                                    onMediaChanged()
                                                    showDownloadDialog = false
                                                    downloadUrl = ""
                                                    downloadRequestHeaders = emptyMap()
                                                    downloadExpectedMimeType = null
                                                    downloadProgressText = ""
                                                    downloadPercent = 0
                                                    downloadSpeedText = ""
                                                    downloadEtaText = ""
                                                    verificationResult = null
                                                    Toast.makeText(context, "✅ تم تنزيل الملف وحفظه في المجلد المناسب بنجاح.", Toast.LENGTH_LONG).show()
                                                } else if (progress.errorMsg != null) {
                                                    isDownloading = false
                                                    isDownloadPaused = false
                                                    Toast.makeText(context, progress.errorMsg, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                        } catch (error: Exception) {
                                            isDownloading = false
                                            isDownloadPaused = false
                                            isVerifyingUrl = false
                                            Toast.makeText(
                                                context,
                                                "تعذر بدء التنزيل: ${error.localizedMessage ?: "تحقق من الرابط والاتصال"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "يرجى إدخال أو لصق رابط الفيديو أولاً!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isVerifyingUrl,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230)),
                            modifier = Modifier.fillMaxWidth().testTag("start_download_btn")
                        ) {
                            Text("تنزيل الرابط تلقائياً 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                val links = DownloadLinkAnalyzer.extractUrls(downloadUrl)
                                if (links.isEmpty()) {
                                    Toast.makeText(context, "أدخل رابط موقع أو ملف HTTP/HTTPS أولاً.", Toast.LENGTH_SHORT).show()
                                } else if (links.size == 1) {
                                    inspectWithUniversalEngine(links.first(), downloadRequestHeaders)
                                } else {
                                    downloadWithUniversalEngine(links)
                                }
                            },
                            enabled = !isUniversalDownloading && !isUniversalInspecting,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("universal_media_engine_button")
                        ) {
                            Text("فحص متقدم للجودة أو تنزيل عدة روابط", color = Color(0xFF9D7BFF), fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                val links = DownloadLinkAnalyzer.extractUrls(downloadUrl)
                                if (links.isEmpty()) {
                                    Toast.makeText(context, "ألصق رابط HTTP أو HTTPS صالحاً أولاً.", Toast.LENGTH_SHORT).show()
                                } else {
                                    isVerifyingUrl = true
                                    scope.launch {
                                        try {
                                            if (links.size == 1) {
                                                when (
                                                    val check = com.example.media.RealMediaDownloader.verifyVideoBeforeDownload(
                                                        links.first(),
                                                        downloadRequestHeaders,
                                                        downloadExpectedMimeType
                                                    )
                                                ) {
                                                    is com.example.media.RealMediaDownloader.VideoVerificationResult.SocialWebPage -> {
                                                        inspectWithUniversalEngine(check.pageUrl, downloadRequestHeaders)
                                                    }
                                                    is com.example.media.RealMediaDownloader.VideoVerificationResult.Invalid -> {
                                                        Toast.makeText(context, check.reason, Toast.LENGTH_LONG).show()
                                                    }
                                                    is com.example.media.RealMediaDownloader.VideoVerificationResult.Valid -> {
                                                        com.example.media.RealMediaDownloader.downloadWithSystemManager(
                                                            context = context,
                                                            urlStr = check.directUrl,
                                                            customFileName = check.fileName,
                                                            mimeType = check.mimeType,
                                                            requestHeaders = downloadRequestHeaders,
                                                            wifiOnly = wifiOnlyDownloads
                                                        )
                                                        showDownloadDialog = false
                                                        downloadUrl = ""
                                                        verificationResult = null
                                                        Toast.makeText(
                                                            context,
                                                            "أُضيف ${check.fileName} إلى مدير تنزيلات Android.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                val result = com.example.media.RealMediaDownloader.enqueueDownloadQueue(
                                                    context = context,
                                                    rawLinks = links,
                                                    requestHeaders = downloadRequestHeaders,
                                                    wifiOnly = wifiOnlyDownloads
                                                )
                                                if (result.queuedCount > 0) {
                                                    showDownloadDialog = false
                                                    downloadUrl = ""
                                                    verificationResult = null
                                                }
                                                val summary = buildString {
                                                    append("تمت إضافة ${result.queuedCount} ملف إلى قائمة تنزيلات Android.")
                                                    if (result.skippedPages > 0) append(" تم تجاوز ${result.skippedPages} رابط صفحة ويب أو منصة.")
                                                    if (result.failures.isNotEmpty()) append(" تعذر إضافة ${result.failures.size} رابط؛ أول سبب: ${result.failures.first()}")
                                                }
                                                Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                                            }
                                        } catch (error: Exception) {
                                            Toast.makeText(
                                                context,
                                                "تعذر إضافة التنزيل: ${error.localizedMessage ?: "تحقق من الرابط والاتصال"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } finally {
                                            isVerifyingUrl = false
                                        }
                                    }
                                }
                            },
                            enabled = !isVerifyingUrl,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("system_download_btn")
                        ) {
                            Text("إضافة إلى قائمة التنزيلات بالخلفية (ملف واحد أو عدة روابط)", color = Color(0xFF22E3FF), fontSize = 11.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isDownloading) {
                            com.example.media.RealMediaDownloader.requestPause()
                        }
                        showDownloadDialog = false
                        isDownloading = false
                        isDownloadPaused = false
                        downloadProgressText = ""
                        downloadPercent = 0
                        downloadSpeedText = ""
                        downloadEtaText = ""
                    }
                ) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // Web Video Sniffer Dialog
    if (showSnifferDialog) {
        WebVideoSnifferDialog(
            initialUrl = if (downloadUrl.isNotBlank()) com.example.media.RealMediaDownloader.normalizeUrl(downloadUrl) else "",
            onVideoFound = { source: com.example.media.CapturedVideoSource ->
                downloadUrl = source.url
                downloadRequestHeaders = source.requestHeaders
                verificationResult = null
                showSnifferDialog = false
                showDownloadDialog = true
                if (com.example.media.WebVideoSnifferHelper.isMediaManifestUrl(source.url, source.mimeType)) {
                    // HLS/DASH manifests are not single files. Let yt-dlp resolve
                    // permitted, non-DRM streams and the formats exposed by the page.
                    downloadExpectedMimeType = null
                    inspectWithUniversalEngine(source.url, source.requestHeaders)
                    Toast.makeText(context, "تم العثور على قائمة بث. جاري تحليل الصيغ المتاحة...", Toast.LENGTH_LONG).show()
                } else {
                    // A wildcard MIME describes the HTML element, not the actual server response.
                    downloadExpectedMimeType = source.mimeType?.takeUnless { it.endsWith("/*") }
                    Toast.makeText(context, "تم التقاط مصدر وسائط. افحص الرابط ثم ابدأ التنزيل.", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = {
                showSnifferDialog = false
            }
        )
    }

    // Cloud & Decentralized Sync Dialog
    if (showBackupDialog) {
        CinemaXSyncDialog(
            onDismiss = { showBackupDialog = false },
            onOpenLocalWifiShare = {
                showBackupDialog = false
                if (!isServerRunning) {
                    shareServer.start()
                    Toast.makeText(context, "تم تشغيل خادم المشاركة المحلي لنقل الملفات الكبيرة", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Duplicate Cleaner Dialog
    if (showDuplicateCleanerDialog) {
        DuplicateCleanerDialog(
            onDismiss = { showDuplicateCleanerDialog = false },
            onMediaDeleted = onMediaChanged
        )
    }

    // Audio Extraction Dialog
    if (showAudioExtractDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isExtractingAudio) {
                    showAudioExtractDialog = false
                    selectedVideoForAudio = null
                }
            },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFFFF4081))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استخراج الصوت النقي من الفيديو", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "اختر مقطع فيديو من جهازك لاستخراج المسار الصوتي بصيغة M4A/AAC نقية وحفظها في مجلد الموسيقى:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (videos.isEmpty()) {
                        Text("لا توجد مقاطع فيديو في الذاكرة حالياً.", color = Color(0xFFFF6B8B), fontSize = 12.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(videos) { v ->
                                val isSelected = selectedVideoForAudio?.uri == v.uri
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFF4081).copy(alpha = 0.2f) else Color(0xFF141943)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF4081)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272D66)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedVideoForAudio = v }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 64.dp, height = 44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF272D66))
                                        ) {
                                            com.example.ui.screens.VideoThumbnailImage(
                                                uriString = v.uri,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(0xCC000000))
                                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            ) {
                                                Text(
                                                    text = com.example.media.MediaScanner.formatDuration(v.durationMs),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = v.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${v.resolution} • ${com.example.media.MediaScanner.formatFileSize(v.sizeBytes)}",
                                                color = Color(0xFFFF80AB),
                                                fontSize = 10.sp
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(20.dp))
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
                    onClick = {
                        val video = selectedVideoForAudio ?: return@Button
                        isExtractingAudio = true
                        scope.launch {
                            val result = com.example.media.VideoAudioExtractor.extractAudioTrack(
                                context = context,
                                videoUri = Uri.parse(video.uri),
                                outputTitle = video.title
                            )
                            isExtractingAudio = false
                            result.onSuccess {
                                showAudioExtractDialog = false
                                selectedVideoForAudio = null
                                Toast.makeText(context, "✅ تم استخراج الصوت بنجاح وحفظه في مجلد الموسيقى بالهاتف!", Toast.LENGTH_LONG).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "فشل استخراج الصوت: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = selectedVideoForAudio != null && !isExtractingAudio,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081), contentColor = Color.White)
                ) {
                    if (isExtractingAudio) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري الاستخراج...", fontSize = 11.sp)
                    } else {
                        Text("استخراج الصوت الآن 🎵", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioExtractDialog = false }, enabled = !isExtractingAudio) {
                    Text("إلغاء", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // Codec & Metadata Inspector Dialog
    if (showInspectorDialog) {
        AlertDialog(
            onDismissRequest = {
                showInspectorDialog = false
                selectedVideoForInspect = null
                inspectedSpecs = null
            },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF22E3FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فاحص كودك ومواصفات الفيديو", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "اختر مقطع فيديو لفحص تفاصيل الترميز والدقة الحقيقية ومعدل البت:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (videos.isEmpty()) {
                        Text("لا توجد مقاطع فيديو في الذاكرة حالياً.", color = Color(0xFFFF6B8B), fontSize = 12.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(videos) { v ->
                                val isSelected = selectedVideoForInspect?.uri == v.uri
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF22E3FF).copy(alpha = 0.2f) else Color(0xFF141943)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF22E3FF)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272D66)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedVideoForInspect = v
                                            isInspecting = true
                                            scope.launch {
                                                inspectedSpecs = com.example.media.VideoMetadataInspector.inspectVideo(context, Uri.parse(v.uri))
                                                isInspecting = false
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 56.dp, height = 38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF272D66))
                                        ) {
                                            com.example.ui.screens.VideoThumbnailImage(
                                                uriString = v.uri,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(0xCC000000))
                                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            ) {
                                                Text(
                                                    text = com.example.media.MediaScanner.formatDuration(v.durationMs),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = v.title,
                                                color = if (isSelected) Color(0xFF22E3FF) else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${v.resolution} • ${com.example.media.MediaScanner.formatFileSize(v.sizeBytes)}",
                                                color = Color(0xFF80DEEA),
                                                fontSize = 9.sp
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isInspecting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF22E3FF), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري فحص الترميز والكودك...", color = Color(0xFF22E3FF), fontSize = 11.sp)
                            }
                        } else {
                            inspectedSpecs?.let { specs ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090F1C)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272D66)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("📊 المواصفات التقنية المستخرجة:", color = Color(0xFF22E3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("• الدقة: ${specs.resolution}", color = Color.White, fontSize = 11.sp)
                                        Text("• كودك الفيديو: ${specs.videoCodec}", color = Color(0xFF81C784), fontSize = 11.sp)
                                        Text("• كودك الصوت: ${specs.audioCodec}", color = Color(0xFFFFB74D), fontSize = 11.sp)
                                        Text("• معدل الإطارات: ${specs.frameRate} FPS", color = Color(0xFFBA68C8), fontSize = 11.sp)
                                        Text("• معدل البت: ${specs.bitrateKbps} kbps", color = Color(0xFF4FC3F7), fontSize = 11.sp)
                                        Text("• القنوات الصوتية: ${specs.audioChannels} قنوات (${specs.audioSampleRate} Hz)", color = Color(0xFFA9B4D6), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInspectorDialog = false
                        selectedVideoForInspect = null
                        inspectedSpecs = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
                ) {
                    Text("تم", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = { Text("مؤقت النوم التلقائي", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "اختر بعد كم دقيقة يتم إيقاف تشغيل الفيديو تلقائياً:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sleepMinutes == mins) Color(0xFFE040FB) else Color(0xFF272D66))
                                    .clickable { sleepMinutes = mins }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins د",
                                    color = if (sleepMinutes == mins) Color.White else Color(0xFFA9B4D6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerManager.setSleepTimer(sleepMinutes)
                        showSleepTimerDialog = false
                        Toast.makeText(context, "تم ضبط مؤقت النوم على $sleepMinutes دقيقة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB), contentColor = Color.White)
                ) {
                    Text("تأكيد المؤقت", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("إلغاء", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // Open Source License Dialog
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = { Text("مشروع CinemaX Player السينمائي", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "CinemaX Player تطبيق أندرويد لتشغيل وإدارة الفيديو والصوت والبث المباشر والراديو، مع أدوات لمعالجة الوسائط ومركز إسلامي.\n\nالمكونات الرئيسية:\n• مشغّل Media3 مع مسارات الترجمة والجودة المتاحة وPiP وGoogle Cast\n• مكتبة فيديو وصوت ومجلدات محلية\n• استوديو صوت ومعادل ترددات ومؤثرات صوتية حسب دعم الجهاز\n• قص الفيديو والصوت، استخراج الصوت، الضغط، اللقطات، GIF وفحص الترميز\n• التلفاز المباشر والراديو وفق توفر مصادر البث\n• القرآن الكريم ومواقيت الصلاة والقبلة والأذكار\n• خزنة محلية، نسخ احتياطي ومشاركة عبر Wi-Fi\n• Kotlin وJetpack Compose وAndroidX Media3 وRoom",
                        color = Color(0xFFC3CCE8),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLicenseDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Video Cutter & Scene Trimmer Dialog
    if (showVideoCutterDialog) {
        val totalDurSec = if (selectedVideoForCut != null) (selectedVideoForCut!!.durationMs / 1000f).coerceAtLeast(1f) else 60f
        AlertDialog(
            onDismissRequest = { if (!isTrimmingVideo) showVideoCutterDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFFF6B8B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مقص واقتطاع مشاهد الفيديو السريع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (videos.isEmpty()) {
                        Text("لا توجد مقاطع فيديو محلية متاحة حالياً في الجهاز للقص.", color = Color(0xFFA9B4D6), fontSize = 13.sp)
                    } else {
                        Text("اختر الفيديو المطلوب اقتصاصه:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(videos) { vid ->
                                val isSelected = selectedVideoForCut?.id == vid.id
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFF6B8B).copy(alpha = 0.25f) else Color(0xFF1A1F4A)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(0xFFFF6B8B) else Color(0xFF272D66)
                                    ),
                                    modifier = Modifier
                                        .width(105.dp)
                                        .clickable {
                                            selectedVideoForCut = vid
                                            cutStartSeconds = 0f
                                            val d = (vid.durationMs / 1000f).coerceAtLeast(5f)
                                            cutEndSeconds = (d * 0.5f).coerceAtMost(d)
                                        }
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                        ) {
                                            com.example.ui.screens.VideoThumbnailImage(
                                                uriString = vid.uri,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xCC000000))
                                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = com.example.media.MediaScanner.formatDuration(vid.durationMs),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = vid.title,
                                            color = if (isSelected) Color(0xFFFF6B8B) else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        selectedVideoForCut?.let { curVid ->
                            Text(
                                text = "الفيديو: ${curVid.title} (${curVid.resolution ?: "HD"})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Start time slider
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("نقطة البداية (Start):", color = Color(0xFFC3CCE8), fontSize = 11.sp)
                                Text(
                                    text = String.format("%02d:%02d", cutStartSeconds.toInt() / 60, cutStartSeconds.toInt() % 60),
                                    color = Color(0xFFFF6B8B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = cutStartSeconds,
                                onValueChange = {
                                    cutStartSeconds = it
                                    if (cutEndSeconds <= cutStartSeconds) {
                                        cutEndSeconds = (cutStartSeconds + 2f).coerceAtMost(totalDurSec)
                                    }
                                },
                                valueRange = 0f..totalDurSec,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6B8B), activeTrackColor = Color(0xFFFF6B8B)),
                                enabled = !isTrimmingVideo
                            )

                            // End time slider
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("نقطة النهاية (End):", color = Color(0xFFC3CCE8), fontSize = 11.sp)
                                Text(
                                    text = String.format("%02d:%02d", cutEndSeconds.toInt() / 60, cutEndSeconds.toInt() % 60),
                                    color = Color(0xFFFF6B8B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = cutEndSeconds,
                                onValueChange = {
                                    cutEndSeconds = it
                                    if (cutStartSeconds >= cutEndSeconds) {
                                        cutStartSeconds = (cutEndSeconds - 2f).coerceAtLeast(0f)
                                    }
                                },
                                valueRange = 0f..totalDurSec,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF6B8B), activeTrackColor = Color(0xFFFF6B8B)),
                                enabled = !isTrimmingVideo
                            )

                            val clipLen = (cutEndSeconds - cutStartSeconds).coerceAtLeast(0f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0E1230))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "مدة المقطع الناتج: ${clipLen.toInt()} ثانية (سرعة فائقة بدون إعادة ترميز)",
                                    color = Color(0xFF22E3FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isTrimmingVideo) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { trimProgressState / 100f },
                                    color = Color(0xFFFF6B8B),
                                    trackColor = Color(0xFF272D66),
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = trimStatusState, color = Color(0xFFFFC857), fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedVideoForCut != null && !isTrimmingVideo) {
                    Button(
                        onClick = {
                            val vid = selectedVideoForCut!!
                            val startMs = (cutStartSeconds * 1000L).toLong()
                            val endMs = (cutEndSeconds * 1000L).toLong()
                            isTrimmingVideo = true
                            trimProgressState = 0
                            trimStatusState = "جاري البدء..."

                            scope.launch {
                                val res = VideoCutterTrimmer.trimVideo(
                                    context = context,
                                    sourceUri = Uri.parse(vid.uri),
                                    startMs = startMs,
                                    endMs = endMs,
                                    outputName = vid.title
                                ) { progress ->
                                    trimProgressState = progress.percent
                                    trimStatusState = progress.status
                                }
                                isTrimmingVideo = false
                                res.onSuccess { file ->
                                    Toast.makeText(context, "تم حفظ المقطع في Movies/CinemaX_Clips:\n${file.name}", Toast.LENGTH_LONG).show()
                                    showVideoCutterDialog = false
                                }.onFailure { err ->
                                    Toast.makeText(context, "فشل القص: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White),
                        enabled = (cutEndSeconds - cutStartSeconds) > 0.5f
                    ) {
                        Text("قص وحفظ المقطع", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVideoCutterDialog = false },
                    enabled = !isTrimmingVideo
                ) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // HD Video Frame Grabber & Snapshot Studio Dialog
    if (showSnapshotDialog) {
        val totalDurSec = if (selectedVideoForSnapshot != null) (selectedVideoForSnapshot!!.durationMs / 1000f).coerceAtLeast(1f) else 60f
        AlertDialog(
            onDismissRequest = { if (!isCapturingSnapshot) showSnapshotDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFFFC857))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استوديو اللقطات والصور فائقة الدقة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (videos.isEmpty()) {
                        Text("لا توجد مقاطع فيديو محلية متاحة لالتقاط الصور.", color = Color(0xFFA9B4D6), fontSize = 13.sp)
                    } else {
                        Text("اختر الفيديو:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(videos) { vid ->
                                val isSelected = selectedVideoForSnapshot?.id == vid.id
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFFC857).copy(alpha = 0.25f) else Color(0xFF1A1F4A)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(0xFFFFC857) else Color(0xFF272D66)
                                    ),
                                    modifier = Modifier
                                        .width(105.dp)
                                        .clickable {
                                            selectedVideoForSnapshot = vid
                                            snapshotBitmap = null
                                            snapshotTimeSeconds = 5f.coerceAtMost(vid.durationMs / 1000f)
                                        }
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                        ) {
                                            com.example.ui.screens.VideoThumbnailImage(
                                                uriString = vid.uri,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xCC000000))
                                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = com.example.media.MediaScanner.formatDuration(vid.durationMs),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = vid.title,
                                            color = if (isSelected) Color(0xFFFFC857) else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        selectedVideoForSnapshot?.let { curVid ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("توقيت اللقطة (Seek):", color = Color(0xFFC3CCE8), fontSize = 11.sp)
                                Text(
                                    text = String.format("%02d:%02d (%d ث)", snapshotTimeSeconds.toInt() / 60, snapshotTimeSeconds.toInt() % 60, snapshotTimeSeconds.toInt()),
                                    color = Color(0xFFFFC857),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = snapshotTimeSeconds,
                                onValueChange = {
                                    snapshotTimeSeconds = it
                                },
                                valueRange = 0f..totalDurSec,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFC857), activeTrackColor = Color(0xFFFFC857)),
                                enabled = !isCapturingSnapshot
                            )

                            // Filter selection chips
                            Text("فلتر التصوير:", color = Color(0xFFC3CCE8), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(VideoSnapshotStudio.SnapshotFilter.values().toList()) { flt ->
                                    val isSelected = snapshotFilter == flt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF22E3FF) else Color(0xFF272D66))
                                            .clickable { snapshotFilter = flt }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = flt.displayName,
                                            color = if (isSelected) Color(0xFF0E1230) else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    isCapturingSnapshot = true
                                    scope.launch {
                                        val res = VideoSnapshotStudio.captureFrame(
                                            context = context,
                                            videoUri = Uri.parse(curVid.uri),
                                            timeMs = (snapshotTimeSeconds * 1000L).toLong(),
                                            filter = snapshotFilter
                                        )
                                        isCapturingSnapshot = false
                                        res.onSuccess { bmp ->
                                            snapshotBitmap = bmp
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFFFFC857)),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isCapturingSnapshot
                            ) {
                                if (isCapturingSnapshot) {
                                    CircularProgressIndicator(color = Color(0xFFFFC857), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري استخراج اللقطة...", fontSize = 11.sp)
                                } else {
                                    Text("معاينة واستخراج اللقطة الحالية 📸", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            snapshotBitmap?.let { bmp ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "معاينة اللقطة",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val saveRes = VideoSnapshotStudio.saveSnapshotToGallery(context, bmp, curVid.title)
                                                saveRes.onSuccess {
                                                    Toast.makeText(context, "تم حفظ الصورة بنجاح في المعرض (Pictures/CinemaX_Snapshots)!", Toast.LENGTH_LONG).show()
                                                }.onFailure { e ->
                                                    Toast.makeText(context, "فشل الحفظ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF0E1230)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("حفظ في المعرض", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val shareRes = VideoSnapshotStudio.shareSnapshot(context, bmp, curVid.title)
                                                shareRes.onSuccess { intent ->
                                                    context.startActivity(intent)
                                                }.onFailure { e ->
                                                    Toast.makeText(context, "تعذر المشاركة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("مشاركة اللقطة", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    // Smart Data Saver & Latency Ping Advisor Dialog
    if (showDataSaverDialog) {
        val selectedProfile = DataSaverCalculator.profiles.getOrElse(selectedProfileIdx) { DataSaverCalculator.profiles[1] }
        val estimatedUsageMB = selectedProfile.calculateUsageMB(watchTimeMinutes)
        val usageStr = if (estimatedUsageMB >= 1024f) {
            String.format("%.2f جيجابايت (GB)", estimatedUsageMB / 1024f)
        } else {
            String.format("%.0f ميجابايت (MB)", estimatedUsageMB)
        }

        AlertDialog(
            onDismissRequest = { if (!isTestingPing) showDataSaverDialog = false },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF5EF0BE))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حاسبة استهلاك الباقة وسرعة البث", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("اختر جودة البث لمطابقتها مع باقة هاتفك:", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    DataSaverCalculator.profiles.forEachIndexed { index, profile ->
                        val isSelected = index == selectedProfileIdx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x3369F0AE) else Color(0xFF272D66))
                                .border(1.dp, if (isSelected) Color(0xFF5EF0BE) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedProfileIdx = index }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = profile.nameAr, color = if (isSelected) Color(0xFF5EF0BE) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(text = profile.description, color = Color(0xFFC3CCE8), fontSize = 10.sp)
                            }
                            Text(text = "${profile.bitrateKbps} Kbps", color = Color(0xFF22E3FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("مدة المشاهدة المتوقعة:", color = Color(0xFFC3CCE8), fontSize = 12.sp)
                        val hours = watchTimeMinutes / 60
                        val mins = watchTimeMinutes % 60
                        val durDisplay = if (hours > 0) "$hours س و $mins د" else "$mins دقيقة"
                        Text(durDisplay, color = Color(0xFF5EF0BE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Slider(
                        value = watchTimeMinutes.toFloat(),
                        onValueChange = { watchTimeMinutes = it.toInt() },
                        valueRange = 15f..360f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF5EF0BE), activeTrackColor = Color(0xFF5EF0BE))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E1230))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("الاستهلاك التقديري الفعلي للباقة:", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(usageStr, color = Color(0xFF5EF0BE), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("فاحص سرعة استجابة سيرفرات البث المباشر (Ping):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            isTestingPing = true
                            pingResults = emptyList()
                            scope.launch {
                                val p1 = DataSaverCalculator.testServerPing("https://iptv-org.github.io", "قنوات البث المباشر العالمية (IPTV-Org)")
                                val p2 = DataSaverCalculator.testServerPing("https://de1.api.radio-browser.info", "محطات الراديو العالمية (Radio-Browser)")
                                val p3 = DataSaverCalculator.testServerPing("https://www.google.com", "الإنترنت العام وشبكة الجهاز (Google DNS)")
                                pingResults = listOf(p1, p2, p3)
                                isTestingPing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFF5EF0BE)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTestingPing
                    ) {
                        if (isTestingPing) {
                            CircularProgressIndicator(color = Color(0xFF5EF0BE), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري قياس السرعة والاستجابة...", fontSize = 11.sp)
                        } else {
                            Text("اختبار سرعة الاتصال وسيرفرات البث 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pingResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        pingResults.forEach { ping ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0E1230))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = ping.label, color = Color(0xFFC3CCE8), fontSize = 10.sp, modifier = Modifier.weight(1f))
                                val pingColor = when {
                                    !ping.isSuccess -> Color(0xFFFF6B8B)
                                    ping.pingMs < 120 -> Color(0xFF5EF0BE)
                                    ping.pingMs < 280 -> Color(0xFFFFC857)
                                    else -> Color(0xFFFF6B8B)
                                }
                                val statusText = if (ping.isSuccess) "${ping.pingMs} ms (ممتاز)" else "غير متصل"
                                Text(text = statusText, color = pingColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDataSaverDialog = false }, enabled = !isTestingPing) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }

    if (showGifDialog) {
        VideoToGifDialog(
            videos = videos,
            onDismiss = { showGifDialog = false }
        )
    }

    if (showRingtoneDialog) {
        RingtoneMakerDialog(
            videos = videos,
            onDismiss = { showRingtoneDialog = false }
        )
    }

    if (showCompressorDialog) {
        VideoCompressorDialog(
            videos = videos,
            onDismiss = { showCompressorDialog = false }
        )
    }

    if (showAudioRemoverDialog) {
        VideoAudioRemoverDialog(
            videos = videos,
            onDismiss = { showAudioRemoverDialog = false }
        )
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF272D66)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                action()
            }

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(6.dp))
                extraContent()
            }
        }
    }
}
