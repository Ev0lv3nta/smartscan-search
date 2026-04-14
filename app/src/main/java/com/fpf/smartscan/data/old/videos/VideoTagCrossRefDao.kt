package com.fpf.smartscan.data.old.videos

import androidx.room.*

@Dao
interface VideoTagCrossRefDao {
    @Query("SELECT * FROM video_tag_crossref")
    suspend fun getAll(): List<VideoTagCrossRef>
}
