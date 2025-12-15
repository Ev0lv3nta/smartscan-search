package com.fpf.smartscan.search

import android.graphics.Bitmap
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import com.fpf.smartscansdk.core.embeddings.getTopN
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder
import java.nio.ByteBuffer
import java.security.MessageDigest

class AutoTagger(
    private val tagStore: FileEmbeddingStore,
    private val textEmbedder: ClipTextEmbedder,
    private val imageEmbedder: ClipImageEmbedder
) {

    suspend fun generateTagPrototype(tag: String, images: List<Bitmap>){
        if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()
        val rawEmbeds = imageEmbedder.embedBatch(images)
        val prototype = generatePrototypeEmbedding(rawEmbeds)
        val id = stringToLong(tag)
        tagStore.add(listOf(Embedding(id = id, embeddings = prototype, date = System.currentTimeMillis())))
    }

    suspend fun calculateClassCohesion(tag: String, sampleBatchEmbeddings: List<Embedding>): Float{
        val results = tagStore.get(listOf(stringToLong((tag))))
        if(results.isEmpty()) error("prototype not found")

        val tagPrototype = results[0]
        val sims = getSimilarities(tagPrototype.embeddings, sampleBatchEmbeddings.map{it.embeddings})
        return sims.sum() /sims.size
    }

    suspend fun getSuggestedTag(tagsCohesionMap: Map<String, Float>, rawEmbedding: FloatArray): String?{
        var suggestedTag: String? = null
        var bestSim = 0f

        for((tag, avgSim) in tagsCohesionMap.entries) {
            val results = tagStore.get(listOf(stringToLong((tag))))
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
        val results = tagStore.get(tagIds)
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
}
