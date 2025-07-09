package com.proptech.realestate.repository;

import com.proptech.realestate.model.entity.IdxFeed;
import com.proptech.realestate.model.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for IDX Feed management
 * Phase 7 Implementation
 */
@Repository
public interface IdxFeedRepository extends JpaRepository<IdxFeed, String> {

    /**
     * Find all active IDX feeds
     */
    List<IdxFeed> findByIsActiveTrue();

    /**
     * Find IDX feed by MLS ID
     */
    Optional<IdxFeed> findByMlsId(String mlsId);

    /**
     * Find IDX feeds by sync status
     */
    List<IdxFeed> findByLastSyncStatus(SyncStatus syncStatus);

    /**
     * Find IDX feeds that should be synced
     */
    @Query("SELECT f FROM IdxFeed f WHERE f.isActive = true AND " +
           "(f.lastSyncTimestamp IS NULL OR " +
           "f.lastSyncTimestamp < :cutoffTime)")
    List<IdxFeed> findFeedsDueForSync(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find IDX feeds with sync errors
     */
    @Query("SELECT f FROM IdxFeed f WHERE f.isActive = true AND " +
           "f.lastSyncStatus = 'FAILED' AND " +
           "f.retryCount < f.maxRetries")
    List<IdxFeed> findFeedsWithRetriableErrors();

    /**
     * Find IDX feeds by feed type
     */
    @Query("SELECT f FROM IdxFeed f WHERE f.feedType = :feedType AND f.isActive = true")
    List<IdxFeed> findByFeedTypeAndActive(@Param("feedType") String feedType);

    /**
     * Count active feeds
     */
    @Query("SELECT COUNT(f) FROM IdxFeed f WHERE f.isActive = true")
    Long countActiveFeeds();

    /**
     * Count active feeds
     */
    long countByIsActiveTrue();

    /**
     * Get feeds with recent sync activity
     */
    @Query("SELECT f FROM IdxFeed f WHERE f.lastSyncTimestamp >= :since")
    List<IdxFeed> findFeedsWithRecentActivity(@Param("since") LocalDateTime since);

    /**
     * Find feeds that need attention (failed multiple times)
     */
    @Query("SELECT f FROM IdxFeed f WHERE f.isActive = true AND " +
           "f.retryCount >= f.maxRetries")
    List<IdxFeed> findFeedsNeedingAttention();

    /**
     * Get feed statistics
     */
    @Query("SELECT " +
           "COUNT(f) as totalFeeds, " +
           "SUM(CASE WHEN f.isActive = true THEN 1 ELSE 0 END) as activeFeeds, " +
           "SUM(CASE WHEN f.lastSyncStatus = 'SUCCESS' THEN 1 ELSE 0 END) as successfulFeeds, " +
           "SUM(CASE WHEN f.lastSyncStatus = 'FAILED' THEN 1 ELSE 0 END) as failedFeeds " +
           "FROM IdxFeed f")
    Object[] getFeedStatistics();

    /**
     * Find feeds by MLS name pattern
     */
    @Query("SELECT f FROM IdxFeed f WHERE LOWER(f.mlsName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<IdxFeed> findByMlsNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find recently updated feeds
     */
    List<IdxFeed> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime since);

    /**
     * Check if MLS ID exists
     */
    boolean existsByMlsId(String mlsId);
}
