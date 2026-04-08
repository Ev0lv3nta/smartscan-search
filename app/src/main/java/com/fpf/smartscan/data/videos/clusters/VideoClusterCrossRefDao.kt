package com.fpf.smartscan.data.videos.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoClusterCrossRefDao {
    @Query("SELECT clusterId FROM video_cluster_crossref")
    suspend fun getAllClusters(): List<Long>

    @Query("SELECT videoId FROM video_cluster_crossref")
    suspend fun getAllVideos(): List<Long>

    @Query("SELECT videoId FROM video_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun getVideosInCluster(clusterId: Long): List<Long>

    @Query("SELECT * FROM video_cluster_crossref")
    suspend fun getAllClusterVideoCrossRefs(): List<VideoClusterCrossRef>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun addVideos(videosClusterCrossRefs: List<VideoClusterCrossRef>)

    @Query("DELETE FROM video_cluster_crossref WHERE videoId IN (:ids)")
    suspend fun deleteByVideoIds(ids: List<Long>)

    @Query("DELETE FROM video_cluster_crossref WHERE clusterId IN (:ids)")
    suspend fun deleteByClusterIds(ids: List<Long>)

    @Query("DELETE FROM video_cluster_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM video_cluster_crossref WHERE clusterId = :clusterId ")
    suspend fun count(clusterId: Long): Int

}