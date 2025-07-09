package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * IDX Sync Log entity for tracking synchronization history
 * Phase 7 Implementation
 */
@Entity
@Table(name = "idx_sync_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"idxFeed"})
public class IdxSyncLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idx_feed_id", nullable = false)
    private IdxFeed idxFeed;

    @Column(name = "sync_start_time", nullable = false)
    private LocalDateTime syncStartTime;

    @Column(name = "sync_end_time")
    private LocalDateTime syncEndTime;

    @Column(name = "properties_processed")
    private Integer propertiesProcessed = 0;

    @Column(name = "properties_created")
    private Integer propertiesCreated = 0;

    @Column(name = "properties_updated")
    private Integer propertiesUpdated = 0;

    @Column(name = "properties_deleted")
    private Integer propertiesDeleted = 0;

    @Column(name = "properties_skipped")
    private Integer propertiesSkipped = 0;

    @Column(name = "properties_duplicated")
    private Integer propertiesDuplicated = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private SyncStatus syncStatus;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "sync_duration_ms")
    private Long syncDurationMs;

    @Column(name = "records_per_second")
    private Double recordsPerSecond;

    @Column(name = "memory_used_mb")
    private Double memoryUsedMb;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sync_statistics", columnDefinition = "jsonb")
    private Map<String, Object> syncStatistics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private Map<String, Object> errorDetails;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (syncStartTime == null) {
            syncStartTime = LocalDateTime.now();
        }
    }

    /**
     * Calculate sync duration in milliseconds
     */
    public Long calculateDuration() {
        if (syncStartTime != null && syncEndTime != null) {
            return java.time.Duration.between(syncStartTime, syncEndTime).toMillis();
        }
        return null;
    }

    /**
     * Calculate processing rate (records per second)
     */
    public Double calculateProcessingRate() {
        if (propertiesProcessed != null && propertiesProcessed > 0 && syncDurationMs != null && syncDurationMs > 0) {
            return (propertiesProcessed * 1000.0) / syncDurationMs;
        }
        return null;
    }

    /**
     * Mark sync as completed
     */
    public void markCompleted(SyncStatus finalStatus) {
        this.syncEndTime = LocalDateTime.now();
        this.syncStatus = finalStatus;
        this.syncDurationMs = calculateDuration();
        this.recordsPerSecond = calculateProcessingRate();
    }

    /**
     * Get total properties affected (created + updated + deleted)
     */
    public Integer getTotalPropertiesAffected() {
        int created = propertiesCreated != null ? propertiesCreated : 0;
        int updated = propertiesUpdated != null ? propertiesUpdated : 0;
        int deleted = propertiesDeleted != null ? propertiesDeleted : 0;
        return created + updated + deleted;
    }

    /**
     * Check if sync was successful
     */
    public boolean isSuccessful() {
        return syncStatus != null && syncStatus.isSuccessful();
    }

    /**
     * Get sync success rate as percentage
     */
    public Double getSuccessRate() {
        if (propertiesProcessed == null || propertiesProcessed == 0) {
            return 0.0;
        }
        
        int successful = getTotalPropertiesAffected();
        return (successful * 100.0) / propertiesProcessed;
    }
}
