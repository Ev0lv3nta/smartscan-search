package com.fpf.smartscan.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterCrossRefDao {

    @Query("SELECT * FROM media_cluster_crossref")
    suspend fun getAll(): List<ClusterCrossRef>

    @Query("""
        SELECT metadata.*, COUNT(crossRef.mediaId) AS count
        FROM media_cluster_crossref crossRef
        JOIN cluster_metadata metadata ON metadata.clusterId = crossRef.clusterId
        GROUP BY crossRef.clusterId
        ORDER BY count DESC
    """)
    fun getClustersWithCount(): Flow<List<ClusterMetadataWithCount>>

    @Query("""
    SELECT crossRef.*
    FROM media_cluster_crossref crossRef
    JOIN media_metadata metadata ON metadata.id = crossRef.mediaId
    WHERE metadata.type = :type
    """)
    suspend fun getByType(type: MediaType): List<ClusterCrossRef>

    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId in (:ids)")
    suspend fun getByClusterIds(ids: List<Long>): List<ClusterCrossRef>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRefs: List<ClusterCrossRef>)

    @Query("DELETE FROM media_cluster_crossref WHERE clusterId IN (:clusterIds)")
    suspend fun deleteByClusterIds(clusterIds: List<Long>)

    @Query("DELETE FROM media_cluster_crossref WHERE mediaId IN (:mediaIds)")
    suspend fun deleteByMediaIds(mediaIds: List<Long>)

    @Query("DELETE FROM media_cluster_crossref")
    suspend fun clear()
}