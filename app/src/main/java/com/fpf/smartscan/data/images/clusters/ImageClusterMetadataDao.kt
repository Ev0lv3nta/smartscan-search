package com.fpf.smartscan.data.images.clusters

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

interface ImageClusterMetadataDao {
    @Query("SELECT * FROM image_cluster_metadata ORDER BY createdAt")
    fun getAllFlow(): Flow<List<ImageClusterMetadata>>

    @Query("SELECT * FROM image_cluster_metadata ORDER BY createdAt")
    suspend fun getAll(): List<ImageClusterMetadata>

    @Query("SELECT * FROM image_cluster_metadata WHERE clusterId = :id")
    suspend fun get(id: Long): ImageClusterMetadata?

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(metadata: ImageClusterMetadata)

    @Update
    suspend fun update(metadata: ImageClusterMetadata)

    @Delete
    suspend fun delete(metadata: ImageClusterMetadata)
}