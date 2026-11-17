package com.example.ui.screens.prayer

import com.example.ui.theme.cinemaBackground

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.example.data.model.AdhanVoice
import com.example.data.model.CityLocation
import com.example.data.model.PrayerTimeEntry
import com.example.data.model.PrayerType
import com.example.prayer.AdhanVoicesCatalog
import com.example.prayer.PrayerTimesCalculator
import com.example.prayer.PrayerTimesManager
import kotlin.math.cos
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun PrayerTimesScreen(
    prayerManager: PrayerTimesManager,
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prayerTimes by prayerManager.prayerTimes.collectAsState()
    val selectedCity by prayerManager.selectedCity.collectAsState()
    val isUsingGps by prayerManager.isUsingGps.collectAsState()
    val countdownSec by prayerManager.nextPrayerCountdownSeconds.collectAsState()
    val nextPrayerTitle by prayerManager.nextPrayerTitle.collectAsState()
    val qiblaBearing by prayerManager.qiblaBearing.collectAsState()
    val compassHeading by prayerManager.compassHeading.collectAsState()
    val distanceKm by prayerManager.distanceToKaabaKm.collectAsState()
    val selectedVoice by prayerManager.selectedAdhanVoice.collectAsState()
    val isPlayingPreview by prayerManager.isPlayingAdhanPreview.collectAsState()
    val previewingVoiceId by prayerManager.previewingVoiceId.collectAsState()
    val perPrayerVoices by prayerManager.perPrayerVoices.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, 3)
    }

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    DisposableEffect(selectedTab) {
        if (selectedTab == 1) {
            prayerManager.startCompass()
        }
        onDispose {
            prayerManager.stopCompass()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(context, "تم تفعيل إشعارات الأذان بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    prayerManager.updateCoordinatesFromGps(loc.latitude, loc.longitude)
                    Toast.makeText(context, "تم تحديد موقعك الجغرافي بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "جاري استقبال إحداثيات GPS...", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {}
        } else {
            Toast.makeText(context, "يرجى منح إذن الموقع لتحديد المواقيت والقبلة آلياً", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFD98A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFFFFD98A), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "الصلاة والأذان والتسبيح",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "المؤذن: ${selectedVoice.muazzinName}",
                        color = Color(0xFF2EE6A6),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showDiagnosticDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1F4A))
                        .border(1.dp, Color(0xFF22E3FF).copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "فحص المنبهات والأذان",
                        tint = Color(0xFF22E3FF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1F4A))
                        .border(1.dp, Color(0xFF2EE6A6).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { showCityDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isUsingGps) Icons.Default.MyLocation else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF2EE6A6),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCity.nameAr,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4 Clean Single-Line Navigation Tabs
        val tabTitles = listOf("المواقيت", "القبلة", "المؤذن والمنبهات", "التسبيح والسجل")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFFD98A),
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF2EE6A6)
                )
            }
        ) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("prayer_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> PrayerTimesTabContent(
                prayerManager = prayerManager,
                prayerTimes = prayerTimes,
                nextPrayerTitle = nextPrayerTitle,
                countdownSec = countdownSec,
                distanceKm = distanceKm,
                cityName = selectedCity.nameAr,
                selectedVoice = selectedVoice,
                perPrayerVoices = perPrayerVoices,
                onOpenMuezzinTab = { selectedTab = 2 },
                onDetectGps = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
            1 -> QiblaCompassTabContent(
                qiblaBearing = qiblaBearing,
                compassHeading = compassHeading,
                distanceKm = distanceKm
            )
            2 -> AdhanSettingsAndVoicesTabContent(
                prayerManager = prayerManager,
                selectedVoice = selectedVoice,
                isPlayingPreview = isPlayingPreview,
                previewingVoiceId = previewingVoiceId,
                perPrayerVoices = perPrayerVoices
            )
            3 -> PrayerTasbihAndLogTab(prayerManager = prayerManager)
        }
    }

    if (showCityDialog) {
        CityPickerDialog(
            currentCity = selectedCity,
            onSelectCity = {
                prayerManager.setCity(it)
                showCityDialog = false
            },
            onDetectGps = {
                showCityDialog = false
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { showCityDialog = false }
        )
    }

    if (showDiagnosticDialog) {
        AdhanDiagnosticDialog(
            prayerManager = prayerManager,
            onDismiss = { showDiagnosticDialog = false }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PrayerTimesTabContent(
    prayerManager: PrayerTimesManager,
    prayerTimes: List<PrayerTimeEntry>,
    nextPrayerTitle: String,
    countdownSec: Long,
    distanceKm: Int,
    cityName: String,
    selectedVoice: AdhanVoice,
    perPrayerVoices: Map<PrayerType, String>,
    onOpenMuezzinTab: () -> Unit,
    onDetectGps: () -> Unit
) {
    val context = LocalContext.current
    val preMin by prayerManager.preAdhanMinutesBefore.collectAsState()
    val postMin by prayerManager.postAdhanMinutesAfter.collectAsState()
    var isIgnoringBattery by remember { mutableStateOf(prayerManager.isIgnoringBatteryOptimizations()) }
    var canScheduleExact by remember { mutableStateOf(prayerManager.canScheduleExactAlarms()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Countdown Banner
        item {
            val hours = countdownSec / 3600
            val minutes = (countdownSec % 3600) / 60
            val seconds = countdownSec % 60

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0x66FFD98A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF0B6E5B), Color(0xFF1D5C9A), Color(0xFF3B2A8C))
                        )
                    )
                    .testTag("prayer_countdown_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الصلاة القادمة: صلاة $nextPrayerTitle",
                            color = Color(0xFFFFD98A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$distanceKm كم إلى مكة",
                            color = Color(0xFFA9B4D6),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "متبقي على دخول وقت الأذان في $cityName",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDetectGps,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF), contentColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث الموقع بدقة GPS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Active Muezzin & Pre/Post Reminder Quick Switch Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                border = BorderStroke(1.dp, Color(0xFF22E3FF).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x3322E3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المؤذن المختار: ${selectedVoice.muazzinName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "قبل الأذان: ${if (preMin > 0) "${preMin}د" else "مغلق"} • بعد الأذان: ${if (postMin > 0) "${postMin}د" else "مغلق"}",
                            color = Color(0xFF22E3FF),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = onOpenMuezzinTab,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("change_muezzin_btn")
                    ) {
                        Text("تغيير المؤذن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Test & Stop Adhan Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        prayerManager.testAdhan(5)
                        Toast.makeText(context, "سيصدح صوت الأذان المختار خلال 5 ثوانٍ 🔔", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFF2EE6A6)),
                    border = BorderStroke(1.dp, Color(0xFF2EE6A6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("test_adhan_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختبار الأذان (5ث)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        prayerManager.stopActiveAdhan()
                        Toast.makeText(context, "تم إيقاف صوت الأذان", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color(0xFFFF6B8B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B8B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.StopCircle, contentDescription = "إيقاف الأذان", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إيقاف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (!isIgnoringBattery) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                    border = BorderStroke(1.dp, Color(0xFFFFC857)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("استثناء التطبيق من تحسين البطارية", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("مطلوب لضمان إطلاق الأذان وتنبيهات قبل/بعد الصلاة خارج التطبيق.", color = Color(0xFFA9B4D6), fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                prayerManager.requestIgnoreBatteryOptimizations()
                                isIgnoringBattery = prayerManager.isIgnoringBatteryOptimizations()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF0E1230)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (!canScheduleExact) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261214)),
                    border = BorderStroke(1.dp, Color(0xFFFF6B8B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إذن المنبهات الدقيقة (EXACT_ALARM)", color = Color(0xFFFF6B8B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("يرجى تفعيله ليعمل الأذان والتنبيهات في موعدها بالثانية.", color = Color(0xFFEF9A9A), fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                prayerManager.openExactAlarmSettings()
                                canScheduleExact = prayerManager.canScheduleExactAlarms()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8B), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("منح الإذن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(prayerTimes) { entry ->
            val assignedVoice = perPrayerVoices[entry.type]?.let { AdhanVoicesCatalog.getVoiceById(it) } ?: selectedVoice
            PrayerTimeRowCard(
                entry = entry,
                muezzinLabel = if (entry.type == PrayerType.SUNRISE) null else assignedVoice.muazzinName,
                isCustomVoice = perPrayerVoices.containsKey(entry.type),
                onToggleAdhan = { prayerManager.togglePrayerAdhan(entry.type) }
            )
        }
    }
}

@Composable
private fun PrayerTimeRowCard(
    entry: PrayerTimeEntry,
    muezzinLabel: String?,
    isCustomVoice: Boolean,
    onToggleAdhan: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isNext) Color(0xFF123B5E) else Color(0xFF1A1F4A)
        ),
        border = if (entry.isNext) BorderStroke(1.5.dp, Color(0xFFFFD98A)) else null,
        modifier = Modifier.fillMaxWidth().testTag("prayer_${entry.type.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (entry.isNext) Color(0x33FFD98A) else Color(0xFF272D66)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getPrayerIcon(entry.type),
                        contentDescription = null,
                        tint = if (entry.isNext) Color(0xFFFFD98A) else Color(0xFFA9B4D6),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.type.titleAr,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = if (entry.isNext) FontWeight.Bold else FontWeight.SemiBold
                        )
                        if (entry.isNext) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "• القادمة", color = Color(0xFF2EE6A6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (muezzinLabel != null && entry.adhanEnabled) {
                        Text(
                            text = if (isCustomVoice) "مؤذن مخصص: $muezzinLabel" else "المؤذن: $muezzinLabel",
                            color = if (isCustomVoice) Color(0xFFFFC857) else Color(0xFF8B96BD),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.timeFormatted,
                    color = if (entry.isNext) Color(0xFFFFD98A) else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (entry.type != PrayerType.SUNRISE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onToggleAdhan,
                        modifier = Modifier.size(36.dp).testTag("bell_${entry.type.id}")
                    ) {
                        Icon(
                            imageVector = if (entry.adhanEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = if (entry.adhanEnabled) "تعطيل أذان ${entry.type.titleAr}" else "تفعيل أذان ${entry.type.titleAr}",
                            tint = if (entry.adhanEnabled) Color(0xFF2EE6A6) else Color(0xFF8B96BD),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaCompassTabContent(
    qiblaBearing: Float,
    compassHeading: Float,
    distanceKm: Int
) {
    val diff = (compassHeading - qiblaBearing + 360) % 360
    val isAligned = diff in 0f..5f || diff in 355f..360f

    val animatedRotation by animateFloatAsState(
        targetValue = -compassHeading,
        animationSpec = tween(150),
        label = "compassRotation"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
            border = BorderStroke(1.dp, if (isAligned) Color(0xFF2EE6A6) else Color(0xFF272D66)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isAligned) "أنت الآن باتجاه الكعبة المشرفة تماماً" else "قم بتدوير الهاتف حتى يتطابق السهم الذهبي مع القبلة",
                    color = if (isAligned) Color(0xFF2EE6A6) else Color(0xFFFFC857),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "زاوية القبلة: %.1f° • المسافة إلى مكة: %d كم".format(qiblaBearing, distanceKm),
                    color = Color(0xFFA9B4D6),
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(Color(0xFF141943))
                .border(3.dp, if (isAligned) Color(0xFF2EE6A6) else Color(0xFF272D66), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxSize().rotate(animatedRotation),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2 - 16.dp.toPx()

                    for (deg in 0 until 360 step 30) {
                        val angleRad = Math.toRadians(deg.toDouble())
                        val start = Offset(
                            (center.x + (radius - 12.dp.toPx()) * sin(angleRad)).toFloat(),
                            (center.y - (radius - 12.dp.toPx()) * cos(angleRad)).toFloat()
                        )
                        val end = Offset(
                            (center.x + radius * sin(angleRad)).toFloat(),
                            (center.y - radius * cos(angleRad)).toFloat()
                        )
                        drawLine(
                            color = if (deg == 0) Color(0xFFFF6B8B) else Color(0xFF455A64),
                            start = start,
                            end = end,
                            strokeWidth = if (deg % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                        )
                    }
                }

                Text(
                    text = "شمال (N)",
                    color = Color(0xFFFF6B8B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp)
                )

                Box(
                    modifier = Modifier.fillMaxSize().rotate(qiblaBearing),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFD700)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕋", fontSize = 18.sp)
                        }
                        Box(modifier = Modifier.width(3.dp).height(50.dp).background(Color(0xFFFFD700)))
                    }
                }
            }

            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(if (isAligned) Color(0xFF2EE6A6) else Color(0xFF22E3FF))
            )
        }

        Text(
            text = "ضع الهاتف بشكل أفقي مسطح للحصول على أعلى دقة للبوصلة",
            color = Color(0xFF8B96BD),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
private fun AdhanSettingsAndVoicesTabContent(
    prayerManager: PrayerTimesManager,
    selectedVoice: AdhanVoice,
    isPlayingPreview: Boolean,
    previewingVoiceId: String?,
    perPrayerVoices: Map<PrayerType, String>
) {
    val context = LocalContext.current
    val isAutoMuezzin by prayerManager.isAutoMuezzinEnabled.collectAsState()
    val preMinutes by prayerManager.preAdhanMinutesBefore.collectAsState()
    val postMinutes by prayerManager.postAdhanMinutesAfter.collectAsState()
    val volumePercent by prayerManager.adhanVolumePercent.collectAsState()
    val vibrationEnabled by prayerManager.adhanVibrationEnabled.collectAsState()

    var voiceForPerPrayerAssignment by remember { mutableStateOf<AdhanVoice?>(null) }
    val allVoices = AdhanVoicesCatalog.voices

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. User-Controlled Muezzin Policy Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                border = BorderStroke(1.dp, if (!isAutoMuezzin) Color(0xFF2EE6A6) else Color(0xFF22E3FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!isAutoMuezzin) "تحكم يدوي بالكامل بالمؤذن ✅" else "اختيار تلقائي حسب المدينة",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isAutoMuezzin) {
                                "يتم استخدام المؤذن الذي تحدده أنت بنفسك ولا يتم تغييره تلقائياً عند تبديل المدينة."
                            } else {
                                "يقوم النظام باختيار مؤذن يناسب منطقتك الجغرافية تلقائياً."
                            },
                            color = Color(0xFFA9B4D6),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isAutoMuezzin,
                        onCheckedChange = { prayerManager.setAutoMuezzinEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF22E3FF)),
                        modifier = Modifier.testTag("auto_muezzin_switch")
                    )
                }
            }
        }

        // 2. Pre-Adhan & Post-Adhan Timers Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنبيه قبل الأذان (للاستعداد والوضوء)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "إيقاف", 5 to "5 د", 10 to "10 د", 15 to "15 د", 20 to "20 د", 30 to "30 د").forEach { (min, label) ->
                            val isSel = preMinutes == min
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFF22E3FF) else Color(0xFF272D66))
                                    .clickable { prayerManager.setPreAdhanMinutes(min) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("pre_adhan_chip_$min")
                            ) {
                                Text(label, color = if (isSel) Color(0xFF0E1230) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AlarmOn, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنبيه بعد الأذان (الإقامة + الأذكار والتسبيح)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "إيقاف", 5 to "5 د", 10 to "10 د", 15 to "15 د", 20 to "20 د").forEach { (min, label) ->
                            val isSel = postMinutes == min
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFFFC857) else Color(0xFF272D66))
                                    .clickable { prayerManager.setPostAdhanMinutes(min) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("post_adhan_chip_$min")
                            ) {
                                Text(label, color = if (isSel) Color(0xFF0E1230) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                prayerManager.testPreAdhan(2)
                                Toast.makeText(context, "سيظهر إشعار قبل الأذان بعد ثانيتين", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, Color(0xFF22E3FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تجربة قبل الأذان", color = Color(0xFF22E3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                prayerManager.testPostAdhan(2)
                                Toast.makeText(context, "سيظهر إشعار بعد الأذان والتسبيح بعد ثانيتين", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, Color(0xFFFFC857)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تجربة بعد الأذان", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Volume & Vibration Control Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مستوى صوت الأذان ($volumePercent%)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("اهتزاز", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { prayerManager.setAdhanVibrationEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2EE6A6))
                            )
                        }
                    }
                    Slider(
                        value = volumePercent.toFloat(),
                        onValueChange = { prayerManager.setAdhanVolumePercent(it.toInt()) },
                        valueRange = 20f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2EE6A6),
                            activeTrackColor = Color(0xFF2EE6A6)
                        )
                    )
                }
            }
        }

        item {
            Text(
                text = "اختر صوت المؤذن الرئيسي أو خصصه لكل صلاة (${allVoices.size} أصوات كاملة):",
                color = Color(0xFFA9B4D6),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(allVoices) { voice ->
            val isSelected = selectedVoice.id == voice.id
            val isThisVoicePlaying = isPlayingPreview && previewingVoiceId == voice.id
            val assignedPrayers = perPrayerVoices.filterValues { it == voice.id }.keys
            val durationMin = voice.durationSec / 60
            val durationSecRem = voice.durationSec % 60
            val durationFormatted = "%02d:%02d".format(durationMin, durationSecRem)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF272D66) else Color(0xFF1A1F4A)
                ),
                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2EE6A6)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        prayerManager.setSelectedAdhanVoice(voice)
                        Toast.makeText(context, "تم اختيار ${voice.muazzinName} للأذان", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("voice_${voice.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0x332EE6A6) else Color(0xFF141943)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF2EE6A6) else Color(0xFFA9B4D6),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = voice.nameAr,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${voice.muazzinName} • $durationFormatted",
                                color = Color(0xFFA9B4D6),
                                fontSize = 11.sp
                            )
                            if (assignedPrayers.isNotEmpty()) {
                                Text(
                                    text = "مخصص لـ: ${assignedPrayers.joinToString("، ") { it.titleAr }}",
                                    color = Color(0xFFFFC857),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { prayerManager.previewAdhanVoice(voice) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isThisVoicePlaying) Color(0xFF2EE6A6) else Color(0xFF141943),
                                contentColor = if (isThisVoicePlaying) Color(0xFF0E1230) else Color(0xFF2EE6A6)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isThisVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isThisVoicePlaying) "إيقاف المعاينة" else "استماع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { voiceForPerPrayerAssignment = voice },
                            border = BorderStroke(1.dp, Color(0xFF22E3FF).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("assign_prayer_${voice.id}")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تخصيص لصلاة", color = Color(0xFF22E3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Per-Prayer Voice Assignment Dialog
    voiceForPerPrayerAssignment?.let { targetVoice ->
        val prayers = listOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)
        AlertDialog(
            onDismissRequest = { voiceForPerPrayerAssignment = null },
            containerColor = Color(0xFF1A1F4A),
            title = {
                Text(
                    text = "تخصيص ${targetVoice.muazzinName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر الصلوات التي تريد أن يصدح فيها هذا المؤذن، أو اجعله الصوت العام لجميع الصلوات:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp
                    )
                    prayers.forEach { prayer ->
                        val isAssignedHere = perPrayerVoices[prayer] == targetVoice.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAssignedHere) Color(0x332EE6A6) else Color(0xFF272D66))
                                .clickable {
                                    if (isAssignedHere) {
                                        prayerManager.clearPrayerAdhanVoice(prayer)
                                    } else {
                                        prayerManager.setPrayerAdhanVoice(prayer, targetVoice)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صلاة ${prayer.titleAr}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isAssignedHere) "مفعّل ✓" else "تعيين",
                                color = if (isAssignedHere) Color(0xFF2EE6A6) else Color(0xFF22E3FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prayerManager.setSelectedAdhanVoice(targetVoice)
                        prayers.forEach { prayerManager.clearPrayerAdhanVoice(it) }
                        voiceForPerPrayerAssignment = null
                        Toast.makeText(context, "تم اعتماد ${targetVoice.muazzinName} لجميع الصلوات", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230))
                ) {
                    Text("اعتماد للكل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { voiceForPerPrayerAssignment = null }) {
                    Text("إغلاق", color = Color(0xFFA9B4D6))
                }
            }
        )
    }
}

@Composable
private fun CityPickerDialog(
    currentCity: CityLocation,
    onSelectCity: (CityLocation) -> Unit,
    onDetectGps: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val cities = PrayerTimesCalculator.defaultCities
    val filtered = remember(query) {
        if (query.isBlank()) cities
        else cities.filter { it.nameAr.contains(query.trim()) || it.nameEn.contains(query.trim(), ignoreCase = true) || it.country.contains(query.trim()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F4A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2EE6A6))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "اختر المدينة لحساب المواقيت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.height(360.dp)) {
                Button(
                    onClick = onDetectGps,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استخدام موقعي الجغرافي التلقائي (GPS)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("بحث عن مدينة (مكة، القاهرة، الرياض، دبي)...", color = Color(0xFF8B96BD), fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2EE6A6),
                        unfocusedBorderColor = Color(0xFF272D66)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { city ->
                        val isSel = currentCity.nameAr == city.nameAr
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0x332EE6A6) else Color(0xFF141943))
                                .clickable { onSelectCity(city) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${city.nameAr} (${city.country})", color = Color.White, fontSize = 13.sp)
                            if (isSel) {
                                Text(text = "محدد", color = Color(0xFF2EE6A6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFFA9B4D6))
            }
        }
    )
}

private fun getPrayerIcon(type: PrayerType): ImageVector {
    return when (type) {
        PrayerType.FAJR -> Icons.Default.NightsStay
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.LightMode
        PrayerType.ASR -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.Bedtime
        PrayerType.ISHA -> Icons.Default.DarkMode
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AdhanDiagnosticDialog(
    prayerManager: PrayerTimesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nova_prayer_prefs", Context.MODE_PRIVATE) }

    val hasNotificationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    val canExact = prayerManager.canScheduleExactAlarms()
    val isIgnoringBattery = prayerManager.isIgnoringBatteryOptimizations()
    val lastTriggeredTime = prefs.getString("last_adhan_triggered_time", "لم يُسجل تشغيل أذان حتى الآن") ?: "غير مسجل"
    val lastTestScheduled = prefs.getString("last_test_alarm_scheduled", "لا يوجد") ?: "لا يوجد"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F4A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF22E3FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "فحص نظام الأذان والمنبهات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(380.dp)
            ) {
                item {
                    Text("1. حالة صلاحيات المنظومة:", color = Color(0xFF22E3FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    DiagnosticRow(
                        title = "إذن الإشعارات (POST_NOTIFICATIONS)",
                        statusText = if (hasNotificationPerm) "مفعّل" else "غير مفعّل",
                        isSuccess = hasNotificationPerm
                    )
                    DiagnosticRow(
                        title = "إذن المنبهات الدقيقة (EXACT_ALARM)",
                        statusText = if (canExact) "مفعّل" else "معطّل (تحتاج تفعيل)",
                        isSuccess = canExact,
                        onFix = if (!canExact) { { prayerManager.openExactAlarmSettings() } } else null
                    )
                    DiagnosticRow(
                        title = "استثناء تحسين البطارية",
                        statusText = if (isIgnoringBattery) "مستثنى" else "مقيّد",
                        isSuccess = isIgnoringBattery,
                        onFix = if (!isIgnoringBattery) { { prayerManager.requestIgnoreBatteryOptimizations() } } else null
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("2. المنبهات المجدولة (الأذان + قبل + بعد):", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(
                        PrayerType.FAJR,
                        PrayerType.DHUHR,
                        PrayerType.ASR,
                        PrayerType.MAGHRIB,
                        PrayerType.ISHA
                    ).forEach { prayerType ->
                        val savedStatus = prefs.getString("diagnostic_alarm_${prayerType.id}", "جاري الجدولة...") ?: "غير معروف"
                        DiagnosticRow(
                            title = prayerType.titleAr,
                            statusText = savedStatus,
                            isSuccess = !savedStatus.contains("معطّل")
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("3. سجل العمليات والمنبه التجريبي:", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    DiagnosticRow(
                        title = "آخر تشغيل فعلي للأذان",
                        statusText = lastTriggeredTime,
                        isSuccess = !lastTriggeredTime.contains("لم يُسجل")
                    )
                    if (lastTestScheduled != "لا يوجد") {
                        DiagnosticRow(
                            title = "آخر منبه اختبار مجدول",
                            statusText = "الساعة $lastTestScheduled",
                            isSuccess = true
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            prayerManager.testAdhan(10)
                            Toast.makeText(context, "تمت جدولة الأذان بعد 10 ثوانٍ", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جدولة أذان تجريبي بعد 10 ثوانٍ", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DiagnosticRow(
    title: String,
    statusText: String,
    isSuccess: Boolean,
    onFix: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141943))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(text = statusText, color = if (isSuccess) Color(0xFF2EE6A6) else Color(0xFFFF6B8B), fontSize = 10.sp)
        }
        if (onFix != null) {
            TextButton(
                onClick = onFix,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("تعديل", color = Color(0xFFFFD600), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
