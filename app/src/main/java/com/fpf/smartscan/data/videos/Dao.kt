package com.fpf.smartscan.data.videos

import androidx.room.*

@Dao
interface VideoMetadataDao {
    @Query("SELECT * FROM video_metadata")
    suspend fun getAll(): List<VideoMetadata>

    @Query("SELECT * FROM video_metadata WHERE id IN (:ids)")
    suspend fun get(ids: List<Long>): List<VideoMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: VideoMetadata)

    @Query("DELETE FROM video_metadata WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM video_metadata")
    suspend fun deleteAll()
}
