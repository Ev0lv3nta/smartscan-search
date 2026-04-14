package com.fpf.smartscan.search

import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.updatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.getSimilarities

//TODO: update to use clusters
class AutoTagger(private val store: FileEmbeddingStore) {
    private suspend fun generateTagPrototype(id: Long, sampleEmbeddings: List<FloatArray>){
        val prototype = generatePrototypeEmbedding(sampleEmbeddings)
        store.add(listOf(StoredEmbedding(id = id, embedding = prototype, date = System.currentTimeMillis())))
    }
    suspend fun updateTagPrototype(tag: Tag, newEmbeddings: List<FloatArray>): Int{
        if(!store.exists){
//            generateTagPrototype(tag.prototypeId, newEmbeddings )
            return newEmbeddings.size
        }

//        val result = store.get(listOf(tag.prototypeId))

//        if(result.isEmpty()){
//            generateTagPrototype(tag.prototypeId, newEmbeddings )
//            return newEmbeddings.size
//        }
//        val prototype = result[0].embedding
//        val (updatedPrototype, newN) = updatePrototypeEmbedding(prototype, newEmbeddings, tag.nPrototype)
//        store.add(listOf(StoredEmbedding(id = tag.prototypeId, date = System.currentTimeMillis(), embedding = updatedPrototype)))
        return 0
    }

    suspend fun calculateCohesionScore(tag: Tag, sampleEmbeddings: List<FloatArray>): Float?{
//        if(!store.exists) return null
//        val results = store.get(listOf((tag.prototypeId)))
//        if(results.isEmpty()) null
//
//        val tagPrototype = results[0]
//        val sims = getSimilarities(tagPrototype.embedding, sampleEmbeddings)
        return 0f
    }

    suspend fun getSuggestedTags(tags: List<Tag>, embedding: FloatArray): TagSuggestionsResult {
        if(!store.exists) return TagSuggestionsResult()

        var suggestedTag: Tag? = null
        var bestSim = 0f
        var secondBestSim = 0f

        var lastUsedTag: Tag? = null
        var lastUsed = 0L

//        for(tag in tags) {
//            val results = store.get(listOf(tag.prototypeId))
//            if(results.isEmpty()) continue
//
//            val tagPrototype = results[0]
//            val sim = tagPrototype.embedding dot embedding
//            if(tag.cohesionScore != null && sim >= tag.cohesionScore!! && sim > bestSim){
//                suggestedTag = tag
//                secondBestSim = bestSim
//                bestSim = sim
//            }
//            if(tag.lastUsedAt != null && tag.lastUsedAt!! > lastUsed){
//                lastUsedTag = tag
//                lastUsed = tag.lastUsedAt!!
//            }
//        }

        val confidence = bestSim - secondBestSim
        return TagSuggestionsResult(suggestedTag, lastUsedTag, confidence)
    }
}
