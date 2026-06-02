package com.fpf.smartscan.ui.screens.collections

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.media.CollectionType

@Composable
fun EmptyCollectionScreen(
    isVisible: Boolean,
    isIndexing: Boolean,
    collectionType: CollectionType
) {
    if (!isVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isIndexing && collectionType == CollectionType.CLUSTER) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = stringResource(R.string.collections_scan_in_progress_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(96.dp)
                        .rotate(rotation.value)
                )

                Text(
                    text = stringResource(R.string.collections_scan_in_progress_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    text = stringResource(R.string.collections_scan_in_progress_description),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = "Album icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp)
                )

                Text(
                    text = stringResource(R.string.collections_no_collections_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    text = stringResource(R.string.collections_no_collections_description),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}