package com.example.ui.screens.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.model.LocalAudio
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 6 Dynamic Non-Repeating Visualizer Modes
 */
enum class VisualizerEffect(val titleAr: String, val badge: String) {
    NEON_SPECTRUM("طيف النيون الحي", "⚡"),
    COSMIC_ORBIT("مدار الجسيمات الكونية", "✨"),
    CYBER_WAVES("أمواج الصوت السائلة", "🌊"),
    VINYL_RIPPLE("تموجات الأسطوانة", "💿"),
    AURORA_BEAMS("شفق الأورورا الضوئي", "🌌"),
    KALEIDO_RINGS("التردد الهندسي الدائري", "🔮")
}

data class VisualizerPalette(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val backgroundBrush: Brush
)

object VisualizerThemeEngine {
    private val palettes = listOf(
        VisualizerPalette(
            primary = Color(0xFF22E3FF),
            secondary = Color(0xFF9D7BFF),
            accent = Color(0xFF2EE6A6),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF060D1A), Color(0xFF0A192F)))
        ),
        VisualizerPalette(
            primary = Color(0xFFFF4081),
            secondary = Color(0xFF9D7BFF),
            accent = Color(0xFFFFC857),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF160A1D), Color(0xFF1E0E2C)))
        ),
        VisualizerPalette(
            primary = Color(0xFF2EE6A6),
            secondary = Color(0xFF5B8CFF),
            accent = Color(0xFF5EF0BE),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF051710), Color(0xFF0A261D)))
        ),
        VisualizerPalette(
            primary = Color(0xFFFF9100),
            secondary = Color(0xFFFF6B8B),
            accent = Color(0xFFFFC857),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF1F0D05), Color(0xFF2C140A)))
        ),
        VisualizerPalette(
            primary = Color(0xFFE040FB),
            secondary = Color(0xFF7C4DFF),
            accent = Color(0xFF22E3FF),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF160620), Color(0xFF200B30)))
        ),
        VisualizerPalette(
            primary = Color(0xFFFFC857),
            secondary = Color(0xFFFF6D00),
            accent = Color(0xFFFFF176),
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF1A1406), Color(0xFF291E09)))
        )
    )

    fun getPaletteForTrack(audio: LocalAudio): VisualizerPalette {
        val seed = Math.abs(audio.title.hashCode() xor audio.artist.hashCode() xor audio.id.hashCode())
        return palettes[seed % palettes.size]
    }

    fun getInitialEffectForTrack(audio: LocalAudio): VisualizerEffect {
        val seed = Math.abs(audio.title.hashCode() xor audio.id.hashCode())
        val effects = VisualizerEffect.values()
        return effects[seed % effects.size]
    }
}

/**
 * Interactive Dynamic Audio Visualizer:
 * Renders procedural 60fps animations that dynamically adapt to each song without repeating.
 * Tap to cycle effect or randomize theme.
 */
