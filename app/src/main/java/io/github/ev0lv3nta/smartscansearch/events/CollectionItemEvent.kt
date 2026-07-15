package io.github.ev0lv3nta.smartscansearch.events

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