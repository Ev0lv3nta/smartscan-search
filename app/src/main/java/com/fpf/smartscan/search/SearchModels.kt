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

interface MediaTag{
    val prototypeId: Long
    val name: String
    val createdAt: Long?
    val lastUsedAt: Long?
    val cohesionScore: Float?
    val nPrototype: Int
}

data class TagSuggestionsResult(
    val bestMatch: MediaTag? = null,
    val lastedUsed: MediaTag? = null,
    val confidence: Float = 0f
)

