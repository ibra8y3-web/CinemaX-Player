package com.example.ui.screens

import com.example.ui.theme.cinemaBackground

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.data.model.VaultItemEntity
import com.example.media.MediaScanner
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VaultScreen(
    vaultItems: List<VaultItemEntity>,
    prefsManager: PreferencesManager,
    cryptoManager: VaultCryptoManager,
    onPlayEncryptedVideo: (File, String) -> Unit,
    onDeleteItem: (VaultItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPinSet by prefsManager.isVaultPinSet.collectAsState()

    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isSettingUpPin by remember { mutableStateOf(!isPinSet) }
    var currentSessionPin by remember { mutableStateOf("") }
    var itemForCustomPin by remember { mutableStateOf<VaultItemEntity?>(null) }
    var customFilePin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
            .padding(16.dp)
    ) {
        // Vault Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF22E3FF),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "الخزنة المشفرة الآمنة (AES-256)",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تشفير عسكري لحماية الفيديوهات السرية",
                        color = Color(0xFF22E3FF),
                        fontSize = 11.sp
                    )
                }
            }

            if (isUnlocked) {
                IconButton(onClick = {
                    isUnlocked = false
                    enteredPin = ""
                    currentSessionPin = ""
                    cryptoManager.cleanTempFiles()
                    Toast.makeText(context, "تم قفل الخزنة بأمان", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Lock, contentDescription = "قفل", tint = Color(0xFFFF6B8B))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isPinSet || isSettingUpPin) {
            // First time PIN setup
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x3322E3FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF22E3FF), modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "تعيين رمز PIN السري للخزنة",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يتم اشتقاق مفتاح التشفير من هذا الرمز بواسطة خوارزمية SHA-256 لحماية ملفاتك من أي اختراق",
                            color = Color(0xFFA9B4D6),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6) newPin = it },
                            label = { Text("رمز PIN الجديد (4-6 أرقام)", color = Color(0xFFA9B4D6)) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF22E3FF),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("vault_new_pin")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 6) confirmPin = it },
                            label = { Text("تأكيد الرمز السري", color = Color(0xFFA9B4D6)) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF22E3FF),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("vault_confirm_pin")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (newPin.length >= 4 && newPin == confirmPin) {
                                    val hash = VaultCryptoManager.hashPin(newPin)
                                    prefsManager.setVaultPinHash(hash)
                                    isSettingUpPin = false
                                    isUnlocked = true
                                    currentSessionPin = newPin
                                    Toast.makeText(context, "تم حفظ الرمز السري بنجاح", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "الرمز غير متطابق أو أقل من 4 أرقام", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_vault_pin_button")
                        ) {
                            Text("تفعيل وتأمين الخزنة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else if (!isUnlocked) {
            // Lock Screen Keypad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                    modifier = Modifier.fillMaxWidth()
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
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF6B8B), modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "الخزنة مقفلة برمز حماية",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "أدخل رمز PIN لفك التشفير واستعراض الفيديوهات الخاصة",
                            color = Color(0xFFA9B4D6),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 6) {
                                    enteredPin = it
                                    if (it.length >= 4) {
                                        val storedHash = prefsManager.getVaultPinHash()
                                        if (VaultCryptoManager.verifyPin(it, storedHash)) {
                                            isUnlocked = true
                                            currentSessionPin = it
                                            Toast.makeText(context, "تم فتح الخزنة المشفرة بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            label = { Text("رمز PIN السري", color = Color(0xFFA9B4D6)) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF22E3FF),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("vault_unlock_pin_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val storedHash = prefsManager.getVaultPinHash()
                                if (VaultCryptoManager.verifyPin(enteredPin, storedHash)) {
                                    isUnlocked = true
                                    currentSessionPin = enteredPin
                                    Toast.makeText(context, "تم فتح الخزنة المشفرة بنجاح", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "رمز PIN غير صحيح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("vault_unlock_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح الخزنة الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Unlocked Content: Encrypted items list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${vaultItems.size} ملف مشفر داخل الخزنة",
                    color = Color(0xFF22E3FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (vaultItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF363D80), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "الخزنة فارغة حالياً",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لتشفير أي فيديو وإخفائه، انقر على زر القفل بجانب أي فيديو في قائمة الفيديوهات",
                            color = Color(0xFF8B96BD),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vaultItems, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F4A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    itemForCustomPin = item
                                    customFilePin = ""
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF272D66)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF22E3FF))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.originalTitle,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${MediaScanner.formatFileSize(item.fileSize)} • مشفر برمز خاص",
                                        color = Color(0xFFA9B4D6),
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        itemForCustomPin = item
                                        customFilePin = ""
                                    }
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color(0xFF22E3FF))
                                }

                                IconButton(onClick = { onDeleteItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف نهائي", tint = Color(0xFFFF6B8B))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for files encrypted with an individual or custom PIN
        itemForCustomPin?.let { item ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { itemForCustomPin = null },
                containerColor = Color(0xFF1A1F4A),
                title = {
                    Text("فك تشفير وتشغيل الفيديو المشفر", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "أدخل رمز التشفير الخاص بهذا الفيديو (${item.originalTitle}) لفك تشفيره وتشغيله عبر مشغل CinemaX Player:",
                            color = Color(0xFFA9B4D6),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = customFilePin,
                            onValueChange = { customFilePin = it },
                            label = { Text("رمز PIN الخاص بهذا الفيديو", color = Color(0xFFA9B4D6)) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF22E3FF),
                                unfocusedBorderColor = Color(0xFF272D66)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customFilePin.isNotBlank()) {
                                scope.launch {
                                    try {
                                        val decryptedFile = cryptoManager.decryptToTempForPlayback(
                                            item.encryptedFileName,
                                            customFilePin
                                        )
                                        itemForCustomPin = null
                                        onPlayEncryptedVideo(decryptedFile, item.originalTitle)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "رمز PIN غير صحيح لهذا الملف: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22E3FF), contentColor = Color(0xFF0E1230))
                    ) {
                        Text("تشغيل الآن", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemForCustomPin = null }) {
                        Text("إلغاء", color = Color(0xFFA9B4D6))
                    }
                }
            )
        }
    }
}
