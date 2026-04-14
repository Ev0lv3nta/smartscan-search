package com.fpf.smartscan.data

import android.net.Uri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.data.tags.TagCrossRefRepository


class MediaPagingSource(
    private val mediaType: MediaType,
    private val tagId: Long,
    private val tagsCrossRefRepository: TagCrossRefRepository,
    private val mediaIdToUri: (Long, MediaType) -> Uri
) : PagingSource<Int, Uri>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Uri> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val ids = tagsCrossRefRepository.getMediaIds(tagId, pageSize, offset)
            val uris = ids.map { id -> mediaIdToUri(id, mediaType) }

            LoadResult.Page(
                data = uris,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (ids.size < pageSize) null else page + 1
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Uri>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }
}