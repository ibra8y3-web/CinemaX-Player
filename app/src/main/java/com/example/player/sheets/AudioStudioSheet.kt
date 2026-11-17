package com.example.player.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPreset
import com.example.audio.AudioStudioManager
import com.example.audio.AudioStudioState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioStudioSheet(
    audioStudioManager: AudioStudioManager,
    audioState: AudioStudioState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F141C),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Drag indicator
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363D80))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22E3FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = Color(0xFF22E3FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "استوديو الصوت السينمائي (Equalizer)",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مضخم الصوت حتى 200%، تعزيز الباس، ومؤثرات صوتية ثلاثية الأبعاد",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets
            Text(
                text = "الأوضاع الجاهزة (Presets):",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AudioPreset.values()) { preset ->
                    val isSelected = audioState.currentPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color(0xFF22E3FF)
                                else Color(0xFF161B42)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF22E3FF) else Color(0xFF363D80),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { audioStudioManager.applyPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("audio_preset_${preset.name}")
                    ) {
                        Text(
                            text = preset.titleAr,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1A1F4A), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Super Volume Boost (Loudness Enhancer)
            val boostPercent = 100 + (audioState.volumeBoostMb / 10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF22E3FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "مضخم الصوت الفائق:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "$boostPercent%",
                    color = if (boostPercent > 100) Color(0xFF22E3FF) else Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = audioState.volumeBoostMb.toFloat(),
                onValueChange = { audioStudioManager.setVolumeBoost(it.toInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF22E3FF),
                    activeTrackColor = Color(0xFF22E3FF),
                    inactiveTrackColor = Color(0xFF1A1F4A)
                ),
                modifier = Modifier.testTag("volume_boost_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bass Boost Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = Color(0xFFFF9100), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تعزيز الباس العميق (Bass Boost):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "${audioState.bassBoostStrength / 10}%",
                    color = Color(0xFFFF9100),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = audioState.bassBoostStrength.toFloat(),
                onValueChange = { audioStudioManager.setBassBoost(it.toInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9100),
                    activeTrackColor = Color(0xFFFF9100),
                    inactiveTrackColor = Color(0xFF1A1F4A)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Virtualizer (3D Surround)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SurroundSound, null, tint = Color(0xFF76FF03), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "المحيط الثلاثي الأبعاد (3D Spatial):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "${audioState.virtualizerStrength / 10}%",
                    color = Color(0xFF76FF03),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = audioState.virtualizerStrength.toFloat(),
                onValueChange = { audioStudioManager.setVirtualizer(it.toInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF76FF03),
                    activeTrackColor = Color(0xFF76FF03),
                    inactiveTrackColor = Color(0xFF1A1F4A)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1A1F4A), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 5-Band Equalizer sliders
            Text(
                text = "موازن الترددات 5-Band Equalizer:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
            audioState.bandLevels.forEachIndexed { index, level ->
                if (index < bandLabels.size) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bandLabels[index],
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            modifier = Modifier.width(55.dp)
                        )
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { audioStudioManager.setBandLevel(index, it.toInt()) },
                            valueRange = -1000f..1000f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF22E3FF),
                                inactiveTrackColor = Color(0xFF1A1F4A)
                            )
                        )
                        Text(
                            text = "${level / 100} dB",
                            color = Color(0xFFD1D5DB),
                            fontSize = 10.sp,
                            modifier = Modifier.width(45.dp)
                        )
                    }
                }
            }
        }
    }
}
