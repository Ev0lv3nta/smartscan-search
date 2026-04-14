package com.fpf.smartscan.search

import com.fpf.smartscan.data.tags.Tag

enum class QueryType {
    TEXT, IMAGE
}

enum class IndexingStatus {IDLE, ACTIVE, COMPLETE, FAILED }

data class TagSuggestionsResult(
    val bestMatch: Tag? = null,
    val lastedUsed: Tag? = null,
    val confidence: Float = 0f
)

