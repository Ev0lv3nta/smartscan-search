package com.fpf.smartscan.data.videos.clusters

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoClusterMetadataDao {
    @Query("SELECT * FROM video_cluster_metadata ORDER BY createdAt")
    fun getAllFlow(): Flow<List<VideoClusterMetadata>>

    @Query("SELECT * FROM video_cluster_metadata ORDER BY createdAt")
    suspend fun getAll(): List<VideoClusterMetadata>

    @Query("SELECT * FROM video_cluster_metadata WHERE clusterId = :id")
    suspend fun get(id: Long): VideoClusterMetadata?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(metadatas: List<VideoClusterMetadata>)

    @Query("DELETE FROM video_cluster_metadata WHERE clusterId = :id")
    suspend fun delete(id: Long)
}