package com.fpf.smartscan.data.old.images

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ImageTagDao {
    @Query("SELECT * FROM image_tag")
    suspend fun getAll(): List<ImageTag>
}
