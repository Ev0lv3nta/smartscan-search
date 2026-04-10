package com.fpf.smartscan.search

import com.fpf.smartscan.data.MediaTag

enum class QueryType {
    TEXT, IMAGE
}

enum class IndexingStatus {IDLE, ACTIVE, COMPLETE, FAILED }

data class TagSuggestionsResult(
    val bestMatch: MediaTag? = null,
    val lastedUsed: MediaTag? = null,
    val confidence: Float = 0f
)

