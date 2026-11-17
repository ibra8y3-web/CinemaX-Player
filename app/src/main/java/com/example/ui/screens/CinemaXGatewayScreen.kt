package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.NavTab
import kotlinx.coroutines.delay

/**
 * Compact CinemaX welcome gateway.
 * The primary actions and feature overview are kept inside one viewport on
 * ordinary phone layouts; no long introductory scroll is required.
 */
@Composable
fun CinemaXGatewayScreen(
    onEnterApp: (initialTab: NavTab) -> Unit
) {
    var countdownSeconds by remember { mutableIntStateOf(3) }
    var isAutoEnterActive by remember { mutableStateOf(true) }

    LaunchedEffect(isAutoEnterActive) {
        if (isAutoEnterActive) {
            while (countdownSeconds > 0) {
                delay(1_000)
                countdownSeconds -= 1
            }
            if (isAutoEnterActive) onEnterApp(NavTab.VIDEOS)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1028), Color(0xFF141943), Color(0xFF0A1F3F))
                )
            )
    ) {
        val compact = maxHeight < 680.dp
        val sidePadding = if (maxWidth < 360.dp) 12.dp else 16.dp
        val itemGap = if (compact) 5.dp else 7.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = sidePadding, vertical = if (compact) 6.dp else 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(itemGap)
        ) {
            // Brand header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 76.dp else 90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x6622E3FF), RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cinemax_hero),
                        contentDescription = "خلفية CinemaX Player",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x880E1230)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 58.dp else 66.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF22E3FF),
                                            Color(0xFFFFC857),
                                            Color(0xFFE040FB),
                                            Color(0xFF22E3FF)
                                        )
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
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "شعار CinemaX Player",
                                    tint = Color(0xFF22E3FF),
                                    modifier = Modifier.size(if (compact) 34.dp else 40.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "CinemaX Player",
                    color = Color.White,
                    fontSize = if (compact) 22.sp else 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "مشغّل الوسائط والبث المباشر",
                    color = Color(0xFF22E3FF),
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x222EE6A6),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x772EE6A6))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2EE6A6))
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "مكتبة فيديو وصوت وأدوات وسائط في تطبيق واحد",
                        color = Color(0xFF5CE6B5),
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Concise feature overview in two rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(itemGap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(itemGap)) {
                    GatewayFeatureCard(
                        icon = Icons.Default.Movie,
                        iconTint = Color(0xFF22E3FF),
                        title = "مشغّل الفيديو",
                        desc = "ترجمات · PiP · Cast · إيماءات",
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                    GatewayFeatureCard(
                        icon = Icons.Default.Headphones,
                        iconTint = Color(0xFF9D7BFF),
                        title = "الموسيقى والصوت",
                        desc = "تشغيل بالخلفية · Equalizer",
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(itemGap)) {
                    GatewayFeatureCard(
                        icon = Icons.Default.LiveTv,
                        iconTint = Color(0xFFFF6B8B),
                        title = "التلفزيون والراديو",
                        desc = "مصادر وقنوات ومحطات",
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                    GatewayFeatureCard(
                        icon = Icons.Default.Widgets,
                        iconTint = Color(0xFFFFC857),
                        title = "أدوات الوسائط",
                        desc = "قص · ضغط · استخراج · GIF",
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(itemGap)) {
                    GatewayFeatureCard(
                        icon = Icons.Default.Bolt,
                        iconTint = Color(0xFF2EE6A6),
                        title = "المشاركة والمزامنة",
                        desc = "Wi-Fi · نسخ واستعادة",
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                    GatewayFeatureCard(
                        icon = Icons.Default.Radio,
                        iconTint = Color(0xFF22E3FF),
                        title = "المركز الإسلامي",
                        desc = "قرآن · صلاة · قبلة · أذكار",
                        compact = compact,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "دخول سريع إلى القسم",
                color = Color(0xFFA9B4D6),
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DirectNavChip("فيديو", NavTab.VIDEOS, Modifier.weight(1f), compact) {
                    isAutoEnterActive = false
                    onEnterApp(it)
                }
                DirectNavChip("موسيقى", NavTab.MUSIC, Modifier.weight(1f), compact) {
                    isAutoEnterActive = false
                    onEnterApp(it)
                }
                DirectNavChip("قنوات", NavTab.LIVE, Modifier.weight(1f), compact) {
                    isAutoEnterActive = false
                    onEnterApp(it)
                }
                DirectNavChip("راديو", NavTab.RADIO, Modifier.weight(1f), compact) {
                    isAutoEnterActive = false
                    onEnterApp(it)
                }
                DirectNavChip("إسلاميات", NavTab.ISLAMIC, Modifier.weight(1f), compact) {
                    isAutoEnterActive = false
                    onEnterApp(it)
                }
            }

            Button(
                onClick = {
                    isAutoEnterActive = false
                    onEnterApp(NavTab.VIDEOS)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22E3FF),
                    contentColor = Color(0xFF0E1230)
                ),
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = if (compact) 10.dp else 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enter_cinema_gateway_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (isAutoEnterActive && countdownSeconds > 0)
                        "دخول المشغّل الآن · تلقائي خلال $countdownSeconds ثوانٍ"
                    else
                        "دخول CinemaX Player",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.sp else 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = if (isAutoEnterActive) "إلغاء الدخول التلقائي والبقاء هنا" else "اختر أي قسم من الأزرار بالأعلى",
                color = Color(0xFFA9B4D6),
                fontSize = 9.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isAutoEnterActive = false }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun GatewayFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(if (compact) 49.dp else 56.dp),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272D66))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 28.dp else 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 9.sp else 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = desc,
                    color = Color(0xFFA9B4D6),
                    fontSize = if (compact) 7.sp else 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DirectNavChip(
    label: String,
    targetTab: NavTab,
    modifier: Modifier = Modifier,
    compact: Boolean,
    onClick: (NavTab) -> Unit
) {
    Surface(
        color = Color(0xFF20285A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6622E3FF)),
        modifier = modifier
            .height(if (compact) 31.dp else 35.dp)
            .clickable { onClick(targetTab) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
