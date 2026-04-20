package com.fpf.smartscan.data.tags

data class TagWithCount(
    val id: Long,
    val name: String,
    val lastUsedAt: Long?,
    val count: Int
)