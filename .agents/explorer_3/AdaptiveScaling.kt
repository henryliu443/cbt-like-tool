package com.henryliu.cbtreframe.ui

import android.content.res.Resources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Global adaptive scaling based on standard mobile width of 412dp.
 * Uses global display metrics to remain safe for non-composable scopes (like Canvas).
 */
object AdaptiveScaling {
    const val BASE_WIDTH_DP = 412f

    val scaleFactor: Float
        get() {
            val metrics = Resources.getSystem().displayMetrics
            val screenWidthDp = metrics.widthPixels / metrics.density
            return (screenWidthDp / BASE_WIDTH_DP).coerceIn(0.85f, 1.15f)
        }
}

val Number.adaptiveDp: Dp
    get() = (this.toFloat() * AdaptiveScaling.scaleFactor).dp

val Number.adaptiveSp: TextUnit
    get() = (this.toFloat() * AdaptiveScaling.scaleFactor).sp
