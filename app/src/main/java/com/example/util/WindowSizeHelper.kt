package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT,   // Phones portrait (<600dp)
    MEDIUM,    // Foldables / Small tablets (600dp .. 839dp)
    EXPANDED   // Large tablets / Landscape (>840dp)
}

enum class WindowHeightClass {
    COMPACT,   // Phones landscape (<480dp)
    MEDIUM,    // Phones portrait / Tablets (480dp .. 899dp)
    EXPANDED   // Tall screens (>900dp)
}

data class WindowSizeInfo(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp
) {
    val isCompactWidth: Boolean get() = widthClass == WindowWidthClass.COMPACT
    val isMediumWidth: Boolean get() = widthClass == WindowWidthClass.MEDIUM
    val isExpandedWidth: Boolean get() = widthClass == WindowWidthClass.EXPANDED

    val isCompactHeight: Boolean get() = heightClass == WindowHeightClass.COMPACT
    val isPortrait: Boolean get() = screenHeightDp >= screenWidthDp
    val isLandscape: Boolean get() = screenWidthDp > screenHeightDp

    // Dynamic scale factor for icons and paddings
    val scaleFactor: Float
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> (screenWidthDp.value / 380f).coerceIn(0.85f, 1.15f)
            WindowWidthClass.MEDIUM -> 1.1f
            WindowWidthClass.EXPANDED -> 1.25f
        }
}

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val widthClass = when {
        screenWidthDp < 600.dp -> WindowWidthClass.COMPACT
        screenWidthDp < 840.dp -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    val heightClass = when {
        screenHeightDp < 480.dp -> WindowHeightClass.COMPACT
        screenHeightDp < 900.dp -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }

    return WindowSizeInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp
    )
}
