package com.fpf.smartscan.data.images

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageTagCrossRefDao {
    @Query("SELECT DISTINCT tag FROM image_tag_crossref WHERE imageId = :imageId")
    suspend fun getTagsForImage(imageId: Long): List<String>
    @Query("SELECT imageId FROM image_tag_crossref WHERE tag = :tag")
    suspend fun getImageIds(tag: String): List<Long>

    @Query("SELECT * FROM image_tag_crossref")
    suspend fun getAllCrossRefs(): List<ImageTagCrossRef>

    @Query("SELECT imageId FROM image_tag_crossref WHERE tag = :tag LIMIT :limit OFFSET :offset")
    suspend fun getImageIds(tag: String, limit: Int, offset: Int): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tags: List<ImageTagCrossRef>)

    @Query("DELETE FROM image_tag_crossref WHERE imageId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM image_tag_crossref WHERE tag IN (:tags)")
    suspend fun deleteByTags(tags: List<String>)

    @Query("DELETE FROM image_tag_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM image_tag_crossref WHERE tag = :tag ")
    suspend fun count(tag: String): Int
}
