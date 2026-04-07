package com.fpf.smartscan.data.images.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageClusterCrossRefDao {
    @Query("SELECT clusterId FROM image_cluster_crossref")
    suspend fun getAllClusters(): Set<Long>


    @Query("SELECT imageId FROM image_cluster_crossref")
    suspend fun getAllImages(): Set<Long>

    @Query("SELECT imageId FROM image_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun getImagesInCluster(clusterId: Long): Set<Long>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun addImages(imageClusterCrossRefs: List<ImageClusterCrossRef>)

    @Query("DELETE FROM image_cluster_crossref WHERE imageId IN (:ids)")
    suspend fun deleteByImageIds(ids: List<Long>)

    @Query("DELETE FROM image_cluster_crossref WHERE clusterId IN (:ids)")
    suspend fun deleteByClusterIds(ids: List<Long>)

    @Query("DELETE FROM image_cluster_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM image_cluster_crossref WHERE clusterId = :clusterId ")
    suspend fun count(clusterId: Long): Int

}