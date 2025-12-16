package com.fpf.smartscan.search

import android.content.ContentUris
import android.net.Uri
import com.fpf.smartscan.data.images.ImageTagCrossRef
import com.fpf.smartscan.data.images.ImageTagCrossRefRepository
import com.fpf.smartscan.data.images.ImageTagRepository
import com.fpf.smartscan.data.videos.VideoTagCrossRef
import com.fpf.smartscan.data.videos.VideoTagCrossRefRepository
import com.fpf.smartscan.data.videos.VideoTagRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import com.fpf.smartscansdk.core.embeddings.getTopN
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder
import java.nio.ByteBuffer
import java.security.MessageDigest

class TagManager(
    private val store: FileEmbeddingStore,
    private val textEmbedder: ClipTextEmbedder,
    private val imageTagsRepository: ImageTagRepository,
    private val videoTagsRepository: VideoTagRepository,
    private val imageTagsCrossRefRepository: ImageTagCrossRefRepository,
    private val videoTagsCrossRefRepository: VideoTagCrossRefRepository,
    ) {

    suspend fun addTag(tag: String, selectedResults: List<Uri>, mediaType: MediaType){
        val ids = selectedResults.map { ContentUris.parseId(it) }

        when (mediaType) {
            MediaType.IMAGE -> {
                val tagEntries = ids.map { ImageTagCrossRef(imageId = it, tag = tag.trim()) }
                imageTagsCrossRefRepository.addTags(tagEntries)
            }
            MediaType.VIDEO -> {
                val tagEntries = ids.map { VideoTagCrossRef(videoId = it, tag = tag.trim()) }
                videoTagsCrossRefRepository.addTags(tagEntries)
            }
        }
    }

    suspend fun getMediaIds(mediaType: MediaType, tag: String?): List<Long>{
        return when {
            mediaType == MediaType.IMAGE && !tag.isNullOrBlank() -> {
                imageTagsCrossRefRepository.getImageIds(tag)
            }
            mediaType == MediaType.VIDEO && !tag.isNullOrBlank() -> {
                videoTagsCrossRefRepository.getVideoIds(tag)
            }
            else -> { emptyList()}
        }
    }

    suspend fun generateTagPrototype(tag: String, sampleImageEmbeddings: List<Embedding>){
        val prototype = generatePrototypeEmbedding(sampleImageEmbeddings.map{it.embeddings})
        val id = stringToLong(tag)
        store.add(listOf(Embedding(id = id, embeddings = prototype, date = System.currentTimeMillis())))
    }

    suspend fun updateTagPrototype(tag: String, mediaType: MediaType, newItemEmbeddings: List<Embedding>){
        val id = stringToLong(tag)
        val result = store.get(listOf(id))
        if(result.isEmpty()) return

        var prototype = result[0].embeddings
        val nPrototype: Int = when(mediaType){
            MediaType.VIDEO -> {
                videoTagsRepository.getByName(tag)?.nPrototype
            }
            MediaType.IMAGE -> {
                imageTagsRepository.getByName(tag)?.nPrototype
            }
        }?: error("nPrototype unknown")

        // newPrototype = ((N * currentPrototype) + sum(newEmbedding)) / N + newN
        scaleEmbedding(prototype, nPrototype.toFloat())
        prototype =  sumEmbeddings(newItemEmbeddings.map { it.embeddings } + prototype)
        scaleEmbedding(prototype, 1f/(nPrototype + newItemEmbeddings.size))

        store.add(listOf(Embedding(id = id, date = System.currentTimeMillis(), embeddings = prototype)))
    }

    suspend fun calculateClassCohesion(tag: String, sampleBatchEmbeddings: List<Embedding>): Float{
        val results = store.get(listOf(stringToLong((tag))))
        if(results.isEmpty()) error("prototype not found")

        val tagPrototype = results[0]
        val sims = getSimilarities(tagPrototype.embeddings, sampleBatchEmbeddings.map{it.embeddings})
        return sims.sum() /sims.size
    }

    suspend fun getSuggestedTag(tagsCohesionMap: Map<String, Float>, rawEmbedding: FloatArray): String?{
        var suggestedTag: String? = null
        var bestSim = 0f

        for((tag, avgSim) in tagsCohesionMap.entries) {
            val results = store.get(listOf(stringToLong((tag))))
            if(results.isEmpty()) continue

            val tagPrototype = results[0]
            val sim = tagPrototype.embeddings dot rawEmbedding
            if(sim >= avgSim && sim > bestSim){
                suggestedTag = tag
                bestSim = sim
            }
        }
        return suggestedTag
    }

    suspend fun orderBySimilarity(tags: List<String>, rawEmbedding: FloatArray): List<String>{
        val tagIds = tags.map{stringToLong(it)}
        val results = store.get(tagIds)
        if(results.isEmpty() || results.size != tags.size) return tags

        if(!textEmbedder.isInitialized()) textEmbedder.initialize()
        val rawEmbeds = textEmbedder.embedBatch(tags)
        val sims = getSimilarities(rawEmbedding, rawEmbeds)
        val orderedTagEmbedsIndices = getTopN(sims, sims.size)
        return orderedTagEmbedsIndices.map{ tags[it] }
    }

    private fun stringToLong(str: String): Long{
        val bytes = str.toByteArray()
        val hash =  MessageDigest.getInstance("SHA-256").digest(bytes)
        return ByteBuffer.wrap(hash.sliceArray(0..7)).long
    }

    private fun sumEmbeddings(embeddings: List<FloatArray>): FloatArray {
        val sum = FloatArray(embeddings[0].size)
        for (emb in embeddings) {
            for (i in emb.indices) {
                sum[i] += emb[i]
            }
        }
        return sum
    }

    private fun scaleEmbedding(rawEmbedding: FloatArray, x: Float) {
        for (i in rawEmbedding.indices) {
            rawEmbedding[i] *= x
        }
    }
}
