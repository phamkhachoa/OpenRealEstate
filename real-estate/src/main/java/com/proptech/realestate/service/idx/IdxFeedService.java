package com.proptech.realestate.service.idx;

import com.proptech.realestate.dto.idx.IdxFeedResult;
import com.proptech.realestate.model.entity.*;
import com.proptech.realestate.repository.IdxFeedRepository;
import com.proptech.realestate.repository.IdxSyncLogRepository;
import com.proptech.realestate.service.PropertyService;
import com.proptech.realestate.service.search.PropertySearchIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main IDX Feed Service for managing all feed operations
 * Phase 7 Implementation
 */
@Service
@Transactional
@Slf4j
public class IdxFeedService {

    @Autowired
    private IdxFeedRepository idxFeedRepository;

    @Autowired
    private IdxSyncLogRepository syncLogRepository;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertySearchIndexService searchIndexService;

    @Autowired
    private List<IdxFeedProcessor> feedProcessors;

    @Autowired
    private IdxDataAggregationService aggregationService;

    // Track running sync jobs
    private final Map<String, CompletableFuture<IdxSyncLog>> runningSyncs = new ConcurrentHashMap<>();

    /**
     * Sync all active feeds
     */
    public void syncAllFeeds() {
        List<IdxFeed> activeFeeds = idxFeedRepository.findByIsActiveTrue();
        log.info("Starting sync for {} active IDX feeds", activeFeeds.size());

        for (IdxFeed feed : activeFeeds) {
            if (feed.shouldSync() && !isSyncRunning(feed.getId())) {
                syncFeedAsync(feed.getId());
            }
        }
    }

