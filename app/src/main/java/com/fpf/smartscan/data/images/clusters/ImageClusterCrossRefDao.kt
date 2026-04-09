package com.fpf.smartscan.data.images.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageClusterCrossRefDao {
    @Query("SELECT clusterId FROM image_cluster_crossref")
    suspend fun getAllClusters(): List<Long>

    @Query("SELECT imageId FROM image_cluster_crossref")
    suspend fun getAllImages(): List<Long>

    @Query("SELECT imageId FROM image_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun getImagesInCluster(clusterId: Long): List<Long>

    @Query("SELECT * FROM image_cluster_crossref")
    suspend fun getAllClusterImageCrossRefs(): List<ImageClusterCrossRef>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun addImages(imageClusterCrossRefs: List<ImageClusterCrossRef>)

    @Transaction
    @Query("DELETE FROM image_cluster_crossref WHERE imageId IN (:ids)")
    suspend fun deleteByImageIds(ids: List<Long>)

    @Transaction
    @Query("DELETE FROM image_cluster_crossref WHERE clusterId IN (:ids)")
    suspend fun deleteByClusterIds(ids: List<Long>)

    @Query("DELETE FROM image_cluster_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM image_cluster_crossref WHERE clusterId = :clusterId ")
    suspend fun count(clusterId: Long): Int

}