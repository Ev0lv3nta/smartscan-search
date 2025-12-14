package com.fpf.smartscan.data.images

import androidx.room.*

@Dao
interface ImageMetadataDao {
    @Query("SELECT * FROM image_metadata")
    suspend fun getAll(): List<ImageMetadata>

    @Query("SELECT * FROM image_metadata WHERE id IN (:ids)")
    suspend fun get(ids: List<Long>): List<ImageMetadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: ImageMetadata)

    @Query("DELETE FROM image_metadata WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM image_metadata")
    suspend fun deleteAll()
}
