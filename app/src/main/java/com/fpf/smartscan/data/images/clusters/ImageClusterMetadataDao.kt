package com.fpf.smartscan.data.images.clusters

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageClusterMetadataDao {
    @Query("SELECT * FROM image_cluster_metadata")
    fun getAllFlow(): Flow<List<ImageClusterMetadata>>

    @Query("SELECT * FROM image_cluster_metadata")
    suspend fun getAll(): List<ImageClusterMetadata>

    @Query("SELECT * FROM image_cluster_metadata WHERE clusterId = :id")
    suspend fun get(id: Long): ImageClusterMetadata?

    @Query("SELECT clusterId FROM image_cluster_metadata WHERE label = :label")
    suspend fun getIdFromLabel(label: String): Long?

    @Query("SELECT label FROM image_cluster_metadata WHERE label IS NOT NULL")
    fun getLabels(): Flow<List<String>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(metadatas: List<ImageClusterMetadata>)

    @Query("DELETE FROM image_cluster_metadata WHERE clusterId = :id")
    suspend fun delete(id: Long)
}