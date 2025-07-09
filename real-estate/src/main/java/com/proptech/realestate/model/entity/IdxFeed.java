package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IDX Feed entity for managing Multiple Listing Service data feeds
 * Phase 7 Implementation: IDX Feeds & Data Aggregation
 */
@Entity
@Table(name = "idx_feeds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"syncLogs"})
public class IdxFeed {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "mls_id", unique = true, nullable = false, length = 50)
    private String mlsId;

    @Column(name = "mls_name", nullable = false, length = 200)
    private String mlsName;

    @Column(name = "feed_url", nullable = false, length = 500)
    private String feedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false)
    private IdxFeedType feedType;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false)
    private AuthenticationType authType;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "client_secret", length = 500)
    private String clientSecret;

    @Column(name = "last_sync_timestamp")
    private LocalDateTime lastSyncTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status")
    private SyncStatus lastSyncStatus;

    @Column(name = "total_properties_synced")
    private Integer totalPropertiesSynced = 0;

    @Column(name = "sync_frequency_minutes")
    private Integer syncFrequencyMinutes = 60; // Default 1 hour

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 300; // 5 minutes

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feed_configuration", columnDefinition = "jsonb")
    private Map<String, Object> feedConfiguration;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "idxFeed", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<IdxSyncLog> syncLogs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this feed should be synced based on frequency and last sync time
     */
    public boolean shouldSync() {
        if (!isActive || lastSyncTimestamp == null) {
            return isActive; // Sync if active and never synced before
        }

        LocalDateTime nextSyncTime = lastSyncTimestamp.plusMinutes(syncFrequencyMinutes);
        return LocalDateTime.now().isAfter(nextSyncTime);
    }

    /**
     * Update sync status and timestamp
     */
    public void updateSyncStatus(SyncStatus status, String errorMessage) {
        this.lastSyncStatus = status;
        this.lastSyncTimestamp = LocalDateTime.now();
        this.errorMessage = errorMessage;
        
        if (status == SyncStatus.SUCCESS) {
            this.retryCount = 0;
        } else if (status == SyncStatus.FAILED) {
            this.retryCount = (this.retryCount != null) ? this.retryCount + 1 : 1;
        }
    }

    /**
     * Check if feed has exceeded max retries
     */
    public boolean hasExceededMaxRetries() {
        return retryCount != null && retryCount >= maxRetries;
    }
}
