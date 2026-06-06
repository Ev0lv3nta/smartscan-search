package com.fpf.smartscan.events

enum class ModelEventType {
    IMPORT,
}

data class ModelEvent (
    val type: ModelEventType,
    val success: Boolean,
    val message: String? = null
)