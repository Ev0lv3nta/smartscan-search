package io.github.ev0lv3nta.smartscansearch.data.tags

data class TagWithCount(
    val id: Long,
    val name: String,
    val lastUsedAt: Long?,
    val count: Int
)