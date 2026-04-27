package com.fpf.smartscan.data.metadata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fpf.smartscan.media.MediaType

@Dao
interface MediaMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<MediaMetadata>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaMetadata)

    @Query("SELECT * FROM media_metadata WHERE id IN (:mediaIds)")
    suspend fun getByIds(mediaIds: List<Long>): List<MediaMetadata>

    @Query("SELECT * FROM media_metadata WHERE type = :type")
    suspend fun getByType(type: MediaType): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTag(
        tagId: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTagAndType(
        tagId: Long,
        type: MediaType,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.dateAdded BETWEEN :startDate AND :endDate
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTagAndDateRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.type = :type
          AND m.dateAdded BETWEEN :startDate AND :endDate
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTagTypeAndDateRange(
        tagId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

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
    suspend fun countByTagAndType(tagId: Long, type: MediaType): Int

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.dateAdded BETWEEN :startDate AND :endDate
    """)
    suspend fun countByTagAndDateRange(
        tagId: Long,
        startDate: Long,
        endDate: Long
    ): Int

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN tag_crossref c ON c.mediaId = m.id
        WHERE c.tagId = :tagId
          AND m.type = :type
          AND m.dateAdded BETWEEN :startDate AND :endDate
    """)
    suspend fun countByTagTypeAndDateRange(
        tagId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long
    ): Int

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByCluster(
        clusterId: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByClusterAndType(
        clusterId: Long,
        type: MediaType,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.dateAdded BETWEEN :startDate AND :endDate
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByClusterAndDateRange(
        clusterId: Long,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.type = :type
          AND m.dateAdded BETWEEN :startDate AND :endDate
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByClusterTypeAndDateRange(
        clusterId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<MediaMetadata>

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
    suspend fun countByClusterAndType(clusterId: Long, type: MediaType): Int

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.dateAdded BETWEEN :startDate AND :endDate
    """)
    suspend fun countByClusterAndDateRange(
        clusterId: Long,
        startDate: Long,
        endDate: Long
    ): Int

    @Query("""
        SELECT COUNT(*)
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c ON c.mediaId = m.id
        WHERE c.clusterId = :clusterId
          AND m.type = :type
          AND m.dateAdded BETWEEN :startDate AND :endDate
    """)
    suspend fun countByClusterTypeAndDateRange(
        clusterId: Long,
        type: MediaType,
        startDate: Long,
        endDate: Long
    ): Int

    @Query("DELETE FROM media_metadata")
    suspend fun clear()
}