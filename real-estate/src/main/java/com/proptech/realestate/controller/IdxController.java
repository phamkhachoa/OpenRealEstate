package com.proptech.realestate.controller;

import com.proptech.realestate.dto.idx.IdxFeedResult;
import com.proptech.realestate.model.entity.IdxFeed;
import com.proptech.realestate.model.entity.IdxSyncLog;
import com.proptech.realestate.service.idx.IdxFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for IDX Feed Management
 * Provides endpoints for managing IDX feeds and monitoring sync operations
 * Phase 7 Implementation
 */
@RestController
@RequestMapping("/api/v1/idx")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IDX Management", description = "IDX Feed Management APIs")
public class IdxController {

    private final IdxFeedService idxFeedService;

    /**
     * Get all IDX feeds
     */
    @GetMapping("/feeds")
    @Operation(summary = "Get all IDX feeds", description = "Retrieve all configured IDX feeds")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Page<IdxFeed>> getAllFeeds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "mlsName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<IdxFeed> feeds = idxFeedService.getAllFeeds(pageable);
        return ResponseEntity.ok(feeds);
    }

    /**
     * Get IDX feed by ID
     */
    @GetMapping("/feeds/{feedId}")
    @Operation(summary = "Get IDX feed by ID", description = "Retrieve specific IDX feed configuration")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<IdxFeed> getFeedById(@PathVariable String feedId) {
        IdxFeed feed = idxFeedService.getFeedById(feedId);
        return ResponseEntity.ok(feed);
    }

    /**
     * Create new IDX feed
     */
    @PostMapping("/feeds")
    @Operation(summary = "Create IDX feed", description = "Create a new IDX feed configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IdxFeed> createFeed(@RequestBody IdxFeed feed) {
        IdxFeed createdFeed = idxFeedService.createFeed(feed);
        return ResponseEntity.ok(createdFeed);
    }

    /**
     * Update IDX feed
     */
    @PutMapping("/feeds/{feedId}")
    @Operation(summary = "Update IDX feed", description = "Update existing IDX feed configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IdxFeed> updateFeed(@PathVariable String feedId, @RequestBody IdxFeed feed) {
        IdxFeed updatedFeed = idxFeedService.updateFeed(feedId, feed);
        return ResponseEntity.ok(updatedFeed);
    }

    /**
     * Delete IDX feed
     */
    @DeleteMapping("/feeds/{feedId}")
    @Operation(summary = "Delete IDX feed", description = "Delete IDX feed configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFeed(@PathVariable String feedId) {
        idxFeedService.deleteFeed(feedId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Test IDX feed connection
     */
    @PostMapping("/feeds/{feedId}/test")
    @Operation(summary = "Test IDX feed connection", description = "Test connectivity to IDX feed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> testFeedConnection(@PathVariable String feedId) {
        boolean connectionSuccessful = idxFeedService.testFeedConnection(feedId);
        return ResponseEntity.ok(Map.of(
                "feedId", feedId,
                "connectionSuccessful", connectionSuccessful,
                "testedAt", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Sync specific IDX feed
     */
    @PostMapping("/feeds/{feedId}/sync")
    @Operation(summary = "Sync IDX feed", description = "Trigger manual sync for specific IDX feed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> syncFeed(@PathVariable String feedId) {
        CompletableFuture<IdxSyncLog> syncFuture = idxFeedService.syncFeedAsync(feedId);
        
        return ResponseEntity.ok(Map.of(
                "feedId", feedId,
                "status", "SYNC_STARTED",
                "message", "IDX feed sync started successfully",
                "startedAt", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Sync all active IDX feeds
     */
    @PostMapping("/feeds/sync-all")
    @Operation(summary = "Sync all IDX feeds", description = "Trigger sync for all active IDX feeds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncAllFeeds() {
        idxFeedService.syncAllFeeds();
        
        return ResponseEntity.ok(Map.of(
                "status", "SYNC_ALL_STARTED",
                "message", "Sync started for all active IDX feeds",
                "startedAt", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Get sync status for specific feed
     */
    @GetMapping("/feeds/{feedId}/sync-status")
    @Operation(summary = "Get sync status", description = "Get current sync status for specific feed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> getSyncStatus(@PathVariable String feedId) {
        Map<String, Object> status = idxFeedService.getSyncStatus(feedId);
        return ResponseEntity.ok(status);
    }

    /**
     * Get sync logs for feed
     */
    @GetMapping("/feeds/{feedId}/logs")
    @Operation(summary = "Get sync logs", description = "Get sync logs for specific IDX feed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Page<IdxSyncLog>> getSyncLogs(
            @PathVariable String feedId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "syncStartTime"));
        Page<IdxSyncLog> logs = idxFeedService.getSyncLogsForFeed(feedId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get all sync logs
     */
    @GetMapping("/logs")
    @Operation(summary = "Get all sync logs", description = "Get sync logs for all IDX feeds")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Page<IdxSyncLog>> getAllSyncLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mlsId) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "syncStartTime"));
        Page<IdxSyncLog> logs = idxFeedService.getAllSyncLogs(status, mlsId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get sync statistics for feed
     */
    @GetMapping("/feeds/{feedId}/statistics")
    @Operation(summary = "Get feed statistics", description = "Get sync statistics for specific IDX feed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> getFeedStatistics(@PathVariable String feedId) {
        Map<String, Object> statistics = idxFeedService.getFeedStatistics(feedId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get overall sync statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get overall statistics", description = "Get overall IDX sync statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> getOverallStatistics() {
        Map<String, Object> statistics = idxFeedService.getOverallSyncStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Cancel running sync
     */
    @PostMapping("/feeds/{feedId}/cancel-sync")
    @Operation(summary = "Cancel sync", description = "Cancel currently running sync for specific feed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelSync(@PathVariable String feedId) {
        boolean cancelled = idxFeedService.cancelRunningSync(feedId);
        
        return ResponseEntity.ok(Map.of(
                "feedId", feedId,
                "cancelled", cancelled,
                "message", cancelled ? "Sync cancelled successfully" : "No running sync found",
                "cancelledAt", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Enable/disable IDX feed
     */
    @PatchMapping("/feeds/{feedId}/status")
    @Operation(summary = "Update feed status", description = "Enable or disable IDX feed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IdxFeed> updateFeedStatus(
            @PathVariable String feedId,
            @RequestParam boolean active) {
        
        IdxFeed updatedFeed = idxFeedService.updateFeedStatus(feedId, active);
        return ResponseEntity.ok(updatedFeed);
    }

    /**
     * Get supported feed types
     */
    @GetMapping("/feed-types")
    @Operation(summary = "Get supported feed types", description = "Get list of supported IDX feed types")
    public ResponseEntity<List<Map<String, Object>>> getSupportedFeedTypes() {
        List<Map<String, Object>> feedTypes = idxFeedService.getSupportedFeedTypes();
        return ResponseEntity.ok(feedTypes);
    }

    /**
     * Health check for IDX system
     */
    @GetMapping("/health")
    @Operation(summary = "IDX system health check", description = "Check health status of IDX system")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = idxFeedService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get MLS coverage information
     */
    @GetMapping("/coverage")
    @Operation(summary = "Get MLS coverage", description = "Get information about MLS coverage and feeds")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BROKER')")
    public ResponseEntity<Map<String, Object>> getMlsCoverage() {
        Map<String, Object> coverage = idxFeedService.getMlsCoverage();
        return ResponseEntity.ok(coverage);
    }
}
