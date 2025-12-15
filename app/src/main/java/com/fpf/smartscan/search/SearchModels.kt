package com.fpf.smartscan.search

import android.net.Uri
import com.fpf.smartscan.media.MediaType


enum class QueryType {
    TEXT, IMAGE
}

enum class SearchMode {
    GENERAL, FACE
}

enum class ProcessorStatus {IDLE, ACTIVE, COMPLETE, FAILED }

sealed interface SearchQuery{
    data class ImageQuery(val uri: Uri, val mediaType: MediaType): SearchQuery
    data class TextQuery(val text: String, val mediaType: MediaType): SearchQuery
}
