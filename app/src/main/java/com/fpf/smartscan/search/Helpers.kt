package com.fpf.smartscan.search

import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot

suspend fun dedupe(store: FileEmbeddingStore, searchResults: List<Long>, duplicateThreshold: Float): List<Long>{
    val validEmbeds = mutableListOf<FloatArray>()
    val validIds = mutableListOf<Long>()

    val resultEmbeds = store.get(searchResults)

    for (res in resultEmbeds){
        var isDuplicate = false
        for(emb in validEmbeds){
            val sim = res.embedding dot emb
            if (sim >= duplicateThreshold){
                isDuplicate = true
                break
            }
        }
        if (!isDuplicate){
            validIds.add(res.id)
            validEmbeds.add(res.embedding)
        }
    }
    return validIds
}

fun parseQuery(query: String): Pair<String?, String>{
    val regex = Regex("""^#([a-zA-Z0-9_]+)""")
    val match = regex.find(query)
    val tag = match?.groupValues?.get(1)
    val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
    val actualQuery = query.substring(actualQueryStart).trim()
    return Pair(tag, actualQuery)
}

fun getPaginatedResult(currentItemsCount: Int, batchSize: Int, cachedIds:  MutableList<Long>): List<Long>{
    val end = (currentItemsCount + batchSize).coerceAtMost(cachedIds.size)
    if (currentItemsCount >= end) return emptyList()
    return cachedIds.subList(currentItemsCount, end)
}

fun rerankItems(
    queryResults: List<Long>,
    clusterResults: List<Long>,
    clusterToMediaIdsMap: Map<Long, Set<Long>>
): List<Long> {

    val itemRank = queryResults.withIndex().associate { it.value to it.index + 1 }
    val clusterRank = clusterResults.withIndex().associate { it.value to it.index + 1 }

    val itemToCluster = buildMap {
        clusterToMediaIdsMap.forEach { (c, items) ->
            items.forEach { put(it, c) }
        }
    }

    return queryResults.sortedWith(
        compareByDescending<Long> { id ->
            val clusterId = itemToCluster[id]
            val clusterPosition = clusterId?.let { clusterRank[it] }
            val clusterScore = clusterPosition?.let { 1.0 / it } ?: -1.0
            clusterScore
        }.thenByDescending { id ->
            val itemPosition = itemRank[id] ?: queryResults.size
            val itemScore = 1.0 / itemPosition
            itemScore
        }
    )
}





