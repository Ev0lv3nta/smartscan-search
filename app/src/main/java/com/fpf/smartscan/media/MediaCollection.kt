package com.fpf.smartscan.media

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaCollection (
    val id: Long,
    val name: String,
    val thumbNail: Uri,
    val size: Int,
    val isAutoCollection: Boolean = false
): Parcelable{
    companion object {
        const val UNLABELLED_COLLECTION = "?"
    }
}