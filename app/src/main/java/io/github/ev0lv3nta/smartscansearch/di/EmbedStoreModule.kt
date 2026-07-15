package io.github.ev0lv3nta.smartscansearch.di

import android.app.Application
import io.github.ev0lv3nta.smartscansearch.constants.EmbeddingStoresFilesQuant
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

private const val EMBEDDING_DIM = 512

val IMAGE_STORE = named("image_store")
val VIDEO_STORE = named("video_store")
val CLUSTER_STORE = named("cluster_store")

val embedStoreModule = module {
    single(IMAGE_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.IMAGE), EMBEDDING_DIM, quantize = true)
    }

    single(VIDEO_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.VIDEO), EMBEDDING_DIM, quantize = true)
    }
    single(CLUSTER_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.CLUSTER), EMBEDDING_DIM, quantize = true)
    }
}