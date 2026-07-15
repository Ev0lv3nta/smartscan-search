package io.github.ev0lv3nta.smartscansearch.utils

fun <T> reservoirSample(list: List<T>, n: Int): List<T> {
    if (n <= 0) return emptyList()
    if (n >= list.size) return list.toList()

    val result = list.take(n).toMutableList()

    for (i in n until list.size) {
        val j = kotlin.random.Random.nextInt(i + 1)
        if (j < n) result[j] = list[i]
    }

    return result
}