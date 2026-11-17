package com.example.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtitles.SubtitleStyleConfig

@Composable
fun LiveSubtitleOverlay(
    activeCueText: String,
    translatedCueText: String,
    style: SubtitleStyleConfig,
    modifier: Modifier = Modifier
) {
    val displayedText = if (style.isLiveTranslationEnabled && translatedCueText.isNotEmpty()) {
        translatedCueText
    } else {
        activeCueText
    }

    AnimatedVisibility(
        visible = displayedText.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(style.backgroundColor))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = displayedText,
                    color = Color(style.textColor),
                    fontSize = style.fontSizeSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = (style.fontSizeSp + 6).sp
                )
            }
        }
    }
}
