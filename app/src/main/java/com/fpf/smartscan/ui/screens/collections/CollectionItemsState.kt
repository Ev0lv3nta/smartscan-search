package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType

data class CollectionItemsState(
    val collection: MediaCollection? = null,
    val mediaType: MediaType? = null,
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selectedMediaItems: Set<MediaItem> = emptySet(),
    val excludedMediaItems: Set<MediaItem> = emptySet(),
    val selectAll: Boolean = false,
    val selectedCount: Int = 0
    )