package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.IdxSyncLog;
import com.proptech.realestate.model.entity.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for IDX Sync Log management
 * Phase 7 Implementation
 */
@Repository
public interface IdxSyncLogRepository extends JpaRepository<IdxSyncLog, String> {

    /**
     * Find sync logs by IDX feed ID
     */
    List<IdxSyncLog> findByIdxFeedIdOrderByCreatedAtDesc(String idxFeedId);

    /**
     * Find sync logs by IDX feed ID with pagination
     */
    Page<IdxSyncLog> findByIdxFeedIdOrderByCreatedAtDesc(String idxFeedId, Pageable pageable);

    /**
     * Find sync logs by feed ID
     */
    Page<IdxSyncLog> findByFeedId(String feedId, Pageable pageable);

    /**
     * Find sync logs by feed MLS ID and status
     */
    Page<IdxSyncLog> findByFeedMlsIdAndSyncStatus(String mlsId, SyncStatus status, Pageable pageable);

    /**
     * Find sync logs by status
     */
    List<IdxSyncLog> findBySyncStatusOrderByCreatedAtDesc(SyncStatus syncStatus);

    /**
     * Find sync logs by feed MLS ID
     */
    Page<IdxSyncLog> findByFeedMlsId(String mlsId, Pageable pageable);

    /**
     * Find recent sync logs
     */
    List<IdxSyncLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    /**
     * Find latest sync log for each feed
     */
    @Query("SELECT l FROM IdxSyncLog l WHERE l.createdAt IN " +
           "(SELECT MAX(l2.createdAt) FROM IdxSyncLog l2 GROUP BY l2.idxFeed.id)")
    List<IdxSyncLog> findLatestSyncLogForEachFeed();

    /**
     * Find sync logs by date range
     */
    @Query("SELECT l FROM IdxSyncLog l WHERE l.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY l.createdAt DESC")
    List<IdxSyncLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get sync statistics for a feed
     */
    @Query("SELECT " +
           "COUNT(l) as totalSyncs, " +
           "SUM(CASE WHEN l.syncStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulSyncs, " +
           "SUM(CASE WHEN l.syncStatus = 'FAILED' THEN 1 ELSE 0 END) as failedSyncs, " +
           "AVG(l.syncDurationMs) as avgDurationMs, " +
           "SUM(l.propertiesProcessed) as totalProcessed, " +
           "SUM(l.propertiesCreated) as totalCreated, " +
           "SUM(l.propertiesUpdated) as totalUpdated " +
           "FROM IdxSyncLog l WHERE l.idxFeed.id = :feedId")
    Object[] getSyncStatisticsForFeed(@Param("feedId") String feedId);

    /**
     * Get overall sync statistics
     */
    @Query("SELECT " +
           "COUNT(l) as totalSyncs, " +
           "SUM(CASE WHEN l.syncStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulSyncs, " +
           "SUM(CASE WHEN l.syncStatus = 'FAILED' THEN 1 ELSE 0 END) as failedSyncs, " +
           "AVG(l.syncDurationMs) as avgDurationMs, " +
           "SUM(l.propertiesProcessed) as totalProcessed " +
           "FROM IdxSyncLog l")
    Object[] getOverallSyncStatistics();

    /**
     * Find longest running syncs
     */
    @Query("SELECT l FROM IdxSyncLog l WHERE l.syncDurationMs IS NOT NULL " +
           "ORDER BY l.syncDurationMs DESC")
    List<IdxSyncLog> findLongestRunningSyncs(Pageable pageable);

    /**
     * Find most productive syncs (highest properties processed)
     */
    @Query("SELECT l FROM IdxSyncLog l WHERE l.propertiesProcessed IS NOT NULL " +
           "ORDER BY l.propertiesProcessed DESC")
    List<IdxSyncLog> findMostProductiveSyncs(Pageable pageable);

    /**
     * Find sync logs with errors
     */
    @Query("SELECT l FROM IdxSyncLog l WHERE l.syncStatus IN ('FAILED', 'TIMEOUT') " +
           "ORDER BY l.createdAt DESC")
    List<IdxSyncLog> findSyncLogsWithErrors();

    /**
     * Get average sync duration for a feed
     */
    @Query("SELECT AVG(l.syncDurationMs) FROM IdxSyncLog l " +
           "WHERE l.idxFeed.id = :feedId AND l.syncStatus = 'SUCCESS'")
    Optional<Double> getAverageSyncDurationForFeed(@Param("feedId") String feedId);

    /**
     * Get sync success rate for a feed
     */
    @Query("SELECT " +
           "(COUNT(CASE WHEN l.syncStatus = 'SUCCESS' THEN 1 END) * 100.0 / COUNT(l)) " +
           "FROM IdxSyncLog l WHERE l.idxFeed.id = :feedId")
    Optional<Double> getSyncSuccessRateForFeed(@Param("feedId") String feedId);

    /**
     * Find latest successful sync for a feed
     */
    @Query("SELECT l FROM IdxSyncLog l WHERE l.idxFeed.id = :feedId AND l.syncStatus = 'SUCCESS' " +
           "ORDER BY l.createdAt DESC")
    Optional<IdxSyncLog> findLatestSuccessfulSyncForFeed(@Param("feedId") String feedId);

    /**
     * Count syncs by status for a feed
     */
    @Query("SELECT COUNT(l) FROM IdxSyncLog l WHERE l.idxFeed.id = :feedId AND l.syncStatus = :status")
    Long countSyncsByStatusForFeed(@Param("feedId") String feedId, @Param("status") SyncStatus status);

    /**
     * Delete old sync logs (cleanup)
     */
    @Query("DELETE FROM IdxSyncLog l WHERE l.createdAt < :cutoffDate")
    void deleteOldSyncLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find top 1 sync log by feed ID ordered by sync start time desc
     */
    List<IdxSyncLog> findTop1ByFeedIdOrderBySyncStartTimeDesc(String feedId);

    /**
     * Count sync logs by sync start time after
     */
    long countBySyncStartTimeAfter(LocalDateTime since);

    /**
     * Get feed-specific sync statistics
     */
    @Query("SELECT COUNT(l), " +
           "SUM(CASE WHEN l.syncStatus = 'SUCCESS' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN l.syncStatus = 'FAILED' THEN 1 ELSE 0 END), " +
           "AVG(l.syncDurationMs), " +
           "SUM(l.propertiesProcessed) " +
           "FROM IdxSyncLog l WHERE l.idxFeed.id = :feedId")
    Object[] getFeedSyncStatistics(@Param("feedId") String feedId);
}
