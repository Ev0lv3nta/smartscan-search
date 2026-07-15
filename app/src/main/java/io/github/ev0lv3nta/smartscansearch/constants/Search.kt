package io.github.ev0lv3nta.smartscansearch.constants

import io.github.ev0lv3nta.smartscansearch.media.MediaType

val mediaTypeOptions = mapOf(
    MediaType.IMAGE to "Images",
    MediaType.VIDEO to "Videos",
)

object EmbeddingStoresFiles{
    const val IMAGE: String = "image_index.bin"
    const val VIDEO: String = "video_index.bin"
    const val MEDIA_CLUSTER: String = "media_cluster_index.bin"
    const val IMAGE_CLUSTER: String = "image_cluster_index.bin"
    const val VIDEO_CLUSTER: String = "video_cluster_index.bin"
    const val TAGS: String = "tags_store.bin"
}

object EmbeddingStoresFilesQuant{
    const val IMAGE: String = "image_index_quant.bin"
    const val VIDEO: String = "video_index_quant.bin"
    const val CLUSTER: String = "cluster_index_quant.bin"
}