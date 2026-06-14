package com.fpf.smartscan.search

import kotlin.math.pow

fun rerankItems(
    itemToSimMap: Map<Long, Float>,
    clusterToSimMap: Map<Long, Float>,
    clusterToMediaIdsMap: Map<Long, Set<Long>>,
    strictness: Float = 0f
): List<Long> {

    val itemToCluster = buildMap {
        clusterToMediaIdsMap.forEach { (clusterId, items) ->
            items.forEach { itemId -> put(itemId, clusterId) }
        }
    }

    val clusterSims = itemToSimMap.keys
        .mapNotNull { itemId ->
            itemToCluster[itemId]?.let(clusterToSimMap::get)?.toDouble()
        }
        .sorted()

    val itemSims = itemToSimMap.values.map { it.toDouble() }.sorted()
    val iP10 = itemSims.getOrNull((itemSims.size * 0.1).toInt()) ?: 0.0
    val iP90 = itemSims.getOrNull((itemSims.size * 0.9).toInt()) ?: 0.0
    val itemSpread = iP90 - iP10

    val p10 = clusterSims.getOrNull((clusterSims.size * 0.1).toInt()) ?: 0.0
    val p90 = clusterSims.getOrNull((clusterSims.size * 0.9).toInt()) ?: 0.0
    val clusterSpread = p90 - p10

    val eps = 1e-3
    val ratio = clusterSpread / itemSpread.coerceAtLeast(eps)
    val k = ratio.pow(3).coerceAtLeast(1.0)

    val scoredItems = itemToSimMap.keys.map { itemId ->
        val itemScore = itemToSimMap[itemId]?.toDouble() ?: 0.0
        val clusterScore = itemToCluster[itemId]?.let(clusterToSimMap::get)?.toDouble() ?: 0.0
        itemId to (itemScore * (1 + k * clusterScore))
    }.sortedByDescending { it.second }

    if (scoredItems.size <= 1) {
        return scoredItems.map { it.first }
    }

    val scores = scoredItems.map { it.second }.sorted()
    val minScore = scores.min()
    val medianScore = scores[scores.size / 2]
    val minAllowedScore = minScore + strictness.coerceIn(0f, 1f).toDouble() * (medianScore - minScore)

    return scoredItems
        .filter { it.second >= minAllowedScore }
        .map { it.first }
}
