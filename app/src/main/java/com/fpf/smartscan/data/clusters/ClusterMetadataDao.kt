package com.fpf.smartscan.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterMetadataDao {
    @Query("SELECT * FROM cluster_metadata")
    fun getAllFlow(): Flow<List<MediaClusterMetadata>>


    @Query("SELECT * FROM cluster_metadata WHERE type = :type AND (prototypeSize >= :minSize) ORDER BY prototypeSize DESC")
    fun getByTypeFlow(type: MediaType, minSize: Int = 1): Flow<List<MediaClusterMetadata>>

    @Query("SELECT * FROM cluster_metadata")
    suspend fun getAll(): List<MediaClusterMetadata>

    @Query("SELECT * FROM cluster_metadata WHERE clusterId IN (:ids)")
    suspend fun get(ids: List<Long>): List<MediaClusterMetadata>

    @Query("SELECT * FROM cluster_metadata WHERE type = :type")
    suspend fun getByType(type: MediaType): List<MediaClusterMetadata>

    @Query("SELECT clusterId FROM cluster_metadata WHERE label = :label")
    suspend fun getIdFromLabel(label: String): Long?

    @Query("SELECT label FROM cluster_metadata WHERE label IS NOT NULL")
    fun getLabels(): Flow<List<String>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(metadatas: List<MediaClusterMetadata>): List<Long>

    @Transaction
    @Update
    suspend fun update(metadatas: List<MediaClusterMetadata>)

    @Transaction
    @Query("DELETE FROM cluster_metadata WHERE clusterId IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM cluster_metadata WHERE prototypeSize >= :minSize")
    suspend fun count(minSize: Int = 1): Int}