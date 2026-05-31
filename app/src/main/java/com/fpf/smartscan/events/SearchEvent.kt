package com.fpf.smartscan.events


enum class SearchEventType {
    TAG,
}

data class SearchEvent (
    val type: SearchEventType,
    val success: Boolean,
    val message: String? = null,
)