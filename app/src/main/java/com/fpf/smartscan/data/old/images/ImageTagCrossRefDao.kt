package com.fpf.smartscan.data.old.images

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ImageTagCrossRefDao {
    @Query("SELECT * FROM image_tag_crossref")
    suspend fun getAll(): List<ImageTagCrossRef>
}
