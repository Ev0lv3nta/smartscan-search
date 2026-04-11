package com.fpf.smartscan.data

import com.fpf.smartscan.model.MediaTag

data class TagWithCount(
    val id: Long,
    val name: String,
    val lastUsedAt: Long?,
    val count: Int
){
    fun toMediaCount() = MediaTag(id, name, lastUsedAt)
}