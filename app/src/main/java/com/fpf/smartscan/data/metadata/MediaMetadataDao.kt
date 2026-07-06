package com.fpf.smartscan.data.metadata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fpf.smartscan.media.MediaType

@Dao
interface MediaMetadataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(items: List<MediaMetadata>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaMetadata)

    @Update
    suspend fun update(items: List<MediaMetadata>)

    @Update
    suspend fun update(item: MediaMetadata)

    @Query("SELECT id FROM media_metadata")
    suspend fun getAllIds(): List<Long>
    @Query("SELECT * FROM media_metadata WHERE id IN (:mediaIds)")
    suspend fun getByIds(mediaIds: List<Long>): List<MediaMetadata>

    @Query("SELECT * FROM media_metadata WHERE type = :type")
    suspend fun getByType(type: MediaType): List<MediaMetadata>

    @Query("SELECT id FROM media_metadata WHERE id NOT IN (SELECT mediaId FROM media_cluster_crossref)")
    suspend fun getUnclusteredItemIds(): List<Long>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c ON c.mediaId = m.id
    WHERE c.tagId = :tagId
    ORDER BY m.dateAdded DESC, m.id DESC
""")
    suspend fun getByTag(tagId: Long): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTag(tagId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata>


    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
    """)
    suspend fun getByTag(tagId: Long, type: MediaType ): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c ON c.mediaId = m.id
    WHERE c.tagId = :tagId
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByTag(tagId: Long, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c ON c.mediaId = m.id
    WHERE c.tagId = :tagId
      AND m.type = :type
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c ON c.mediaId = m.id
    WHERE c.tagId = :tagId
      AND m.type = :type
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
    ORDER BY m.dateAdded DESC, m.id DESC
""")
    suspend fun getByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?): List<MediaMetadata>

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
    """)
    suspend fun countByTag(tagId: Long): Int

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.type = :type
    """)

    suspend fun countByTag(tagId: Long, type: MediaType): Int
    @Query("""
    SELECT COUNT(*)
    FROM media_metadata m
    INNER JOIN tag_crossref c ON c.mediaId = m.id
    WHERE c.tagId = :tagId
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
""")

    suspend fun countByTag(tagId: Long, startDate: Long?, endDate: Long?): Int

    @Query("""
    SELECT COUNT(*)
    FROM media_metadata m
    INNER JOIN tag_crossref c ON c.mediaId = m.id
    WHERE c.tagId = :tagId
      AND m.type = :type
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
""")
    suspend fun countByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?): Int

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByCluster(clusterId: Long, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
    WHERE c.clusterId = :clusterId
    ORDER BY m.dateAdded DESC, m.id DESC
""")
    suspend fun getByCluster(clusterId: Long): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByCluster(clusterId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
    WHERE c.clusterId = :clusterId
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByCluster(clusterId: Long, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
    WHERE c.clusterId = :clusterId
      AND m.type = :type
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByCluster(clusterId: Long, type: MediaType, startDate: Long?, endDate: Long?, limit: Int, offset: Int): List<MediaMetadata>

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
    """)
    suspend fun countByCluster(clusterId: Long): Int

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.type = :type
    """)
    suspend fun countByCluster(clusterId: Long, type: MediaType): Int

    @Query("""
    SELECT COUNT(*)
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
    WHERE c.clusterId = :clusterId
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
""")
    suspend fun countByCluster(clusterId: Long, startDate: Long?, endDate: Long?): Int


    @Query("""
    SELECT COUNT(*)
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
    WHERE c.clusterId = :clusterId
      AND m.type = :type
      AND (:startDate IS NULL OR m.dateAdded >= :startDate)
      AND (:endDate IS NULL OR m.dateAdded <= :endDate)
""")
    suspend fun countByCluster(clusterId: Long, type: MediaType, startDate: Long?, endDate: Long?): Int

    @Query("""
    DELETE FROM media_metadata
    WHERE id IN (
        SELECT mediaId
        FROM tag_crossref
        WHERE tagId = :tagId
    )
""")
    suspend fun deleteByTag(tagId: Long)

    @Query("""
    DELETE FROM media_metadata
    WHERE id IN (
        SELECT mediaId
        FROM media_cluster_crossref
        WHERE clusterId = :clusterId
    )
""")
    suspend fun deleteByCluster(clusterId: Long)

    @Query("""
    DELETE FROM media_metadata
    WHERE id IN (:mediaIds)
""")
    suspend fun deleteByIds(mediaIds: List<Long>)

    @Query("DELETE FROM media_metadata")
    suspend fun clear()
}