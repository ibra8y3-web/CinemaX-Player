package com.example.ui.screens.prayer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.data.model.AdhanAlarmType
import com.example.data.model.AdhanLogEntry
import com.example.prayer.AdhanLogStore
import com.example.prayer.PrayerTimesManager

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerTasbihAndLogTab(prayerManager: PrayerTimesManager) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val selectedDhikr by prayerManager.selectedDhikr.collectAsState()
    val tasbihCount by prayerManager.tasbihCount.collectAsState()
    val tasbihTarget by prayerManager.tasbihTarget.collectAsState()
    val totalTasbihCount by prayerManager.totalTasbihCount.collectAsState()
    val completedCycles by prayerManager.tasbihCompletedCycles.collectAsState()
    val adhanLogs by prayerManager.adhanLogs.collectAsState()

    var logFilter by remember { mutableStateOf("ALL") }
    var bounceTrigger by remember { mutableStateOf(false) }
    val ringScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tasbihScale",
        finishedListener = { bounceTrigger = false }
    )

    LaunchedEffect(Unit) {
        prayerManager.refreshLogs()
    }

    val filteredLogs = remember(adhanLogs, logFilter) {
        when (logFilter) {
            "ADHAN" -> adhanLogs.filter { it.eventType == AdhanAlarmType.ADHAN || it.eventType == AdhanAlarmType.STOPPED }
            "REMINDERS" -> adhanLogs.filter { it.eventType == AdhanAlarmType.PRE_ADHAN || it.eventType == AdhanAlarmType.POST_ADHAN }
            "SCHEDULED" -> adhanLogs.filter { it.eventType == AdhanAlarmType.SCHEDULED || it.eventType == AdhanAlarmType.TEST }
            else -> adhanLogs
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Interactive Post-Adhan Tasbih Counter Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                border = BorderStroke(1.dp, Color(0xFF2EE6A6).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("tasbih_card")
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
                        Column {
                            Text(
                                text = "السبحة الإلكترونية وأذكار ما بعد الأذان",
                                color = Color(0xFF2EE6A6),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إجمالي التسبيحات: $totalTasbihCount • الدورات المكتملة: $completedCycles",
                                color = Color(0xFFA9B4D6),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { prayerManager.resetTasbih() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF272D66))
                                .testTag("tasbih_reset_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تصفير العداد", tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dhikr Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(prayerManager.dhikrCatalog) { phrase ->
                            val selected = phrase.id == selectedDhikr.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Color(0xFF2EE6A6) else Color(0xFF272D66))
                                    .clickable { prayerManager.selectDhikr(phrase) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = phrase.textAr,
                                    color = if (selected) Color(0xFF0E1230) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Virtue Banner
                    Text(
                        text = selectedDhikr.virtueAr,
                        color = Color(0xFFFFC857),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Glowing Interactive Ring
                    val sweep = if (tasbihTarget > 0) {
                        ((tasbihCount.toFloat() / tasbihTarget.toFloat()).coerceIn(0f, 1f)) * 360f
                    } else {
                        ((tasbihCount % 33) / 33f) * 360f
                    }

                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(Color(0xFF0E1230))
                            .clickable {
                                bounceTrigger = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val completed = prayerManager.incrementTasbih()
                                if (completed) {
                                    Toast.makeText(
                                        context,
                                        "تقبل الله! أتممت $tasbihTarget من «${selectedDhikr.textAr}»",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .testTag("tasbih_tap_circle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            drawArc(
                                color = Color(0xFF272D66),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFF2EE6A6),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "$tasbihCount",
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (tasbihTarget > 0) "من $tasbihTarget" else "وضع مفتوح",
                                color = Color(0xFF2EE6A6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFFA9B4D6), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اضغط للتسبيح", color = Color(0xFFA9B4D6), fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Selector Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الهدف:", color = Color(0xFFA9B4D6), fontSize = 11.sp)
                        listOf(33 to "33", 99 to "99", 100 to "100", 0 to "مفتوح").forEach { (value, label) ->
                            val isSel = tasbihTarget == value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0x3322E3FF) else Color(0xFF272D66))
                                    .border(
                                        1.dp,
                                        if (isSel) Color(0xFF22E3FF) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { prayerManager.setTasbihTarget(value) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color(0xFF22E3FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Post-Adhan Dua & Post-Prayer Adhkar Cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                border = BorderStroke(1.dp, Color(0x33FFD54F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دعاء ما بعد الأذان",
                            color = Color(0xFFFFC857),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Post Adhan Dua", AdhanLogStore.postAdhanDuaAr))
                                Toast.makeText(context, "تم نسخ دعاء ما بعد الأذان", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الدعاء", tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = AdhanLogStore.postAdhanDuaAr,
                        color = Color(0xFFE6EAFA),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "سنّة ثابتة: ترديد كلمات الأذان خلف المؤذن ثم الصلاة على النبي ﷺ وقراءة هذا الدعاء والدعاء بين الأذان والإقامة لا يُرد.",
                        color = Color(0xFFA9B4D6),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 3. Full Adhan & Alarm Event Log Header
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "سجل الأذان والتنبيهات الكامل (${adhanLogs.size})",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "توثيق كامل لأوقات الأذان والتنبيهات المسبقة واللاحقة والجدولة",
                                    color = Color(0xFFA9B4D6),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { prayerManager.refreshLogs() },
                                modifier = Modifier.size(36.dp).testTag("refresh_adhan_logs_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث السجل", tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                            }
                            if (adhanLogs.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        prayerManager.clearAdhanLogs()
                                        Toast.makeText(context, "تم مسح سجل الأذان", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp).testTag("clear_adhan_logs_btn")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "مسح السجل", tint = Color(0xFFFF6B8B), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "الكل",
                            "ADHAN" to "الأذان والإيقاف",
                            "REMINDERS" to "قبل وبعد الأذان",
                            "SCHEDULED" to "الجدولة والاختبار"
                        ).forEach { (key, title) ->
                            val isSel = logFilter == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Color(0xFF22E3FF) else Color(0xFF272D66))
                                    .clickable { logFilter = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSel) Color(0xFF0E1230) else Color(0xFFE6EAFA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141943)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF546E7A), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد سجلات ضمن هذا التصنيف بعد", color = Color(0xFFA9B4D6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "سيتم تسجيل كل أذان فعلي وتنبيه قبل أو بعد الصلاة وتجربة صوت تلقائياً هنا.",
                            color = Color(0xFF607D8B),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { entry ->
                AdhanLogItemCard(entry)
            }
        }
    }
}

@Composable
private fun AdhanLogItemCard(entry: AdhanLogEntry) {
    val (accentColor, icon) = when (entry.eventType) {
        AdhanAlarmType.ADHAN -> Color(0xFF2EE6A6) to Icons.Default.NotificationsActive
        AdhanAlarmType.PRE_ADHAN -> Color(0xFF22E3FF) to Icons.Default.Alarm
        AdhanAlarmType.POST_ADHAN -> Color(0xFFFFC857) to Icons.Default.AlarmOn
        AdhanAlarmType.TEST -> Color(0xFFE040FB) to Icons.Default.CheckCircle
        AdhanAlarmType.STOPPED -> Color(0xFFFF6B8B) to Icons.Default.StopCircle
        AdhanAlarmType.SCHEDULED -> Color(0xFFA9B4D6) to Icons.Default.Schedule
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
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
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = entry.eventType.labelAr, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entry.eventType.labelAr} • ${entry.prayerName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.formattedTime.takeLast(8),
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (entry.voiceName.isNotBlank()) {
                    Text(
                        text = "المؤذن: ${entry.voiceName}" + if (entry.cityName.isNotBlank()) " • ${entry.cityName}" else "",
                        color = Color(0xFFC3CCE8),
                        fontSize = 11.sp
                    )
                }
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        color = Color(0xFF8B96BD),
                        fontSize = 10.sp
                    )
                }
                if (entry.formattedTime.length > 10) {
                    Text(
                        text = entry.formattedTime.take(10),
                        color = Color(0xFF546E7A),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
