package com.fpf.smartscan.search

import android.util.Log
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot
import kotlin.math.ln1p
import kotlin.math.pow
import kotlin.math.sqrt

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
    itemToSimMap: Map<Long, Float>,
    clusterToSimMap: Map<Long, Float>,
    clusterToMediaIdsMap: Map<Long, Set<Long>>,
): List<Long> {

    val itemToCluster = buildMap {
        clusterToMediaIdsMap.forEach { (clusterId, items) ->
            items.forEach { itemId -> put(itemId, clusterId) }
        }
    }

    val clusterScores = itemToSimMap.keys
        .mapNotNull { itemId -> itemToCluster[itemId]?.let(clusterToSimMap::get)?.toDouble() }
        .sorted()


    val itemScores = itemToSimMap.values.map { it.toDouble() }.sorted()
    val iP10 = itemScores.getOrNull((itemScores.size * 0.1).toInt()) ?: 0.0
    val iP90 = itemScores.getOrNull((itemScores.size * 0.9).toInt()) ?: 0.0
    val itemSpread = iP90 - iP10

    val p10 = clusterScores.getOrNull((clusterScores.size * 0.1).toInt()) ?: 0.0
    val p90 = clusterScores.getOrNull((clusterScores.size * 0.9).toInt()) ?: 0.0
    val clusterSpread = p90 - p10
    val eps = 1e-3
    val ratio = clusterSpread / itemSpread.coerceAtLeast(eps)
    val k = ratio.pow(3).coerceAtLeast(1.0)

//    Log.d("rerankItems", "k: $k | cluster spread: $clusterSpread | item spread: $itemSpread")

    return itemToSimMap.keys.sortedByDescending { itemId ->
        val itemScore = itemToSimMap[itemId]?.toDouble() ?: 0.0
        val clusterScore = itemToCluster[itemId]?.let(clusterToSimMap::get)?.toDouble() ?: 0.0
        itemScore * (1 + k * clusterScore)
    }
}