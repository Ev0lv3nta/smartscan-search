package com.fpf.smartscan.data.videos.clusters

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

interface VideoClusterMetadataDao {
    @Query("SELECT * FROM video_cluster_metadata ORDER BY createdAt")
    fun getAllFlow(): Flow<List<VideoClusterMetadata>>

    @Query("SELECT * FROM video_cluster_metadata ORDER BY createdAt")
    suspend fun getAll(): List<VideoClusterMetadata>

    @Query("SELECT * FROM video_cluster_metadata WHERE clusterId = :id")
    suspend fun get(id: Long): VideoClusterMetadata?

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(metadata: VideoClusterMetadata)

    @Update
    suspend fun update(metadata: VideoClusterMetadata)

    @Delete
    suspend fun delete(metadata: VideoClusterMetadata)
}