@Composable
fun DynamicMusicVisualizer(
    audio: LocalAudio,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Determine unique non-repeating palette and default effect based on current track
    val palette = remember(audio.id, audio.title) {
        VisualizerThemeEngine.getPaletteForTrack(audio)
    }

    // User can manually cycle through effects or let it automatically select per song
    var currentEffectIndex by remember(audio.id) {
        mutableIntStateOf(VisualizerThemeEngine.getInitialEffectForTrack(audio).ordinal)
    }

    val effects = VisualizerEffect.values()
    val activeEffect = effects[currentEffectIndex % effects.size]

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2400 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val beatPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 550 else 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.backgroundBrush)
            .clickable {
                // Interactive tap to cycle visualizer effects
                currentEffectIndex = (currentEffectIndex + 1) % effects.size
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Header Bar: Active Effect Badge & Interactive Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activeEffect.badge} ${activeEffect.titleAr}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "• مباشر" else "• متوقف",
                        color = if (isPlaying) palette.accent else Color(0xFFA9B4D6),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "المس لتبديل التأثير ⚡",
                        color = palette.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Canvas Stage (60fps interactive procedural visualizer)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33000000))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val centerX = w / 2f

                    when (activeEffect) {
                        VisualizerEffect.NEON_SPECTRUM -> {
                            // 28 Equalizer frequency bars with gradient and bounce
                            val bars = 26
                            val barWidth = (w / bars) * 0.65f
                            val gap = (w / bars) * 0.35f

                            for (i in 0 until bars) {
                                val offsetRatio = i.toFloat() / bars
                                val waveFactor = sin(phase + offsetRatio * 5f)
                                val baseHeight = if (isPlaying) (0.25f + 0.70f * Math.abs(waveFactor)) else 0.15f
                                val barH = (h * 0.85f * baseHeight).coerceAtLeast(4f)
                                val x = i * (barWidth + gap) + gap / 2f
                                val y = centerY - barH / 2f

                                val barBrush = Brush.verticalGradient(
                                    colors = listOf(palette.primary, palette.secondary, palette.accent),
                                    startY = y,
                                    endY = y + barH
                                )

                                drawRoundRect(
                                    brush = barBrush,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                )

                                // Glowing peak cap
                                drawCircle(
                                    color = Color.White,
                                    radius = barWidth * 0.45f,
                                    center = Offset(x + barWidth / 2f, y - 3f)
                                )
                            }
                        }

                        VisualizerEffect.COSMIC_ORBIT -> {
                            // 3 Orbiting particle rings swirling outward
                            val particleCount = 32
                            for (i in 0 until particleCount) {
                                val angle = (i * (2 * PI / particleCount) + phase * (if (i % 2 == 0) 1f else -1f)).toFloat()
                                val orbitRadius = (20f + (i % 4) * 8f) * beatPulse
                                val px = centerX + cos(angle) * orbitRadius * (w / 140f)
                                val py = centerY + sin(angle) * orbitRadius * 0.7f

                                val pColor = if (i % 3 == 0) palette.primary else if (i % 3 == 1) palette.secondary else palette.accent
                                drawCircle(
                                    color = pColor.copy(alpha = if (isPlaying) 0.85f else 0.4f),
                                    radius = (2.5f + (i % 3) * 1.5f),
                                    center = Offset(px.coerceIn(4f, w - 4f), py.coerceIn(4f, h - 4f))
                                )
                            }

                            // Glowing core ring
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(palette.accent.copy(alpha = 0.5f), Color.Transparent),
                                    center = Offset(centerX, centerY),
                                    radius = 35f * beatPulse
                                ),
                                center = Offset(centerX, centerY),
                                radius = 35f * beatPulse
                            )
                        }

                        VisualizerEffect.CYBER_WAVES -> {
                            // Triple intertwining sine waves
                            for (waveIdx in 0..2) {
                                val path = Path()
                                val wavePhase = phase * (1f + waveIdx * 0.35f) + waveIdx * (PI.toFloat() / 2f)
                                val waveColor = when (waveIdx) {
                                    0 -> palette.primary
                                    1 -> palette.secondary
                                    else -> palette.accent
                                }

                                path.moveTo(0f, centerY)
                                var stepX = 0f
                                while (stepX <= w) {
                                    val normalX = stepX / w
                                    val amp = (h * 0.32f) * (if (isPlaying) sin(normalX * 4f * PI.toFloat() + wavePhase) else 0.2f * sin(normalX * 2f * PI.toFloat() + wavePhase))
                                    path.lineTo(stepX, centerY + amp)
                                    stepX += 10f
                                }

                                drawPath(
                                    path = path,
                                    color = waveColor.copy(alpha = 0.85f),
                                    style = Stroke(width = if (waveIdx == 0) 3.5f else 2.2f, cap = StrokeCap.Round)
                                )
                            }
                        }

                        VisualizerEffect.VINYL_RIPPLE -> {
                            // Concentric pulsing audio ripples
                            val rippleCount = 4
                            for (r in 1..rippleCount) {
                                val radius = ((r * 14f * beatPulse) + (phase * 6f) % 20f).coerceAtMost(h * 0.9f)
                                val alpha = (1f - (radius / (h * 0.9f))).coerceIn(0.1f, 0.9f)
                                drawCircle(
                                    color = palette.primary.copy(alpha = alpha),
                                    radius = radius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 2.5f)
                                )
                            }

                            // Center disc
                            drawCircle(
                                color = palette.secondary,
                                radius = 10f,
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = Offset(centerX, centerY)
                            )
                        }

                        VisualizerEffect.AURORA_BEAMS -> {
                            // Aurora flowing gradient wave
                            val path = Path()
                            path.moveTo(0f, h)
                            var x = 0f
                            while (x <= w) {
                                val normalX = x / w
                                val yOffset = centerY + sin(normalX * 6f + phase) * (h * 0.35f) * beatPulse
                                path.lineTo(x, yOffset)
                                x += 12f
                            }
                            path.lineTo(w, h)
                            path.close()

                            drawPath(
                                path = path,
                                brush = Brush.verticalGradient(
                                    listOf(palette.primary.copy(alpha = 0.5f), palette.secondary.copy(alpha = 0.2f), Color.Transparent),
                                    startY = 0f,
                                    endY = h
                                )
                            )
                        }

                        VisualizerEffect.KALEIDO_RINGS -> {
                            // Geometric frequency kaleidoscope
                            val segments = 8
                            for (s in 0 until segments) {
                                val angle = s * (2 * PI / segments) + phase
                                val r1 = 12f * beatPulse
                                val r2 = 32f * beatPulse
                                val x1 = centerX + cos(angle).toFloat() * r1
                                val y1 = centerY + sin(angle).toFloat() * r1
                                val x2 = centerX + cos(angle).toFloat() * r2
                                val y2 = centerY + sin(angle).toFloat() * r2

                                drawLine(
                                    color = if (s % 2 == 0) palette.primary else palette.accent,
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = palette.secondary,
                                    radius = 3f,
                                    center = Offset(x2, y2)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
