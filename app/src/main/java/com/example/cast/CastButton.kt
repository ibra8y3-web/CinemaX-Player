package com.example.cast

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.media.model.LocalVideo

@Composable
fun CastButton(
    castManager: CastManager,
    currentVideo: LocalVideo? = null,
    currentMediaUri: String? = null,
    currentTitle: String? = null,
    currentPositionMs: Long = 0L,
    isLiveStream: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isCasting by castManager.isCasting.collectAsState()
    var showCastDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "cast_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "castPulseScale"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (isCasting) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFF22E3FF).copy(alpha = 0.25f))
            )
        }

        IconButton(
            onClick = { showCastDialog = true },
            modifier = Modifier.testTag("google_cast_button")
        ) {
            Icon(
                imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                contentDescription = if (isCasting) "متصل بـ Chromecast" else "بث إلى التلفاز",
                tint = if (isCasting) Color(0xFF22E3FF) else Color.White
            )
        }
    }

    if (showCastDialog) {
        CastDialog(
            castManager = castManager,
            currentVideo = currentVideo,
            currentMediaUri = currentMediaUri,
            currentTitle = currentTitle,
            currentPositionMs = currentPositionMs,
            isLiveStream = isLiveStream,
            onDismissRequest = { showCastDialog = false }
        )
    }
}
