package com.fpf.smartscan.data.videos.clusters

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoClusterMetadataDao {
    @Query("SELECT * FROM video_cluster_metadata")
    fun getAllFlow(): Flow<List<VideoClusterMetadata>>

    @Query("SELECT * FROM video_cluster_metadata")
    suspend fun getAll(): List<VideoClusterMetadata>

    @Query("SELECT * FROM video_cluster_metadata WHERE clusterId = :id")
    suspend fun get(id: Long): VideoClusterMetadata?

    @Query("SELECT clusterId FROM video_cluster_metadata WHERE label = :label")
    suspend fun getIdFromLabel(label: String): Long?

    @Query("SELECT label FROM video_cluster_metadata WHERE label IS NOT NULL")
    fun getLabels(): Flow<List<String>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(metadatas: List<VideoClusterMetadata>)

    @Query("DELETE FROM video_cluster_metadata WHERE clusterId = :id")
    suspend fun delete(id: Long)
}