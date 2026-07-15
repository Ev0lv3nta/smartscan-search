package io.github.ev0lv3nta.smartscansearch.media

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType
)