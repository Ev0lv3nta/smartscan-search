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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crossRefs: List<ClusterCrossRef>)

    @Query("DELETE FROM media_cluster_crossref WHERE clusterId IN (:clusterIds)")
    suspend fun deleteByClusterIds(clusterIds: List<Long>)

    @Query("DELETE FROM media_cluster_crossref WHERE mediaId IN (:mediaIds)")
    suspend fun deleteByMediaIds(mediaIds: List<Long>)

    @Query("DELETE FROM media_cluster_crossref")
    suspend fun clear()

    @Query("""
        SELECT m.*, COUNT(c.mediaId) AS count
        FROM media_cluster_crossref c
        JOIN cluster_metadata m ON m.clusterId = c.clusterId
        GROUP BY c.clusterId
        ORDER BY count DESC
    """)
    fun getClustersWithCount(): Flow<List<ClusterMetadataWithCount>>

    @Query("""
    SELECT c.*
    FROM media_cluster_crossref c
    JOIN cluster_metadata m ON m.clusterId = c.clusterId
    WHERE m.type = :type
""")
    suspend fun getByType(type: MediaType): List<ClusterCrossRef>
}