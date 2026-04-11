package com.fpf.smartscan.data

import kotlinx.coroutines.flow.Flow

interface MediaTagRepository<T: MediaTag> {

    val allTags: Flow<List<T>>

    suspend fun getAllTags(): List<T>

    suspend fun getTag(name: String): T?

    suspend fun insertTags(mediaTags: List<T>): List<Long>

    suspend fun updateTags(mediaTags: List<T>)

    suspend fun deleteTag(mediaTag: T)

    suspend fun deleteTagByName(name: String)

    suspend fun clear()

}