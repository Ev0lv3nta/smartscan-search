package com.fpf.smartscan.search

import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.updatePrototype
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities
import com.fpf.smartscansdk.core.embeddings.getTopN
import com.fpf.smartscansdk.ml.providers.embeddings.clip.ClipTextEmbedder


class AutoTagger(
    private val store: FileEmbeddingStore,
    private val textEmbedder: ClipTextEmbedder,
    ) {

    private suspend fun generateTagPrototype(id: Long, sampleEmbeddings: List<FloatArray>){
        val prototype = generatePrototypeEmbedding(sampleEmbeddings)
        store.add(listOf(StoredEmbedding(id = id, embedding = prototype, date = System.currentTimeMillis())))
    }
    suspend fun updateTagPrototype(tag: MediaTag, newEmbeddings: List<FloatArray>): Int{
        if(!store.exists){
            generateTagPrototype(tag.prototypeId, newEmbeddings )
            return newEmbeddings.size
        }

        val result = store.get(listOf(tag.prototypeId))

        if(result.isEmpty()){
            generateTagPrototype(tag.prototypeId, newEmbeddings )
            return newEmbeddings.size
        }
        val prototype = result[0].embedding
        val (updatedPrototype, newN) = updatePrototype(prototype, newEmbeddings, tag.nPrototype)
        store.add(listOf(StoredEmbedding(id = tag.prototypeId, date = System.currentTimeMillis(), embedding = updatedPrototype)))
        return newN
    }

    suspend fun calculateCohesionScore(tag: MediaTag, sampleEmbeddings: List<FloatArray>): Float?{
        if(!store.exists) return null
        val results = store.get(listOf((tag.prototypeId)))
        if(results.isEmpty()) null

        val tagPrototype = results[0]
        val sims = getSimilarities(tagPrototype.embedding, sampleEmbeddings)
        return sims.sum() /sims.size
    }

    suspend fun getSuggestedTags(tags: List<MediaTag>, selectedMediaPrototype: FloatArray): SuggestedTags {
        if(!store.exists) return SuggestedTags()

        var suggestedTag: MediaTag? = null
        var bestSim = 0f

        var lastUsedTag: MediaTag? = null
        var lastUsed = 0L

        for(tag in tags) {
            val results = store.get(listOf(tag.prototypeId))
            if(results.isEmpty()) continue

            val tagPrototype = results[0]
            val sim = tagPrototype.embedding dot selectedMediaPrototype
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
        if(!store.exists) return emptyList()

        val results = store.get(tags.map{ it.prototypeId})
        if(results.isEmpty() || results.size != tags.size) return tags

        if(!textEmbedder.isInitialized()) textEmbedder.initialize()
        val rawEmbeds = textEmbedder.embedBatch(tags.map { it.name })
        val sims = getSimilarities(selectedMediaPrototype, rawEmbeds)
        val orderedTagEmbedsIndices = getTopN(sims, sims.size)
        return orderedTagEmbedsIndices.map{ tags[it] }
    }
}
