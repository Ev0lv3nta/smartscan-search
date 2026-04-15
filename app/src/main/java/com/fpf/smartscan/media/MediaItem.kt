package com.fpf.smartscan.media

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType
)