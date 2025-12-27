package com.fpf.smartscan.ui.components.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.fpf.smartscan.media.MediaType

@Composable
fun ImageDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxSize:Int = 512,
    mediaType: MediaType,
) {
    val context = LocalContext.current

    val request = ImageRequest.Builder(context)
        .allowHardware(true)
        .crossfade(false)
        .data(uri)
        .size(maxSize, maxSize)
        .build()

    Box(modifier = modifier.background(Color.Transparent), contentAlignment = Alignment.Center) {
        if(mediaType == MediaType.VIDEO){
            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.align(
                Alignment.Center).zIndex(1f))
        }
        AsyncImage(
            model = request,
            contentDescription = "Displayed image",
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
