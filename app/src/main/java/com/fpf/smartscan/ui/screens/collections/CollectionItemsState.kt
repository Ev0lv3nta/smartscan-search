package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType

data class CollectionItemsState(
    val collectionName: String? = null,
    val clusterId: Long = -1L,
    val mediaType: MediaType? = null,
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selectedMediaItems: Set<MediaItem> = emptySet(),
)