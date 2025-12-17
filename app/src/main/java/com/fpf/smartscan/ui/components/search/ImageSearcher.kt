package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.components.SelectorItem
import com.fpf.smartscan.ui.components.media.ImageDisplay


@Composable
fun ImageSearcher(
    uri: Uri?,
    mediaType: MediaType,
    mediaTypeSelectorEnabled: Boolean,
    onMediaTypeChange: (type: MediaType) -> Unit,
    onSearch: () -> Unit,
    onRemoveImage: () -> Unit,
    imageSize: Dp = 150.dp
){
    if(uri == null) return

    LaunchedEffect(mediaType) {
        onSearch()
    }

    Row (
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .heightIn(max=160.dp)
                .size(imageSize)
                .border(0.5.dp, MaterialTheme.colorScheme.outline)
                .dropShadow(
                    shape = RoundedCornerShape(4.dp),
                    shadow = Shadow(
                        radius = 4.dp,
                        spread = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.15f),
                        offset = DpOffset(x = 2.dp, 2.dp)
                    )
                )
        ) {
            ImageDisplay(
                maxSize = 1024,
                uri = uri,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = { onRemoveImage() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    .size(16.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove Image",
                    tint = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
        SelectorItem(
            enabled = mediaTypeSelectorEnabled, // prevent switching modes when indexing in progress
            label = "Media type",
            showLabel = false,
            options = mediaTypeOptions.values.toList(),
            selectedOption = mediaTypeOptions[mediaType]!!,
            onOptionSelected = { selected ->
                val newMode = mediaTypeOptions.entries
                    .find { it.value == selected }
                    ?.key ?: MediaType.IMAGE
                onMediaTypeChange(newMode)
            }
        )
    }
}