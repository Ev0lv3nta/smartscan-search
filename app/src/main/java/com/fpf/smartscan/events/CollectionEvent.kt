package com.fpf.smartscan.events

enum class CollectionEventType {
    MOVE,
    REMOVE,
    COPY,
    SHARE
}
data class CollectionEvent (
    val type: CollectionEventType,
    val success: Boolean,
    val message: String? = null,
    )