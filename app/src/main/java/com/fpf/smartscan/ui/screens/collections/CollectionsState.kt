package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaType

data class CollectionsState(
    val showAllCollections: Boolean = false,
    val viewAutoCollections: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val collectToView: MediaCollection? = null,
    val selectedCollections: Set<MediaCollection> = emptySet(),
    val totalCollections: Int = 0
)