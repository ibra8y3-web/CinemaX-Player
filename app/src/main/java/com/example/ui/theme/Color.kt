package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- CinemaX palette: deep indigo base, cyan + violet + coral accents ----
val CyanPrimary = Color(0xFF22E3FF)
val CyanSecondary = Color(0xFF5B8CFF)
val VioletAccent = Color(0xFF8B5CF6)
val CoralAccent = Color(0xFFFF6B8B)
val EmeraldAccent = Color(0xFF2EE6A6)
val AccentAmber = Color(0xFFFFC857)

val DarkBackground = Color(0xFF0E1230)
val DarkSurface = Color(0xFF1A1F4A)
val DarkSurfaceVariant = Color(0xFF272D66)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA9B4D6)

// Signature gradients
val BackgroundGradient = Brush.verticalGradient(
    listOf(Color(0xFF111541), Color(0xFF1A1250), Color(0xFF0A1F3F))
)
val BrandGradient = Brush.linearGradient(listOf(CyanPrimary, VioletAccent))
val EmeraldGradient = Brush.linearGradient(listOf(Color(0xFF0FA67E), Color(0xFF1D6F8F)))
val AmberGradient = Brush.linearGradient(listOf(Color(0xFFFF9A3C), Color(0xFFE5456B)))

/** Rich gradient background used on every main screen instead of flat black. */
fun Modifier.cinemaBackground(): Modifier = this.background(BackgroundGradient)
