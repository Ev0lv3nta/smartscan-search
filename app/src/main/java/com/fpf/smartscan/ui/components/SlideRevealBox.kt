package com.fpf.smartscan.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SlideRevealBox(
    height: Int,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    visibilityPercent: Float = 1f,
    content: @Composable () -> Unit,
    ) {
    if (!isVisible) return

    val density = LocalDensity.current
    val heightPx = with(density) { height.dp.toPx() }
    val animatedPct by animateFloatAsState(
        targetValue = visibilityPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        modifier = modifier
            .offset { IntOffset(0, ((1f - animatedPct) * heightPx).roundToInt()) }
            .fillMaxWidth()
    ) {
        content()
    }
}