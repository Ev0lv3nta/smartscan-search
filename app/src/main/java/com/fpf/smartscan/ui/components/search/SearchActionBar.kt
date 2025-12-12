package com.fpf.smartscan.ui.components.search

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SearchActionBar(
    isVisible: Boolean,
    searchEnabled: Boolean,
    onSearch: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    visibilityPercent: Float = 1f,
) {
    if(!isVisible) return

    var heightPx by remember { mutableIntStateOf(0) }

    val animatedPct by animateFloatAsState(
        targetValue = visibilityPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        modifier = modifier
            .height(70.dp)
            .onGloballyPositioned { coords -> heightPx = coords.size.height }
            .offset { IntOffset(0, ((1f - animatedPct) * heightPx).roundToInt()) }
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                enabled = searchEnabled,
                onClick = { onSearch() }
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                    )
                    Text(
                        "Search",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Button (
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { onShare() }
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share",
                    )
                    Text("Share", style = MaterialTheme.typography.labelMedium)
                }
            }

            Button (
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { onDelete() }
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally){
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                    )
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}