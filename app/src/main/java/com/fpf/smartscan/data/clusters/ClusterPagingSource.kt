package com.fpf.smartscan.data.clusters

import android.net.Uri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fpf.smartscan.media.MediaType

class ClusterPagingSource(
    private val mediaType: MediaType? = null,
    private val clusterId: Long,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val mediaIdToUri: (Long, MediaType) -> Uri
) : PagingSource<Int, Uri>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Uri> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        // over-fetch by 1 item to detect end of data without using count()
        return try {
            val crossRefs = if(mediaType != null){
                clusterCrossRefRepository.getByClusterIdAndType(clusterId = clusterId, mediaType, limit = pageSize + 1, offset = offset)
            }else{
                clusterCrossRefRepository.getByClusterId(clusterId = clusterId, limit = pageSize + 1, offset = offset)
            }
            val hasMore = crossRefs.size > pageSize
            val pageItems = if (hasMore) crossRefs.dropLast(1) else crossRefs

            val uris = pageItems.map { crossRef ->
                mediaIdToUri(crossRef.mediaId, crossRef.type)
            }

            LoadResult.Page(
                data = uris,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (hasMore) page + 1 else null
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