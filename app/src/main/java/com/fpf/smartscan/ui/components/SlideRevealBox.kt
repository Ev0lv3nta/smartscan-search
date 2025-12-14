package com.fpf.smartscan.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import kotlin.math.roundToInt

@Composable
fun SlideRevealBox(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    visibilityPercent: Float = 1f,
    reverse: Boolean = false,
    content: @Composable () -> Unit,
    ) {
    if (!isVisible) return

    var contentHeight by remember { mutableIntStateOf(0) }

    val animatedPct by animateFloatAsState(
        targetValue = visibilityPercent,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        )
    )

    val direction = if (reverse) -1 else 1

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val placeables = subcompose("content", content).map { it.measure(constraints) }
        contentHeight = placeables.maxOfOrNull { it.height } ?: 0
        val animatedHeight = (contentHeight * animatedPct).roundToInt()
        layout(constraints.maxWidth, animatedHeight) {
            val animatedY = direction * (contentHeight - animatedHeight)

            placeables.forEach {
                it.place(
                    x = 0,
                    y = animatedY
                )
            }
        }
    }
}