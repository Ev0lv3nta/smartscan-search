package com.fpf.smartscan.events

enum class MediaEventType {
    MOVE,
    REMOVE,
    COPY,
    SHARE,
    TAG,
}
data class MediaEvent (
    val type: MediaEventType,
    val success: Boolean,
    val message: String? = null,
    )