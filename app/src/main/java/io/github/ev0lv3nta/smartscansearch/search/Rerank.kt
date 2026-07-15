package io.github.ev0lv3nta.smartscansearch.search

import android.util.Log
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

fun rerankItems(
    itemToSimMap: Map<Long, Float>,
    clusterToSimMap: Map<Long, Float>,
    clusterToMediaIdsMap: Map<Long, Set<Long>>,
    strictness: Float = 0f,
    baseCutOffPercent: Float = 0.6f,
    maxCutOffPercent: Float = 0.85f
): List<Long> {

    val itemToCluster = buildMap {
        clusterToMediaIdsMap.forEach { (clusterId, items) ->
            items.forEach { itemId -> put(itemId, clusterId) }
        }
    }

    val clusterSims = itemToSimMap.keys.mapNotNull { itemId ->
            itemToCluster[itemId]?.let(clusterToSimMap::get)?.toDouble()
        }.sorted()

    val itemSims = itemToSimMap.values.map { it.toDouble() }.sorted()
    val iP10 = itemSims.getOrNull((itemSims.size * 0.1).toInt()) ?: 0.0
    val iP90 = itemSims.getOrNull((itemSims.size * 0.9).toInt()) ?: 0.0
    val itemSpread = iP90 - iP10

    val cP10 = clusterSims.getOrNull((clusterSims.size * 0.1).toInt()) ?: 0.0
    val cP90 = clusterSims.getOrNull((clusterSims.size * 0.9).toInt()) ?: 0.0
    val clusterSpread = cP90 - cP10

    val eps = 1e-3
    val ratio = clusterSpread / itemSpread.coerceAtLeast(eps)
    val k = ratio.pow(3).coerceAtLeast(1.0)

    val scoredItems = itemToSimMap.keys.map { itemId ->
        val itemScore = itemToSimMap[itemId]?.toDouble() ?: 0.0
        val clusterScore = itemToCluster[itemId]?.let(clusterToSimMap::get)?.toDouble() ?: 0.0
        itemId to (itemScore * (1 + k * clusterScore))
    }.sortedByDescending { it.second }

    // Early return if only a single or no result
    if (scoredItems.size <= 1) return scoredItems.map { it.first }

    val scores = scoredItems.map { it.second }.sorted()
    val maxScore = scores.max()
    val minScore = scores.min()
    val medianScore = scores[scores.size / 2]
    val meanScore = scores.average()
    val stdScore = sqrt(scores.map{(it - meanScore).pow(2)}.average())
    val centrality = medianScore - minScore
    val safeStrictness = strictness.coerceIn(0f, 1f)
    val strictnessDamping = (maxScore - medianScore) / (maxScore).coerceAtLeast(eps)
    val cutOffPercent = (baseCutOffPercent * (1f + strictnessDamping.toFloat() * safeStrictness)).coerceIn(baseCutOffPercent, maxCutOffPercent)
    val baseCutOff = cutOffPercent * maxScore
    val dynamicCutOff = medianScore - stdScore - (1 - safeStrictness) * centrality.pow(1.0 + safeStrictness)
    val minAllowedScore = max(baseCutOff, dynamicCutOff)
//    Log.d("rerankItems", "strictnessDamping:$strictnessDamping\ncutoff:$cutOffPercent")
    return scoredItems.filter { it.second >= minAllowedScore }.map { it.first }
}
