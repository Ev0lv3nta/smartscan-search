package io.github.ev0lv3nta.smartscansearch.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.ev0lv3nta.smartscansearch.media.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterMetadataDao {
    @Query("SELECT * FROM cluster_metadata")
    fun getAllFlow(): Flow<List<MediaClusterMetadata>>

    @Query("SELECT * FROM cluster_metadata")
    suspend fun getAll(): List<MediaClusterMetadata>

    @Query("SELECT * FROM cluster_metadata WHERE clusterId IN (:ids)")
    suspend fun get(ids: List<Long>): List<MediaClusterMetadata>
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(metadatas: List<MediaClusterMetadata>): List<Long>

    @Update
    suspend fun update(metadatas: List<MediaClusterMetadata>)

    @Transaction
    @Query("DELETE FROM cluster_metadata WHERE clusterId IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM cluster_metadata WHERE prototypeSize >= :minSize")
    suspend fun count(minSize: Int = 1): Int

    @Query("DELETE FROM cluster_metadata")
    suspend fun clear()

}

