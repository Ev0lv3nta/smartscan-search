package com.fpf.smartscan.di

import android.app.Application
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

private const val EMBEDDING_DIM = 512

val IMAGE_STORE = named("image_store")
val VIDEO_STORE = named("video_store")

val IMAGE_CLUSTER_STORE = named("image_cluster_store")
val VIDEO_CLUSTER_STORE = named("video_cluster_store")

val embedStoreModule = module {

    single(IMAGE_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(
            File(app.filesDir, EmbeddingStoresFiles.IMAGE),
            EMBEDDING_DIM
        )
    }

    single(VIDEO_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(
            File(app.filesDir, EmbeddingStoresFiles.VIDEO),
            EMBEDDING_DIM
        )
    }
    single(IMAGE_CLUSTER_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(
            File(app.filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER),
            EMBEDDING_DIM
        )
    }
    single(VIDEO_CLUSTER_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(
            File(app.filesDir, EmbeddingStoresFiles.VIDEO_CLUSTER),
            EMBEDDING_DIM
        )
    }
}