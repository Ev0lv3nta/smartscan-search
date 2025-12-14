package com.fpf.smartscan.search

import android.net.Uri


enum class QueryType {
    TEXT, IMAGE
}

enum class SearchMode {
    GENERAL, FACE
}

enum class ProcessorStatus {IDLE, ACTIVE, COMPLETE, FAILED }

sealed interface SearchQuery{
    data class ImageQuery(val uri: Uri): SearchQuery
    data class TextQuery(val text: String): SearchQuery
}
