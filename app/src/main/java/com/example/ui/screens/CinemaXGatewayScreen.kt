package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.NavTab
import kotlinx.coroutines.delay

/**
 * CinemaX Gateway / Entrance Screen (بوابة الدخول السينمائية الذكية)
 * Displayed upon launching the application, presenting features, system readiness,
 * and direct entrance routes into specific sections.
 */
@Composable
fun CinemaXGatewayScreen(
    onEnterApp: (initialTab: NavTab) -> Unit
) {
    var countdownSeconds by remember { mutableIntStateOf(5) }
    var isAutoEnterActive by remember { mutableStateOf(true) }

    // Pulsing and rotating cinema animations
    val infiniteTransition = rememberInfiniteTransition(label = "gateway_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val reelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelRotation"
    )

    // Optional 5-second smooth entrance timer
    LaunchedEffect(isAutoEnterActive) {
        if (isAutoEnterActive) {
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
            onEnterApp(NavTab.VIDEOS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1230),
                        Color(0xFF141943),
                        Color(0xFF0A1F3F)
                    )
                )
            )
    ) {
        // Ambient background glow circles
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .clip(CircleShape)
                .background(Color(0x1522E3FF))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cinematic Hero Banner Backdrop with Glowing Reel Emblem
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x4422E3FF), RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cinemax_hero),
                        contentDescription = "خلفية بوابة CinemaX Player السينمائية",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x550E1230), Color(0xBB0E1230))
                                )
                            )
                    )
                    // Cinematic Glowing Logo Reel centered on Banner
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(Color(0xFF22E3FF), Color(0xFFFFC857), Color(0xFFE040FB), Color(0xFF22E3FF))
                                )
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF0E1230)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "CinemaX Logo",
                                tint = Color(0xFF22E3FF),
                                modifier = Modifier
                                    .size(40.dp)
                                    .rotate(reelRotation)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App Branding Title
                Text(
                    text = "CinemaX Player",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "مشغل الوسائط والبث المباشر الذكي فائق الأداء",
                    color = Color(0xFF22E3FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Readiness Badge
                Surface(
                    color = Color(0x332EE6A6),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E676)),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2EE6A6))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المحرك السينمائي v2.5 جاهز 100% للتشغيل الفوري",
                            color = Color(0xFF2EE6A6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Highlights Gateway Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GatewayFeatureCard(
                        icon = Icons.Default.Movie,
                        iconTint = Color(0xFF22E3FF),
                        title = "مشغل 4K وفلاتر ألوان حية (Cinema HDR)",
                        desc = "تشغيل سلس لكافة الصيغ مع فلاتر AMOLED وWarm Cinema وNight Shield"
                    )

                    GatewayFeatureCard(
                        icon = Icons.Default.Radio,
                        iconTint = Color(0xFFFFC857),
                        title = "إذاعات وبث صوتي مباشر (+45,000 محطة)",
                        desc = "آلاف المحطات الإذاعية الحقيقية من مصر وفرنسا وأوروبا وقرآن كريم موفر للباقة"
                    )

                    GatewayFeatureCard(
                        icon = Icons.Default.LiveTv,
                        iconTint = Color(0xFFFF6B8B),
                        title = "دليل القنوات الحية والبث التلفزيوني",
                        desc = "قنوات إخبارية ورياضية ووثائقية حقيقية مع فاحص ذكي للروابط الحية"
                    )

                    GatewayFeatureCard(
                        icon = Icons.Default.Headphones,
                        iconTint = Color(0xFF9D7BFF),
                        title = "استوديو الموسيقى وقص النغمات والمشاركة",
                        desc = "مؤثرات بصرية حركية، موازن صوت Equalizer، وقص الأغاني ومشاركتها فوراً"
                    )

                    GatewayFeatureCard(
                        icon = Icons.Default.Bolt,
                        iconTint = Color(0xFF2EE6A6),
                        title = "المزامنة اللامركزية ومشاركة Wi-Fi",
                        desc = "استعادة السجل والمفضلة برمز سحابي موحد PIN وبث الفيديوهات للكمبيوتر"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Fast Direct Navigation Chips
                Text(
                    text = "أو اختر قسماً للدخول المباشر إليه:",
                    color = Color(0xFFA9B4D6),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DirectNavChip("فيديوهات 🎬", NavTab.VIDEOS, Modifier.weight(1f)) {
                        isAutoEnterActive = false
                        onEnterApp(it)
                    }
                    DirectNavChip("موسيقى 🎵", NavTab.MUSIC, Modifier.weight(1f)) {
                        isAutoEnterActive = false
                        onEnterApp(it)
                    }
                    DirectNavChip("قنوات 📺", NavTab.LIVE, Modifier.weight(1f)) {
                        isAutoEnterActive = false
                        onEnterApp(it)
                    }
                    DirectNavChip("راديو 📻", NavTab.RADIO, Modifier.weight(1f)) {
                        isAutoEnterActive = false
                        onEnterApp(it)
                    }
                    DirectNavChip("إسلاميات 🕌", NavTab.ISLAMIC, Modifier.weight(1f)) {
                        isAutoEnterActive = false
                        onEnterApp(it)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Entrance Action Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        isAutoEnterActive = false
                        onEnterApp(NavTab.VIDEOS)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22E3FF),
                        contentColor = Color(0xFF0E1230)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enter_cinema_gateway_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAutoEnterActive && countdownSeconds > 0)
                                "دخول المشغل الآن (تلقائي خلال $countdownSeconds ثوانٍ) 🚀"
                            else
                                "دخول مشغل CinemaX Player 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pause timer link
                if (isAutoEnterActive) {
                    Text(
                        text = "اضغط هنا لإلغاء الدخول التلقائي والبقاء في بوابة الترحيب",
                        color = Color(0xFFA9B4D6),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clickable { isAutoEnterActive = false }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GatewayFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272D66)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(desc, color = Color(0xFFA9B4D6), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DirectNavChip(
    label: String,
    targetTab: NavTab,
    modifier: Modifier = Modifier,
    onClick: (NavTab) -> Unit
) {
    Surface(
        color = Color(0xFF272D66),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4422E3FF)),
        modifier = modifier.clickable { onClick(targetTab) }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
