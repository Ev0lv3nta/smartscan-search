package com.fpf.smartscan.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.media.MediaType

@Dao
interface ClusterCrossRefDao {
    @Query("SELECT * FROM media_cluster_crossref")
    suspend fun getAll(): List<ClusterCrossRef>

    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun getByClusterId(clusterId: Long): List<ClusterCrossRef>

    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId = :clusterId LIMIT :limit OFFSET :offset")
    suspend fun getByClusterId(clusterId: Long, limit: Int, offset: Int): List<ClusterCrossRef>


    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId = :clusterId AND type =:type")
    suspend fun getByClusterIdAndType(clusterId: Long, type: MediaType): List<ClusterCrossRef>

    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId = :clusterId  AND type =:type LIMIT :limit OFFSET :offset")
    suspend fun getByClusterIdAndType(clusterId: Long, type: MediaType, limit: Int, offset: Int): List<ClusterCrossRef>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(crossRefs: List<ClusterCrossRef>)

    @Transaction
    @Query("DELETE FROM media_cluster_crossref WHERE clusterId IN (:ids)")
    suspend fun deleteByClusterIds(ids: List<Long>)

    @Transaction
    @Query("DELETE FROM media_cluster_crossref WHERE mediaId IN (:ids)")
    suspend fun deleteByMediaIds(ids: List<Long>)

    @Query("DELETE FROM media_cluster_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM media_cluster_crossref WHERE clusterId = :clusterId ")
    suspend fun count(clusterId: Long): Int

}