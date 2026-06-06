package com.fpf.smartscan.events

enum class CollectionItemEventType {
    MOVE,
    REMOVE,
    COPY,
    SHARE,
    TAG,
}
data class CollectionItemEvent (
    val type: CollectionItemEventType,
    val success: Boolean,
    val message: String? = null,
    )