package com.fpf.smartscan.ui.components.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade

@Composable
fun ImageDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxSize:Int = 512
) {
    val context = LocalContext.current

    val request = ImageRequest.Builder(context)
        .allowHardware(true)
        .crossfade(false)
        .data(uri)
        .size(maxSize, maxSize)
        .build()

    Box(modifier = modifier.background(Color.Transparent), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = request,
            contentDescription = "Displayed image",
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
