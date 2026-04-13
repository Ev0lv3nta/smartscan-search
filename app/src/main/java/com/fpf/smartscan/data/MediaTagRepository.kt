package com.fpf.smartscan.data

import kotlinx.coroutines.flow.Flow

interface MediaTagRepository<T: MediaTag> {

    val allTags: Flow<List<T>>

    suspend fun getAllTags(): List<T>

    suspend fun getTagsByName(names: List<String>): List<T>

    suspend fun getTagsById(ids: List<Long>): List<T>

    suspend fun insertTags(mediaTags: List<T>): List<Long>

    suspend fun updateTags(mediaTags: List<T>)

    suspend fun deleteTags(mediaTags: List<T>)

    suspend fun deleteTagsByName(names: List<String>)

    suspend fun deleteTagsById(ids: List<Long>)

    suspend fun clear()

}