package com.fpf.smartscan.ui.screens.collections

import android.net.Uri
import com.fpf.smartscan.media.MediaType

data class CollectionItemsState(
    val collectionName: String? = null,
    val clusterId: Long = -1L,
    val mediaType: MediaType = MediaType.IMAGE,
    val loading: Boolean = false,
    val error: String? = null,
    val mediaToView: Uri? = null,
    val selectedMediaItems: List<Uri> = emptyList(),
)