    /**
     * Sync a specific feed asynchronously
     */
    @Async("idxSyncExecutor")
    public CompletableFuture<IdxSyncLog> syncFeedAsync(String feedId) {
        CompletableFuture<IdxSyncLog> syncFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return syncFeed(feedId);
            } catch (Exception e) {
                log.error("Async sync failed for feed: {}", feedId, e);
                throw new RuntimeException(e);
            }
        });

        runningSyncs.put(feedId, syncFuture);
        
        // Remove from running syncs when completed
        syncFuture.whenComplete((result, throwable) -> {
            runningSyncs.remove(feedId);
        });

        return syncFuture;
    }

    /**
     * Sync a specific feed synchronously
     */
    public IdxSyncLog syncFeed(String feedId) {
        IdxFeed feed = idxFeedRepository.findById(feedId)
            .orElseThrow(() -> new RuntimeException("IDX Feed not found: " + feedId));

        return syncFeed(feed);
    }

    /**
     * Sync a specific feed
     */
    public IdxSyncLog syncFeed(IdxFeed feed) {
        log.info("Starting IDX sync for feed: {} ({})", feed.getMlsName(), feed.getMlsId());

        // Create sync log
        IdxSyncLog syncLog = createSyncLog(feed);

        try {
            // Update feed status to in progress
            feed.updateSyncStatus(SyncStatus.IN_PROGRESS, null);
            idxFeedRepository.save(feed);

            // Get appropriate processor
            IdxFeedProcessor processor = getFeedProcessor(feed.getFeedType());
            if (processor == null) {
                throw new RuntimeException("No processor found for feed type: " + feed.getFeedType());
            }

            // Validate feed before processing
            if (!processor.validateFeedConfiguration(feed)) {
                throw new RuntimeException("Feed configuration validation failed");
            }

            // Process feed
            long startTime = System.currentTimeMillis();
            IdxFeedResult result = processor.processFeed(feed);
            long endTime = System.currentTimeMillis();

            // Update sync log with results
            updateSyncLog(syncLog, result, startTime, endTime);

            // Post-process results
            postProcessResults(feed, result);

            // Update feed with success status
            feed.updateSyncStatus(SyncStatus.SUCCESS, null);
            feed.setTotalPropertiesSynced(result.getTotalProcessed());
            idxFeedRepository.save(feed);

            log.info("IDX sync completed successfully for feed: {} - {} properties processed", 
                feed.getMlsName(), result.getTotalProcessed());

            return syncLog;

        } catch (Exception e) {
            log.error("IDX sync failed for feed: {}", feed.getMlsName(), e);
            
            // Update sync log with error
            syncLog.setSyncStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.markCompleted(SyncStatus.FAILED);
            syncLogRepository.save(syncLog);

            // Update feed with error status
            feed.updateSyncStatus(SyncStatus.FAILED, e.getMessage());
            idxFeedRepository.save(feed);

            throw new RuntimeException("Sync failed for feed: " + feed.getMlsName(), e);
        }
    }

    /**
     * Scheduled sync - runs every 5 minutes to check for feeds due for sync
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scheduledSync() {
        try {
            log.debug("Running scheduled IDX feed sync check");
            
            LocalDateTime cutoffTime = LocalDateTime.now();
            List<IdxFeed> feedsDueForSync = idxFeedRepository.findFeedsDueForSync(cutoffTime);
            
            log.info("Found {} feeds due for sync", feedsDueForSync.size());
            
            for (IdxFeed feed : feedsDueForSync) {
                if (!isSyncRunning(feed.getId()) && !feed.hasExceededMaxRetries()) {
                    log.info("Scheduling sync for feed: {}", feed.getMlsName());
                    syncFeedAsync(feed.getId());
                }
            }

            // Also retry failed feeds that are within retry limits
            retryFailedFeeds();

        } catch (Exception e) {
            log.error("Error in scheduled sync", e);
        }
    }

    /**
     * Retry feeds with retriable errors
     */
    public void retryFailedFeeds() {
        List<IdxFeed> retriableFeeds = idxFeedRepository.findFeedsWithRetriableErrors();
        
        for (IdxFeed feed : retriableFeeds) {
            if (!isSyncRunning(feed.getId())) {
                log.info("Retrying failed feed: {} (attempt {}/{})", 
                    feed.getMlsName(), feed.getRetryCount() + 1, feed.getMaxRetries());
                syncFeedAsync(feed.getId());
            }
        }
    }

    /**
     * Check if sync is currently running for a feed
     */
    public boolean isSyncRunning(String feedId) {
        CompletableFuture<IdxSyncLog> runningSync = runningSyncs.get(feedId);
        return runningSync != null && !runningSync.isDone();
    }

    /**
     * Cancel running sync for a feed
     */
    public boolean cancelSync(String feedId) {
        CompletableFuture<IdxSyncLog> runningSync = runningSyncs.get(feedId);
        if (runningSync != null && !runningSync.isDone()) {
            boolean cancelled = runningSync.cancel(true);
            if (cancelled) {
                runningSyncs.remove(feedId);
                
                // Update feed status
                IdxFeed feed = idxFeedRepository.findById(feedId).orElse(null);
                if (feed != null) {
                    feed.updateSyncStatus(SyncStatus.CANCELLED, "Sync cancelled by user");
                    idxFeedRepository.save(feed);
                }
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Get feed processor for feed type
     */
    private IdxFeedProcessor getFeedProcessor(IdxFeedType feedType) {
        return feedProcessors.stream()
            .filter(processor -> processor.supports(feedType.name()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Create sync log entry
     */
    private IdxSyncLog createSyncLog(IdxFeed feed) {
        IdxSyncLog syncLog = IdxSyncLog.builder()
            .idxFeed(feed)
            .syncStartTime(LocalDateTime.now())
            .syncStatus(SyncStatus.IN_PROGRESS)
            .build();

        return syncLogRepository.save(syncLog);
    }

    /**
     * Update sync log with results
     */
    private void updateSyncLog(IdxSyncLog syncLog, IdxFeedResult result, long startTime, long endTime) {
        syncLog.setPropertiesProcessed(result.getTotalProcessed());
        syncLog.setPropertiesCreated(result.getTotalCreated());
        syncLog.setPropertiesUpdated(result.getTotalUpdated());
        syncLog.setPropertiesDeleted(result.getTotalDeleted());
        syncLog.setPropertiesSkipped(result.getTotalSkipped());
        syncLog.setPropertiesDuplicated(result.getTotalDuplicated());
        
        syncLog.setSyncDurationMs(endTime - startTime);
        syncLog.setRecordsPerSecond(result.getRecordsPerSecond());
        
        if (result.isSuccessful()) {
            syncLog.setSyncStatus(result.hasWarnings() ? SyncStatus.PARTIAL_SUCCESS : SyncStatus.SUCCESS);
        } else {
            syncLog.setSyncStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(String.join("; ", result.getErrors()));
        }

        syncLog.markCompleted(syncLog.getSyncStatus());
        syncLogRepository.save(syncLog);
    }

    /**
     * Post-process sync results
     */
    private void postProcessResults(IdxFeed feed, IdxFeedResult result) {
        if (result.getProperties() != null && !result.getProperties().isEmpty()) {
            
            // 1. Run data aggregation and deduplication
            aggregationService.processNewProperties(result.getProperties(), feed.getMlsId());
            
            // 2. Update search index
            try {
                searchIndexService.indexProperties(result.getProperties());
                log.info("Updated search index with {} properties from feed: {}", 
                    result.getProperties().size(), feed.getMlsName());
            } catch (Exception e) {
                log.error("Failed to update search index for feed: {}", feed.getMlsName(), e);
                // Don't fail the sync for search index errors
            }
        }
    }

    /**
     * Get all feeds with pagination
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<IdxFeed> getAllFeeds(org.springframework.data.domain.Pageable pageable) {
        return idxFeedRepository.findAll(pageable);
    }

    /**
     * Get feed by ID
     */
    @Transactional(readOnly = true)
    public IdxFeed getFeedById(String feedId) {
        return idxFeedRepository.findById(feedId)
                .orElseThrow(() -> new RuntimeException("IDX Feed not found: " + feedId));
    }

    /**
     * Create new feed
     */
    public IdxFeed createFeed(IdxFeed feed) {
        feed.setCreatedAt(LocalDateTime.now());
        feed.setUpdatedAt(LocalDateTime.now());
        return idxFeedRepository.save(feed);
    }

    /**
     * Update existing feed
     */
    public IdxFeed updateFeed(String feedId, IdxFeed feedUpdate) {
        IdxFeed existingFeed = getFeedById(feedId);
        
        // Update fields
        existingFeed.setMlsName(feedUpdate.getMlsName());
        existingFeed.setMlsId(feedUpdate.getMlsId());
        existingFeed.setFeedUrl(feedUpdate.getFeedUrl());
        existingFeed.setFeedType(feedUpdate.getFeedType());
        existingFeed.setAuthenticationType(feedUpdate.getAuthenticationType());
        existingFeed.setUsername(feedUpdate.getUsername());
        existingFeed.setPassword(feedUpdate.getPassword());
        existingFeed.setFeedConfiguration(feedUpdate.getFeedConfiguration());
        existingFeed.setSyncFrequencyMinutes(feedUpdate.getSyncFrequencyMinutes());
        existingFeed.setIsActive(feedUpdate.getIsActive());
        existingFeed.setUpdatedAt(LocalDateTime.now());
        
        return idxFeedRepository.save(existingFeed);
    }

    /**
     * Delete feed
     */
    public void deleteFeed(String feedId) {
        IdxFeed feed = getFeedById(feedId);
        
        // Cancel any running sync
        cancelRunningSync(feedId);
        
        // Delete the feed
        idxFeedRepository.delete(feed);
        log.info("Deleted IDX feed: {}", feed.getMlsName());
    }

    /**
     * Test feed connection
     */
    public boolean testFeedConnection(String feedId) {
        IdxFeed feed = getFeedById(feedId);
        
        IdxFeedProcessor processor = getProcessorForFeed(feed);
        if (processor != null) {
            return processor.testConnection(feed);
        }
        
        return false;
    }

    /**
     * Get sync status for feed
     */
    public Map<String, Object> getSyncStatus(String feedId) {
        IdxFeed feed = getFeedById(feedId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("feedId", feedId);
        status.put("feedName", feed.getMlsName());
        status.put("isActive", feed.getIsActive());
        status.put("lastSyncTime", feed.getLastSyncTimestamp());
        
        // Check if sync is currently running
        boolean isRunning = runningSyncs.containsKey(feedId);
        status.put("isRunning", isRunning);
        
        if (isRunning) {
            status.put("status", "RUNNING");
        } else {
            status.put("status", "IDLE");
        }
        
        // Get latest sync log
        List<IdxSyncLog> recentLogs = syncLogRepository.findTop1ByFeedIdOrderBySyncStartTimeDesc(feedId);
        if (!recentLogs.isEmpty()) {
            IdxSyncLog latestLog = recentLogs.get(0);
            status.put("lastSyncStatus", latestLog.getSyncStatus());
            status.put("lastSyncDuration", latestLog.getSyncDurationMs());
            status.put("lastSyncProcessed", latestLog.getPropertiesProcessed());
        }
        
        return status;
    }

    /**
     * Get sync logs for feed
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<IdxSyncLog> getSyncLogsForFeed(String feedId, org.springframework.data.domain.Pageable pageable) {
        return syncLogRepository.findByFeedId(feedId, pageable);
    }

    /**
     * Get all sync logs with filtering
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<IdxSyncLog> getAllSyncLogs(String status, String mlsId, org.springframework.data.domain.Pageable pageable) {
        if (status != null && mlsId != null) {
            return syncLogRepository.findByFeedMlsIdAndSyncStatus(mlsId, SyncStatus.valueOf(status), pageable);
        } else if (status != null) {
            return syncLogRepository.findBySyncStatus(SyncStatus.valueOf(status), pageable);
        } else if (mlsId != null) {
            return syncLogRepository.findByFeedMlsId(mlsId, pageable);
        } else {
            return syncLogRepository.findAll(pageable);
        }
    }

    /**
     * Get feed statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFeedStatistics(String feedId) {
        IdxFeed feed = getFeedById(feedId);
        
        // Get sync statistics for this feed
        Object[] stats = syncLogRepository.getFeedSyncStatistics(feedId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("feedId", feedId);
        statistics.put("feedName", feed.getMlsName());
        statistics.put("mlsId", feed.getMlsId());
        
        if (stats != null && stats.length > 0) {
            statistics.put("totalSyncs", stats[0]);
            statistics.put("successfulSyncs", stats[1]);
            statistics.put("failedSyncs", stats[2]);
            statistics.put("avgDurationMs", stats[3]);
            statistics.put("totalProcessed", stats[4]);
        } else {
            statistics.put("totalSyncs", 0);
            statistics.put("successfulSyncs", 0);
            statistics.put("failedSyncs", 0);
            statistics.put("avgDurationMs", 0);
            statistics.put("totalProcessed", 0);
        }
        
        // Get property count from this MLS
        long propertyCount = propertyService.countByMlsId(feed.getMlsId());
        statistics.put("currentPropertyCount", propertyCount);
        
        return statistics;
    }

    /**
     * Cancel running sync
     */
    public boolean cancelRunningSync(String feedId) {
        CompletableFuture<IdxSyncLog> runningSync = runningSyncs.get(feedId);
        if (runningSync != null) {
            boolean cancelled = runningSync.cancel(true);
            if (cancelled) {
                runningSyncs.remove(feedId);
                log.info("Cancelled running sync for feed: {}", feedId);
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Update feed status (enable/disable)
     */
    public IdxFeed updateFeedStatus(String feedId, boolean active) {
        IdxFeed feed = getFeedById(feedId);
        feed.setIsActive(active);
        feed.setUpdatedAt(LocalDateTime.now());
        
        IdxFeed updatedFeed = idxFeedRepository.save(feed);
        log.info("Updated feed status for {}: {}", feed.getMlsName(), active ? "ENABLED" : "DISABLED");
        
        return updatedFeed;
    }

    /**
     * Get supported feed types
     */
    public List<Map<String, Object>> getSupportedFeedTypes() {
        List<Map<String, Object>> feedTypes = new ArrayList<>();
        
        for (IdxFeedType feedType : IdxFeedType.values()) {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("type", feedType.name());
            typeInfo.put("displayName", getFeedTypeDisplayName(feedType));
            typeInfo.put("description", getFeedTypeDescription(feedType));
            typeInfo.put("supported", isFeedTypeSupported(feedType));
            feedTypes.add(typeInfo);
        }
        
        return feedTypes;
    }

    /**
     * Get system health
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            long totalFeeds = idxFeedRepository.count();
            long activeFeeds = idxFeedRepository.countByIsActiveTrue();
            
            health.put("status", "HEALTHY");
            health.put("totalFeeds", totalFeeds);
            health.put("activeFeeds", activeFeeds);
            health.put("runningJobs", runningSyncs.size());
            health.put("checkTime", LocalDateTime.now());
            
            // Check recent sync activity
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            long recentSyncs = syncLogRepository.countBySyncStartTimeAfter(since);
            health.put("recentSyncs24h", recentSyncs);
            
        } catch (Exception e) {
            health.put("status", "UNHEALTHY");
            health.put("error", e.getMessage());
            health.put("checkTime", LocalDateTime.now());
        }
        
        return health;
    }

    /**
     * Get MLS coverage information
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMlsCoverage() {
        Map<String, Object> coverage = new HashMap<>();
        
        List<IdxFeed> allFeeds = idxFeedRepository.findAll();
        List<IdxFeed> activeFeeds = idxFeedRepository.findByIsActiveTrue();
        
        coverage.put("totalFeeds", allFeeds.size());
        coverage.put("activeFeeds", activeFeeds.size());
        
        // Group by MLS
        Map<String, List<IdxFeed>> feedsByMls = allFeeds.stream()
                .collect(java.util.stream.Collectors.groupingBy(IdxFeed::getMlsId));
        
        coverage.put("totalMlsProviders", feedsByMls.size());
        
        // Get coverage details
        List<Map<String, Object>> mlsDetails = new ArrayList<>();
        for (Map.Entry<String, List<IdxFeed>> entry : feedsByMls.entrySet()) {
            String mlsId = entry.getKey();
            List<IdxFeed> mlsFeeds = entry.getValue();
            
            Map<String, Object> mlsInfo = new HashMap<>();
            mlsInfo.put("mlsId", mlsId);
            mlsInfo.put("feedCount", mlsFeeds.size());
            mlsInfo.put("activeFeedCount", mlsFeeds.stream().mapToInt(f -> f.getIsActive() ? 1 : 0).sum());
            mlsInfo.put("propertyCount", propertyService.countByMlsId(mlsId));
            
            // Get latest sync info
            mlsFeeds.stream()
                    .filter(f -> f.getLastSyncTimestamp() != null)
                    .max(java.util.Comparator.comparing(IdxFeed::getLastSyncTimestamp))
                    .ifPresent(latestFeed -> mlsInfo.put("lastSyncTime", latestFeed.getLastSyncTimestamp()));
            
            mlsDetails.add(mlsInfo);
        }
        
        coverage.put("mlsDetails", mlsDetails);
        
        return coverage;
    }

    // Helper methods
    private String getFeedTypeDisplayName(IdxFeedType feedType) {
        return switch (feedType) {
            case RESO_WEB_API -> "RESO Web API";
            case RETS -> "RETS";
            case CSV -> "CSV File";
            case XML -> "XML File";
            case FTP -> "FTP";
            case SFTP -> "SFTP";
        };
    }

    private String getFeedTypeDescription(IdxFeedType feedType) {
        return switch (feedType) {
            case RESO_WEB_API -> "RESO-compliant Web API using OData standard";
            case RETS -> "Real Estate Transaction Standard (RETS) feed";
            case CSV -> "Comma-separated values file";
            case XML -> "XML file format";
            case FTP -> "File Transfer Protocol";
            case SFTP -> "Secure File Transfer Protocol";
        };
    }

    private boolean isFeedTypeSupported(IdxFeedType feedType) {
        // Check if we have a processor for this feed type
        return feedProcessors.stream()
                .anyMatch(processor -> processor.supports(feedType));
    }
}
