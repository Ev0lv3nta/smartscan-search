package com.fpf.smartscan.search

import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import com.fpf.smartscansdk.core.embeddings.getTopN
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder


class AutoTagger(
    private val store: FileEmbeddingStore,
    private val textEmbedder: ClipTextEmbedder,
    ) {

    private suspend fun generateTagPrototype(id: Long, sampleImageEmbeddings: List<Embedding>){
        val prototype = generatePrototypeEmbedding(sampleImageEmbeddings.map{it.embeddings})
        store.add(listOf(Embedding(id = id, embeddings = prototype, date = System.currentTimeMillis())))
    }
    suspend fun updateTagPrototype(tag: MediaTag, newItemEmbeddings: List<Embedding>): Int{
        if(!store.exists){
            generateTagPrototype(tag.prototypeId, newItemEmbeddings )
            return newItemEmbeddings.size
        }

        val result = store.get(listOf(tag.prototypeId))

        if(result.isEmpty()){
            generateTagPrototype(tag.prototypeId, newItemEmbeddings )
            return newItemEmbeddings.size
        }
        var prototype = result[0].embeddings

        // newPrototype = ((N * currentPrototype) + sum(newEmbedding)) / N + newN
        if(tag.nPrototype > 0) scaleEmbedding(prototype, tag.nPrototype.toFloat())
        val nPrototypeNew = tag.nPrototype + newItemEmbeddings.size
        prototype =  sumEmbeddings(newItemEmbeddings.map { it.embeddings } + prototype)
        scaleEmbedding(prototype, 1f/nPrototypeNew)

        store.add(listOf(Embedding(id = tag.prototypeId, date = System.currentTimeMillis(), embeddings = prototype)))
        return nPrototypeNew
    }

    suspend fun calculateCohesionScore(tag: MediaTag, sampleBatchEmbeddings: List<Embedding>): Float{
        val results = store.get(listOf((tag.prototypeId)))
        if(results.isEmpty()) error("prototype not found")

        val tagPrototype = results[0]
        val sims = getSimilarities(tagPrototype.embeddings, sampleBatchEmbeddings.map{it.embeddings})
        return sims.sum() /sims.size
    }

    suspend fun getSuggestedTags(tags: List<MediaTag>, selectedMediaPrototype: FloatArray): SuggestedTags {
        var suggestedTag: MediaTag? = null
        var bestSim = 0f

        var lastUsedTag: MediaTag? = null
        var lastUsed = 0L

        for(tag in tags) {
            val results = store.get(listOf(tag.prototypeId))
            if(results.isEmpty()) continue

            val tagPrototype = results[0]
            val sim = tagPrototype.embeddings dot selectedMediaPrototype
            if(tag.cohesionScore != null && sim >= tag.cohesionScore!! && sim > bestSim){
                suggestedTag = tag
                bestSim = sim
            }
            if(tag.lastUsedAt != null && tag.lastUsedAt!! > lastUsed){
                lastUsedTag = tag
                lastUsed = tag.lastUsedAt!!
            }
        }

        return SuggestedTags(suggestedTag, lastUsedTag)
    }


    suspend fun orderBySimilarity(tags: List<MediaTag>, selectedMediaPrototype: FloatArray): List<MediaTag>{
        val results = store.get(tags.map{ it.prototypeId})
        if(results.isEmpty() || results.size != tags.size) return tags

        if(!textEmbedder.isInitialized()) textEmbedder.initialize()
        val rawEmbeds = textEmbedder.embedBatch(tags.map { it.name })
        val sims = getSimilarities(selectedMediaPrototype, rawEmbeds)
        val orderedTagEmbedsIndices = getTopN(sims, sims.size)
        return orderedTagEmbedsIndices.map{ tags[it] }
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
