package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.media.MediaCollection

data class CollectionsState(
    val showAllCollections: Boolean = false,
    val groupBySimilarity: Boolean = true,
    val loading: Boolean = false,
    val collectToView: MediaCollection? = null,
    val selectedCollections: Set<MediaCollection> = emptySet(),
    val totalCollections: Int = 0
)