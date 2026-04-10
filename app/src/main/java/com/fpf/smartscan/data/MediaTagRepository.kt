package com.fpf.smartscan.data

import com.fpf.smartscan.collections.MediaTag
import kotlinx.coroutines.flow.Flow

interface MediaTagRepository {

    val allTags: Flow<List<MediaTag>>

    suspend fun getAllTags(): List<MediaTag>

    suspend fun getTag(name: String): MediaTag?

    suspend fun insertTags(mediaTags: List<MediaTag>)

    suspend fun updateTags(mediaTags: List<MediaTag>)

    suspend fun deleteTag(mediaTag: MediaTag)

    suspend fun clear()

}