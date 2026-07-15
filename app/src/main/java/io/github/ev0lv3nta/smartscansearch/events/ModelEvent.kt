package io.github.ev0lv3nta.smartscansearch.events

enum class ModelEventType {
    IMPORT,
}

data class ModelEvent (
    val type: ModelEventType,
    val success: Boolean,
    val message: String? = null
)