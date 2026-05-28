package com.fpf.smartscan.media

import android.net.Uri

data class MediaCollection (
    val id: Long,
    val name: String,
    val thumbNail: Uri,
    val size: Int,
    val isAutoCollection: Boolean = false
){
    companion object {
        const val UNLABELLED_COLLECTION = "?"
    }
}