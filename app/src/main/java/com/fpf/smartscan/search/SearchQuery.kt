package com.fpf.smartscan.search

import android.net.Uri
import com.fpf.smartscan.media.MediaType

sealed interface SearchQuery{
    data class ImageQuery(val uri: Uri, val mediaType: MediaType): SearchQuery
    data class TextQuery(val text: String, val mediaType: MediaType): SearchQuery
}