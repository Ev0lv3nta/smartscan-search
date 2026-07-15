package io.github.ev0lv3nta.smartscansearch.events

enum class CollectionEventType {
    MERGE,
    DELETE,
    RENAME,
    COPY
}
data class CollectionEvent (
    val type: CollectionEventType,
    val success: Boolean,
    val message: String? = null,
)