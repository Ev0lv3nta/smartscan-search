package com.fpf.smartscan.search

import com.fpf.smartscan.data.images.ImageTag
import com.fpf.smartscan.data.videos.VideoTag
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import com.fpf.smartscansdk.core.embeddings.getTopN
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder
import java.nio.ByteBuffer
import java.security.MessageDigest

class AutoTagger(
    private val store: FileEmbeddingStore,
    private val textEmbedder: ClipTextEmbedder,
    ) {

    private suspend fun generateTagPrototype(id: Long, sampleImageEmbeddings: List<Embedding>){
        val prototype = generatePrototypeEmbedding(sampleImageEmbeddings.map{it.embeddings})
        store.add(listOf(Embedding(id = id, embeddings = prototype, date = System.currentTimeMillis())))
    }

    suspend fun updateImageTagPrototype(tag: ImageTag, newItemEmbeddings: List<Embedding>): Int{
        val id = stringToLong(tag.name)
        val result = store.get(listOf(id))
        if(result.isEmpty()){
            generateTagPrototype(id, newItemEmbeddings )
            return newItemEmbeddings.size
        }
        var prototype = result[0].embeddings

        // newPrototype = ((N * currentPrototype) + sum(newEmbedding)) / N + newN
        if(tag.nPrototype > 0) scaleEmbedding(prototype, tag.nPrototype.toFloat())
        prototype =  sumEmbeddings(newItemEmbeddings.map { it.embeddings } + prototype)
        val nPrototypeNew = tag.nPrototype + newItemEmbeddings.size
        scaleEmbedding(prototype, 1f/nPrototypeNew)

        store.add(listOf(Embedding(id = id, date = System.currentTimeMillis(), embeddings = prototype)))
        return nPrototypeNew
    }

    suspend fun updateVideoTagPrototype(tag: VideoTag, newItemEmbeddings: List<Embedding>): Int{
        val id = stringToLong(tag.name)
        val result = store.get(listOf(id))
        if(result.isEmpty()){
            generateTagPrototype(id, newItemEmbeddings )
            return newItemEmbeddings.size
        }
        var prototype = result[0].embeddings

        // newPrototype = ((N * currentPrototype) + sum(newEmbedding)) / N + newN
        if(tag.nPrototype > 0) scaleEmbedding(prototype, tag.nPrototype.toFloat())
        val nPrototypeNew = tag.nPrototype + newItemEmbeddings.size
        prototype =  sumEmbeddings(newItemEmbeddings.map { it.embeddings } + prototype)
        scaleEmbedding(prototype, 1f/nPrototypeNew)

        store.add(listOf(Embedding(id = id, date = System.currentTimeMillis(), embeddings = prototype)))
        return nPrototypeNew
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
