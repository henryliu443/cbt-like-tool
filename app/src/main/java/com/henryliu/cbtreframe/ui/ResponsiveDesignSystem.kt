package com.henryliu.cbtreframe.ui

import android.content.res.Resources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WindowWidthSizeClass {
    Compact, Medium, Expanded
}

object WindowSizeState {
    val current: WindowWidthSizeClass
        get() {
            val metrics = Resources.getSystem().displayMetrics
            val widthDp = metrics.widthPixels / metrics.density
            return when {
                widthDp < 600 -> WindowWidthSizeClass.Compact
                widthDp < 840 -> WindowWidthSizeClass.Medium
                else -> WindowWidthSizeClass.Expanded
            }
        }
}
