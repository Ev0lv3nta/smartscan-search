package com.fpf.smartscan.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ClusterCrossRefDao {
    @Query("SELECT clusterId FROM media_cluster_crossref")
    suspend fun getAllClusters(): List<Long>

    @Query("SELECT mediaId FROM media_cluster_crossref")
    suspend fun getAllMedia(): List<Long>

    @Query("SELECT mediaId FROM media_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun getMediaInCluster(clusterId: Long): List<Long>

    @Query("SELECT * FROM media_cluster_crossref")
    suspend fun getAllCrossRefs(): List<ClusterCrossRef>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun addMedia(crossRefs: List<ClusterCrossRef>)

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