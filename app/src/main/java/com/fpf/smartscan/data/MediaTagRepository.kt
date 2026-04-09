package com.fpf.smartscan.data

import kotlinx.coroutines.flow.Flow

interface MediaTagRepository {

    val allTags: Flow<List<MediaTag>>

    suspend fun getAllTags(): List<MediaTag>

    suspend fun getTag(name: String): MediaTag?

    suspend fun insertTag(mediaTag: MediaTag)

    suspend fun upsertTag(mediaTag: MediaTag)

    suspend fun deleteTag(mediaTag: MediaTag)
}