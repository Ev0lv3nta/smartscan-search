package com.fpf.smartscan.data.images

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageTagCrossRefDao {
    @Query("SELECT imageId FROM image_tag_crossref WHERE tag = :tag")
    suspend fun getImageIds(tag: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tags: List<ImageTagCrossRef>)

    @Query("DELETE FROM image_tag_crossref WHERE imageId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM image_tag_crossref WHERE tag IN (:tags)")
    suspend fun deleteByTags(tags: List<String>)

    @Query("DELETE FROM image_tag_crossref")
    suspend fun clear()
}
