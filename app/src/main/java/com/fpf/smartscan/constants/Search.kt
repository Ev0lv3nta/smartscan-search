package com.fpf.smartscan.constants

import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.QueryType

val mediaTypeOptions = mapOf(
    MediaType.IMAGE to "Images",
    MediaType.VIDEO to "Videos",
)

val queryOptions = mapOf(
    QueryType.TEXT to "Text",
    QueryType.IMAGE to "Image"
)

object EmbeddingStoresFiles{
    const val IMAGE: String = "image_index.bin"
    const val VIDEO: String = "video_index.bin"
    const val TAGS: String = "tags_store.bin"
}