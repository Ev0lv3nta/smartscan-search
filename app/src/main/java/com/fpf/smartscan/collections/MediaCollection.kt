package com.fpf.smartscan.collections

import android.net.Uri

data class MediaCollection (
    val name: String,
    val thumbNail: Uri,
    val size: Int,
)