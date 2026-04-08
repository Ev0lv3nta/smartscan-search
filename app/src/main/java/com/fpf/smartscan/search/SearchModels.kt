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

data class TagSuggestionsResult(
    val bestMatch: MediaTag? = null,
    val lastedUsed: MediaTag? = null,
    val confidence: Float = 0f
)

