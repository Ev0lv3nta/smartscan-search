package com.fpf.smartscan.data

import android.net.Uri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.data.images.tags.ImageTagCrossRefRepository
import com.fpf.smartscan.data.videos.tags.VideoTagCrossRefRepository
import kotlinx.coroutines.flow.first


class MediaPagingSource(
    private val mediaType: MediaType,
    private val tagId: Long,
    private val imageRepo: ImageTagCrossRefRepository,
    private val videoRepo: VideoTagCrossRefRepository,
    private val mediaIdToUri: (Long, MediaType) -> Uri
) : PagingSource<Int, Uri>() {

    private lateinit var ids: List<Long>

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Uri> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        try {
            loadIdsIfNeeded()

            val fromIndex = page * pageSize
            val toIndex = (fromIndex + pageSize).coerceAtMost(ids.size)

            if (fromIndex >= ids.size) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }

            val uris = ids.subList(fromIndex, toIndex).map { id -> mediaIdToUri(id, mediaType) }

            return LoadResult.Page(
                data = uris,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (toIndex < ids.size) page + 1 else null
            )

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    private suspend fun loadIdsIfNeeded() {
        if (::ids.isInitialized) return

        ids = when (mediaType) {
            MediaType.IMAGE -> imageRepo.getMediaIdsFlow(tagId).first()
            MediaType.VIDEO -> videoRepo.getMediaIdsFlow(tagId).first()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Uri>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }
}