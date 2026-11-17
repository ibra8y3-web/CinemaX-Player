package com.example.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CinemaXSyncDialog(
    onDismiss: () -> Unit,
    onOpenLocalWifiShare: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import

    // Export States
    var isLoadingExport by remember { mutableStateOf(true) }
    var exportPayload by remember { mutableStateOf<CinemaXSyncEngine.CinemaXSyncPayload?>(null) }
    var exportSummary by remember { mutableStateOf<CinemaXSyncEngine.SyncSummary?>(null) }
    var generatedCloudPin by remember { mutableStateOf<String?>(null) }
    var isUploadingToCloud by remember { mutableStateOf(false) }
    var cloudUploadError by remember { mutableStateOf<String?>(null) }

    // Import States
    var inputCodeOrPin by remember { mutableStateOf("") }
    var isFetchingImport by remember { mutableStateOf(false) }
    var fetchedPayload by remember { mutableStateOf<CinemaXSyncEngine.CinemaXSyncPayload?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Load local stats on open
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val payload = CinemaXSyncEngine.createBackupPayload(context)
                exportPayload = payload
                exportSummary = payload.toSummary()
                isLoadingExport = false
            } catch (e: Exception) {
                isLoadingExport = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141943),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x3322E3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFF22E3FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "المزامنة السحابية والنسخ الذكي ⚡",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "نقل واستيراد السجل والمفضلة والإعدادات بين الهواتف",
                        color = Color(0xFFA9B4D6),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Navigation Tabs (Export vs Import)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF272D66),
                    contentColor = Color(0xFF22E3FF),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF22E3FF),
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير ومزامنة 📤", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد واستعادة 📥", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==================== TAB 0: EXPORT ====================
                if (selectedTab == 0) {
                    if (isLoadingExport) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF22E3FF), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("جاري تجميع بيانات المشغل والسجل...", color = Color(0xFFA9B4D6), fontSize = 12.sp)
                            }
                        }
                    } else {
                        exportSummary?.let { sum ->
                            // Local Stats Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("جاهز للنسخ والمزامنة 📦", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(sum.deviceName, color = Color(0xFF9D7BFF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metric Grid
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        StatBadge(icon = Icons.Default.History, label = "سجل المشاهدة", count = sum.historyCount, color = Color(0xFF5B8CFF))
                                        StatBadge(icon = Icons.Default.Favorite, label = "المفضلة", count = sum.favoritesCount, color = Color(0xFFFF4081))
                                        StatBadge(icon = Icons.Default.Tv, label = "القنوات والبث", count = sum.channelsCount, color = Color(0xFFFFC857))
                                        StatBadge(icon = Icons.Default.Radio, label = "الراديو", count = sum.radioCount, color = Color(0xFF5EF0BE))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Instant Cloud PIN Section
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2634)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0x4422E3FF), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("المزامنة السحابية برمز فوري (Cloud PIN):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "يقوم النظام برفع الحزمة سحابياً وتوليد كود قصير (مثل CX-9A2B). بمجرد إدخاله على أي هاتف في العالم، يسترجع كل شيء فوراً!",
                                        color = Color(0xFFA9B4D6),
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (generatedCloudPin != null) {
                                        // Display generated PIN with 1-click Copy
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF061A24))
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("رمز المزامنة السحابي الخاص بك:", color = Color(0xFF80CBC4), fontSize = 11.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = generatedCloudPin!!,
                                                    color = Color(0xFF22E3FF),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 2.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("CinemaX Cloud PIN", generatedCloudPin))
                                                        Toast.makeText(context, "تم نسخ الرمز السحابي بنجاح! الصقه في الهاتف الآخر", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("نسخ الرمز السحابي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                if (exportPayload != null) {
                                                    isUploadingToCloud = true
                                                    cloudUploadError = null
                                                    scope.launch {
                                                        val syncCode = CinemaXSyncEngine.compressToSyncCode(exportPayload!!)
                                                        val result = CinemaXSyncEngine.uploadToCloudRelay(syncCode)
                                                        result.onSuccess { pin ->
                                                            generatedCloudPin = pin
                                                            isUploadingToCloud = false
                                                            Toast.makeText(context, "تم رفع الحزمة وتوليد الرمز بنجاح! ☁️", Toast.LENGTH_SHORT).show()
                                                        }.onFailure { err ->
                                                            isUploadingToCloud = false
                                                            cloudUploadError = err.message ?: "فشل الاتصال بالسيرفر السحابي"
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isUploadingToCloud,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("generate_cloud_pin_btn")
                                        ) {
                                            if (isUploadingToCloud) {
                                                CircularProgressIndicator(color = Color(0xFF0E1230), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("جاري توليد الكود السحابي...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("توليد كود سحابي فوري (Cloud PIN) ☁️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        if (cloudUploadError != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(cloudUploadError!!, color = Color(0xFFFF6B8B), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Offline Universal Encrypted Code
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF272D66)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("كود المزامنة المشفر الكامل (بدون نت 100%):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "كود مضغوط بنظام GZIP مشفر يحتوي كامل بياناتك ويمكن لصقه مباشرة دون الحاجة لأي اتصال بالإنترنت.",
                                        color = Color(0xFFA9B4D6),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            if (exportPayload != null) {
                                                val syncCode = CinemaXSyncEngine.compressToSyncCode(exportPayload!!)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("CinemaX Universal Sync Code", syncCode))
                                                Toast.makeText(context, "تم نسخ كود المزامنة المشفر الشامل بنجاح! 📋", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC857)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC857)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("copy_universal_code_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("نسخ كود المزامنة الكامل للمشاركة 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. Real Media Wi-Fi Beam
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF132E27)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("نقل ملفات الفيديو والصور الأصلية بين الهاتفين:", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "لنقل الفيديوهات والصور بحجمها الكامل بدون استهلاك أي باقة إنترنت، شغل خادم المشاركة المحلي P2P واستعرض الملفات فوراً من متصفح الهاتف الآخر.",
                                        color = Color(0xFF80CBC4),
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenLocalWifiShare()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فتح خادم النقل المحلي بدون نت (Wi-Fi P2P)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 1: IMPORT ====================
                if (selectedTab == 1) {
                    Text(
                        text = "أدخل الرمز السحابي (مثل CX-9A2B) أو الصق كود المزامنة المشفر لاسترجاع كافة الفيديوهات، المفضلة، والإعدادات فورياً:",
                        color = Color(0xFFA9B4D6),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputCodeOrPin,
                            onValueChange = {
                                inputCodeOrPin = it
                                fetchedPayload = null
                                importError = null
                                restoreSuccessMessage = null
                            },
                            placeholder = { Text("رمز PIN (مثل CX-7K8L) أو كود المزامنة...", color = Color(0xFF8B96BD), fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF22E3FF),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.weight(1f).testTag("sync_input_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        inputCodeOrPin = text.trim()
                                        fetchedPayload = null
                                        importError = null
                                        Toast.makeText(context, "تم لصق كود المزامنة بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الحافظة فارغة!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFF22E3FF)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text("لصق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fetch / Verify Button
                    Button(
                        onClick = {
                            if (inputCodeOrPin.isNotBlank()) {
                                isFetchingImport = true
                                importError = null
                                fetchedPayload = null
                                restoreSuccessMessage = null
                                scope.launch {
                                    val result = CinemaXSyncEngine.fetchFromCloudRelay(inputCodeOrPin)
                                    result.onSuccess { payload ->
                                        fetchedPayload = payload
                                        isFetchingImport = false
                                        Toast.makeText(context, "تم جلب وفحص حزمة المزامنة بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        isFetchingImport = false
                                        importError = err.message ?: "تعذر فك ترميز كود المزامنة"
                                    }
                                }
                            } else {
                                Toast.makeText(context, "يرجى كتابة أو لصق رمز المزامنة أولاً!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isFetchingImport && inputCodeOrPin.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("inspect_sync_package_btn")
                    ) {
                        if (isFetchingImport) {
                            CircularProgressIndicator(color = Color(0xFF0E1230), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري جلب وفحص الحزمة...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص واستيراد الحزمة 🔍", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (importError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF381214)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF6B8B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(importError!!, color = Color(0xFFFFCDD2), fontSize = 11.sp)
                            }
                        }
                    }

                    // Inspection Preview of the Fetched Payload
                    fetchedPayload?.let { payload ->
                        val summary = payload.toSummary()
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF063520)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تم التحقق من الحزمة بنجاح! 🎯", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الجهاز الأصلي: ${summary.deviceName}", color = Color.White, fontSize = 11.sp)
                                    Text("تاريخ النسخ: ${summary.formattedDate}", color = Color(0xFF81C784), fontSize = 10.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Metrics in payload
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    StatBadge(icon = Icons.Default.History, label = "سجل الفيديوهات", count = summary.historyCount, color = Color(0xFF5B8CFF))
                                    StatBadge(icon = Icons.Default.Favorite, label = "المفضلة", count = summary.favoritesCount, color = Color(0xFFFF4081))
                                    StatBadge(icon = Icons.Default.Tv, label = "القنوات", count = summary.channelsCount, color = Color(0xFFFFC857))
                                    StatBadge(icon = Icons.Default.Radio, label = "الراديو", count = summary.radioCount, color = Color(0xFF5EF0BE))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Restore Actions
                                if (isRestoring) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF2EE6A6), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري استعادة السجل والمفضلة والإعدادات...", color = Color.White, fontSize = 11.sp)
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = {
                                                isRestoring = true
                                                scope.launch {
                                                    val result = CinemaXSyncEngine.restorePayload(context, payload, CinemaXSyncEngine.RestoreMode.SMART_MERGE)
                                                    isRestoring = false
                                                    result.onSuccess { msg ->
                                                        restoreSuccessMessage = msg
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }.onFailure { err ->
                                                        importError = "حدث خطأ أثناء الاستعادة: ${err.message}"
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EE6A6), contentColor = Color(0xFF0E1230)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("smart_merge_btn")
                                        ) {
                                            Text("استعادة ودمج ذكي 🚀", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        OutlinedButton(
                                            onClick = {
                                                isRestoring = true
                                                scope.launch {
                                                    val result = CinemaXSyncEngine.restorePayload(context, payload, CinemaXSyncEngine.RestoreMode.FULL_REPLACE)
                                                    isRestoring = false
                                                    result.onSuccess { msg ->
                                                        restoreSuccessMessage = msg
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }.onFailure { err ->
                                                        importError = "حدث خطأ أثناء الاستعادة: ${err.message}"
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC857)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC857)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(0.9f)
                                        ) {
                                            Text("استبدال شامل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (restoreSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2818)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = restoreSuccessMessage!!,
                                color = Color(0xFF5EF0BE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272D66), contentColor = Color(0xFFA9B4D6))
            ) {
                Text("إغلاق", fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
private fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text("$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(label, color = Color(0xFFA9B4D6), fontSize = 9.sp)
    }
}
