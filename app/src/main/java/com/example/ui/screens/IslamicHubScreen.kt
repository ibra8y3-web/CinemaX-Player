package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duas.DuaManager
import com.example.prayer.PrayerTimesManager
import com.example.radio.RadioPlayerManager
import com.example.ui.screens.duas.DuasScreen
import com.example.ui.screens.prayer.PrayerTimesScreen
import com.example.ui.screens.quran.QuranReaderScreen
import com.example.ui.theme.EmeraldGradient
import com.example.ui.theme.cinemaBackground

private enum class IslamicPage { HUB, QURAN, PRAYER, DUAS }

/**
 * Dedicated bottom-tab for every Islamic tool: Quran, prayer times & adhan,
 * Hisn Al-Muslim (adhkar), tasbih and adhan alarms.
 * (These used to be squeezed above the radio list.)
 */
@Composable
fun IslamicHubScreen(
    radioManager: RadioPlayerManager,
    prayerManager: PrayerTimesManager?,
    duaManager: DuaManager?,
    requestedPrayerTab: Int? = null,
    onConsumePrayerTab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(IslamicPage.HUB) }
    var prayerInitialTab by remember { mutableStateOf(0) }

    LaunchedEffect(requestedPrayerTab) {
        requestedPrayerTab?.let { idx ->
            prayerInitialTab = idx
            page = IslamicPage.PRAYER
            onConsumePrayerTab()
        }
    }

    BackHandler(enabled = page != IslamicPage.HUB) { page = IslamicPage.HUB }

    when (page) {
        IslamicPage.QURAN -> {
            QuranReaderScreen(
                radioManager = radioManager,
                onBack = { page = IslamicPage.HUB },
                modifier = modifier
            )
            return
        }
        IslamicPage.PRAYER -> {
            val pm = prayerManager ?: remember { PrayerTimesManager(context) }
            PrayerTimesScreen(
                prayerManager = pm,
                initialTab = prayerInitialTab,
                onBack = { page = IslamicPage.HUB },
                modifier = modifier
            )
            return
        }
        IslamicPage.DUAS -> {
            val dm = duaManager ?: remember { DuaManager(context) }
            DuasScreen(
                duaManager = dm,
                onBack = { page = IslamicPage.HUB },
                modifier = modifier
            )
            return
        }
        IslamicPage.HUB -> Unit
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EmeraldGradient)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mosque, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("إسلاميات", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "المصحف • الأذان • الأذكار • التسبيح",
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            HubCard(
                icon = Icons.Default.MenuBook,
                title = "المصحف الشريف",
                subtitle = "114 سورة كاملة مع إمكانية القراءة أثناء الاستماع للإذاعة",
                colors = listOf(Color(0xFF22E3FF), Color(0xFF5B8CFF)),
                tag = "open_quran_reader_btn"
            ) { page = IslamicPage.QURAN }
        }
        item {
            HubCard(
                icon = Icons.Default.Explore,
                title = "مواقيت الصلاة والقبلة",
                subtitle = "العد التنازلي للصلاة القادمة، القبلة، واختيار المدينة أو GPS",
                colors = listOf(Color(0xFF2EE6A6), Color(0xFF0FA67E)),
                tag = "open_prayer_times_btn"
            ) { prayerInitialTab = 0; page = IslamicPage.PRAYER }
        }
        item {
            HubCard(
                icon = Icons.Default.Favorite,
                title = "الأذكار والأدعية وحصن المسلم",
                subtitle = "أذكار الصباح والمساء، الرقية، تفريج الكرب مع عداد ونطق صوتي",
                colors = listOf(Color(0xFFFFC857), Color(0xFFFF8A3D)),
                tag = "open_duas_screen_btn"
            ) { page = IslamicPage.DUAS }
        }
        item {
            HubCard(
                icon = Icons.Default.TouchApp,
                title = "المسبحة وسجل الأذكار",
                subtitle = "عداد تسبيح ذكي وسجل كامل لأذكارك",
                colors = listOf(Color(0xFFB388FF), Color(0xFF8B5CF6)),
                tag = "open_tasbih_btn"
            ) { prayerInitialTab = 3; page = IslamicPage.PRAYER }
        }
        item {
            HubCard(
                icon = Icons.Default.Notifications,
                title = "منبهات الأذان والمؤذنين",
                subtitle = "اختيار المؤذن لكل صلاة وتذكير قبل الأذان والإقامة",
                colors = listOf(Color(0xFFFF6B8B), Color(0xFFE5456B)),
                tag = "open_adhan_alarms_btn"
            ) { prayerInitialTab = 2; page = IslamicPage.PRAYER }
        }
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: List<Color>,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1F4A))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Color(0xFFA9B4D6), fontSize = 12.sp, lineHeight = 17.sp)
        }
        Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFFA9B4D6))
    }
}
